# Database Setup

The database setup scripts, found under `resources/gorilla-db`, should be
executed in the following sequence:

1. **gorilla_create_database.sql**
    + creates a dedicated database for gorilla
    + typically run as a DBMS admin user

2. **gorilla_create_schema.sql**
    + creates database schemas to house gorilla and OACC-specific tables
    + run this script while connected to the database you set up with the
    `gorilla_create_database.sql` script above

3. **gorilla_create_tables.sql**
    + creates gorilla specific tables
    + run this script while connected to the database you set up with the
    `gorilla_create_database.sql` script above

4. **oacc_create_tables.sql**
    + creates OACC sequences, tables and constraints
    + run this script while connected to the database you set up with the
    `gorilla_create_database.sql` script above

4. **gorilla_create_user.sql**
    + creates a database user for gorilla - _**Note:**_ update this script to
    set the gorilla's database user's password!
    + grants privileges to connect to the gorilla database you set up with the
    `groilla_create_database.sql` script above
    + grants privileges to the gorilla and OACC sequences and tables

There is a fifth script, `oacc_drop_tables.sql`, to facilitate removal of OACC
constraints, tables and sequences, which you would only run when uninstalling
OACC from your project.

**Note:** that for SQLite only two scripts (besides the `oacc_drop_tables.sql`
script) exist, because in SQLite a database is just a file and it has no
concept of schemas and users.

# Initialize OACC

For development you should run:

`boot initialize-oacc -u <jdbc-db-url> -U <db-user> -P <db-user-pwd> -p <oacc-root-pwd>`

For production it is done automatically when launching the application.
