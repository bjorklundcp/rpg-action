(ns rpg-action.gamestate
  (:require [rpg-action.models.cards :as cards]
            [rpg-action.models.players :as players]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.pprint :as pprint]))

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

(defn list-players
  []
  (->> (:players @state)
       (sort-by :player-name)))

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

(defn equals-ignore-case?
  [a b]
  (= (str/lower-case a) (str/lower-case b)))

(defn remove-player-by-character-name!
  [character-name]
  (let [players-to-remove (filter #(equals-ignore-case? (:character-name %) character-name) (:players @state))]
    (swap! state
           (fn [orig-state]
             (update orig-state
                     :players
                     (fn [players]
                       (remove #(equals-ignore-case? (:character-name %) character-name) players)))))
    players-to-remove))

(s/fdef remove-player-by-character-name!
        :args (s/cat :character-name string?))

(defn remove-player-by-player-name!
  [player-name]
  (let [players-to-remove (filter #(equals-ignore-case? (:player-name %) player-name) (:players @state))]
    (swap! state
           (fn [orig-state]
             (update orig-state
                     :players
                     (fn [players]
                       (remove #(equals-ignore-case? (:player-name %) player-name) players)))))
    players-to-remove))
(s/fdef remove-player-by-player-name!
        :args (s/cat :player-name string?))

(s/def ::turn (s/keys :req-un [::cards/card ::players/player]))
(s/def ::turns (s/coll-of ::turn))

(def new-round-str "New Round:")

(defn deal-round!
  []
  (when (seq (:players @state))
    (let [cards (->> (draw-cards! (reduce #(+ %1
                                              (get-in %2 [:card-modifier :number]))
                                          0
                                          (:players @state)))
                     (into []))]
      (->> (reduce (fn [{:keys [i] :as acc} player]
                     (let [num-cards (get-in player [:card-modifier :number])
                           positive? (get-in player [:card-modifier :positive?])
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
           (reverse)))))
(s/fdef deal-round!
        :ret (s/nilable ::turns))