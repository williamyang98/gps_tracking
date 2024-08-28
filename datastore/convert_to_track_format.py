from collections import namedtuple
import argparse
import csv
from datetime import datetime

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

def csv_row_to_gps_data(row):
    field_data = []
    for field, field_type, in zip(row, gps_data_field_types):
        if field:
            field = field_type(field)
        else:
            field = None
        field_data.append(field)
    return GPS_Data(*field_data)

def convert_to_trackpoint(d):
    time = datetime.fromtimestamp(d.unix_time_millis / 1000)
    # time format: yyyy-mm-dd hh:mm:ss
    time_str = time.strftime("%Y-%m-%d %H:%M:%S")
    return ("T", time_str, d.latitude, d.longitude)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", default="./data/gps_data.csv", type=str)
    parser.add_argument("--output", default="./data/gps_visualiser_track.csv", type=str)
    parser.add_argument("--user-id", default=0, type=int)
    args = parser.parse_args()

    with open(args.input, "r") as fp:
        reader = csv.reader(fp)
        next(reader, None) # skip header
        gps_data = [csv_row_to_gps_data(row) for row in reader if row]

    user_data = [d for d in gps_data if d.user_id == args.user_id]
    user_data.sort(key=lambda d: d.unix_time_millis, reverse=True)

    # SOURCE: https://www.gpsvisualizer.com/tutorials/waypoints.html
    with open(args.output, "w+", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow(("type", "name", "latitude", "longitude"))
        writer.writerows(map(convert_to_trackpoint, user_data))

if __name__ == "__main__":
    main()
