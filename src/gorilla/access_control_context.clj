(ns gorilla.access-control-context
  (:import java.lang.IllegalArgumentException
           (com.acciente.oacc AccessControlContext AuthorizationException
                              PasswordCredentials Resource ResourcePermissions
                              ResourceCreatePermissions Resources)
           (com.acciente.oacc.sql SQLProfile SQLAccessControlContextFactory)))

(declare has-create-permission? make-resource remove-resource
         when-create-permission when-permission)

(def ^:private DEFAULT_DOMAIN "APP_DOMAIN")

(defn authenticate
  [^AccessControlContext acc id pwd]
  (.authenticate acc
                 (Resources/getInstance id)
                 (PasswordCredentials/newInstance (.toCharArray pwd))))

(defn add-role
  "
  Adds a new role with name to the system.
  Returns the newly created resources on success else nil.
  "
  [acc name]
  (let [accessor (.getSessionResource acc)]
    (make-resource acc accessor "ROLE" DEFAULT_DOMAIN name)))

(defn add-service
  "
  Adds a new service with name and the password pwd that is used for
  authentication.

  Returns the newly created service Resource on success else nil.
  "
  [acc name pwd]
  (let [accessor (.getSessionResource acc)]
    (make-resource acc accessor "SERVICE" DEFAULT_DOMAIN name pwd)))

(defn add-user
  "
  Add a new user with name and password pwd. cls determines if it's a
  regular USER or an ADMIN.

  Return the newly created user Resource on success else nil.
  "
  [^AccessControlContext acc name pwd cls]
  (let [accessor (.getSessionResource acc)]
    (make-resource acc accessor cls DEFAULT_DOMAIN name pwd)))

(defn has-permission?
  "
  Check if accessor has permission on accessed resource.

  Note that we use a set for ResourcePermission, because selecting another
  method implementation via type hints doesn't work :/.
  "
  [permission ^AccessControlContext acc ^Resource accessor ^Resource accessed]
  (try
    (.hasResourcePermissions acc
                             accessor
                             accessed
                             #{(ResourcePermissions/getInstance permission)})
    (catch IllegalArgumentException e false)
    (catch AuthorizationException e false)))

(defn has-create-permission?
  "
  Check if accessor has permission to create a resource of resource-class.

  Note that we use a set for ResourceCreatePermission, because selecting another
  method implementation via type hints doesn't work :/.
  "
  [^AccessControlContext acc ^Resource accessor resource-class]
  (try
    (.hasResourceCreatePermissions acc
                                   accessor
                                   resource-class
                                   DEFAULT_DOMAIN
                                   #{(ResourceCreatePermissions/getInstance "*CREATE")})
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

(defn make-resource
  ([^AccessControlContext acc ^Resource accessor rsrc-cls rsrc-name]
   (when-create-permission acc accessor rsrc-cls
     (.createResource acc rsrc-cls DEFAULT_DOMAIN rsrc-name)))
  ([^AccessControlContext acc ^Resource accessor rsrc-cls rsrc-name pwd]
   (when-create-permission acc accessor rsrc-cls
     (.createResource acc
                      rsrc-cls
                      DEFAULT_DOMAIN
                      rsrc-name
                      (PasswordCredentials/newInstance pwd)))))

(defn remove-resource
  [^AccessControlContext acc ^Resource accessor ^Resource accessed]
  (when-permission "*DELETE" acc accessor accessed
    (.deleteResource acc accessed)))

(defn remove-role
  "Remove a the role with name. Return true on success and false on failure."
  [^AccessControlContext acc name]
  (let [accessor (.getSessionResource acc)
        role (Resources/getInstance name)]
    (remove-resource acc accessor role)))

(defn remove-service
  "Remove a service with name. Return true on sucess and false on failure."
  [^AccessControlContext acc name]
  (let [accessor (.getSessionResource acc)
        svc (Resources/getInstance name)]
    (remove-resource acc accessor svc)))

(defn remove-user
  "Remove a user with name. Return true on success and false on failure."
  [^AccessControlContext acc name]
  (let [accessor (.getSessionResource acc)
        user (Resources/getInstance name)]
    (remove-resource acc accessor user)))

(defn unauthenticate
  [^AccessControlContext acc]
  (.unauthenticate acc))

(defmacro when-create-permission
  "
  Just like with-permission but specialized to the *CREATE permission. And it takes
  a resource class instead of an accessed resource.
  "
  [^AccessControlContext acc ^Resource accessor rsrc-cls & body]
  `(if (not (has-create-permission? acc accessor rsrc-cls))
     false
     (do ~@body)))

(defmacro when-permission
  "
  Evaluate body if accessor has permission on accessed resource.

  Return false if accessor doesn't have permission. Thus for a more consistent
  interface the bodies last expression should return a truthy value.
  "
  [permission ^AccessControlContext acc ^Resource accessor ^Resource accessed & body]
  `(if (not (has-permission? ~permission ~acc ~accessor ~accessed))
     false
     (do ~@body)))
