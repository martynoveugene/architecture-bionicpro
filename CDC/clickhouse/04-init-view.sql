-- Мост из Kafka в сырую таблицу
CREATE MATERIALIZED VIEW bio_analytics.mv_kafka_to_sales_raw TO bio_analytics.sales_raw AS
SELECT
    if(op = 'd', before.id, after.id) AS id,
    if(op = 'd', before.customerid, after.customerid) AS customerId,
    if(op = 'd', before.amount, after.amount) AS amount,
    if(op = 'd', toDateTime(before.datetime / 1000000), toDateTime(after.datetime / 1000000)) AS datetime,
    if(op = 'd', before.datetime, after.datetime) AS version,
    -- Если удаление, ставим маркер -1, иначе 1
    if(op = 'd', -1, 1) AS sign
FROM bio_analytics.kafka_sales_queue
WHERE op IN ('c', 'u', 'd');

-- Мост из сырых данных в витрину отчетов
CREATE MATERIALIZED VIEW bio_analytics.mv_sales_to_daily_report TO bio_analytics.daily_sales_report AS
SELECT
    toDate(datetime) AS sale_date,
    sum(amount * sign) AS total_amount
FROM bio_analytics.sales_raw
GROUP BY sale_date;
