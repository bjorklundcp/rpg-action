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
            [rpg-action.gamestate :as gamestate]))

(use-fixtures :each players-test/clean-players-fixture cards-test/death-wagon-fixture cards-test/stack-deck-fixture)

(deftest commands
  (testing "help command"
    (let [response (app (mock/request :post "/slack/command" {:text "help"}))]
      (is (= slack/generic-help-text (-> (get-in response [:body])
                                         (cheshire/parse-string true)
                                         :text)))))
  (testing "add/remove player command"
    (let [response (app (mock/request :post "/slack/command" {:text "addplayer bobhuggins @bobhuggins"}))]
      (is (= slack/generic-ok (-> (get-in response [:body])
                                  (cheshire/parse-string true)
                                  :text))))
    (let [response (app (mock/request :post "/slack/command" {:text "removeplayer bobhuggins"}))]
      (is (= slack/generic-ok (-> (get-in response [:body])
                                  (cheshire/parse-string true)
                                  :text))))
    (let [response (app (mock/request :post "/slack/command" {:text "addplayer bobhuggins @bobhuggins +3"}))]
      (is (= slack/generic-ok (-> (get-in response [:body])
                                  (cheshire/parse-string true)
                                  :text))))
    (let [response (app (mock/request :post "/slack/command" {:text "listplayers"}))]
      (is (= (utils/long-str "Character: Thog Player: @abel Modifier: Adv 1"
                             "Character: bobhuggins Player: @bobhuggins Modifier: Adv 3"
                             "Character: Teclis Player: @cbjorklund Modifier: Adv 2"
                             "Character: Myzrael Player: @chardizzeroony Modifier: Adv 1"
                             "Character: Squiddlykins Player: @chartwig Modifier: Disadv  2"
                             "Character: Ulruk Player: @res378 Modifier: Adv 1")
             (-> (get-in response [:body])
                 (cheshire/parse-string true)
                 :text))))
    (let [response (app (mock/request :post "/slack/command" {:text "removeplayer @bobhuggins"}))]
      (is (= slack/generic-ok (-> (get-in response [:body])
                                  (cheshire/parse-string true)
                                  :text))))
    (let [response (app (mock/request :post "/slack/command" {:text "removeplayer @bobhuggins"}))]
      (is (= (format slack/no-player-found-text "@bobhuggins")
             (-> (get-in response [:body])
                 (cheshire/parse-string true)
                 :text)))))

  (testing "rounds for dayz"
    (let [response (app (mock/request :post "/slack/command" {:text "deal"}))]
      (is (= (utils/long-str gamestate/new-round-str
                             "Myzrael (@chardizzeroony) :king::spades:"
                             "Ulruk (@res378) :king::clubs:"
                             "Thog (@abel) :eight::spades:"
                             "Teclis (@cbjorklund) :five::diamonds:"
                             "Squiddlykins (@chartwig) :two::spades:")
             (-> (get-in response [:body])
                 (cheshire/parse-string true)
                 :text)))))

  (testing "unknown command"
    (let [response (app (mock/request :post "/slack/command" {:text "onion"}))]
      (is (= slack/not-found-text (-> (get-in response [:body])
                                      (cheshire/parse-string true)
                                      :text))))))