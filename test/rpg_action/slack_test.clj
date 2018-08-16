(ns rpg-action.slack-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [rpg-action.handler :refer :all]
            [rpg-action.communications.slack :as slack]
            [cheshire.core :as cheshire]
            [rpg-action.cards-test :as cards-test]
            [rpg-action.players-test :as players-test]
            [clojure.spec.test.alpha :as st]
            [rpg-action.utils :as utils]
            [rpg-action.gamestate :as gamestate]
            [environ.core :refer [env]]
            [pandect.algo.sha256 :as sha256]))

(use-fixtures :each players-test/clean-players-fixture cards-test/death-wagon-fixture cards-test/stack-deck-fixture)

(defn mock-request-with-credentials
  ([method uri]
   (mock-request-with-credentials method uri nil))
  ([method uri params]
   (let [current-time (System/currentTimeMillis)
         body (#'mock/encode-params params)
         computed-signature-basestring (-> (str slack/slack-api-version ":" current-time ":" body))
         signing-secret (env :slack-signing-secret)
         signature (str slack/slack-api-version "=" (sha256/sha256-hmac computed-signature-basestring signing-secret))]
     (-> (mock/request method uri params)
         (mock/header "X-Slack-Request-Timestamp" current-time)
         (mock/header "X-Slack-Signature" signature)))))

(deftest unauthorized-command
  (let [response (app (mock/request :post "/slack/command" {:text "help"}))]
    (is (= slack/forbidden-text (-> (get-in response [:body])
                                    (cheshire/parse-string true)
                                    :text)))
    (is (= 403 (:status response)))))

(deftest help-command
  (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "help"}))]
    (is (= slack/generic-help-text (-> (get-in response [:body])
                                       (cheshire/parse-string true)
                                       :text)))))
(deftest player-commands
  (testing "add a player, then remove it"
    (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "addplayer bobhuggins @bobhuggins"}))]
      (is (= slack/generic-ok-text (-> (get-in response [:body])
                                       (cheshire/parse-string true)
                                       :text))))
    (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "removeplayer bobhuggins"}))]
      (is (= slack/generic-ok-text (-> (get-in response [:body])
                                       (cheshire/parse-string true)
                                       :text)))))
  (testing "add it again with a modifier, list all, then remove"
    (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "addplayer bobhuggins @bobhuggins +3"}))]
      (is (= slack/generic-ok-text (-> (get-in response [:body])
                                       (cheshire/parse-string true)
                                       :text))))
    (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "listplayers"}))]
      (is (= (utils/long-str "Character: Thog Player: @abel Modifier: Adv 1"
                             "Character: bobhuggins Player: @bobhuggins Modifier: Adv 3"
                             "Character: Teclis Player: @cbjorklund Modifier: Adv 2"
                             "Character: Myzrael Player: @chardizzeroony Modifier: Adv 1"
                             "Character: Squiddlykins Player: @chartwig Modifier: Disadv  2"
                             "Character: Ulruk Player: @res378 Modifier: Adv 1")
             (-> (get-in response [:body])
                 (cheshire/parse-string true)
                 :text))))
    (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "removeplayer @bobhuggins"}))]
      (is (= slack/generic-ok-text (-> (get-in response [:body])
                                       (cheshire/parse-string true)
                                       :text)))))
  (testing "remove a player when does not exist"
    (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "removeplayer @bobhuggins"}))]
      (is (= (format slack/no-player-found-text "@bobhuggins")
             (-> (get-in response [:body])
                 (cheshire/parse-string true)
                 :text))))))


(deftest cards-commands
  (testing "rounds for dayz"
    (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "deal"}))]
      (is (= (utils/long-str gamestate/new-round-str
                             "Myzrael (@chardizzeroony) :king::spades:"
                             "Ulruk (@res378) :king::clubs:"
                             "Thog (@abel) :eight::spades:"
                             "Teclis (@cbjorklund) :five::diamonds:"
                             "Squiddlykins (@chartwig) :two::spades:")
             (-> (get-in response [:body])
                 (cheshire/parse-string true)
                 :text))))))


(deftest unknown-command
  (let [response (app (mock-request-with-credentials :post "/slack/command" {:text "onion"}))]
    (is (= slack/not-found-text (-> (get-in response [:body])
                                    (cheshire/parse-string true)
                                    :text)))))