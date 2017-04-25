(ns gorilla.test-fixtures
  (:import java.sql.DriverManager
           java.io.File
           org.sqlite.JDBC
           com.acciente.oacc.sql.internal.SQLAccessControlSystemInitializer
           (com.acciente.oacc PasswordCredentials ResourcePermissions Resources)
           (com.acciente.oacc.sql SQLProfile SQLAccessControlContextFactory))
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.test.check.generators :refer [generate]]
            [gorilla.test-data-utils :as data]))

(declare initialize-oacc make-resource)

(def ^:dynamic *ACC* nil)

(def ^:dynamic *DB* nil)

(def DEFAULT-DOMAIN "APP_DOMAIN")

(def DEFAULT-RESOURCE-CLASSES [[1 "ADMIN" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                               [2 "ROLE" false false ["VIEW" "EDIT"]]
                               [3 "SERVICE" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                               [4 "USER" true false ["VIEW" "EDIT" "DEACTIVATE"]]])

(def OACC-PWD "oaccpwd")

(def rsrc-cls-id #(nth % 0))

(def rsrc-cls-name #(nth % 1))

(def rsrc-cls-auth #(nth % 2))

(def rsrc-cls-create #(nth % 3))

(def rsrc-cls-perms #(nth % 4))

(def SQLITE-CREATE-TABLES
  (.getFile (io/resource "oacc-db-2.0.0-rc.7/SQLite_3_8/create_tables.sql")))

(defn to-int
  [b]
  (int (if b 1 0)))

(defn create-tmp-db
  []
  (let [tmp-db-file (File/createTempFile "test_" ".sqlite")]
    (sh "/bin/sh"
        "-c"
        (str "sqlite3 "
             (.getAbsolutePath tmp-db-file)
             " < "
             SQLITE-CREATE-TABLES))
    tmp-db-file))

(defn initialize-oacc
  [db-conn]
  (SQLAccessControlSystemInitializer/initializeOACC db-conn
                                                    nil
                                                    (.toCharArray OACC-PWD))
  (let [acc (doto (SQLAccessControlContextFactory/getAccessControlContext
                   db-conn
                   nil
                   SQLProfile/SQLite_3_8_RECURSIVE)
              (.authenticate (Resources/getInstance 0)
                             (PasswordCredentials/newInstance (.toCharArray
                                                               OACC-PWD))))]
    (doseq [cls DEFAULT-RESOURCE-CLASSES]
      (.createResourceClass acc
                            (rsrc-cls-name cls)
                            (rsrc-cls-auth cls)
                            (rsrc-cls-create cls))
      (doseq [p (rsrc-cls-perms cls)]
        (.createResourcePermission acc
                                   (rsrc-cls-name cls)
                                   p)))
    (.createDomain acc DEFAULT-DOMAIN)))

(defn make-access-control-ctx
  []
  (doto (SQLAccessControlContextFactory/getAccessControlContext
         *DB*
         nil
         (SQLProfile/SQLite_3_8_RECURSIVE))
    (.authenticate (Resources/getInstance 0)
                   (PasswordCredentials/newInstance (.toCharArray OACC-PWD)))))

(defn make-admin
  ([]
   (make-admin (generate data/external-id)))
  ([name]
   (make-resource "ADMIN" name #(generate data/admin))))

(defn make-resource
  [cls rsrc-name g]
  (let [rsrc-map (g)
        password (:password rsrc-map)
        rsrc (if (not password)
               (.createResource *ACC* cls DEFAULT-DOMAIN rsrc-name)
               (.createResource *ACC*
                                cls
                                DEFAULT-DOMAIN
                                rsrc-name
                                (PasswordCredentials/newInstance
                                 (.toCharArray password))))]
    (assoc rsrc-map
           :id (.getId rsrc)
           :resource rsrc
           :name (.getExternalId rsrc))))

(defn make-role
  ([]
   (make-role (generate data/external-id)))
  ([name]
   (make-resource "ROLE" name (constantly {}))))

(defn make-service
  ([]
   (make-service (generate data/external-id)))
  ([name]
   (make-resource "SERVICE" name #(generate data/service))))

(defn make-user
  ([]
   (make-user (generate data/external-id)))
  ([name]
   (make-resource "USER" name #(generate data/user))))

(defn set-permissions
  [perms {accessor :resource :as rsrc1} {accessed :resource}]
  (let [perms (into #{} (map #(ResourcePermissions/getInstance %) perms))]
    (.setResourcePermissions *ACC* accessor accessed perms))
  rsrc1)

;;; Fixtures

(defn with-sqlite-db
  [f]
  (let [tmp-db (create-tmp-db)
        db-conn (DriverManager/getConnection (str "jdbc:sqlite:"
                                                  (.getAbsolutePath tmp-db)))]
    (initialize-oacc db-conn)
    (alter-var-root #'*DB* (constantly db-conn))
    (alter-var-root #'*ACC* (constantly
                             (doto (SQLAccessControlContextFactory/getAccessControlContext
                                    db-conn
                                    nil
                                    SQLProfile/SQLite_3_8_RECURSIVE)
                               (.authenticate (Resources/getInstance 0)
                                              (PasswordCredentials/newInstance (.toCharArray
                                                                                OACC-PWD))))))
    (f)
    (.close *DB*)
    (.unauthenticate *ACC*)
    (.delete tmp-db)))
