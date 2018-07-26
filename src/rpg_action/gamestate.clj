(ns rpg-action.gamestate
  (:require [rpg-action.models.cards :as cards]
            [rpg-action.models.players :as players]
            [clojure.spec.alpha :as s]))

(defonce state (atom {:players [] :deck (shuffle cards/deck)}))

(defn shuffle-deck!
  []
  (swap! state
         (fn [orig-state]
           (assoc orig-state
             :deck
             (shuffle cards/deck)))))

(defn draw-cards!
  [num]
  (if (> num (count (:deck @state)))
    (let [cards-pre-shuffle (draw-cards! (count (:deck @state)))]
      (shuffle-deck!)
      (concat cards-pre-shuffle (draw-cards! (- num (count (:deck @state))))))
    (let [cards (take num (:deck @state))]
      (swap! state
             (fn [orig-state]
               (update orig-state
                       :deck
                       #(drop num %))))
      (when (or (contains? (set cards) cards/red-joker)
                (contains? (set cards) cards/black-joker))
        (shuffle-deck!))
      cards)))
(s/fdef draw-cards!
        :args (s/and (s/cat :num int?)
                     #(> (:num %) 0)))

(defn add-player!
  [player]
  (swap! state
         (fn [orig-state]
           (update orig-state
                   :players
                   conj
                   player))))
(s/fdef add-player!
        :args (s/cat :player ::players/player))

(defn remove-player-by-character-name!
  [character-name]
  (swap! state
         (fn [orig-state]
           (update orig-state
                   :players
                   (fn [players]
                     (remove #(= (:character-name %) character-name) players))))))

(s/fdef remove-player-by-character-name!
        :args (s/cat :character-name string?))

(defn remove-player-by-slack-name!
  [slack-name]
  (swap! state
         (fn [orig-state]
           (update orig-state
                   :players
                   (fn [players]
                     (remove #(= (:slack-name %) slack-name) players))))))
(s/fdef remove-player-by-slack-name!
        :args (s/cat :slack-name string?))

(s/def ::turn (s/keys :req-un [::cards/card ::players/player]))
(s/def ::turns (s/coll-of ::turn))

(defn deal-round!
  []
  (let [cards (->> (draw-cards! (reduce #(+ %1
                                            (get-in %2 [:card-modifier :number] 1))
                                        0
                                        (:players @state)))
                   (into []))]
    (->> (reduce (fn [{:keys [i] :as acc} player]
                   (let [num-cards (get-in player [:card-modifier :number] 1)
                         positive? (get-in player [:card-modifier :positive?] true)
                         my-cards (subvec cards i (+ i num-cards))
                         my-sorted-cards (sort-by cards/order-card my-cards)
                         my-card (if positive?
                                   (last my-sorted-cards)
                                   (first my-sorted-cards))]
                     (-> acc
                         (update :results conj {:card my-card :player player})
                         (update :i + num-cards))))
                 {:i 0 :results []}
                 (:players @state))
         :results
         (sort-by (comp cards/order-card :card))
         (reverse))))
(s/fdef deal-round!
        :ret ::turns)
