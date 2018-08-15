(ns rpg-action.models.commands
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.string :as str]))

; COMMANDS
(defmulti command first)

; help command
(def help-options #{:gm :dice})
(defn help-option?
  [option]
  (contains? help-options (keyword option)))
(defmethod command :help [_]
  (s/cat :type #{:help} :help-option (s/? help-option?)))

; addplayer command
(def card-modifier-regex #"^([+-]?)(\d+)$")
(defn card-modifier?
  [cm]
  (re-matches card-modifier-regex cm))
(defmethod command :addplayer [_]
  (s/cat :type #{:addplayer} :character-name string? :player-name string? :card-modifier (s/? card-modifier?)))

; removeplayer command
(defmethod command :removeplayer [_]
  (s/cat :type #{:removeplayer} :name string?))

; listplayers command
(defmethod command :listplayers [_]
  (s/cat :type #{:listplayers}))

; deal command
(defmethod command :deal [_]
  (s/cat :type #{:deal}))



(s/def ::command (s/multi-spec command (fn [genv _tag] genv)))


; CONVERSIONS

(def conversions
  {:addplayer (fn [value]
                (update value :card-modifier #(if %
                                                (let [[_ sign number] (re-matches card-modifier-regex %)]
                                                  {:number (Integer. (re-find  #"\d+" number))
                                                   :positive? (boolean (not= "-" sign))})
                                                {:number 1 :positive? true})))})

(defn conform
  [[type & _  :as value]]
  (let [conformed (s/conform ::command value)
        conversion (get conversions type)]
    (-> (if conversion
          (conversion conformed)
          conformed)
        (dissoc :type))))
