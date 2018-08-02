(ns rpg-action.models.commands
  (:require [clojure.spec.alpha :as s]))

(def commands #{:help})
(defn command?
  [cmd]
  (contains? commands cmd))

(defmulti command first)
(defmethod command :help [_]
  (s/cat :type #{:help} :command (s/? command?)))

(s/def ::command (s/multi-spec command (fn [genv _tag] genv)))