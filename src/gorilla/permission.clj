(ns gorilla.permission
  (:import (com.acciente.oacc ResourceCreatePermissions ResourcePermissions)))

(defn get-permission
  "
  Get an instance of ResourcePermission that wraps the given permission.

  Note it doesn't touch the database.
  "
  [permission]
  (ResourcePermissions/getInstance permission))
