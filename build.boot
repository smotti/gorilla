(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha15"]
                 [org.postgresql/postgresql "42.0.0.jre7"]
                 [com.acciente.oacc/acciente-oacc "2.0.0-rc.7"]])

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
    (let [conn (DriverManager/getConnection db-url db-user db-pwd)]
      (SQLAccessControlSystemInitializer/initializeOACC conn "oacc" (.toCharArray oacc-root-pwd))
      (.close conn))
    fs))
