(ns rpg-action.communications.slack
  (:require [clojure.string :as str]
            [ring.util.response :as ring-response]
            [clojure.spec.alpha :as s]
            [rpg-action.models.commands :as commands]
            [compojure.core :refer :all]
            [rpg-action.utils :as utils])
  (:import (clojure.lang PersistentArrayMap)))

(defmulti response type)
(defmethod response String [resp]
  (ring-response/response {:text resp}))
(defmethod response PersistentArrayMap [resp]
  (ring-response/response resp))
(defmethod response :default [resp]
  resp)

(defn return-help-text
  [help-option]
  (case help-option
    "gm" (utils/long-str "To add a player: /rpgaction addplayer <character name> <player handle> [<+/- card advantage]"
                         "To draw initiation cards: /rpgaction draw")
    "dice" "TODO"
    (utils/long-str "Valid commands: save, list, draw, <action name>"
                    "To save a new action: /rpgaction save <action name> <dice notation> <modifiers>"
                    "To list your saved actions: /rpgaction list"
                    "To use one of your saved actions: /rpgaction <action name> <modifiers>")))

(def not-found-text "Command not found... I think you're doing it wrong... :face_with_rolling_eyes:")

(defn wrap-slack [h]
  (fn [request]
    (let [command (-> (get-in request [:params :text] "")
                      (str/split #" ")
                      (update 0 keyword))]
      (if (s/valid? ::commands/command command)
         (h (assoc request :command (s/conform ::commands/command command)))
         (response not-found-text)))))

(defroutes slack-routes
  (POST "/command" request
    (case (get-in request [:command :type])
      :help (response (return-help-text (get-in request [:command :help-option] nil))))))
