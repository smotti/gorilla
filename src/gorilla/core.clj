(ns gorilla.core
  (:gen-class)
  (:import java.sql.DriverManager
           org.postgresql.Driver
           org.sqlite.JDBC
           (com.acciente.oacc AccessControlContext PasswordCredentials Resources)
           com.acciente.oacc.sql.internal.SQLAccessControlSystemInitializer
           (com.acciente.oacc.sql SQLAccessControlContextFactory SQLProfile))
  (:require [clojure.tools.logging :as log]))

;; To capture the output of oacc
(log/log-capture! 'gorilla.core)

(declare resource-class-exists?)

(def ^:private DEFAULT-DOMAIN "APP-DOMAIN")

(def ^:private DEFAULT-RESOURCE-CLASSES [["ADMIN" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                                         ["ROLE" false false ["VIEW" "EDIT"]]
                                         ["SERVICE" true false ["VIEW" "EDIT" "DEACTIVATE"]]
                                         ["USER" true false ["VIEW" "EDIT" "DEACTIVATE"]]])

(def rsrc-cls-name #(nth % 0))

(def rsrc-cls-auth #(nth % 1))

(def rsrc-cls-create #(nth % 2))

(def rsrc-cls-perms #(nth % 3))

(defn- initialize-oacc
  [db-conn oacc-root-pwd]
  (let [url (.. db-conn (getMetaData) (getURL))
        rdbms (second (into [] (.split url ":")))
        schema (condp = rdbms
                 "postgresql" "oacc"
                 "sqlite" nil
                 "oacc")
        profile (condp = rdbms
                  "postgresql" SQLProfile/PostgreSQL_9_3_RECURSIVE
                  "sqlite" SQLProfile/SQLite_3_8_RECURSIVE)]
    (SQLAccessControlSystemInitializer/initializeOACC db-conn
                                                      schema
                                                      (.toCharArray oacc-root-pwd))
    (let [acc (doto (SQLAccessControlContextFactory/getAccessControlContext
                     db-conn
                     nil
                     profile)
                (.authenticate (Resources/getInstance 0)
                               (PasswordCredentials/newInstance (.toCharArray
                                                                 oacc-root-pwd))))]
      (log/info "Adding resource classes and permissions")
      (doseq [cls DEFAULT-RESOURCE-CLASSES]
        (log/debugf "Adding resource class: %s" cls)
        (when (not (resource-class-exists? acc (rsrc-cls-name cls)))
          (.createResourceClass acc
                                (rsrc-cls-name cls)
                                (rsrc-cls-auth cls)
                                (rsrc-cls-create cls)))
        (doseq [p (rsrc-cls-perms cls)]
          (log/debugf "Adding resource class permission: %s" p)
          (try
            (.createResourcePermission acc (rsrc-cls-name cls) p)
            (catch IllegalArgumentException e
              (log/debugf e
                          "Failed to add resource class permission: %s"
                          p)))))
      (log/info "Adding default application domain")
      (try
        (.createDomain acc DEFAULT-DOMAIN)
        (catch IllegalArgumentException e
          (log/debugf e
                      "Failed to add default application domain: %s"
                      DEFAULT-DOMAIN))))))

(defn- load-config
  [filename]
  (log/debugf "Reading config: %s" filename)
  (binding [*read-eval* false]
    ((comp read-string slurp) filename)))

(defn- resource-class-exists?
  [^AccessControlContext acc class-name]
  (try
    (.getResourceClassInfo acc class-name)
    true
    (catch IllegalArgumentException e false)))

(defn -main
  [& args]
  (let [cfg-file (first args)
        cfg (load-config cfg-file)
        db-conn (DriverManager/getConnection (:db-url cfg)
                                             (:db-user cfg)
                                             (:db-pwd cfg))]
    (initialize-oacc db-conn (:oacc-root-pwd cfg))))
