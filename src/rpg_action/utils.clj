(ns rpg-action.utils
  (:require [clojure.string :as string]))

(def roll-regex #"(\d+)?(d\d+)(!)?")
(def modifier-and-number-regex #"([\+-])?(\d+)")
(def caret-regex #"\^")

(defn long-str
  "Append a bunch of strings into one giant one broken up with new lines"
  [& strings]
  (string/join "\n" strings))
