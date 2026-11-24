-- Khoi tao database cho tung bounded context.
-- Script chi chay lan dau khi volume pgdata con trong.
-- PostgreSQL dang o database POSTGRES_DB (mac dinh: ticketgo).

SELECT 'CREATE DATABASE identity_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'identity_db')\gexec

SELECT 'CREATE DATABASE inventory_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db')\gexec

SELECT 'CREATE DATABASE order_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'order_db')\gexec

SELECT 'CREATE DATABASE payment_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')\gexec

SELECT 'CREATE DATABASE ai_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ai_db')\gexec

-- pgvector cho AI service
\connect ai_db
CREATE EXTENSION IF NOT EXISTS vector;
