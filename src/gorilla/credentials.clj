(ns gorilla.credentials
  (:import (com.acciente.oacc PasswordCredentials)))

(defn make-password
  "
  Get an instance of PasswordCredentials the wraps the password.

  Note there's no database interaction happening in this case.
  "
  [password]
  (PasswordCredentials/newInstance (.toCharArray password)))
