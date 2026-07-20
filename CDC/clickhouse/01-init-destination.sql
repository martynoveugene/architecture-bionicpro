CREATE TABLE bio_analytics.kafka_sales_queue (
   before Tuple(id Int32, customerid String, amount Decimal(10, 2), datetime Int64),
   after  Tuple(id Int32, customerid String, amount Decimal(10, 2), datetime Int64),
   op     String
) ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:29092',
    kafka_topic_list = 'cdc.public.sales',
    kafka_group_name = 'clickhouse_sales_cdc_consumers',
    kafka_format = 'JSONEachRow',
    kafka_skip_broken_messages = 1;
