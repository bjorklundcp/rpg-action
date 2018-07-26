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
  (gamestate/add-player! {:slack-name "@chardizzeroony" :character-name "Myzrael"})
  (is (= [{:character-name "Myzrael"
           :slack-name     "@chardizzeroony"}]
         (:players @gamestate/state)))
  (gamestate/add-player! {:slack-name "@cbjorklund" :character-name "Teclis" :card-modifier {:number 2 :positive? true}})
  (is (= [{:character-name "Myzrael"
           :slack-name     "@chardizzeroony"}
          {:character-name "Teclis"
           :slack-name "@cbjorklund"
           :card-modifier {:number 2 :positive? true}}]
         (:players @gamestate/state)))
  (gamestate/remove-player-by-character-name! "Myzrael")
  (is (= [{:character-name "Teclis"
           :slack-name "@cbjorklund"
           :card-modifier {:number 2 :positive? true}}]
         (:players @gamestate/state)))
  (gamestate/remove-player-by-slack-name! "@cbjorklund")
  (is (= []
         (:players @gamestate/state))))

(ts/instrument)