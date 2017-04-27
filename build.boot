(set-env!
 :resource-paths #{"resources"}
 :source-paths #{"src" "test"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha15"]
                 [org.postgresql/postgresql "42.0.0.jre7"]
                 [org.xerial/sqlite-jdbc "3.16.1"]
                 [com.acciente.oacc/acciente-oacc "2.0.0-rc.7"]
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [org.clojure/test.check "0.9.0" :scope "test"]
                 [org.slf4j/slf4j-api "1.8.0-alpha1"]
                 [org.slf4j/slf4j-log4j12 "1.8.0-alpha1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-ldap-auth/clj-ldap-auth "0.2.0"]])

(require '[adzerk.boot-test :refer :all])

(task-options!
 pom {:project 'gorilla
      :description "An identity and access management service"
      :version "0.1.0"
      :license {"CLOSED" "CLOSED"}
      :developers {"John Liu" "john.liu@medicustek.com"
                   "Jordan Lin" "jordan.lin@medicustek.com"
                   "William Ott" "william.ott@medicustek.com"}})

(deftask build
  []
  (comp
   (aot :namespace #{'gorilla.core})
   (uber)
   (pom)
   (jar :file "gorilla.jar" :main 'gorilla.core)
   (sift :include #{#"gorilla.jar"})
   (target)))

(import 'java.sql.DriverManager
        'org.postgresql.Driver
        'com.acciente.oacc.sql.internal.SQLAccessControlSystemInitializer)

(deftask initialize-oacc
  "Initialize OACC on an existing RDBMS."
  [u db-url URL str "A JDBC database url."
   p oacc-root-pwd PASSWORD str "The root password for the framework admin."
   U db-user USER str "Database user."
   P db-pwd PASSWORD str "Database user password."]
  (with-pre-wrap fs
    (let [conn (DriverManager/getConnection db-url db-user db-pwd)
          rdbms (second (into [] (.split db-url ":")))
          schema (condp = rdbms
                   "postgresql" "oacc"
                   "sqlite" nil
                   "oacc")]
      (SQLAccessControlSystemInitializer/initializeOACC conn schema (.toCharArray oacc-root-pwd))
      (.close conn))
    fs))
