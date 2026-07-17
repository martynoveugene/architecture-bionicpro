CREATE TABLE IF NOT EXISTS bio_analytics.bio_sensor_daily (
      day Date,
      customerId String,
      avg_temperature Float32,
      min_chargeLevel UInt8,
      max_chargeLevel UInt8
) ENGINE = MergeTree()
    ORDER BY (day, customerId);
