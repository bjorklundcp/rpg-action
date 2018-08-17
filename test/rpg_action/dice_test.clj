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

(deftest test-generate-roll-data
  (testing "We should get the correct roll data for different roll commands"
    (with-redefs [rand-int (fn [max] 4)]
      (is (= {:roll-tree       [[{:roll    "1d6"
                                  :display "1d6"
                                  :result  [5]
                                  :total   5}]]
              :total-modifiers []
              :total           5}
             (dice/generate-roll-data ["d6"])))
      (is (= {:roll-tree       [[{:roll    "10d6"
                                  :display "10d6"
                                  :result  [5 5 5 5 5 5 5 5 5 5]
                                  :total   50}]]
              :total-modifiers []
              :total           50}
             (dice/generate-roll-data ["10d6"])))
      (is (= {:roll-tree       [[{:roll    "1d6"
                                  :display "1d6"
                                  :result  [5]
                                  :total   5}]]
              :total-modifiers [{:modifier "+"
                                 :value    2
                                 :display  "+ 2"
                                 :total    2}]
              :total           7}
             (dice/generate-roll-data ["d6" "2"])))
      (is (= {:roll-tree       [[{:roll    "1d6"
                                  :display "1d6"
                                  :result  [5]
                                  :total   5}]]
              :total-modifiers [{:modifier "-"
                                 :value    2
                                 :display  "- 2"
                                 :total    -2}]
              :total           3}
             (dice/generate-roll-data ["d6" "-2"])))
      (is (= {:roll-tree       [[{:roll    "1d6"
                                  :display "1d6"
                                  :result  [5]
                                  :total   5}]
                                [{:roll    "1d6"
                                  :display "1d6"
                                  :result  [5]
                                  :total   5}]]
              :total-modifiers []
              :total           5}
             (dice/generate-roll-data ["d6" "^" "d6"])))
      (is (= {:roll-tree       [[{:roll    "1d6"
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
                                  :total   5}]]
              :total-modifiers []
              :total           20}
             (dice/generate-roll-data ["d6" "d8" "d10" "d20"])))
      (is (= {:roll-tree       [[{:roll    "1d6"
                                  :display "1d6"
                                  :result  [5]
                                  :total   5}
                                 {:modifier "+"
                                  :value    10
                                  :display  "+ 10"
                                  :total    10}]
                                [{:roll     "1d20"
                                  :display  "1d20"
                                  :result   [5]
                                  :total    5}]]
              :total-modifiers [{:modifier "+"
                                 :value    30
                                 :display  "+ 30"
                                 :total    30}]
              :total           45}
             (dice/generate-roll-data ["d6" "10" "^" "d20" "30"])))
      (let [loaded-dice (atom 7)]
        (with-redefs [rand-int (fn [max]
                                 (if (= @loaded-dice 7)
                                   (do
                                     (reset! loaded-dice 2)
                                     7)
                                   (do
                                     (reset! loaded-dice 7)
                                     2)))]
          (is (= {:roll-tree       [[{:roll    "1d8"
                                      :display "1d8"
                                      :result  [8]
                                      :total   8}
                                     {:modifier "+"
                                      :value    10
                                      :display  "+ 10"
                                      :total    10}]
                                    [{:roll     "1d20"
                                      :display  "1d20"
                                      :result   [3]
                                      :total    3}]]
                  :total-modifiers []
                  :total           18}
                 (dice/generate-roll-data ["d8" "10" "^" "d20"])))
          (is (= {:roll-tree       [[{:roll    "1d8"
                                      :display "1d8!"
                                      :result  [8 3]
                                      :total   11}]]
                  :total-modifiers []
                  :total           11}
                 (dice/generate-roll-data ["d8!"])))))))
  (testing "An integer with no modifier should be treated as addition"
    (with-redefs [rand-int (fn [max] 1)]
      (is (= (dice/generate-roll-data ["d6" "+2"]) (dice/generate-roll-data ["d6" "2"]))))))

(deftest test-process-roll
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
                 (dice/process-roll "d8")))
          (is (= {:roll    "3d8"
                  :display "3d8"
                  :result  [3 8 3]
                  :total   14}
                 (dice/process-roll "3d8")))
          (is (= {:roll    "1d8"
                  :display "1d8!"
                  :result  [8 3]
                  :total   11}
                 (dice/process-roll "d8!")))))))

(deftest test-process-modifier
  (testing "We should get a modifier object when passed a single modifier command"
    (is (= {:modifier "+"
            :value    2
            :display  "+ 2"
            :total    2}
           (dice/process-modifier "+2")))
    (is (= {:modifier "-"
            :value    2
            :display  "- 2"
            :total    -2}
           (dice/process-modifier "-2"))))
  (testing "A modifier should get processed the same if it is +N or just N"
    (is (= (dice/process-modifier "100") (dice/process-modifier "+100")))))

(deftest test-generate-message-pretext
  (testing "We should get an appropriate pre-text string when passed roll-data"
    (is (= "You rolled 5"
           (dice/generate-message-pretext {:roll-tree       [[{:roll    "1d6"
                                                               :display "1d6"
                                                               :result  [5]
                                                               :total   5}]]
                                           :total-modifiers []
                                           :total           5})))
    (is (= "You rolled 2 + 7 + 8 + 18 = 35"
           (dice/generate-message-pretext {:roll-tree       [[{:roll    "1d6"
                                                               :display "1d6"
                                                               :result  [2]
                                                               :total   2}
                                                              {:roll    "1d8"
                                                               :display "1d8"
                                                               :result  [7]
                                                               :total   7}
                                                              {:roll    "1d10"
                                                               :display "1d10"
                                                               :result  [8]
                                                               :total   8}
                                                              {:roll    "1d20"
                                                               :display "1d20"
                                                               :result  [18]
                                                               :total   18}]]
                                           :total-modifiers []
                                           :total           35})))
    (is (= "You rolled 18 - 2 = 16"
           (dice/generate-message-pretext {:roll-tree       [[{:roll    "1d20"
                                                               :display "1d20"
                                                               :result  [18]
                                                               :total   18}
                                                              {:modifier "-"
                                                               :value    2
                                                               :display  "- 2"
                                                               :total    -2}]]
                                           :total-modifiers []
                                           :total           16})))
    (is (= "You rolled 18 - 2 ^ 20 + 50 = 70"
           (dice/generate-message-pretext {:roll-tree       [[{:roll    "1d20"
                                                               :display "1d20"
                                                               :result  [18]
                                                               :total   18}
                                                              {:modifier "-"
                                                               :value    2
                                                               :display  "- 2"
                                                               :total    -2}]
                                                             [{:roll     "4d8"
                                                               :display "4d8"
                                                               :result  [5 6 7 2]
                                                               :total   20}]]
                                           :total-modifiers [{:modifier "+"
                                                              :value    50
                                                              :display  "+ 50"
                                                              :total    50}]
                                           :total           70})))
    (is (= "You rolled -10 + 50 = 40"
           (dice/generate-message-pretext {:roll-tree       [[{:modifier "-"
                                                               :value    10
                                                               :display  "- 10"
                                                               :total    -10}]]
                                           :total-modifiers [{:modifier "+"
                                                              :value    50
                                                              :display  "+ 50"
                                                              :total    50}]
                                           :total           40})))
    (is (= "You rolled 1000 - 50 = 950"
           (dice/generate-message-pretext {:roll-tree       [[{:modifier "+"
                                                               :value    1000
                                                               :display  "+ 10"
                                                               :total    1000}]]
                                           :total-modifiers [{:modifier "-"
                                                              :value    50
                                                              :display  "- 50"
                                                              :total    -50}]
                                           :total           950})))))

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
