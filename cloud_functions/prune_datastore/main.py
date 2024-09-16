from cloudevents.http import CloudEvent
from google.cloud import datastore
import base64
import functions_framework
import os
import time

# https://cloud.google.com/functions/docs/tutorials/pubsub#functions_helloworld_pubsub_tutorial-python
@functions_framework.cloud_event
def prune_datastore(cloud_event: CloudEvent) -> None:
    attributes = cloud_event.data["message"]["attributes"]
    older_than_days = attributes["older_than_days"]
    older_than_days = int(older_than_days)

    client = datastore.Client(os.environ["PROJECT_ID"])
    query = client.query(kind="gps")

    print(f"Ignoring entries older than {older_than_days} days")
    unix_time = int(time.time())
    seconds_in_day = 60*60*24
    cutoff_time = unix_time - older_than_days*seconds_in_day
    cutoff_time_millis = cutoff_time*1000
    query.add_filter(filter=datastore.query.PropertyFilter("unix_time_millis", "<", cutoff_time_millis))
    query.order = ["-unix_time_millis"]
    results = query.fetch()

    del_keys = [row.key for row in results]
    if len(del_keys) == 0:
        print("No rows to delete")
        return

    print(f"Preparing to delete {len(del_keys)} rows")
    with client.transaction():
        client.delete_multi(del_keys)
    print("Finished delete operation")
