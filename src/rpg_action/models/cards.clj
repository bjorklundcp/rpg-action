(ns rpg-action.models.cards
  (:require [clojure.spec.alpha :as s]))

(def ordered-suits [:clubs :diamonds :hearts :spades])
(def suits (set ordered-suits))

(def ordered-ranks [:2 :3 :4 :5 :6 :7 :8 :9 :10 :J :Q :K :A])
(def ranks (set ordered-ranks))

(def red-joker :red-joker)
(def black-joker :black-joker)

(s/def ::red-joker #(= % red-joker))
(s/def ::black-joker #(= % black-joker))

(s/def ::suit suits)
(s/def ::rank ranks)

(s/def ::regular-card (s/keys :req-un [::rank ::suit]))

(s/def ::card (s/or :red-joker ::red-joker
                    :black-joker ::black-joker
                    :regular-card ::regular-card))

(defn order-card
  [card]
  (cond
    (= card red-joker)
    53

    (= card black-joker)
    52

    :default
    (+ (* (.indexOf ordered-ranks (:rank card)) 4)
       (.indexOf ordered-suits (:suit card)))))
(s/fdef order-card
        :args (s/cat :card ::card)
        :ret int?)
;
;(defn compare-card
;  [x y]
;  (cond
;    (= x red-joker)
;    false
;
;    (= y red-joker)
;    true
;
;    (= x black-joker)
;    false
;
;    (= y black-joker)
;    true
;
;    (< (.indexOf ordered-ranks (:rank x))
;       (.indexOf ordered-ranks (:rank y)))
;    true
;
;    (> (.indexOf ordered-ranks (:rank x))
;       (.indexOf ordered-ranks (:rank y)))
;    false
;
;    :default
;    (< (.indexOf ordered-suits (:suit x))
;       (.indexOf ordered-suits (:suit y)))))

(def deck (conj (for [rank ordered-ranks
                      suit ordered-suits]
                  {:rank rank :suit suit})
                black-joker
                red-joker))
