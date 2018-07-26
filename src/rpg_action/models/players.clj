(ns rpg-action.models.players
  (:require [clojure.spec.alpha :as s]))

(s/def ::character-name string?)
(s/def ::slsack-name string?)

(s/def ::player (s/keys :req-un [::character-name ::slack-name]))