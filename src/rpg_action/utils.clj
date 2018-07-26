(ns rpg-action.utils
  (:require [clojure.string :as string]))

(defn long-str
  "Append a bunch of strings into one giant one broken up with new lines"
  [& strings]
  (string/join "\n" strings))

