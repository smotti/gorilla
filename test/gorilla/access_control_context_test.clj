(ns gorilla.access-control-context-test
  (:import java.lang.IllegalArgumentException
           (com.acciente.oacc Resource))
  (:require [gorilla.access-control-context :as sut]
            [gorilla.test-fixtures :refer [make-access-control-ctx make-admin
                                           make-role make-service make-user
                                           set-cls-permissions
                                           set-create-permissions
                                           set-rsrc-permissions
                                           with-sqlite-db]]
            [clojure.test :as t]))

(t/use-fixtures :each with-sqlite-db)

(t/deftest test-authenticate
  (t/testing "authenticatable admin"
    (let [admin (make-admin "auth-admin")
          acc (make-access-control-ctx)]
      (t/is (sut/authenticate acc (:name admin) (:password admin)))))
  (t/testing "authenticatable service"
    (let [service (make-service "auth-service")
          acc (make-access-control-ctx)]
      (t/is (sut/authenticate acc (:name service) (:password service)))))
  (t/testing "unauthenticatable role"
    (let [role (make-role "unauth-role")
          acc (make-access-control-ctx)]
      (t/is (not (sut/authenticate acc (:name role) "none")))))
  (t/testing "authenticatable user"
    (let [user (make-user "auth-user")
          acc (make-access-control-ctx)]
      (t/is (sut/authenticate acc (:name user) (:password user))))))

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
        (t/is (not (sut/has-permission? "*DELETE"
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

(t/deftest test-remove-resource
  (let [admin-cls "ADMIN"
        role-cls "ROLE"
        service-cls "SERVICE"
        user-cls "USER"
        acc (make-access-control-ctx)
        admin-builder (comp #(set-cls-permissions ["*DELETE"] % admin-cls)
                            #(set-cls-permissions ["*DELETE"] % role-cls)
                            #(set-cls-permissions ["*DELETE"] % service-cls)
                            #(set-cls-permissions ["*DELETE"] % user-cls)
                            make-admin)
        admin (admin-builder "fst-admin")
        role (make-role "test-role")
        service (make-service "test-service")
        user (make-user "test-user")]
    (t/testing "role can't remove admin"
      (t/is (not (sut/remove-resource acc
                                      (:resource role)
                                      (:resource admin)))))
    (t/testing "role can't remove role"
      (let [another-role (make-role "another-role")]
        (t/is (not (sut/remove-resource acc
                                        (:resource role)
                                        (:resource another-role))))))
    (t/testing "role can't remove service"
      (t/is (not (sut/remove-resource acc
                                      (:resource role)
                                      (:resource service)))))
    (t/testing "role can't remove user"
      (t/is (not (sut/remove-resource acc
                                      (:resource role)
                                      (:resource user)))))
    (t/testing "service can't remove admin"
      (t/is (not (sut/remove-resource acc
                                      (:resource service)
                                      (:resource admin)))))
    (t/testing "service can't remove role"
      (t/is (not (sut/remove-resource acc
                                      (:resource service)
                                      (:resource role)))))
    (t/testing "service can't remove service"
      (let [another-svc (make-service "another-service")]
        (t/is (not (sut/remove-resource acc
                                        (:resource service)
                                        (:resource another-svc))))))
    (t/testing "service can't remove user"
      (t/is (not (sut/remove-resource acc
                                      (:resource service)
                                      (:resource user)))))
    (t/testing "user can't remove admin"
      (t/is (not (sut/remove-resource acc
                                      (:resource user)
                                      (:resource admin)))))
    (t/testing "user can't remove role"
      (t/is (not (sut/remove-resource acc
                                      (:resource user)
                                      (:resource role)))))
    (t/testing "user can't remove service"
      (t/is (not (sut/remove-resource acc
                                      (:resource user)
                                      (:resource service)))))
    (t/testing "user can't remove user"
      (let [another-user (make-user "another-user")]
        (t/is (not (sut/remove-resource acc
                                        (:resource user)
                                        (:resource another-user))))))
    (t/testing "admin can remove admin"
      (let [another-admin (make-admin "another-admin")]
        (t/is (sut/remove-resource acc
                                   (:resource admin)
                                   (:resource another-admin)))))
    (t/testing "admin can remove role"
      (t/is (sut/remove-resource acc
                                 (:resource admin)
                                 (:resource role))))
    (t/testing "admin can remove service"
      (t/is (sut/remove-resource acc
                                 (:resource admin)
                                 (:resource service))))
    (t/testing "admin can remove user"
      (t/is (sut/remove-resource acc
                                 (:resource admin)
                                 (:resource user))))))
