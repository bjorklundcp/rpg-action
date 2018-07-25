(ns rpg-action.dice)

(defn roll
  [max]
  (+ (rand-int max) 1))

(defn process-roll
  ([max]
   (process-roll max false))
  ([max explode?]
   (if explode?
     (loop [last-roll (roll max)
            rolls [last-roll]]
       (if (= last-roll max)
         (let [new-roll (roll max)]
           (recur new-roll (conj rolls new-roll)))
         rolls))
     (roll max))))
