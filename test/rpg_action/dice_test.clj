(ns rpg-action.dice-test
  (:require [clojure.test :refer :all]
            [rpg-action.dice :as dice]))

(deftest test-roll
  (testing "we should get value between 1 and max on a dice roll"
    (with-redefs [rand-int (fn [max] 2)]
      (is (= 3 (dice/basic-roll 8))))))

(deftest test-explode-roll
  (let [loaded-dice (atom 8)]
    (testing "We should get an explode if max on the dice was rolled"
      (with-redefs [dice/basic-roll (fn [max]
                                     (let [roll @loaded-dice]
                                       (reset! loaded-dice 2)
                                       roll))]
        (is (= [8,2] (dice/explode-roll 8)))))))

;(deftest test-dice-notation
;  (let [loaded-dice (atom 8)]
;    (testing "We should get an explode roll if we request a dice roll with !"
;      (with-redefs [dice/basic-roll (fn [max]
;                                     (let [roll @loaded-dice]
;                                       (reset! loaded-dice 2)
;                                       roll))]
;        (is (= {:roll [8,2]
;                :total 10
;                :modifier 0,
;                :compare? false}
;               (dice/process-roll-command ["d8!", "d8", "!", nil, nil])))))
;    (reset! loaded-dice 8)
;    (testing "We should not get an explode roll if there is no ! in the command"
;      (with-redefs [dice/basic-roll (fn [max]
;                                     (let [roll @loaded-dice]
;                                       (reset! loaded-dice 2)
;                                       roll))]
;        (is (= {:roll [8]
;                :total 8
;                :modifier 0,
;                :compare? false}
;               (dice/process-roll-command ["d8", "d8", nil, nil, nil])))))
;    (reset! loaded-dice 8)
;    (testing "We should get a modifier if we supply one"
;      (with-redefs [dice/basic-roll (fn [max]
;                                     (let [roll @loaded-dice]
;                                       (reset! loaded-dice 2)
;                                       roll))]
;        (is (= {:roll [8]
;                :total 8
;                :modifier 2,
;                :compare? false}
;               (dice/process-roll-command ["d8", "d8", nil, "+2", nil])))))
;    (reset! loaded-dice 8)
;    (testing "We should get compare = true if we send in the '^'"
;      (with-redefs [dice/basic-roll (fn [max]
;                                     (let [roll @loaded-dice]
;                                       (reset! loaded-dice 2)
;                                       roll))]
;        (is (= {:roll [8]
;                :total 8
;                :modifier 0,
;                :compare? true}
;               (dice/process-roll-command ["d8", "d8", nil, nil, "^"])))))))

