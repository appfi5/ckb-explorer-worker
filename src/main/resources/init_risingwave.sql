
CREATE TABLE IF NOT EXISTS "dob_extend"(
    "id"  BIGINT  PRIMARY KEY,
    "dob_script_id" BIGINT  NULL DEFAULT NULL,
    "dob_code_hash" BYTEA NULL DEFAULT NULL,
    "dob_script_hash" BYTEA  NULL DEFAULT NULL,
    "args" BYTEA  NULL DEFAULT NULL,
    "block_timestamp" BIGINT  NULL DEFAULT NULL,
    "name" VARCHAR  NULL DEFAULT NULL,
    "description" VARCHAR  NULL DEFAULT NULL,
    "tags" character varying[] DEFAULT NULL,
    "lock_script_id" BIGINT  NULL DEFAULT NULL,
    "creator" VARCHAR  NULL DEFAULT NULL
);

CREATE  INDEX IF NOT EXISTS "index_dob_script_id" ON "dob_extend" ("dob_script_id");

CREATE  INDEX IF NOT EXISTS  "index_dob_script_code_args" ON "dob_extend" ("args","dob_code_hash");

-- testnet sql
INSERT INTO "dob_extend" (id,name,description,dob_script_hash) values(0,'Unique items','Only for no cluster spore cell','\x2981ed0498836ae970473f56ebf61d8e0eaf2dbe97286d160658d7c2787ce69b');

-- mainnet sql
-- INSERT INTO "dob_extend" (id,name,description,dob_script_hash) values(0,'Unique items','Only for no cluster spore cell','\xcf9e0cdbd169550492b29d3d1181d27048ab80126b797840965d2864607a892d');

CREATE TABLE IF NOT EXISTS "dob_code"(
    "dob_code_script_id" BIGINT,
    "dob_extend_id" BIGINT NOT NULL,
    "dob_code_script_args" BYTEA NULL DEFAULT NULL,
    PRIMARY KEY("dob_code_script_id")
);

CREATE TABLE IF NOT EXISTS lock_script_extend (script_id BIGINT PRIMARY KEY, lock_type INT);
CREATE TABLE IF NOT EXISTS type_script_extend (script_id BIGINT PRIMARY KEY, cell_type INT);

CREATE  INDEX IF NOT EXISTS "index_block_timestamp" ON "dob_extend" ("block_timestamp");

