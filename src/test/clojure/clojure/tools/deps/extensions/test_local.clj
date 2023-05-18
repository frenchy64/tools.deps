;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.deps.extensions.test-local
  (:require
    [clojure.java.io :as jio]
    [clojure.test :refer [are deftest testing]]
    [clojure.tools.deps.extensions :as ext]
    [clojure.tools.deps.extensions.local]
    [clojure.tools.deps.test-deps :refer [*test-dir* with-test-dir]])
  (:import
    [clojure.lang ExceptionInfo]
    [java.io File]))

(set! *warn-on-reflection* true)

(defn- existing-file
  ^File [path]
  (-> (jio/file *test-dir* path)
      (doto .createNewFile (-> .isFile assert))))

(deftest manifest-type
  (testing "degenerate cases"
    (are [coord root]
      (thrown-with-msg?
        ExceptionInfo
        (re-pattern (str "^Local lib com.example/my-lib not found: " root "$"))
        (ext/manifest-type 'com.example/my-lib coord {}))

      {:local/root "non/existing"} "non/existing"

      {:local/root "non/existing" :deps/root "either"} "non/existing/either"))

  (testing "provided manifest"
    (are [coord expected]
      (= expected (ext/manifest-type 'com.example/my-lib coord {}))

      {:local/root "a" :deps/manifest :fkn}
      {:deps/root "a" :deps/manifest :fkn}

      {:local/root "a" :deps/root "b" :deps/manifest :fkn}
      {:deps/root "a/b" :deps/manifest :fkn}))

  (testing "jar file"
    (with-test-dir
      (let [a-manifest (existing-file "a/a.jar")
            b-manifest (existing-file "a/b/b.jar")]

        (are [coord expected]
          (= expected (ext/manifest-type 'com.example/my-lib coord {}))

          {:local/root (.getAbsolutePath a-manifest)}
          {:deps/root (.getAbsolutePath a-manifest) :deps/manifest :jar}

          ;; local/root is already a jar then ignore deps/root
          {:local/root (.getAbsolutePath a-manifest) :deps/root "b/b.jar"}
          {:deps/root (.getAbsolutePath a-manifest) :deps/manifest :jar}

          {:local/root (-> a-manifest .getParentFile .getAbsolutePath) :deps/root "b/b.jar"}
          {:deps/root (.getAbsolutePath b-manifest) :deps/manifest :jar}))))

  (testing "detected manifest"
    (with-test-dir
      (let [a-manifest (existing-file "a/deps.edn")
            b-manifest (existing-file "a/b/pom.xml")]

        (are [coord expected]
          (= expected (ext/manifest-type 'com.example/my-lib coord {}))

          {:local/root (-> a-manifest .getParentFile .getAbsolutePath)}
          {:deps/root (-> a-manifest .getParentFile .getAbsolutePath) :deps/manifest :deps}

          {:local/root (-> a-manifest .getParentFile .getAbsolutePath) :deps/root "b"}
          {:deps/root (-> b-manifest .getParentFile .getAbsolutePath) :deps/manifest :pom})))))
