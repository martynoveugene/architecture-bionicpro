CREATE TABLE bio_analytics.sales_raw (
       id Int32,
       customerId String,
       amount Decimal(10, 2),
       datetime DateTime,
       version Int64,
       sign Int8
) ENGINE = ReplacingMergeTree(version)
ORDER BY (id);
