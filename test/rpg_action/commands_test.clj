(ns rpg-action.commands-test
  (:require [clojure.test :refer :all]
            [rpg-action.models.commands :as commands]
            [clojure.spec.alpha :as s]))

(deftest help
  (is (s/valid? ::commands/command [:help]))
  (is (s/valid? ::commands/command [:help :gm]))
  (is (s/valid? ::commands/command [:help :dice]))
  (is (not (s/valid? ::commands/command [:help :spoon]))))