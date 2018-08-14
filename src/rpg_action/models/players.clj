(ns rpg-action.models.players
  (:require [clojure.spec.alpha :as s]))

(s/def :card-modifier/number number?)
(s/def :card-modifier/positive? boolean?)

(s/def ::character-name string?)
(s/def ::player-name string?)
(s/def ::card-modifier (s/keys :req-un [:card-modifier/number :card-modifier/positive?]))

(s/def ::player (s/keys :req-un [::character-name ::player-name ::card-modifier]))

(defn player-to-string
  [{:keys [character-name player-name card-modifier] :as player}]
  (str "Character: "
       character-name
       " Player: "
       player-name
       (cond
         (= true (:positive? card-modifier))
         (str " Modifier: Adv " (:number card-modifier))
         (= false (:positive? card-modifier))
         (str " Modifier: Disadv  " (:number card-modifier))
         :default
         " ")))
(s/fdef player-to-string
        :args (s/cat :player ::player))