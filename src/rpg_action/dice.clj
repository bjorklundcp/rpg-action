(ns rpg-action.dice
  (:require [clojure.string :as string]
            [rpg-action.utils :as utils]))

(defn basic-roll
  "Executes a dice roll. The kind of dice being rolled is the max"
  [max]
  (+ (rand-int max) 1))

(defn explode-roll
  "Executes a dice roll that will be re-rolled when the die max is rolled"
  [max]
  (loop [last-roll (basic-roll max)
         rolls [last-roll]]
    (if (= last-roll max)
      (let [new-roll (basic-roll max)]
        (recur new-roll (conj rolls new-roll)))
      rolls)))

(defn roll
  "Processes a roll with options.
  Max is required. It is the type of dice being rolled.
  The explode? option will re-roll dice that = max.
  Returns a list of rolls."
  ([max]
   (basic-roll max))
  ([max explode?]
   (if explode?
     (explode-roll max)
     [(basic-roll max)])))

(defn process-roll
  "Processes a roll string into actual rolls and data.
  Returns an object of the following form:
  {:roll    the dice that was rolled (2d6, d8, etc)
   :display the string that was passed in (2d6!, d8, etc)
   :result  a list of the roll(s) that happened
   :total   the total of all the rolls}"
  [roll-command]
  (let [de-structured-roll-command (re-find utils/roll-regex roll-command)
        number-of-dice (if (some? (nth de-structured-roll-command 1))
                         (Integer. (nth de-structured-roll-command 1))
                         1)
        dice (Integer. (re-find #"\d+" (nth de-structured-roll-command 2)))
        explode? (some? (nth de-structured-roll-command 3))
        rolls (into [] (flatten (repeatedly number-of-dice #(roll dice explode?))))
        total (reduce + rolls)]
    {:roll    (str number-of-dice "d" dice)
     :display (str number-of-dice "d" dice (if explode? "!" ""))
     :result  rolls
     :total   total}))

(defn process-modifier
  "Processes a modifier for the roll. If no + or - is on the number, + is assumed.
  Returns an object of the following form:
  {:modifer + or -
   :value   the integer value
   :display the string with the modifier and the number (+2, -4, etc)
   :total   the integer total of the modifier}"
  [modifier-command]
  (let [de-structured-modifier-command (re-find utils/modifier-and-number-regex modifier-command)
        modifier (or (nth de-structured-modifier-command 1) "+")
        modifier-value (Integer. (re-find #"\d+" (nth de-structured-modifier-command 2)))]
    {:modifier modifier
     :value    modifier-value
     :display  (str modifier " " modifier-value)
     :total    (if (= "-" modifier)
                 (* -1 modifier-value)
                 modifier-value)}))

(defn bold-numbers-in-string
  "Given a string, will surround each integer with *
  '1 + 2 + 3' --> '*1* + *2* + *3*'"
  [input]
  (string/replace input #"\d+" #(str "*" %1 "*")))

(defn generate-message-pretext
  "Given roll-data, this fn will produce a string that is the first line in a slack message
  (roll data) ^ (roll data) ^ ... (total modifiers)
  {:roll-tree [[{:roll '1d6',
                 :display '1d6',
                 :result [5],
                 :total 5}
                {:modifier '+',
                 :display '+ 2',
                 :total 2}]
               [{:roll '1d8',
                 :display '1d8',
                 :result [2],
                 :total 2}]],
   :total-modifiers [{:modifier '+',
                      :display '+ 2',
                      :total 2}],
   :total 9} --> '5 + 2 ^ 2 + 2 = 9'"
  [roll-data]
  (let [formatted-dice-rolls (loop [final-string ""
                                    roll-data (:roll-tree roll-data)]
                               (if roll-data
                                 (let [roll-string (map-indexed (fn [i data]
                                                                  (if (zero? i)
                                                                    (str (:total data))
                                                                    (if (:modifier data)
                                                                      (str " " (:modifier data) " " (:value data))
                                                                      (str " + " (:total data)))))
                                                                (first roll-data))]
                                   (if (next roll-data)
                                     (recur (str final-string (string/join roll-string) " ^ ") (next roll-data))
                                     (recur (str final-string (string/join roll-string)) (next roll-data))))
                                 final-string))
        formatted-modifiers (->> (:total-modifiers roll-data)
                                 (map #(str (:modifier %) " " (:value %)))
                                 string/join)]
    (if (and (not (re-find #"([\+\^-])" formatted-dice-rolls)) (= "" formatted-modifiers))
      (str "You rolled "
           formatted-dice-rolls)
      (str "You rolled "
           formatted-dice-rolls
           (when-not (= "" formatted-modifiers) (str " " formatted-modifiers))
           " = "
           (:total roll-data)))))

(defn generate-message-body
  "Given a roll tree, this will produce a list of fields that can be used within
  slack attachments on a message. Each dice roll will result in two fields, a dice one,
  and a roll results one.
  [[{:roll '2d6',
     :display '2d6',
     :result [6 6],
     :total 12}
    {:roll '3d8',
     :display '3d8',
     :result [6 7 7],
     :total 20}]] --> [{:title 'Dice'
                        :value '2d6'
                        :short true}
                       {:title 'Rolls'
                        :value '6 6'
                        :short true}
                       {:title 'Dice'
                        :value '3d8'
                        :short true}
                       {:title 'Rolls'
                        :value '6 6 7'
                        :short true}]"
  [roll-tree]
  (let [dice-rolls (filterv #(:result %) (flatten roll-tree))]
    (into []
          (mapcat (fn [roll]
                    [{:title "Dice"
                      :value (:display roll)
                      :short true}
                     {:title "Rolls"
                      :value (string/join " " (:result roll))
                      :short true}])
                  dice-rolls))))

(defn format-slack-message
  "Given rolls and data, creates a slack message to be returned to the client"
  [roll-data]
  (if (and (empty? (first (:roll-tree roll-data))) (empty? (:total-modifiers roll-data)) (zero? (:total roll-data)))
    "Could not find anything parsable. It's probably my fault. :disappointed:"
    {:response_type "in_channel"
     :attachments [{:color  (if (>= (:total roll-data) 4) "good" "danger")
                    :text   (->> (generate-message-pretext roll-data)
                                 bold-numbers-in-string)
                    :fields (generate-message-body (:roll-tree roll-data))}]}))

(defn generate-roll-data
  "Consumes a list of roll commands, parses each command and processes them into rolls/data.
   unknown artifacts are ignored and passed over
   Example: ['2d6' '+2' '^' 'd8!' '-2'] --> {:roll-tree [[{:roll    '2d6'
                                                           :display '2d6'
                                                           :result  [4 6]
                                                           :total   10}
                                                          {:modifier '+'
                                                           :display  '+2'
                                                           :total    2}]
                                                         [{:roll    '1d8'
                                                           :display '1d8!'
                                                           :result  [8 1]
                                                           :total   9}]]
                                             :total-modifiers [{:modifier '-'
                                                                :display  '-2'
                                                                :total    2-}]
                                             :total 10}
   roll-tree is a list of lists. Each list is rolls and modifiers that need to be compared to each other
   total-modifiers are any +/-N that are at the end of the expression. These are not a part of any comparisons in the roll-tree
   but rather applied to the final total
   total is the final total of the roll expression. It is max(sum(totals in roll-tree)) + sum(totals in total-modifiers)
   In the above example total is 12 ^ 9 - 2 = 10"
  [tokenized-roll-command]
  (let [roll-data (loop [roll-tree [[]]
                         depth 0
                         roll-command tokenized-roll-command]
                    (if roll-command
                      (cond
                        (re-matches utils/roll-regex (first roll-command))
                        (recur
                          (update-in roll-tree [depth] conj (process-roll (first roll-command)))
                          depth
                          (next roll-command))
                        (re-matches utils/modifier-and-number-regex (first roll-command))
                        (recur
                          (update-in roll-tree [depth] conj (process-modifier (first roll-command)))
                          depth
                          (next roll-command))
                        (re-matches utils/caret-regex (first roll-command))
                        (recur
                          (conj roll-tree [])
                          (inc depth)
                          (next roll-command))
                        :default
                        (recur
                          roll-tree
                          depth
                          (next roll-command)))
                      (let [total-modifiers (filterv
                                              #(contains? % :modifier)
                                              (get-in roll-tree [depth]))]
                        {:roll-tree (update-in roll-tree
                                               [depth]
                                               (fn [last-roll]
                                                 (into []
                                                       (remove #(contains? % :modifier) last-roll))))
                         :total-modifiers total-modifiers})))
        roll-total (->> (:roll-tree roll-data)
                        (map
                          (fn [roll]
                            (reduce
                              (fn [total roll]
                                (+ total (:total roll)))
                              0
                              roll)))
                        (apply max))
        final-total (->> (:total-modifiers roll-data)
                         (map :total)
                         (reduce +)
                         (+ roll-total))]
    (assoc roll-data :total final-total)))

(defn interpret-roll-command
  "The entry point for generating a dice roll
  1. Take in a list of dice commands
  2. Strip excess white space from each of them
  3. Generate roll data from the list of commands
  4. Process those into a properly formatted slack message
  5. Returns the slack message"
  [roll-command]
  (let [tokenized-roll-command (map string/trim roll-command)]
    (format-slack-message (generate-roll-data tokenized-roll-command))))
