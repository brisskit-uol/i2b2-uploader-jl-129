/*===========================================================
  Postgres SQL for granting 
  (i) connection privilege to the i2b2 database
  (ii) all other privileges on a project's schema
  
  Substitutions required!!!
  ===========================================================*/ 

GRANT CONNECT ON DATABASE <DB_NAME> TO <DB_USER_NAME> ;
GRANT ALL PRIVILEGES ON SCHEMA <DB_SCHEMA_NAME> to <DB_SCHEMA_NAME> ;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA <DB_SCHEMA_NAME> TO <DB_SCHEMA_NAME> ;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA <DB_SCHEMA_NAME> TO <DB_SCHEMA_NAME> ;