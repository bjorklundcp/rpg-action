(ns rpg-action.dice)

(defn roll
  "Executes a dice roll. The kind of dice being rolled is the max"
  [max]
  (+ (rand-int max) 1))

(defn process-roll
  "Processes a roll with options.
  Max is required. It is the type of dice being rolled.
  The explode? option will re-roll dice that = max and return a list of rolls"
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
