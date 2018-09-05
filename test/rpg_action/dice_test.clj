(ns rpg-action.dice-test
  (:require [clojure.test :refer :all]
            [rpg-action.dice :as dice]))

(deftest test-basic-roll
  (testing "We should get 3 when we roll and and redef rand-int"
    (with-redefs [rand-int (fn [max] 2)]
      (is (= 3 (dice/basic-roll 8))))))

(deftest test-explode-roll
  (let [loaded-dice (atom 7)]
    (testing "We should get an explode if max on the dice was rolled"
      (with-redefs [rand-int (fn [max]
                                 (let [roll @loaded-dice]
                                   (reset! loaded-dice 2)
                                   roll))]
        (is (= [8 3] (dice/explode-roll 8)))))))

(deftest test-roll
  (let [loaded-dice (atom 7)]
    (testing "We should get an explode roll if we pass true to roll"
      (with-redefs [rand-int (fn [max]
                               (let [roll @loaded-dice]
                                 (reset! loaded-dice 2)
                                 roll))]
        (is (= [8 3] (dice/roll 8 true)))))
    (testing "We should not explode if passed false"
      (with-redefs [rand-int (fn [max] 7)]
        (is (= [8] (dice/roll 8 false)))))))

(deftest test-parse-command-into-data
  (testing "Given a string command, we should generate either
  roll data, modifier data, value data, or simply return a ^"
    (with-redefs [rand-int (fn [max] 4)]
      (is (= {:roll    "1d6"
              :display "1d6"
              :result  [5]
              :total   5}
             (dice/parse-command-into-data "d6"))))
    (is (= {:display  "X"
            :modifier *}
           (dice/parse-command-into-data "*")))
    (is (= {:total   5}
           (dice/parse-command-into-data "5")))
    (is (= "^"
           (dice/parse-command-into-data "^")))))

(deftest test-generate-roll-data
  (let [loaded-dice (atom 7)]
    (with-redefs [rand-int (fn [max]
                             (if (= @loaded-dice 7)
                               (do
                                 (reset! loaded-dice 2)
                                 7)
                               (do
                                 (reset! loaded-dice 7)
                                 2)))]
      (testing "We should get a roll with accompanied meta data when passed a single roll command"
        (is (= {:roll    "1d8"
                :display "1d8"
                :result  [8]
                :total   8}
               (dice/generate-roll-data "d8")))
        (is (= {:roll    "3d8"
                :display "3d8"
                :result  [3 8 3]
                :total   14}
               (dice/generate-roll-data "3d8")))
        (is (= {:roll    "1d8"
                :display "1d8!"
                :result  [8 3]
                :total   11}
               (dice/generate-roll-data "d8!")))))))

(deftest test-total-up-roll-data-segment
  (testing "given a list of roll data, we should get an integer of the evaluated total"
    (is (= 0
           (dice/total-up-roll-data-segment [])))
    (is (= 10
           (dice/total-up-roll-data-segment [] 10)))
    (is (= 100
           (dice/total-up-roll-data-segment [{:total 100}])))
    (is (= 200
           (dice/total-up-roll-data-segment [{:total 100}] 100)))
    (is (= 2
           (dice/total-up-roll-data-segment [{:total 1} {:modifier +} {:total 1}])))
    (is (= 0
           (dice/total-up-roll-data-segment [{:total 5} {:modifier -} {:total 5}])))
    (is (= 5
           (dice/total-up-roll-data-segment [{:modifier -} {:total 5}] 10)))
    (is (= 95
           (dice/total-up-roll-data-segment [{:total 5}
                                             {:modifier *}
                                             {:total 5}
                                             {:modifier +}
                                             {:total 50}
                                             {:modifier +}
                                             {:total 20}])))
    (is (= 30
           (dice/total-up-roll-data-segment [{:total 2}
                                             {:modifier *}
                                             {:total 5}
                                             {:modifier +}
                                             {:total 15}
                                             {:modifier +}
                                             {:total 5}
                                             {:modifier *}
                                             {:total 10}
                                             {:modifier /}
                                             {:total 5}
                                             {:modifier -}
                                             {:total 10}
                                             {:modifier -}
                                             {:total 20}])))
    (is (= 910
           (dice/total-up-roll-data-segment [{:modifier *}
                                             {:total 5}
                                             {:modifier +}
                                             {:total 15}
                                             {:modifier /}
                                             {:total 5}
                                             {:modifier -}
                                             {:total 10}
                                             {:modifier *}
                                             {:total 10}
                                             {:modifier -}
                                             {:total 20}]
                                            100)))
    (is (= 1
           (dice/total-up-roll-data-segment [{:total 3}
                                             {:modifier /}
                                             {:total 2}])))))

(deftest test-get-total-from-roll-data
  (testing "Given a list of roll data, generate the total of the roll data"
    (is (= 0
           (dice/get-total-from-roll-data [])))
    (is (= 5
           (dice/get-total-from-roll-data [{:total 5}])))
    (is (= 35
           (dice/get-total-from-roll-data [{:total 5}
                                           {:modifier +}
                                           {:total 10}
                                           {:modifier +}
                                           {:total 20}])))
    (is (= 40
           (dice/get-total-from-roll-data [{:total 5}
                                           {:modifier +}
                                           {:total 10}
                                           "^"
                                           {:total 20}
                                           {:modifier +}
                                           {:total 20}])))
    (is (= 50
           (dice/get-total-from-roll-data [{:total 5}
                                           {:modifier +}
                                           {:total 10}
                                           "^"
                                           {:total 20}
                                           {:modifier +}
                                           {:total 20}
                                           "^"
                                           {:total 5}
                                           {:modifier *}
                                           {:total 20}
                                           {:modifier -}
                                           {:total 50}])))))

(deftest test-roll-command-to-roll-data
  (testing "We should get the correct roll data for a list of roll commands"
    (with-redefs [rand-int (fn [max] 4)]
      (is (= {:roll-data       [{:roll    "1d6"
                                 :display "1d6"
                                 :result  [5]
                                 :total   5}]
              :final-modifiers []
              :total           5}
             (dice/roll-command-to-roll-data ["d6"])))
      (is (= {:roll-data       [{:roll    "1d6"
                                 :display "1d6"
                                 :result  [5]
                                 :total   5}
                                {:display "+"
                                 :modifier +}
                                {:roll    "1d6"
                                 :display "1d6"
                                 :result  [5]
                                 :total   5}]
              :final-modifiers []
              :total           10}
             (dice/roll-command-to-roll-data ["d6" "+" "d6"])))
      (is (= {:roll-data       [{:roll    "1d6"
                                 :display "1d6"
                                 :result  [5]
                                 :total   5}
                                {:display "+"
                                 :modifier +}
                                {:roll    "1d6"
                                 :display "1d6"
                                 :result  [5]
                                 :total   5}
                                "^"
                                {:roll    "1d8"
                                 :display "1d8"
                                 :result  [5]
                                 :total   5}]
              :final-modifiers []
              :total           10}
             (dice/roll-command-to-roll-data ["d6" "+" "d6" "^" "d8"])))
      (is (= {:roll-data       [{:roll    "1d6"
                                 :display "1d6!"
                                 :result  [5]
                                 :total   5}
                                {:display "+"
                                 :modifier +}
                                {:roll    "1d6"
                                 :display "1d6!"
                                 :result  [5]
                                 :total   5}
                                "^"
                                {:roll    "1d8"
                                 :display "1d8!"
                                 :result  [5]
                                 :total   5}]
              :final-modifiers []
              :total           10}
             (dice/roll-command-to-roll-data ["d6!" "+" "d6!" "^" "d8!"])))
      (is (= {:roll-data       [{:roll    "1d6"
                                 :display "1d6!"
                                 :result  [5]
                                 :total   5}
                                {:display "+"
                                 :modifier +}
                                {:roll    "1d6"
                                 :display "1d6!"
                                 :result  [5]
                                 :total   5}
                                "^"
                                {:roll    "1d8"
                                 :display "1d8!"
                                 :result  [5]
                                 :total   5}]
              :final-modifiers [{:modifier -
                                 :display "-"}
                                {:total 2}]
              :total           8}
             (dice/roll-command-to-roll-data ["d6!" "+" "d6!" "^" "d8!" "-" "2"]))))))

(deftest test-generate-message-pretext
  (testing "We should get an appropriate pre-text string when passed roll-data"
    (is (= "You rolled 5"
           (dice/generate-message-pretext {:roll-data       [{:total 5}]
                                           :total-modifiers []
                                           :total           5})))
    (is (= "You rolled 2 + 7 + 8 + 18 = 35"
           (dice/generate-message-pretext {:roll-data       [{:total 2}
                                                             {:display "+"}
                                                             {:total 7}
                                                             {:display "+"}
                                                             {:total 8}
                                                             {:display "+"}
                                                             {:total 18}]
                                           :total-modifiers []
                                           :total           35})))
    (is (= "You rolled 2 X 7 / 7 + 50 - 2 = 50"
           (dice/generate-message-pretext {:roll-data       [{:total 2}
                                                             {:display "X"}
                                                             {:total 7}
                                                             {:display "/"}
                                                             {:total 7}
                                                             {:display "+"}
                                                             {:total 50}
                                                             {:display "-"}
                                                             {:total 2}]
                                           :total-modifiers []
                                           :total           50})))
    (is (= "You rolled 4 + 6 ^ 11 = 11"
           (dice/generate-message-pretext {:roll-data       [{:total 4}
                                                             {:display "+"}
                                                             {:total 6}
                                                             "^"
                                                             {:total 11}]
                                           :total-modifiers []
                                           :total           11})))
    (is (= "You rolled 4 + 6 ^ 11 + 2 = 13"
           (dice/generate-message-pretext {:roll-data       [{:total 4}
                                                             {:display "+"}
                                                             {:total 6}
                                                             "^"
                                                             {:total 11}]
                                           :total-modifiers [{:display "+"}
                                                             {:total 2}]
                                           :total           13})))))

(deftest test-generate-message-body
  (testing "We should get an appropriate list of slack attachment fields when passed a roll tree"
    (is (= [{:title "Dice"
             :value "1d6"
             :short true}
            {:title "Rolls"
             :value "5"
             :short true}]
           (dice/generate-message-body [[{:roll    "1d6"
                                          :display "1d6"
                                          :result  [5]
                                          :total   5}]])))
    (is (= [{:title "Dice"
             :value "1d6"
             :short true}
            {:title "Rolls"
             :value "5"
             :short true}
            {:title "Dice"
             :value "1d8"
             :short true}
            {:title "Rolls"
             :value "5"
             :short true}
            {:title "Dice"
             :value "1d10"
             :short true}
            {:title "Rolls"
             :value "5"
             :short true}
            {:title "Dice"
             :value "1d20"
             :short true}
            {:title "Rolls"
             :value "5"
             :short true}]
           (dice/generate-message-body [[{:roll    "1d6"
                                          :display "1d6"
                                          :result  [5]
                                          :total   5}
                                         {:roll    "1d8"
                                          :display "1d8"
                                          :result  [5]
                                          :total   5}
                                         {:roll    "1d10"
                                          :display "1d10"
                                          :result  [5]
                                          :total   5}
                                         {:roll    "1d20"
                                          :display "1d20"
                                          :result  [5]
                                          :total   5}]])))
    (is (= [{:title "Dice"
             :value "10d6"
             :short true}
            {:title "Rolls"
             :value "1 2 3 4 5 6 1 2 3 4"
             :short true}]
           (dice/generate-message-body [[{:roll    "10d6"
                                          :display "10d6"
                                          :result  [1 2 3 4 5 6 1 2 3 4]
                                          :total   31}]])))))

(deftest test-bold-numbers-in-string
  (testing "given a string, should return a string with all integers surrounded in '*'"
    (is (= "*123*abc"
           (dice/bold-numbers-in-string "123abc")))
    (is (= "abc"
           (dice/bold-numbers-in-string "abc")))
    (is (= "*1* + *2* + *3* = *6*"
           (dice/bold-numbers-in-string "1 + 2 + 3 = 6")))))

(deftest test-separate-final-modifiers
  (testing "given a list of commands, returns a list of two sets.
  The first being the roll command and the
  second being modifiers that need to be applied to the whole roll."
    (is (= ['("d6") '()]
           (dice/separate-final-modifiers ["d6"])))
    (is (= ['("d6" "+" "2" "^" "d8") '("*" "2")]
           (dice/separate-final-modifiers ["d6" "+" "2" "^" "d8" "*" "2"])))
    (is (= ['("d6" "+" "2" "^" "d8" "-" "2" "^" "d12" "^" "d20") '("+" "2")]
           (dice/separate-final-modifiers ["d6" "+" "2" "^" "d8" "-" "2" "^" "d12" "^" "d20" "+" "2"])))))

(deftest test-add-implied-modifiers
  (testing "given a list of commands, return the list of commands with implied addition modifier
  between dice rolls and numbers"
    (is (= ["d6" "+" "2"]
           (dice/add-implied-modifiers ["d6" "+" "2"])))
    (is (= ["d6" "+" "2"]
           (dice/add-implied-modifiers ["d6" "2"])))
    (is (= ["d6" "+" "2" "-" "5"]
           (dice/add-implied-modifiers ["d6" "2" "-" "5"])))))

(deftest test-break-apart-modifiers-and-numbers
  (testing "given a list of commands, return a list of commands where [<modifier><integer>] is broken
  apart into ]<modifier> <integer>]"
    (is (= ["d6" "+" "2"]
           (dice/break-apart-modifiers-and-numbers ["d6" "+" "2"])))
    (is (= ["d6" "+" "2"]
           (dice/break-apart-modifiers-and-numbers ["d6" "+2"])))
    (is (= ["d6" "+" "2" "-" "2" "*" "2" "/" "2"]
           (dice/break-apart-modifiers-and-numbers ["d6" "+2" "-2" "*2" "/2"])))))

(deftest test-remove-ending-modifiers
  (testing "given a list of commands, if there are modifiers on the end they should be removed"
    (is (= ["d6" "+" "2"]
           (dice/remove-ending-modifiers ["d6" "+" "2" "+"])))
    (is (= []
           (dice/remove-ending-modifiers ["-" "*" "/" "+"])))))

(deftest test-remove-beginning-modifiers
  (testing "given a list of commands, if there are modifiers at the beginning they should be removed"
    (is (= ["d6" "+" "2"]
           (dice/remove-beginning-modifiers ["+" "d6" "+" "2"])))
    (is (= []
           (dice/remove-beginning-modifiers ["-" "+" "*" "/"])))))

(deftest test-reduce-adjacent-modifiers-to-one
  (testing "given a list of commands, adjacent modifiers should be reduced to the first modifier"
    (is (= ["d6" "+" "2"]
           (dice/reduce-adjacent-modifiers-to-one ["d6" "+" "+" "2"])))
    (is (= ["d6" "*" "2"]
           (dice/reduce-adjacent-modifiers-to-one ["d6" "*" "+" "-" "/" "2"])))))

(deftest test-generate-formatted-command-list
  (testing "given a list of commands, remove tokens that are not valid dice notation"
    (is (= ["d6"]
           (dice/generate-formatted-command-list ["d6"])))
    (is (= ["d6" "^" "d8" "+" "2"]
           (dice/generate-formatted-command-list ["d6" "^" "d8" "2"])))
    (is (= ["d6" "^" "d8" "+" "2" "^" "d10" "^" "d20" "+" "10"]
           (dice/generate-formatted-command-list ["d6" "^" "d8" "2" "^" "d10" "^" "d20" "10"])))
    (is (= ["d6"]
           (dice/generate-formatted-command-list ["d6" "dog" "cat"])))
    (is (= []
           (dice/generate-formatted-command-list ["boop" "dog" "cat"])))))
