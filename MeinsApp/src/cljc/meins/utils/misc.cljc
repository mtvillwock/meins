(ns meins.utils.misc
  (:require [clojure.string :as s]
            [meins.specs]))

(defn duration-string
  "Format duration string from seconds."
  [seconds]
  (let [hours (int (/ seconds 3600))
        seconds (rem seconds 3600)
        min (int (/ seconds 60))
        sec (int (rem seconds 60))]
    (s/trim
      (str (when (pos? hours) (str hours "h "))
           (when (pos? min) (str min "m "))
           (when (pos? sec) (str sec "s"))))))
