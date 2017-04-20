(ns gorilla.core
  (:gen-class)
  (:import java.sql.DriverManager
           org.postgresql.Driver
           org.sqlite.JDBC
           com.acciente.oacc.sql.internal.SQLAccessControlSystemInitializer
           (com.acciente.oacc.sql SQLAccessControlContextFactory))
  (:require [
             gorilla.util :refer [resource-class-exists?]]))

;; Note delete permission is implicit, meaning it's providing by OACC via *DELETE by default.
(def ^:private DEFAULT_RESOURCE_CLASSES [["ADMIN" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                                         ["ROLE" false false ["VIEW" "EDIT"]]
                                         ["SERVICE" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                                         ["USER" true false ["VIEW" "EDIT" "DEACTIVATE"]]])

(defn initialize-oacc
  [db-conn oacc-root-pwd]
  (SQLAccessControlSystemInitializer/initializeOACC db-conn "oacc" (.toCharArray oacc-root-pwd)))

(defn initialize-default-resource-classes
  [db-conn]
  (let []))

(defn load-config
  [filename]
  (binding [*read-eval* false]
    ((comp read-string slurp) filename)))

(defn -main
  [& args]
  (let [cfg-file (first args)
        cfg (load-config cfg-file)
        db-conn (DriverManager/getConnection (:db-url cfg)
                                             (:db-user cfg)
                                             (:db-pwd cfg))]
    (initialize-oacc db-conn (:oacc-root-pwd cfg))
    #_(initialize-default-resource-classes)))
