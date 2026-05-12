-- Runs once on first PostgreSQL data dir init (see docker-entrypoint-initdb.d).
-- Tables are NOT defined here: each Spring Boot service uses spring.jpa.hibernate.ddl-auto=update (Config Server)
-- and creates its schema when it first connects (e.g. orders.orders in database "orders").
CREATE DATABASE orders;
CREATE DATABASE inventory;
CREATE DATABASE payments;
