;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:skip-wiki true}
  clojure.tools.deps.script.prep-lein
  (:require
    [clojure.java.io :as jio]
    [clojure.pprint :as pp]
    [clojure.walk :as walk]
    [clojure.tools.deps :as deps]
    [clojure.tools.deps.extensions.git :as git]
    [clojure.tools.deps.util.io :refer [printerrln]]
    [clojure.tools.gitlibs :as gitlibs]
    [clojure.tools.cli :as cli]
    [clojure.java.shell :as sh])
  (:import
    [clojure.lang IExceptionInfo]
    [java.io File]))

(def ^:private opts
  [])

(defn exec
  [{:keys []}]
  (try
    (let [{:keys [exit out err]} (sh/sh "lein" "jar")]
      (if-not (zero? exit)
        (do (printerrln out)
            (printerrln err)
            (printerrln "Error prepping lein dep.")
            1)
        (let [jars (into [] (filter #(str/ends-with? (File/.getName %) ".jar"))
                         (file-seq "target"))]
          (case (count jars)
            0 (do (printerrln "No jars found in target directory.")
                  1)
            1 (let [built-jar (first jars)
                    prepped-jar ".clojure/tools.deps/prepped.jar"]
                (io/make-parents prepped-jar)
                (io/copy built-jar prepped-jar)
                0)
            (do (printerrln "Multiple jars found in target directory:" (mapv File/.getPath jars))
                1)))))
    (catch Throwable t
      (printerrln "Error prepping lein dep." (.getMessage t))
      (when-not (instance? IExceptionInfo t)
        (.printStackTrace t))
      1)))

(defn -main
  "Main entry point for prep-lein script."
  [& args]
  (let [{:keys [options]} (cli/parse-opts args opts)]
    (System/exit (exec options))))

(comment
  (exec {})
  )
