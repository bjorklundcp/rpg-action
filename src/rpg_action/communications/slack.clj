(ns rpg-action.communications.slack
  (:require [clojure.string :as str]
            [ring.util.response :as ring-response]
            [clojure.spec.alpha :as s]
            [rpg-action.models.commands :as commands]
            [compojure.core :refer :all]
            [rpg-action.utils :as utils]
            [rpg-action.gamestate :as gamestate]
            [rpg-action.models.players :as players]
            [rpg-action.models.cards :as cards]
            [environ.core :refer [env]]
            [pandect.algo.sha256 :as sha256]
            [rpg-action.dice :as dice])
  (:import (clojure.lang PersistentArrayMap PersistentVector PersistentList)))

; Generic Strings
(def no-players-text "No players active in this round")
(def no-player-found-text "No player or character '%s' was found.")
(def not-found-text "Command not found... I think you're doing it wrong... :face_with_rolling_eyes:")
(def generic-ok-text "OK!")
(def forbidden-text "Forbidden")

; Help command Helpers
(def generic-help-text (utils/long-str "Valid commands: save, list, <action>, roll"
                                       "To save a new action: /rpgaction save <action> <dice notation> <modifiers>"
                                       "To list your saved actions: /rpgaction list"
                                       "To use one of your saved actions: /rpgaction <action> <modifiers>"
                                       "To roll dice: /rpgaction roll <dice notation>"
                                       "For more details on dice notation, try /rpgaction help dice"
                                       "For a list of gm commands, try /rpgaction help gm"))

(def dice-notation-help-text (utils/long-str "Information about valid dice notation is as follows:"
                                             "A dice roll - Examples: d6, 5d6, d6!"
                                             "<number of dice>d<sides on dice><explode?>"
                                             "<number of dice> will be the number of dice rolled - defaults to 1"
                                             "d<sides on dice> will be the type of dice rolled"
                                             "<explode?> will re-roll dice and add to the roll if the roll = <sides on dice> - defaults to false"
                                             "A modifier - Can only be `+`, `-`, `*`, or `/`. Order of operations is not taken into account. Modifiers simply apply to the surrounding values"
                                             "A value - Can be any positive integer"
                                             "A caret (^) - This will compare the left and right side of the caret and take the highest"
                                             "In the case that a caret is preset, any modifiers at the end of the dice notation will be applied to the final total"
                                             "Example: 2 + 2 ^ 3 + 2 = (4 ^ 3) + 2 = 6"
                                             "Each part of the dice roll must be separated by a space"
                                             "`d6d8d10` will not parse into anything but `d6 d8 d10` will"
                                             "If you do not provide a modifier between dice rolls or values, addition is assumed"
                                             "`1 2 3` will parse into `1 + 2 + 3`"))

(def gm-help-text (utils/long-str "Valid commands: addplayer, draw"
                                  "To add a player: /rpgaction addplayer <character name> <player handle> [<+/- card advantage]"
                                  "To draw initiation cards: /rpgaction draw"))

(defn help-text
  [help-option]
  (case help-option
    "gm" gm-help-text
    "dice" dice-notation-help-text
    generic-help-text))


; Serialization & Responses

(defn format-card-round
  [round]
  (->> (map (fn [{:keys [card player]}]
              (str (:character-name player) " (" (:player-name player) ") " (cards/repr-map card)))
            round)
       (str/join "\n")
       (str gamestate/new-round-str "\n")))


; API

(defmulti response type)
(defmethod response String [resp]
  (ring-response/response {:text resp}))
(defmethod response PersistentArrayMap [resp]
  (ring-response/response resp))
(defmethod response PersistentVector [resp]
  (ring-response/response resp))
(defmethod response PersistentList [resp]
  (ring-response/response resp))
(defmethod response :default [resp]
  resp)

(def slack-api-version "v0")
(defn wrap-slack-verify-token [h]
  (fn [request]
    (if (= :post (:request-method request))
      (let [body (-> (:original-body request)
                     slurp)
            timestamp (get-in request [:headers "x-slack-request-timestamp"])
            expected-signature-basestring (str slack-api-version ":" timestamp ":" body)
            signing-secret (env :slack-signing-secret)
            expected-signature (str slack-api-version "=" (sha256/sha256-hmac expected-signature-basestring signing-secret))
            actual-signature (get-in request [:headers "x-slack-signature"])]
        (if (= expected-signature actual-signature)
          (h request)
          (-> (response forbidden-text)
              (ring-response/status 403))))
      (-> (response forbidden-text)
          (ring-response/status 403)))))

(defn wrap-slack-command [h]
  (fn [request]
    (let [command (-> (get-in request [:params :text] "")
                      (str/split #" ")
                      (update 0 keyword))]
      (if (s/valid? ::commands/command command)
         (h (assoc request :command (first command)
                           :command-params (commands/conform command)))
         (response not-found-text)))))

(defroutes slack-routes
  (POST "/command" request
    (let [{:keys [command command-params]} request]
      (case command
        :help (response (help-text (:help-option command-params)))
        :addplayer (do (gamestate/add-player! command-params)
                       (response generic-ok-text))
        :removeplayer (if-let [players-removed (seq (concat (gamestate/remove-player-by-player-name! (:name command-params))
                                                            (gamestate/remove-player-by-character-name! (:name command-params))))]
                        (response generic-ok-text)
                        (response (format no-player-found-text (:name command-params))))
        :listplayers (let [players (gamestate/list-players)]
                       (if (seq players)
                         (response (->> (map players/player-to-string players)
                                        (str/join "\n")))
                         (response no-players-text)))
        :deal (if-let [round (gamestate/deal-round!)]
                (response {:response_type "in_channel"
                           :text (format-card-round round)})
                (response no-players-text))
        :roll (response (dice/interpret-roll-command (:dice-option command-params)))))))
