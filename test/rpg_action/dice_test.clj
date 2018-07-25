(ns rpg-action.dice-test
  (:require [clojure.test :refer :all]
            [rpg-action.dice :as dice]))

(deftest test-roll
  (testing "we should get value between 1 and max on a dice roll"
    (let [roll (dice/roll 8)]
      (is (true? (and (< roll 9) (> roll 0)))))))
