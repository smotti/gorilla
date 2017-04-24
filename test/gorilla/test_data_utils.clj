(ns gorilla.test-data-utils
  (:require  [clojure.test :as t]
             [clojure.test.check.generators :as gen]))

(def password
  (gen/not-empty gen/string-alphanumeric))

(def admin
  (gen/hash-map :password password))

(def external-id
  (gen/not-empty gen/string-alphanumeric))

(def service
  (gen/hash-map :password password))

(def user
  (gen/hash-map :password password))
