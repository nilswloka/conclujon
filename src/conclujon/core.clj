(ns conclujon.core
  (:require [net.cgrand.enlive-html :refer [do-> select attr? pred add-class after at emit* html-resource html-snippet]])
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
  "Creates batches of assoc-input and assert-equals nodes so that
   assert-equals functions will called with an input map built from
   all assoc-inputs appearing prior to that assert-equal node."
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
  "Creates an entry for the input map from an assoc-input node."
  (let [key (-> input-node :attrs :data-conclujon:assoc-input (subs 1) keyword)
        value (-> input-node :content first)]
    {key value}))

(defn assert-node-to-function [assert-node]
  "Resolves the assert-equals function inside the current namespace."
  (let [function-name (-> assert-node :attrs :data-conclujon:assert-equals symbol)]
    (resolve function-name)))

(defn with-assert-function [assert-node]
  "Transforms assert-equals node to map containing node and assert-function."
  {:assert-node assert-node :assert-function (assert-node-to-function assert-node)})

(defn with-result [input-map {:keys [assert-node assert-function] :as assert-map}]
  "Merges result of assertion into result map."
  (let [expected (-> assert-node :content first)
        actual (assert-function input-map)
        passed (= expected actual)]
    (merge assert-map {:actual actual :expected expected :passed passed})))

(defn process-batch [{:keys [input-nodes assert-nodes]}]
  "Processes a single batch of assoc-input and assert-equal nodes."
  (let [input-map (reduce into {} (map input-node-to-key-value input-nodes))
        with-assert-functions (map with-assert-function assert-nodes)
        with-results (map (partial with-result input-map) with-assert-functions)]
    with-results))

(defn process-batches [batches]
  "Processes all batches and merges the result into a single result map."
  (apply concat (map process-batch batches)))

(defn apply-test-result [node result]
  "Applies the test result to a assert-equals node."
  (let [passed? (:passed result)
        class (if passed? "passed" "failed")
        add-result-class (add-class class)
        actual (:actual result)
        add-actual (after (html-snippet "<span class='actual'>(actual: " actual ")</span>"))]
    (if passed?
      (add-result-class node)
      (-> node add-result-class add-actual))))

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
