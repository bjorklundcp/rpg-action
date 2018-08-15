(ns rpg-action.models.cards
  (:require [clojure.spec.alpha :as s]))

(def repr-map {{:rank :2 :suit :clubs}     ":two::clubs:"
               {:rank :3 :suit :clubs}     ":three::clubs:"
               {:rank :4 :suit :clubs}     ":four::clubs:"
               {:rank :5 :suit :clubs}     ":five::clubs:"
               {:rank :6 :suit :clubs}     ":six::clubs:"
               {:rank :7 :suit :clubs}     ":seven::clubs:"
               {:rank :8 :suit :clubs}     ":eight::clubs:"
               {:rank :9 :suit :clubs}     ":nine::clubs:"
               {:rank :10 :suit :clubs}    ":ten::clubs:"
               {:rank :J :suit :clubs}     ":jack::clubs:"
               {:rank :Q :suit :clubs}     ":queen::clubs:"
               {:rank :K :suit :clubs}     ":king::clubs:"
               {:rank :A :suit :clubs}     ":a::clubs:"
               {:rank :2 :suit :diamonds}  ":two::diamonds:"
               {:rank :3 :suit :diamonds}  ":three::diamonds:"
               {:rank :4 :suit :diamonds}  ":four::diamonds:"
               {:rank :5 :suit :diamonds}  ":five::diamonds:"
               {:rank :6 :suit :diamonds}  ":six::diamonds:"
               {:rank :7 :suit :diamonds}  ":seven::diamonds:"
               {:rank :8 :suit :diamonds}  ":eight::diamonds:"
               {:rank :9 :suit :diamonds}  ":nine::diamonds:"
               {:rank :10 :suit :diamonds} ":ten::diamonds:"
               {:rank :J :suit :diamonds}  ":jack::diamonds:"
               {:rank :Q :suit :diamonds}  ":queen::diamonds:"
               {:rank :K :suit :diamonds}  ":king::diamonds:"
               {:rank :A :suit :diamonds}  ":a::diamonds:"
               {:rank :2 :suit :hearts}    ":two::hearts:"
               {:rank :3 :suit :hearts}    ":three::hearts:"
               {:rank :4 :suit :hearts}    ":four::hearts:"
               {:rank :5 :suit :hearts}    ":five::hearts:"
               {:rank :6 :suit :hearts}    ":six::hearts:"
               {:rank :7 :suit :hearts}    ":seven::hearts:"
               {:rank :8 :suit :hearts}    ":eight::hearts:"
               {:rank :9 :suit :hearts}    ":nine::hearts:"
               {:rank :10 :suit :hearts}   ":ten::hearts:"
               {:rank :J :suit :hearts}    ":jack::hearts:"
               {:rank :Q :suit :hearts}    ":queen::hearts:"
               {:rank :K :suit :hearts}    ":king::hearts:"
               {:rank :A :suit :hearts}    ":a::hearts:"
               {:rank :2 :suit :spades}    ":two::spades:"
               {:rank :3 :suit :spades}    ":three::spades:"
               {:rank :4 :suit :spades}    ":four::spades:"
               {:rank :5 :suit :spades}    ":five::spades:"
               {:rank :6 :suit :spades}    ":six::spades:"
               {:rank :7 :suit :spades}    ":seven::spades:"
               {:rank :8 :suit :spades}    ":eight::spades:"
               {:rank :9 :suit :spades}    ":nine::spades:"
               {:rank :10 :suit :spades}   ":ten::spades:"
               {:rank :J :suit :spades}    ":jack::spades:"
               {:rank :Q :suit :spades}    ":queen::spades:"
               {:rank :K :suit :spades}    ":king::spades:"
               {:rank :A :suit :spades}    ":a::spades:"
               :black-joker                ":joker:"
               :red-joker                  ":joker:"})

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
