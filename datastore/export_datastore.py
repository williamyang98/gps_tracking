from collections import namedtuple
from google.cloud import datastore
import argparse
import csv
import os

GPS_Data = namedtuple("GPS_Data", [
    "user_id",
    "unix_time_millis",
    "battery_percentage",
    "battery_charging",
    "latitude", "longitude", "accuracy",
    "altitude", "altitude_accuracy",
    "msl_altitude", "msl_altitude_accuracy",
    "speed", "speed_accuracy",
    "bearing", "bearing_accuracy",
])

gps_data_field_types = [
    int, # user_id
    int, # unix_time_millis
    int, # battery_percentage
    bool, # battery_charging
    float, float, float, # latitude, longitude, accuracy
    float, float, # altitude, altitude_accuracy
    float, float, # msl_altitude, msl_altitude_accuracy
    float, float, # speed, speed_accuracy
    float, float, # bearing, bearing_accuracy
]

def datastore_to_gps_data(entry):
    field_data = []
    for field, field_type in zip(GPS_Data._fields, gps_data_field_types):
        data = entry.get(field, None)
        if data != None:
            data = field_type(data)
        field_data.append(data)
    return GPS_Data(*field_data)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="./data/gps_data.csv", type=str)
    args = parser.parse_args()

    client = datastore.Client(os.environ["PROJECT_ID"])
    query = client.query(kind="gps")
    query.order = ["-unix_time_millis"]
    results = query.fetch()
    gps_data = map(lambda x: datastore_to_gps_data(x), results)

    with open(args.output, "w+", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow(GPS_Data._fields)
        total_rows = 0
        for row in gps_data:
            writer.writerow(row)
            total_rows += 1
        print(f"Wrote {total_rows} rows")

if __name__ == "__main__":
    main()
