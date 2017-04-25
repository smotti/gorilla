(ns gorilla.access-control-context-test
  (:import java.lang.IllegalArgumentException
           (com.acciente.oacc Resource))
  (:require [gorilla.access-control-context :as sut]
            [gorilla.test-fixtures :refer [make-access-control-ctx make-admin
                                           make-role make-service make-user
                                           set-create-permissions
                                           set-rsrc-permissions
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

(t/deftest test-make-resource
  (t/testing "make unauthenticatel resources"
    (let [acc (make-access-control-ctx)
          make-fn #(sut/make-resource acc
                                      (.getSessionResource acc)
                                      %
                                      (str "test-" %))]
      (doseq [cls ["ROLE"]]
        (t/is (instance? Resource (make-fn cls))))))
  (t/testing "make authenticatable resources"
    (let [acc (make-access-control-ctx)
          make-fn #(sut/make-resource acc
                                      (.getSessionResource acc)
                                      %
                                      (str "test-" %)
                                      (str "test-" %))]
      (doseq [cls ["ADMIN" "SERVICE" "USER"]]
        (t/is (instance? Resource (make-fn cls)))))))

(t/deftest test-has-permission
  (let [role (make-role "test-role")
        acc (make-access-control-ctx)]
    (t/testing "admin has permission to delete role"
      (let [admin-builder (comp #(set-rsrc-permissions ["*QUERY" "*DELETE"]
                                                       %
                                                       role)
                                make-admin)
            admin (admin-builder "test-admin")]
        (t/is (sut/has-permission? "*DELETE"
                                   acc
                                   (:resource admin)
                                   (:resource role)))))
    (t/testing "user doesn't have permission to delete role"
      (let [user (make-user "test-user")]
        (t/is (not (sut/has-permission? "*DELTE"
                                        acc
                                        (:resource user)
                                        (:resource role))))))))

(t/deftest test-has-create-permission
  (let [rsrc-cls "ROLE"
        acc (make-access-control-ctx)]
    (t/testing "admin has permission to create a role"
      (let [admin-builder (comp #(set-create-permissions ["*CREATE"]
                                                         %
                                                         rsrc-cls)
                                make-admin)
            admin (admin-builder "test-admin")]
        (t/is (sut/has-create-permission? acc
                                          (:resource admin)
                                          rsrc-cls))))
    (t/testing "user doesn't have permission to create a role"
      (let [user (make-user "test-user")]
        (t/is (not (sut/has-create-permission? acc
                                               (:resource user)
                                               rsrc-cls)))))))
