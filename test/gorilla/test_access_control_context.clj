(ns gorilla.test-access-control-context
  (:import java.lang.IllegalArgumentException)
  (:require [gorilla.access-control-context :as sut]
            [gorilla.test-fixtures :refer [make-access-control-ctx make-admin
                                           make-role make-service make-user
                                           with-sqlite-db]]
            [clojure.test :as t]))

(t/use-fixtures :once with-sqlite-db)

(t/deftest test-authenticate
  (t/testing "authenticatable admin"
    (let [admin (make-admin "auth-admin")
          acc (make-access-control-ctx)]
      (println admin)
      (t/is (nil? (sut/authenticate acc (:name admin) (:password admin))))))
  (t/testing "authenticatable service")
  (t/testing "unauthenticatable role")
  (t/testing "authenticatable user"))
