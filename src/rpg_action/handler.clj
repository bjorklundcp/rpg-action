(ns rpg-action.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [rpg-action.dice :as dice]
            [rpg-action.communications.slack :as cm-slack]
            [clojure.java.io :refer [input-stream reader]])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream InputStream)))

(defroutes app-routes
  (context "/slack" []
    (-> #'cm-slack/slack-routes
        cm-slack/wrap-slack-command
        cm-slack/wrap-slack-verify-token))
  (GET "/" [] (let [roll (dice/roll 8 true)]
                (str "Refresh for another d8! roll<br>" roll " = " (reduce + roll))))
  (route/not-found "Not Found"))

(defn slurp-bytes [input]
  (with-open [reader (input-stream input)]
    (let [bais (ByteArrayOutputStream.)]
      (loop []
        (let [b (.read reader)]
          (when-not (= -1 b)
            (.write bais b)
            (recur))))
      (.toByteArray bais))))

(defn copy-body [h]
  (fn [r]
    (if (instance? InputStream (:body r))
      (let [ba (slurp-bytes (:body r))]
        (h (assoc r
             :body (ByteArrayInputStream. ba)
             :original-body (ByteArrayInputStream. ba))))
      (h (assoc r :original-body (:body r))))))

(def app
  (-> app-routes
      wrap-json-response
      (wrap-defaults api-defaults)
      copy-body))
