CREATE TABLE IF NOT EXISTS bio_sensor (
  timestamp TIMESTAMP NOT NULL,
  customerId VARCHAR(255) NOT NULL,
  temperature NUMERIC(5,2),
  chargeLevel INT
);

COPY bio_sensor(timestamp, customerId, temperature, chargeLevel)
    FROM '/tmp/data.csv'
    DELIMITER ','
    CSV HEADER;
