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
    parser.add_argument("--user-id", default=0, type=int)
    parser.add_argument("--latitude", default=-33.896962, type=float)
    parser.add_argument("--longitude", default=150.935728, type=float)
    parser.add_argument("--altitude", default=58.12345, type=float)
    parser.add_argument("--url", default="https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/post-gps", type=str)
    args = parser.parse_args()

    if args.local:
        url = "http://localhost:8080"
    else:
        url = args.url

    unix_time = int(time.time())
    gps_data = GPS_Data(args.user_id, unix_time, args.latitude, args.longitude, args.altitude)
    body = encode_gps_data(gps_data)
    res = requests.post(url, data=body)
    print(f"status_code: {res.status_code}")
    print(f"body: {res.text}")

if __name__ == '__main__':
    main()
