(ns gorilla.util
  (:import java.lang.IllegalArgumentException
           (com.acciente.oacc AccessControlContext)))

(defn resource-class-exists?
  [^AccessControlContext acc class-name]
  (try
    (.getResourceClassInfo acc class-name)
    true
    (catch IllegalArgumentException e false)))
