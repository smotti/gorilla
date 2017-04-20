(ns gorilla.test-fixtures
  (:import java.sql.DriverManager
           java.io.File
           org.sqlite.JDBC
           com.acciente.oacc.sql.internal.SQLAccessControlSystemInitializer)
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]))

(declare initialize-oacc)

(def ^:dynamic *DB* nil)

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

(def SQL-STRINGS
  {:insert-rsrc-cls (str "INSERT INTO "
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
                                    (:insert-rsrc-cls-permission SQL-STRINGS))]
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
            (.executeUpdate)))))))

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
