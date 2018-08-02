(ns rpg-action.cards-test
  (:require [clojure.test :refer :all]
            [rpg-action.gamestate :as gamestate]
            [clojure.pprint :as pprint]
            [clojure.spec.test.alpha :as ts]))

(defn clean-slate-fixture [f]
  (reset! gamestate/state {:players [] :deck (gamestate/shuffle-deck!)})
  (f))

(defn death-wagon-fixture [f]
  (gamestate/add-player! {:character-name "Myzrael" :slack-name "@chardizzeroony"})
  (gamestate/add-player! {:character-name "Squiddlykins" :slack-name "@chartwig" :card-modifier {:number 2 :positive? false}})
  (gamestate/add-player! {:character-name "Teclis" :slack-name "@cbjorklund" :card-modifier {:number 2 :positive? true}})
  (gamestate/add-player! {:character-name "Thog" :slack-name "@abel"})
  (gamestate/add-player! {:character-name "Ulruk" :slack-name "@res378"})
  (f))

(defn stack-deck-fixture [f]
  (swap! gamestate/state (fn [orig-state]
                           (assoc orig-state :deck [{:rank :K, :suit :spades}
                                                    {:rank :2, :suit :spades}
                                                    {:rank :10, :suit :diamonds}
                                                    {:rank :3, :suit :spades}
                                                    {:rank :5, :suit :diamonds}
                                                    {:rank :8, :suit :spades}
                                                    {:rank :K, :suit :clubs}
                                                    {:rank :J, :suit :clubs}
                                                    {:rank :6, :suit :hearts}
                                                    {:rank :10, :suit :clubs}
                                                    {:rank :Q, :suit :hearts}
                                                    {:rank :10, :suit :hearts}
                                                    {:rank :2, :suit :diamonds}
                                                    {:rank :4, :suit :diamonds}
                                                    {:rank :4, :suit :spades}
                                                    {:rank :5, :suit :hearts}
                                                    {:rank :8, :suit :hearts}
                                                    {:rank :K, :suit :hearts}
                                                    {:rank :Q, :suit :spades}
                                                    {:rank :8, :suit :diamonds}
                                                    {:rank :J, :suit :diamonds}
                                                    {:rank :2, :suit :hearts}
                                                    {:rank :J, :suit :hearts}
                                                    {:rank :7, :suit :hearts}
                                                    {:rank :A, :suit :clubs}
                                                    {:rank :6, :suit :spades}
                                                    :red-joker
                                                    {:rank :9, :suit :diamonds}
                                                    {:rank :10, :suit :spades}
                                                    {:rank :9, :suit :clubs}
                                                    {:rank :5, :suit :clubs}
                                                    {:rank :4, :suit :hearts}
                                                    {:rank :7, :suit :clubs}
                                                    {:rank :A, :suit :hearts}
                                                    {:rank :A, :suit :diamonds}
                                                    {:rank :Q, :suit :diamonds}
                                                    {:rank :6, :suit :diamonds}
                                                    {:rank :3, :suit :clubs}
                                                    {:rank :7, :suit :diamonds}
                                                    {:rank :4, :suit :clubs}
                                                    {:rank :5, :suit :spades}
                                                    {:rank :K, :suit :diamonds}
                                                    :black-joker
                                                    {:rank :3, :suit :hearts}
                                                    {:rank :2, :suit :clubs}
                                                    {:rank :J, :suit :spades}
                                                    {:rank :A, :suit :spades}
                                                    {:rank :8, :suit :clubs}
                                                    {:rank :9, :suit :spades}
                                                    {:rank :7, :suit :spades}
                                                    {:rank :Q, :suit :clubs}
                                                    {:rank :9, :suit :hearts}
                                                    {:rank :6, :suit :clubs}
                                                    {:rank :3, :suit :diamonds}])))
  (f))

(use-fixtures :each clean-slate-fixture death-wagon-fixture stack-deck-fixture)

(deftest players-only-rounds
  (testing "Round 1"
    (is (= 54
           (-> @gamestate/state :deck count)))
    (is (= [{:card   {:rank :K
                      :suit :spades}
             :player {:character-name "Myzrael"
                      :slack-name     "@chardizzeroony"}}
            {:card   {:rank :K
                      :suit :clubs}
             :player {:character-name "Ulruk"
                      :slack-name     "@res378"}}
            {:card   {:rank :8
                      :suit :spades}
             :player {:character-name "Thog"
                      :slack-name     "@abel"}}
            {:card   {:rank :5
                       :suit :diamonds}
             :player {:card-modifier  {:number    2
                                       :positive? true}
                      :character-name "Teclis"
                      :slack-name     "@cbjorklund"}}
            {:card   {:rank :2
                      :suit :spades}
             :player {:card-modifier  {:number    2
                                       :positive? false}
                      :character-name "Squiddlykins"
                      :slack-name     "@chartwig"}}]
           (gamestate/deal-round!)))
    (is (= 47
           (-> @gamestate/state :deck count))))
  (testing "Round 2"
    (is (= [{:card   {:rank :Q
                      :suit :hearts}
             :player {:card-modifier  {:number    2
                                       :positive? true}
                      :character-name "Teclis"
                      :slack-name     "@cbjorklund"}}
            {:card   {:rank :J
                      :suit :clubs}
             :player {:character-name "Myzrael"
                      :slack-name     "@chardizzeroony"}}
            {:card   {:rank :6
                      :suit :hearts}
             :player {:card-modifier  {:number    2
                                       :positive? false}
                      :character-name "Squiddlykins"
                      :slack-name     "@chartwig"}}
            {:card   {:rank :4
                      :suit :diamonds}
             :player {:character-name "Ulruk"
                      :slack-name     "@res378"}}
            {:card   {:rank :2
                      :suit :diamonds}
             :player {:character-name "Thog"
                      :slack-name     "@abel"}}]
           (gamestate/deal-round!)))
    (is (= 40
          (-> @gamestate/state :deck count))))
  (testing "Round 3"
    (is (= [{:card   {:rank :K
                      :suit :spades}
             :player {:character-name "Myzrael"
                      :slack-name     "@chardizzeroony"}}
            {:card   {:rank :K
                      :suit :clubs}
             :player {:character-name "Ulruk"
                      :slack-name     "@res378"}}
            {:card   {:rank :8
                      :suit :spades}
             :player {:character-name "Thog"
                      :slack-name     "@abel"}}
            {:card   {:rank :5
                      :suit :diamonds}
             :player {:card-modifier  {:number    2
                                       :positive? true}
                      :character-name "Teclis"
                      :slack-name     "@cbjorklund"}}
            {:card   {:rank :2
                      :suit :spades}
             :player {:card-modifier  {:number    2
                                       :positive? false}
                      :character-name "Squiddlykins"
                      :slack-name     "@chartwig"}}
            (gamestate/deal-round!)]))
    (is (= 33
          (-> @gamestate/state :deck count))))
  (testing "Round 4"
    (is (= [{:card   :red-joker
             :player {:character-name "Thog"
                      :slack-name     "@abel"}}
            {:card   {:rank :A
                      :suit :clubs}
             :player {:card-modifier  {:number    2
                                       :positive? true}
                      :character-name "Teclis"
                      :slack-name     "@cbjorklund"}}
            {:card   {:rank :9
                      :suit :diamonds}
             :player {:character-name "Ulruk"
                      :slack-name     "@res378"}}
            {:card   {:rank :7
                      :suit :hearts}
             :player {:card-modifier  {:number    2
                                       :positive? false}
                      :character-name "Squiddlykins"
                      :slack-name     "@chartwig"}}
            {:card   {:rank :2
                      :suit :hearts}
             :player {:character-name "Myzrael"
                      :slack-name     "@chardizzeroony"}}]
          (gamestate/deal-round!)))
    (is (= 54
           (-> @gamestate/state :deck count)))))

(ts/instrument)