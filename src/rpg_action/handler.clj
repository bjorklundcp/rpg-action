(ns rpg-action.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [rpg-action.dice :as dice]))

(defroutes app-routes
  (GET "/" [] (let [roll (dice/process-roll 8 true)]
                 (str "Refresh for another d8! roll<br>" roll " = " (reduce + roll))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes api-defaults))
