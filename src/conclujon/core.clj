(ns conclujon.core
  (:require [net.cgrand.enlive-html :refer [select attr? pred add-class at emit* html-resource]])
  (:require [clojure.java.io :refer [file make-parents]])
  (:require [clojure.string :refer [split]])
  (:require [clojure.test :refer [deftest is]]))

(defn attr-starting-with? [s]
  "Selector predicate, matches all nodes with attributes starting with s."
  (pred #(some (fn [a] (.startsWith (name a) s)) (-> % :attrs keys))))

(defn all-conclujon-nodes [resource]
  "Returns a list of all nodes in a given resource that have a conclujon attribute."
  (select resource [(attr-starting-with? "data-conclujon:")]))

(defn select-by-attr [attr resource]
  "Returns a list of all nodes in resource that have an attribute attr."
  (select resource [(attr? attr)]))

(defn assoc-input-node? [node]
  "Returns true for nodes with the assoc-input attribute."
  (contains? (-> node :attrs keys set) :data-conclujon:assoc-input))

(defn assert-equals-node? [node]
  "Returns true for nodes with the assert-equals attribute."
  (contains? (-> node :attrs keys set) :data-conclujon:assert-equals))

(defn create-batches [resource] 
  (loop [collector [] nodes (all-conclujon-nodes resource)]
    (let [split-before-assert-node (split-with assoc-input-node? nodes)
          input-nodes (first split-before-assert-node)
          split-after-assert-node (split-with assert-equals-node? (second split-before-assert-node))
          assert-nodes (first split-after-assert-node)
          batch {:input-nodes input-nodes :assert-nodes assert-nodes}
          remaining-nodes (second split-after-assert-node)]
      (if (seq remaining-nodes)
        (recur (conj collector batch) remaining-nodes)
        (conj collector batch)))))

(defn input-node-to-key-value [input-node]
  (let [key (-> input-node :attrs :data-conclujon:assoc-input (subs 1) keyword)
        value (-> input-node :content first)]
    {key value}))

(defn assert-node-to-function [assert-node]
  (let [function-name (-> assert-node :attrs :data-conclujon:assert-equals symbol)]
    (resolve function-name)))

(defn with-assert-function [assert-node]
  {:assert-node assert-node :assert-function (assert-node-to-function assert-node)})

(defn with-result [input-map {:keys [assert-node assert-function] :as assert-map}]
  (let [expected (-> assert-node :content first)
        actual (assert-function input-map)
        passed (= expected actual)]
    (merge assert-map {:actual actual :expected expected :passed passed})))

(defn process-batch [{:keys [input-nodes assert-nodes]}]
  (let [input-map (reduce into {} (map input-node-to-key-value input-nodes))
        with-assert-functions (map with-assert-function assert-nodes)
        with-results (map (partial with-result input-map) with-assert-functions)]
    with-results))

(defn process-batches [batches]
  (apply concat (map process-batch batches)))

(defn apply-test-result [node result]
  (let [class (if (:passed result) "passed" "failed")]
    ((add-class class) node)))

(defn create-result-transformation [results]
  (fn [node]
    (let [result (first @results)]
      (swap! results rest)
      (apply-test-result node result))))

(defn has-failures? [result]
  (some false? (map :passed result)))

(defn process-specification [resource]
  (let [batches (create-batches resource)
        results (process-batches batches)
        failed? (has-failures? results)
        result-transformation (create-result-transformation (atom results))
        assert-selector (pred assert-equals-node?)
        html-output (apply str (emit* (at resource [assert-selector] result-transformation)))]
    {:failed? failed? :output html-output}))

(defn namespace-to-filename []
  (let [current-ns (str *ns*)
        components (map (fn [s] (clojure.string/replace s \- \_)) (split current-ns #"\."))
        file-name (str (last components) ".html")
        file-path (concat (butlast components) [file-name])]
    (str (apply file file-path))))

(defmacro conclujon-spec [name]
  (let [filename (namespace-to-filename)
        output-file (str (file "target" filename))
        resource (html-resource filename)
        result (process-specification resource)]
    `(let [resource# (html-resource ~filename)
           result# (process-specification resource#)]
       (make-parents ~output-file)
       (spit ~output-file (:output result#))
       (deftest ~name
         (is (not (:failed? result#)))))))
