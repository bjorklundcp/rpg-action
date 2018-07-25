(ns rpg-action.dice)

(defn roll
  [max]
  (+ (rand-int max) 1))

(defn process-roll
  ([max]
   (process-roll max false))
  ([max explode?]
   (if explode?
     (let [last-roll (atom (roll max))
           all-rolls (atom (conj [] @last-roll))]
       (while (= @last-roll max)
         (do
           (reset! last-roll (roll max))
           (reset! all-rolls (conj @all-rolls @last-roll))))
       @all-rolls)
     (roll max))))
