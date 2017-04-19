(ns gorilla.access-control-context
  (:import java.lang.IllegalArgumentException
           (com.acciente.oacc AccessControlContext AuthorizationException
                              Resource)
           (com.acciente.oacc.sql SQLProfile SQLAccessControlContextFactory))
  (:require [gorilla.credentials :refer [make-password]]
            [gorilla.permission :refer [get-permission]]
            [gorilla.resource :refer [get-resource make-resource remove-resource]]))

(declare has-create-permission?)

(def ^:private DEFAULT_DOMAIN "APP_DOMAIN")

(defn authenticate
  [^AccessControlContext acc id pwd]
  (.authenticate acc (get-resource id) (make-password pwd)))

(defn add-role
  "
  Adds a new role with name to the system.
  Returns the newly created resources on success else nil.
  "
  [acc name]
  (let [accessor (.getSessionResource acc)]
    (when (has-create-permission? acc accessor "ROLE")
      (make-resource acc "ROLE" DEFAULT_DOMAIN name))))

(defn has-permission?
  "
  Check if accessor has permission on accessed resource.

  Note that we use a set for ResourcePermission, because selecting another
  method implementation via type hints doesn't work :/.
  "
  [acc accessor accessed permission]
  (try
    (.hasResourcePermissions acc
                             accessor
                             accessed
                             #{(get-permission permission)})
    (catch IllegalArgumentException e false)
    (catch AuthorizationException e false)))

(defn has-create-permission?
  "
  Check if accessor has permission to create a resource of resource-class.

  Note that we use a set for ResourceCreatePermission, because selecting another
  method implementation via type hints doesn't work :/.
  "
  [^AccessControlContext acc accessor resource-class]
  (try
    (.hasResourceCreatePermissions acc
                                   accessor
                                   resource-class
                                   DEFAULT_DOMAIN
                                   #{(get-permission "*CREATE")})
    (catch IllegalArgumentException e false)
    (catch AuthorizationException e false)))

(defn make-access-control-context
  ([db-conn sql-profile]
   (let [schema (condp = sql-profile
                  :sqlite nil
                  :postgresql "oacc")
         _sql-profile (condp = sql-profile
                        :sqlite (SQLProfile/SQLite_3_8_RECURSIVE)
                        :postgresql (SQLProfile/PostgreSQL_9_3_RECURSIVE))]
     (SQLAccessControlContextFactory/getAccessControlContext db-conn schema _sql-profile)))
  ([db-conn sql-profile auth-provider]
   (let [schema (condp = sql-profile
                  :sqlite nil
                  :postgresql "oacc")
         _sql-profile (condp = sql-profile
                        :sqlite "SQLite_3_8_RECURSIVE"
                        :postgresql "PostgreSQL_9_3_RECURSIVE")]
     (SQLAccessControlContextFactory/getAccessControlContext db-conn
                                                             schema
                                                             _sql-profile
                                                             auth-provider))))

(defn remove-role
  "Remove a the role with name. Return true on success and false on failure."
  [^AccessControlContext acc name]
  (let [accessor (.getSessionResource acc)
        role (Resources/getInstance name)]
    (when (has-permission? acc accessor role "*DELETE")
      (.deleteResource acc (Resources/getInstance name)))))

(defn unauthenticate
  [^AccessControlContext acc]
  (.unauthenticate acc))
