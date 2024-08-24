from collections import namedtuple
import argparse
import csv
from datetime import datetime

GPS_Data = namedtuple("GPS_Data", ["user_id", "unix_time", "latitude", "longitude", "altitude"])

def csv_row_to_gps_data(row):
    user_id = int(row[0])
    unix_time = int(row[1])
    latitude = float(row[2])
    longitude = float(row[3])
    altitude = float(row[4])
    return GPS_Data(user_id, unix_time, latitude, longitude, altitude)

def convert_to_trackpoint(d):
    time = datetime.fromtimestamp(d.unix_time)
    # time format: yyyy-mm-dd hh:mm:ss
    time_str = time.strftime("%Y-%m-%d %H:%M:%S")
    return ("T", time_str, d.latitude, d.longitude, d.altitude)

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
    user_data.sort(key=lambda d: d.unix_time, reverse=True)

    # SOURCE: https://www.gpsvisualizer.com/tutorials/waypoints.html
    with open(args.output, "w+", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow(("type", "name", "latitude", "longitude", "alt"))
        writer.writerows(map(convert_to_trackpoint, user_data))

if __name__ == "__main__":
    main()
