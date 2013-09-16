(ns conclujon.basic-test
  (:require [conclujon.core :refer [conclujon-spec]]))

(defn greeting [{name :name}] (str "Hello " name "!"))

(conclujon-spec basic-test)
