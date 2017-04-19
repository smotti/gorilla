(ns gorilla.resource
  (:import (com.acciente.oacc AccessControlContext Resources))
  (:require [gorilla.credentials :refer [make-password]]))

(defn get-resource
  "
  Get a ResourceImpl instance with the given id. If the id is a number it is
  used as the internal id, and if it's a string then it's used for the
  external id.
  Another way is to make one with internal and external speficied.

  Note that this actually doesn't touch the database the only thing you
  get is an instance of an obj who's class, ResourceImpl, implements the Resource
  iface.
  "
  ([id] (Resources/getInstance id))
  ([internal external] (Resources/getInstance internal external)))

(defn make-resource
  "
  Make a new resource of class cls in domain dom of name n. This also adds
  a new resource to the database.
  "
  [^AccessControlContext acc cls dom n & {:keys [password]}]
  (if password
    (.createResource acc cls dom n (make-password password))
    (.createResource acc cls dom n)))

(defn remove-resource
  "Remove a resource from the database."
  [^AccessControlContext acc id]
  (.deleteResource acc (get-resource id)))
