CREATE TABLE bio_analytics.daily_sales_report (
    sale_date Date,
    total_amount Decimal(18, 2)
) ENGINE = SummingMergeTree()
ORDER BY sale_date;
