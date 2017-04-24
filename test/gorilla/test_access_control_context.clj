(ns gorilla.test-access-control-context
  (:import java.lang.IllegalArgumentException)
  (:require [gorilla.access-control-context :as sut]
            [gorilla.test-fixtures :refer [make-access-control-ctx make-admin
                                           make-role make-service make-user
                                           with-sqlite-db]]
            [clojure.test :as t]))

(t/use-fixtures :each with-sqlite-db)

(t/deftest test-authenticate
  (t/testing "authenticatable admin"
    (let [admin (make-admin "auth-admin")
          acc (make-access-control-ctx)]
      (t/is (nil? (sut/authenticate acc (:name admin) (:password admin))))))
  (t/testing "authenticatable service"
    (let [service (make-service "auth-service")
          acc (make-access-control-ctx)]
      (t/is (nil? (sut/authenticate acc (:name service) (:password service))))))
  (t/testing "unauthenticatable role"
    (let [role (make-role "unauth-role")
          acc (make-access-control-ctx)]
      (t/is (thrown? IllegalArgumentException
                     (sut/authenticate (:name role))))))
  (t/testing "authenticatable user"
    (let [user (make-user "auth-user")
          acc (make-access-control-ctx)]
      (t/is (nil? (sut/authenticate acc (:name user) (:password user)))))))
