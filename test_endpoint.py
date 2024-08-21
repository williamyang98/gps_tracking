from collections import namedtuple
import requests
import struct
import time
import argparse

GPS_Data = namedtuple("GPS_Data", ["user_id", "unix_time", "latitude", "longitude", "altitude"])

def encode_gps_data(gps_data):
    return struct.pack("<IIddd", *gps_data)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--local", action="store_true")
    args = parser.parse_args()

    if args.local:
        url = "http://localhost:8080"
    else:
        url = "https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/post-gps"

    user_id = 0
    unix_time = int(time.time())
    latitude = -33.896962
    longitude = 150.935728
    altitude = 58.12345
    gps_data = GPS_Data(user_id, unix_time, latitude, longitude, altitude)
    body = encode_gps_data(gps_data)
    res = requests.post(url, data=body)
    print(f"status_code: {res.status_code}")
    print(f"body: {res.text}")

if __name__ == '__main__':
    main()
