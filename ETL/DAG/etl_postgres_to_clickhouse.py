from datetime import datetime, timedelta
from airflow.decorators import dag, task
from airflow.providers.postgres.hooks.postgres import PostgresHook
import clickhouse_connect

CLICKHOUSE_CONN = {
    'host': 'clickhouse-bio-analytics',
    'port': 8123,
    'user': 'clickuser',
    'password': 'clickpassword',
    'database': 'bio_analytics'
}

default_args = {
    'owner': 'analytics',
    'depends_on_past': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

@dag(
    dag_id='etl_postgres_bio_to_clickhouse',
    default_args=default_args,
    schedule='@daily',
    start_date=datetime(2026, 7, 11),
    catchup=True,
    tags=['biometrics', 'postgres', 'clickhouse'],
)
def bio_sensor_etl():

    @task()
    def extract_and_aggregate(ds=None):
        pg_hook = PostgresHook(postgres_conn_id='postgres_default')
        query = """
                SELECT
                    timestamp::date AS day,
                customerId,
                ROUND(AVG(temperature), 2)::float AS avg_temperature,
                MIN(chargeLevel)::int AS min_chargeLevel,
                MAX(chargeLevel)::int AS max_chargeLevel
                FROM bio_sensor
                WHERE timestamp::date = %s
                GROUP BY timestamp::date, customerId; \
                """
        records = pg_hook.get_records(query, parameters=(ds,))
        return records

    @task()
    def load_to_clickhouse(data, ds=None):
        if not data:
            print(f"Нет данных для отправки за дату {ds}")
            return

        client = clickhouse_connect.get_client(**CLICKHOUSE_CONN)

        ds_str = str(ds)
        client.command(f"ALTER TABLE bio_sensor_daily DELETE WHERE day = '{ds_str}'")

        column_names = ['day', 'customerId', 'avg_temperature', 'min_chargeLevel', 'max_chargeLevel']
        client.insert('bio_sensor_daily', data, column_names=column_names)

        print(f"Успешно загружено {len(data)} строк в ClickHouse за {ds_str}")
        client.close()

    aggregated_data = extract_and_aggregate()
    load_to_clickhouse(aggregated_data)

bio_sensor_etl_dag = bio_sensor_etl()
