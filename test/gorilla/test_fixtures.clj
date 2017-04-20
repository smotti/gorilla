(ns gorilla.test-fixtures
  (:import java.sql.DriverManager
           java.io.File
           org.sqlite.JDBC
           com.acciente.oacc.sql.internal.SQLAccessControlSystemInitializer
           (com.acciente.oacc Resources))
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.test.check.generators :refer [generate]]
            [gorilla.test-data-utils :as data]))

(declare initialize-oacc make-resource)

(def ^:dynamic *DB* nil)

(def DEFAULT-DOMAIN [1 "APP_DOMAIN"])

(def DEFAULT-RESOURCE-IDS {:ADMIN 1 :ROLE 2 :SERVICE 3 :USER 4})

(def DEFAULT-RESOURCE-CLASSES [[1 "ADMIN" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                               [2 "ROLE" false false ["VIEW" "EDIT"]]
                               [3 "SERVICE" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                               [4 "USER" true false ["VIEW" "EDIT" "DEACTIVATE"]]])

(def OACC-PWD "oaccpwd")

(def domain-id #(int (nth % 0)))

(def domain-name #(nth % 1))

(def rsrc-cls-id #(nth % 0))

(def rsrc-cls-name #(nth % 1))

(def rsrc-cls-auth #(nth % 2))

(def rsrc-cls-create #(nth % 3))

(def rsrc-cls-perms #(nth % 4))

(def SQLITE-CREATE-TABLES
  (.getFile (io/resource "oacc-db-2.0.0-rc.7/SQLite_3_8/create_tables.sql")))

(def SQL-STRINGS
  {:insert-domain (str "INSERT INTO oac_domain (domainname) VALUES (?)")
   :insert-rsrc (str "INSERT INTO "
                     "oac_resource ("
                     "resourceclassid, "
                     "domainid) "
                     "VALUES (?, ?)")
   :insert-rsrc-cls (str "INSERT INTO "
                         "oac_resourceclass ("
                         "resourceclassname, "
                         "isauthenticatable, "
                         "isunauthenticatedcreateallowed) "
                         "VALUES (?, ?, ?)")
   :insert-rsrc-cls-permission (str "INSERT INTO "
                                    "oac_resourceclasspermission ("
                                    "resourceclassid, "
                                    "permissionname) "
                                    "VALUES (?, ?)")})

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
  (let [insert-rsrc-cls (.prepareStatement db-conn
                                           (:insert-rsrc-cls SQL-STRINGS))
        insert-rsrc-cls-permission (.prepareStatement
                                    db-conn
                                    (:insert-rsrc-cls-permission SQL-STRINGS))
        insert-domain (.prepareStatement db-conn (:insert-domain SQL-STRINGS))]
    (doseq [cls DEFAULT-RESOURCE-CLASSES]
      (let [id (int (rsrc-cls-id cls))]
        (doto insert-rsrc-cls
          (.setString 1 (rsrc-cls-name cls))
          (.setInt 2 (to-int (rsrc-cls-auth cls)))
          (.setInt 3 (to-int (rsrc-cls-create cls)))
          (.executeUpdate))
        (.setInt insert-rsrc-cls-permission 1 id)
        (doseq [perms (rsrc-cls-perms cls)]
          (doto insert-rsrc-cls-permission
            (.setString 2 perms)
            (.executeUpdate)))))
    (doto insert-domain
      (.setString 1 (domain-name DEFAULT-DOMAIN))
      (.executeUpdate))))

(defn make-admin
  []
  (make-resource :ADMIN #(generate data/admin)))

(defn make-resource
  "
  cls is a default resource class: :ADMIN :ROLE :SERVICE :USER
  g is a fn the generates a map of a resource that belongs to that class
  "
  [cls g]
  (let [stmt (doto (.prepareStatement *DB* (:insert-rsrc SQL-STRINGS))
               (.setInt 1 (cls DEFAULT-RESOURCE-IDS))
               (.setInt 2 (domain-id DEFAULT-DOMAIN))
               (.executeUpdate))
        id (.getLong (.getGeneratedKeys stmt) 1)
        rsrc-map (g)
        rsrc (Resources/getInstance id)]
    (assoc rsrc-map :id id :resource rsrc)))

(defn make-role
  []
  (make-resource :ROLE #(constantly {})))

(defn make-service
  []
  (make-resource :SERVICE #(generate data/service)))

(defn make-user
  []
  (make-resource :USER #(generate data/user)))

;;; Fixtures

(defn with-sqlite-db
  [f]
  (let [tmp-db (create-tmp-db)
        db-conn (DriverManager/getConnection (str "jdbc:sqlite:"
                                                  (.getAbsolutePath tmp-db)))]
    (initialize-oacc db-conn)
    (alter-var-root #'*DB* (constantly db-conn))
    (f)
    (.close *DB*)
    (.delete tmp-db)))
