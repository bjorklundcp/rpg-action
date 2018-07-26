(ns rpg-action.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [rpg-action.dice :as dice]
            [rpg-action.utils :as utils]))

(defroutes app-routes
  (GET "/" [] (let [roll (dice/process-roll 8 true)]
                 (str "Refresh for another d8! roll<br>" roll " = " (reduce + roll))))
  (POST "/command" request
    (case (get-in request [:params :text])
      "help" (response {:text (utils/long-str "Valid commands: save, list, draw, <action name>"
                                        "To save a new action: /rpgaction save <action name> <dice notation> <modifiers>"
                                        "To list your saved actions: /rpgaction list"
                                        "To draw initiation cards: /rpgaction draw"
                                        "To use one of your saved actions: /rpgaction <action name> <modifiers>")})
             (response {:text "Command not found... I think you're doing it wrong... :face_with_rolling_eyes:"})))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-json-response
      (wrap-defaults api-defaults)))
