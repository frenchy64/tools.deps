;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:skip-wiki true}
  clojure.tools.deps.extensions.lein
  (:require
    [clojure.java.io :as jio]
    [clojure.tools.deps :as deps]
    [clojure.tools.deps.extensions :as ext]
    [clojure.tools.deps.extensions.local :as jar]
    [clojure.tools.deps.util.dir :as dir]
    [clojure.tools.deps.util.io :as io]
    [clojure.tools.deps.util.session :as session])
  (:import
    [java.io File]))

(set! *warn-on-reflection* true)

#_
(defn- deps-map
  [config dir]
  (let [f (jio/file dir "deps.edn")]
    (session/retrieve
      {:deps :map :file (.getAbsolutePath f)} ;; session key
      #(if (.exists f)
         (deps/merge-edns [(deps/root-deps) (deps/slurp-deps f)])
         (deps/root-deps)))))

(defmethod ext/coord-deps :lein
  [_lib {:keys [deps/root] :as _coord} _mf config]
  ;;TODO
  #_
  (dir/with-dir (jio/file root)
    (seq (:deps (deps-map config root)))))

(defmethod ext/coord-paths :lein
  [_lib {:keys [deps/root] :as _coord} _mf config]
  #_
  (dir/with-dir (jio/file root)
    (->> (:paths (deps-map config root))
      (map #(dir/canonicalize (jio/file %)))
      (map #(do
              (when-not (dir/sub-path? %)
                (throw (ex-info (str "Path " % " external to project " root))))
              %))
      (map #(.getCanonicalPath ^File %))
      vec)))

(defmethod ext/manifest-file :lein
  [_lib {:keys [deps/root] :as _coord} _mf _config]
  (let [manifest (jio/file root "project.clj")]
    (when (.exists manifest)
      (.getAbsolutePath manifest))))

(defmethod ext/coord-usage :lein [lib {:keys [deps/root] :as _coord} manifest-type config]
  #_
  (dir/with-dir (jio/file root)
    (:tools/usage (deps-map config root))))

(defmethod ext/prep-command :lein [lib {:keys [deps/root] :as _coord} manifest-type config]
  (dir/with-dir (jio/file root)
    {:prep/ensure ".clojure/tools.deps/prepped.jar"
     :prep/fn 'clojure.tools.deps.script.prep-lein/exec}))

(comment
  (ext/coord-deps 'org.clojure/core.async {:deps/root "../core.async" :deps/manifest :pom}
    :pom {:mvn/repos maven/standard-repos})
  (ext/coord-deps 'reifyhealth/lein-git-down {:deps/root "../lein-git-down" :deps/manifest :pom}
    :lein {:mvn/repos clojure.tools.deps.util.maven/standard-repos})

  (ext/coord-paths 'org.clojure/core.async {:deps/root "../core.async" :deps/manifest :pom}
    :pom {:mvn/repos maven/standard-repos})
  )
