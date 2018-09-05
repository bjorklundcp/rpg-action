(ns rpg-action.dice
  (:require [clojure.string :as string]
            [rpg-action.utils :as utils]))

(defn is-dice-roll?
  "Checks a string to see if it is a dice roll
  IE: d6, 5d6, d6!"
  [input]
  (boolean (re-matches utils/roll-regex (or input ""))))

(defn is-number?
  "Checks a string to see if it is an integer"
  [input]
  (boolean (re-matches utils/number-regex (or input ""))))

(defn is-modifier-and-number?
  "Checks a string to see if it is a valid modifier and number
  IE: +2, -10, *8, etc."
  [input]
  (boolean (re-matches utils/modifier-and-number-regex (or input ""))))

(defn is-modifier?
  "Checks a string to see if it's a valid modifier
  + - * / are the only acceptable characters"
  [input]
  (boolean (re-matches utils/modifier-regex (or input ""))))

(defn is-caret?
  "Checks a string to see if it's a ^"
  [input]
  (= "^" input))

(defn is-valid-token?
  "Checks if a string is a valid input for the dice interpreter"
  [input]
  (or
    (is-dice-roll? input)
    (is-number? input)
    (is-modifier-and-number? input)
    (is-modifier? input)
    (is-caret? input)))

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

(defn generate-roll-data
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

(defn generate-modifier-data
  "Processes a modifier into an object of the following form:
  {:display the string that will be displayed
   :modifier the clojure function that will be called on surrounding values}"
  [modifier]
  (case modifier
    "-" {:display "-"
         :modifier -}
    "*" {:display "X"
         :modifier *}
    "/" {:display "/"
         :modifier /}
    {:display "+"
     :modifier +}))

(defn generate-value-data
  "Processes a value into an object of the following form:
  {:total the value converted into an int}
  Note: In this form the formatter doesn't care if it's a value or
  result of a dice roll"
  [value]
  {:total (Integer. value)})

(defn parse-command-into-data
  "Takes a command string and turns it into an object containing
  data depending on what the string is. 'd6' gets turned into roll data,
  '+' into modifier data, etc."
  [command]
  (cond
    (is-caret? command) command
    (is-dice-roll? command) (generate-roll-data command)
    (is-number? command) (generate-value-data command)
    (is-modifier? command) (generate-modifier-data command)))

(defn total-up-roll-data-segment
  "Given a set of roll data, total it up by applying modifiers to surrounding values.
  The roll data can be in the form [<modifier> <value> <modifier> ...] or
  [<value> <modifier> <value> ...]"
  ([roll-data]
   (total-up-roll-data-segment roll-data 0))
  ([roll-data starting-total]
   (cond
     (= 1 (count roll-data))
     (+ starting-total (:total (first roll-data)))
     (< 0 (count roll-data))
     (loop [total starting-total
            data roll-data]
       (if data
         (let [first-value (first data)
               second-value (first (next data))
               third-value (first (nthnext data 2))]
           (if (:modifier first-value)
             (recur (-> (apply (:modifier first-value) [total (:total second-value)])
                        Math/floor
                        int)
                    (nthnext data 2))
             (recur (+ total (-> (apply (:modifier second-value) [(:total first-value) (:total third-value)])
                                 Math/floor
                                 int))
                    (nthnext data 3))))
         total))
     :default
     starting-total)))

(defn get-total-from-roll-data
  "Totals up an entire roll taking into account ^ compares
  IE: 10 ^ 11 = 11"
  [roll-data]
  (if (and (< 0 (count roll-data))
           (some is-caret? roll-data))
    (loop [roll-data roll-data
           totals []
           current-data []]
      (if roll-data
        (let [command (first roll-data)]
          (if (is-caret? command)
            (recur (next roll-data)
                   (conj totals (total-up-roll-data-segment current-data))
                   [])
            (recur (next roll-data)
                   totals
                   (conj current-data command))))
        (apply max (conj totals (total-up-roll-data-segment current-data)))))
    (total-up-roll-data-segment roll-data)))

(defn separate-final-modifiers
  "If we have ^ compare operations in our input, we have to separate
  modifiers that are applied to the total from modifiers applied to the
  last compare operation.
  IE: 10 + 2 ^ 5 + 2 = (12 ^ 5) + 2 = 14"
  [input]
  (if (and (< 0 (count input))
           (some is-caret? input))
    (let [index-of-last-caret (.lastIndexOf input "^")
          split-input (split-at (+ 2 index-of-last-caret) input)]
      split-input)
    [(into '() (reverse input)) '()]))

(defn bold-numbers-in-string
  "Given a string, will surround each integer with *
  '1 + 2 + 3' --> '*1* + *2* + *3*'"
  [input]
  (string/replace input #"\d+" #(str "*" %1 "*")))

(defn generate-message-pretext
  "Given roll-data, generate the first line of text in the slack message."
  [roll-data]
  (let [message (->> (concat (:roll-data roll-data) (:total-modifiers roll-data))
                     (map (fn [data]
                            (cond
                              (:total data) (str (:total data))
                              (:display data) (:display data)
                              :default data)))
                     (string/join " "))]
    (if (< 1 (count (:roll-data roll-data)))
      (str "You rolled " message " = " (str (:total roll-data)))
      (str "You rolled " (str (:total roll-data))))))

(defn generate-message-body
  "Given roll-data, generate the body (rolls and their details) in the slack message."
  [roll-data]
  (let [dice-rolls (filterv #(:result %) (flatten roll-data))]
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
  "Given roll-data, generates a slack message."
  [roll-data]
  (if (and (empty? (first (:roll-tree roll-data)))
           (empty? (:total-modifiers roll-data))
           (zero? (:total roll-data)))
    "Could not find anything parsable. It's probably my fault. :disappointed:"
    {:response_type "in_channel"
     :attachments [{:color  (if (>= (:total roll-data) 4) "good" "danger")
                    :text   (->> (generate-message-pretext roll-data)
                                 bold-numbers-in-string)
                    :fields (generate-message-body (:roll-data roll-data))}]}))

(defn roll-command-to-roll-data
  "Takes a tokenized and formatted list of commands, and converts it into an object of the following form:
  {:roll-data the data for each part of the roll
   :final-modifiers the modifiers that should be applied to the total from roll-data
   :total the grand total of the entire roll}"
  [commands]
  (let [[roll-command final-modifiers] (separate-final-modifiers commands)
        roll-data (map parse-command-into-data roll-command)
        final-modifiers-data (map parse-command-into-data final-modifiers)
        roll-total (get-total-from-roll-data roll-data)
        final-total (total-up-roll-data-segment final-modifiers-data roll-total)]
    {:roll-data roll-data
     :final-modifiers final-modifiers-data
     :total final-total}))

(defn remove-ending-modifiers
  "Given a list of user commands, remove modifiers from the end"
  [input]
  (if (and (< 0 (count input)) (is-modifier? (last input)))
    (loop [input input]
      (if (and (< 0 (count input)) (is-modifier? (last input)))
        (recur (drop-last input))
        input))
    input))

(defn remove-beginning-modifiers
  "Given a list of user commands, remove modifiers from the beginning"
  [input]
  (if (and (< 0 (count input)) (is-modifier? (first input)))
    (loop [input input]
      (if (and (< 0 (count input)) (is-modifier? (first input)))
        (recur (drop 1 input))
        input))
    input))

(defn reduce-adjacent-modifiers-to-one
  "Given a list of user commands, reduce adjacent modifiers to just the first one
  IE: [1 + - 2] = [1 + 2]"
  [input]
  (if (< 0 (count input))
    (loop [command-list []
           input input]
      (if input
        (if (and (is-modifier? (last command-list)) (is-modifier? (first input)))
          (recur command-list (next input))
          (recur (conj command-list (first input)) (next input)))
        command-list))
    input))

(defn add-implied-modifiers
  "Given a list of user commands, add the addition modifier inbetween values with no modifier
  IE: [1 2 3] = [1 + 2 + 3]"
  [input]
  (if (< 0 (count input))
    (loop [command-list []
           input input]
      (if input
        (let [command (first input)
              next-command (first (next input))
              command-is-number-or-dice (or (is-dice-roll? command) (is-number? command))
              next-command-is-number-or-dice (or (is-dice-roll? next-command) (is-number? next-command))]
          (if (and command-is-number-or-dice next-command-is-number-or-dice)
            (recur (into [] (concat command-list [command "+"])) (next input))
            (recur (conj command-list command) (next input))))
        command-list))
    input))

(defn break-apart-modifiers-and-numbers
  "Given a list of user commands, breaks apart a <modifier><value> into <modifier> <value>
  IE: [5 +2] = [5 + 2]"
  [input]
  (if (< 0 (count input))
    (loop [command-list []
           input input]
      (if input
        (if (is-modifier-and-number? (first input))
          (let [modifier (nth (re-matches utils/modifier-and-number-regex (first input)) 1)
                value (nth (re-matches utils/modifier-and-number-regex (first input)) 2)]
            (recur (into [] (concat command-list [modifier value])) (next input)))
          (recur (conj command-list (first input)) (next input)))
        command-list))
    input))

(defn generate-formatted-command-list
  "Takes in a list of user commands and does the following
  1. trim white space from each command in the list
  2. remove tokens that are invalid
  3. properly format modifiers in the command list
  4. add implied addition modifiers where there are none"
  [input]
  (->> input
       (map string/trim)
       (filter is-valid-token?)
       break-apart-modifiers-and-numbers
       remove-beginning-modifiers
       remove-ending-modifiers
       add-implied-modifiers
       reduce-adjacent-modifiers-to-one))

(defn interpret-roll-command
  "The entrance point for interpreting a roll command
  1. Format the user command
  2. Get actual data from the request
  3. Turn the data into a slack message"
  [roll-command]
  (-> roll-command
      generate-formatted-command-list
      roll-command-to-roll-data
      format-slack-message))
