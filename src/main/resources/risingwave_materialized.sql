CREATE SOURCE pg_mydb WITH (
    connector = 'postgres-cdc',
    hostname = 'pghost',
    port = '5432',
    username = 'risingwave_cdc',
    password = '123456',
    database.name = 'ckb-rich-indexer',
    schema.name = 'public',
    table.name='output',
    slot.name= 'rw_output',
    debezium.slot.drop.on.stop = 'true'
);

CREATE FUNCTION udt_amount(bytea) RETURNS NUMERIC LANGUAGE rust AS $$
    fn udt_amount(data: &[u8]) -> u128 {
        let mut bytes = [0u8; 16];
let len = data.len().min(16);
bytes[..len].copy_from_slice(&data[..len]);
let value: u128 = u128::from_le_bytes(bytes);
value
}
$$;

CREATE TABLE output  (*) FROM pg_mydb  TABLE 'public.output';

CREATE INDEX idx_output_block_timestamp ON output (block_timestamp);
CREATE INDEX idx_output_lock_script_id ON output (lock_script_id);
CREATE INDEX idx_output_type_script_id ON output (type_script_id);


CREATE MATERIALIZED VIEW live_cells AS
SELECT id,tx_id,tx_hash,output_index,capacity,lock_script_id,
       type_script_id,data,data_size,data_hash,occupied_capacity
        ,block_number,block_timestamp
FROM output
WHERE is_spent = 0;


CREATE MATERIALIZED VIEW address_24h_transaction AS
SELECT
    lock_script_id,
    type_script_id,
    tx_id AS ckb_transaction_id,
    block_timestamp,
    block_number
FROM output
WHERE
        block_timestamp >= (SELECT MAX(block_timestamp) - 86400000 FROM output)
  and is_spent=0
UNION ALL
SELECT
    lock_script_id,
    type_script_id,
    tx_id AS ckb_transaction_id,
    block_timestamp,
    block_number
FROM output
WHERE
        consumed_timestamp >= (SELECT MAX(block_timestamp) - 86400000 FROM output);

-- ${dao_script_id} 为pg库里dao的script id
CREATE MATERIALIZED VIEW deposit_cell AS
SELECT
    output.id AS output_id,
    output.capacity AS value,
    output.tx_id AS tx_id,
    output.tx_hash AS tx_hash,
    output.data AS data,
    output.occupied_capacity AS occupied_capacity,
    output.output_index AS output_index,
    output.block_number AS block_number,
    output.block_timestamp AS block_timestamp,
    output.lock_script_id AS lock_script_id,
    output.consumed_tx_hash AS consumed_tx_hash,
    output.input_index AS input_index,
    output.consumed_block_number AS consumed_block_number,
        output.consumed_timestamp AS consumed_block_timestamp
FROM output
WHERE output.type_script_id = ${dao_script_id} AND output.data = '\x0000000000000000';


CREATE INDEX "index_deposit_cell_lock_script_id" ON "deposit_cell" ("lock_script_id");
CREATE INDEX "index_deposit_cell_block_timestamp" ON "deposit_cell" ("block_timestamp");
CREATE INDEX "index_deposit_cell_consumed_timestamp" ON "deposit_cell" ("consumed_block_timestamp");


CREATE MATERIALIZED VIEW withdraw_cell AS
SELECT
    output.id AS output_id,
    output.capacity AS value,
    output.tx_id AS tx_id,
    output.tx_hash AS tx_hash,
    output.data AS data,
    output.occupied_capacity AS occupied_capacity,
    output.output_index AS output_index,
    output.block_number AS block_number,
    output.block_timestamp AS block_timestamp,
    output.lock_script_id AS lock_script_id,
    output.consumed_tx_hash AS consumed_tx_hash,
    output.input_index AS input_index,
    output.consumed_block_number AS consumed_block_number,
    output.consumed_timestamp AS consumed_block_timestamp
FROM output
WHERE output.type_script_id = ${dao_script_id} AND output.data != '\x0000000000000000';


CREATE INDEX "index_withdraw_cell_lock_script_id" ON "withdraw_cell" ("lock_script_id");
CREATE INDEX "index_withdraw_cell_block_timestamp" ON "withdraw_cell" ("block_timestamp");
CREATE INDEX "index_withdraw_cell_consumed_timestamp" ON "withdraw_cell" ("consumed_block_timestamp");


CREATE MATERIALIZED VIEW dob_output AS
select * from output o where  exists (
    select * from dob_code dc where dc.dob_code_script_id=o.type_script_id);

CREATE INDEX "index_dob_output_type_script_id" ON "dob_output" ("type_script_id");
CREATE INDEX "index_dob_output_block_timestamp" ON "dob_output" ("block_timestamp");

CREATE MATERIALIZED VIEW dob_live_cells AS
select * from dob_output  where  is_spent=0;

CREATE INDEX "index_dob_live_cells_lock_script_id" ON "dob_live_cells" ("lock_script_id");


CREATE MATERIALIZED VIEW statistic_address AS
SELECT
    lock_script_id,
    SUM(capacity) AS balance,
    SUM(case when type_script_id is null then  0 else  capacity  end) AS balance_occupied,
    COUNT(*) AS live_cells_count
FROM live_cells
GROUP BY lock_script_id;

CREATE MATERIALIZED VIEW udt_accounts AS
SELECT
    lock_script_id,
    type_script_id,
    SUM(udt_amount(data)) AS amount
FROM live_cells lc
WHERE exists (select * from type_script_extend ts where ts.script_id = lc.type_script_id  )
GROUP BY lc.lock_script_id,lc.type_script_id;




CREATE MATERIALIZED VIEW udt_holder_allocations AS
SELECT u.type_script_id as type_script_id ,COALESCE(ls.lock_type,1) as lock_type,COUNT(*) as holder_count FROM  udt_accounts u
                                                                                                                    LEFT JOIN lock_script_extend ls ON u.lock_script_id = ls.script_id
GROUP BY u.type_script_id,COALESCE(ls.lock_type,1);


CREATE INDEX IF NOT EXISTS "index_udt_holder_allocations_type_script_id" ON "udt_holder_allocations" ("type_script_id");

CREATE INDEX  "index_live_cells_id" ON "live_cells" ("id");
CREATE INDEX   "index_live_cells_lock_script_id" ON "live_cells" ("lock_script_id");
CREATE INDEX   "index_live_cells_type_script_id" ON "live_cells" ("type_script_id");
CREATE INDEX   "index_live_cells_block_number" ON "live_cells" ("block_number");
CREATE INDEX   "index_live_cells_block_timestamp" ON "live_cells" ("block_timestamp");


CREATE INDEX   "index_statistic_address_lock_script_id" ON "statistic_address" ("lock_script_id");

CREATE INDEX   "index_address_24h_transaction_lock_script_id" ON "address_24h_transaction" ("lock_script_id");
CREATE INDEX   "index_address_24h_ckb_transaction_id" ON "address_24h_transaction" ("ckb_transaction_id");
CREATE INDEX   "index_address_24h_transaction_block_timestamp" ON "address_24h_transaction" ("block_timestamp");
CREATE  INDEX   "index_address_24h_transaction_type_script_id" ON "address_24h_transaction" ("type_script_id");


CREATE INDEX  "index_udt_accounts_lock_script_id" ON "udt_accounts" ("lock_script_id");
CREATE INDEX   "index_udt_accounts_type_script_id" ON "udt_accounts" ("type_script_id");


CREATE INDEX  "index_dob_live_cells_id" ON "dob_live_cells" ("id");


CREATE MATERIALIZED VIEW dob_statistic AS
select dc.dob_extend_id, COUNT(DISTINCT dl.lock_script_id) as holders_count,
COUNT(dc.dob_code_script_id ) as items_count  from dob_code dc left join
dob_live_cells  dl on  dl.type_script_id = dc.dob_code_script_id
group by dc.dob_extend_id ;

create INDEX "dob_statistic_dob_extend_id" on "dob_statistic" ("dob_extend_id");

CREATE MATERIALIZED VIEW dob_24h_statistic AS
select dc.dob_extend_id, count(distinct dop.tx_id) as h24_ckb_transactions_count
from dob_output dop left join dob_code dc  on dop.type_script_id = dc.dob_code_script_id
where dop.block_timestamp >= (SELECT MAX(block_timestamp) - 86400000 FROM output)  group by dc.dob_extend_id ;
create INDEX "dob_24_statistic_dob_extend_id" on "dob_24h_statistic" ("dob_extend_id");
