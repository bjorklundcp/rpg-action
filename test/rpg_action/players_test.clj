(ns rpg-action.players-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as ts]
            [rpg-action.gamestate :as gamestate]
            [taoensso.timbre :as timbre]))

(defn clean-players-fixture [f]
  (swap! gamestate/state (fn [orig-state]
                           (assoc orig-state :players [])))
  (f))

(use-fixtures :each clean-players-fixture)

(deftest add-remove-player
  (gamestate/add-player! {:player-name "@chardizzeroony" :character-name "Myzrael" :card-modifier {:number 1 :positive? true}})
  (is (= [{:card-modifier {:number    1
                           :positive? true}
           :character-name "Myzrael"
           :player-name     "@chardizzeroony"}]
         (:players @gamestate/state)))
  (gamestate/add-player! {:player-name "@cbjorklund" :character-name "Teclis" :card-modifier {:number 2 :positive? true}})
  (is (= [{:card-modifier {:number    1
                           :positive? true}
           :character-name "Myzrael"
           :player-name     "@chardizzeroony"}
          {:character-name "Teclis"
           :player-name "@cbjorklund"
           :card-modifier {:number 2 :positive? true}}]
         (:players @gamestate/state)))
  (gamestate/remove-player-by-character-name! "Myzrael")
  (is (= [{
           :character-name "Teclis"
           :player-name "@cbjorklund"
           :card-modifier {:number 2 :positive? true}}]
         (:players @gamestate/state)))
  (gamestate/remove-player-by-player-name! "@cbjorklund")
  (is (= []
         (:players @gamestate/state))))