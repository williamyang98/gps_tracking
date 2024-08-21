from google.cloud import datastore
from collections import namedtuple
import argparse
import csv

GPS_Data = namedtuple("GPS_Data", ["user_id", "unix_time", "latitude", "longitude", "altitude"])

def datastore_to_gps_data(entry):
    user_id = int(entry["user_id"])
    unix_time = int(entry["unix_time"])
    latitude = float(entry["latitude"])
    longitude = float(entry["longitude"])
    altitude = float(entry["altitude"])
    return GPS_Data(user_id, unix_time, latitude, longitude, altitude)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="gps_data.csv", type=str)
    args = parser.parse_args()

    client = datastore.Client("gps-tracking-433211")
    query = client.query(kind="gps")
    results = query.fetch()
    gps_data = list(map(lambda x: datastore_to_gps_data(x), results))

    with open(args.output, "w+", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow(GPS_Data._fields)
        writer.writerows(gps_data)

if __name__ == "__main__":
    main()
