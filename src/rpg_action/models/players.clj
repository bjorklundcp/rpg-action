(ns rpg-action.models.players
  (:require [clojure.spec.alpha :as s]))

(s/def :card-modifier/number number?)
(s/def :card-modifier/positive? boolean?)

(s/def ::character-name string?)
(s/def ::slack-name string?)
(s/def ::card-modifier (s/keys :req-un [:card-modifier/number :card-modifier/positive?]))

(s/def ::player (s/keys :req-un [::character-name ::slack-name]
                        :opt-un [::card-modifier]))