(ns rpg-action.slack-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [rpg-action.handler :refer :all]
            [rpg-action.communications.slack :as slack]
            [cheshire.core :as cheshire]))

(deftest commands
  (testing "help command"
    (let [response (app (mock/request :post "/slack/command" {:text "help"}))]
      (is (= slack/help-text (-> (get-in response [:body])
                                 (cheshire/parse-string true)
                                 :text)))))
  (testing "unknown command"
    (let [response (app (mock/request :post "/slack/command" {:text "onion"}))]
      (is (= slack/not-found-text (-> (get-in response [:body])
                                      (cheshire/parse-string true)
                                      :text))))))