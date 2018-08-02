(ns rpg-action.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [rpg-action.dice :as dice]
            [rpg-action.utils :as utils]
            [rpg-action.models.commands :as commands]
            [rpg-action.communications.slack :as cm-slack]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.pprint :as pprint]))

(defroutes app-routes
  (context "/slack" []
    (-> #'cm-slack/slack-routes
        cm-slack/wrap-slack))
  (GET "/" [] (let [roll (dice/process-roll 8 true)]
                 (str "Refresh for another d8! roll<br>" roll " = " (reduce + roll))))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-json-response
      (wrap-defaults api-defaults)))

