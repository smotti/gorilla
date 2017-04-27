CREATE TABLE IF NOT EXISTS gorilla_ldapserver(
id              INTEGER PRIMARY KEY,
host            VARCHAR(16) NOT NULL,
port            SMALLINT NOT NULL,
bindDn          TEXT NOT NULL,
password        VARCHAR(64),
connectTimeout  INT,
timeout         INT,
ssl             BOOLEAN,
baseDn          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS gorilla_session(
id                INTEGER PRIMARY KEY,
sessionId         VARCHAR(28) NOT NULL,
startTime         BIGINT NOT NULL,
endTime           BIGINT,
absoluteTimeout   INT,
idleTimeout       INT,
renewalTimeout    INT,
isValid           BOOLEAN NOT NULL;
);
