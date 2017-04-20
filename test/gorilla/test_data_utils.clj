(ns gorilla.test-data-utils
  (:import com.acciente.oacc.PasswordCredentials)
  (:require  [clojure.test :as t]
             [clojure.test.check.generators :as gen]))

(declare password)

(def admin
  (gen/hash-map :password password))

(def password
  (gen/fmap #(PasswordCredentials/newInstance (.toCharArray %))
            (gen/not-empty gen/string-alphanumeric)))

(def service
  (gen/hash-map :password password))

(def user
  (gen/hash-map :password password))
