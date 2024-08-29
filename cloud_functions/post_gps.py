from collections import namedtuple
import requests
import struct
import time
import argparse

GPS_Data = namedtuple("GPS_Data", [
    "unix_time_millis",
    "battery_percentage",
    "battery_charging",
    "latitude", "longitude", "accuracy",
    "altitude", "altitude_accuracy",
    "msl_altitude", "msl_altitude_accuracy",
    "speed", "speed_accuracy",
    "bearing", "bearing_accuracy",
])

def encode_gps_data(d: GPS_Data) -> bytearray:
    encode_data = bytearray([])
    battery_data = int(min(max(d.battery_percentage, 0), 100))
    if d.battery_charging:
        battery_data |= (1 << 7)
    encode_data.extend(struct.pack("<QBdd", d.unix_time_millis, battery_data, d.latitude, d.longitude))

    extension_flags = 0x0000
    extension_format = [
        "f",
        "f", "f",
        "f", "f",
        "f", "f",
        "f", "f",
    ]
    extension_data = [
        d.accuracy,
        d.altitude, d.altitude_accuracy,
        d.msl_altitude, d.msl_altitude_accuracy,
        d.speed, d.speed_accuracy,
        d.bearing, d.bearing_accuracy,
    ]
    struct_format = []
    struct_data = []
    for index, (format, data) in enumerate(zip(extension_format, extension_data)):
        if data == None:
            continue
        extension_flags |= (1 << index)
        struct_format.append(format)
        struct_data.append(data)
    struct_format = ''.join(struct_format)
    encode_data.extend(struct.pack(f"<H{struct_format}", extension_flags, *struct_data))
    return encode_data

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--user-id", default=3, type=int)
    parser.add_argument("--battery-percentage", default=69, type=int)
    parser.add_argument("--battery-charging", action="store_true", default=False)
    parser.add_argument("--latitude", default=-33.896962, type=float)
    parser.add_argument("--longitude", default=150.935728, type=float)
    parser.add_argument("--accuracy", default=None, type=float)
    parser.add_argument("--altitude", default=None, type=float)
    parser.add_argument("--altitude-accuracy", default=None, type=float)
    parser.add_argument("--msl-altitude", default=None, type=float)
    parser.add_argument("--msl-altitude-accuracy", default=None, type=float)
    parser.add_argument("--speed", default=None, type=float)
    parser.add_argument("--speed-accuracy", default=None, type=float)
    parser.add_argument("--bearing", default=None, type=float)
    parser.add_argument("--bearing-accuracy", default=None, type=float)
    parser.add_argument("--url", default="http://localhost:5000", type=str)
    args = parser.parse_args()

    unix_time_millis = int(time.time() * 1000)
    gps_data = GPS_Data(
        unix_time_millis,
        args.battery_percentage, bool(args.battery_charging),
        args.latitude, args.longitude, args.accuracy,
        args.altitude, args.altitude_accuracy,
        args.msl_altitude, args.msl_altitude_accuracy,
        args.speed, args.speed_accuracy,
        args.bearing, args.bearing_accuracy,
    )
    print(gps_data)
    body = encode_gps_data(gps_data)
    post_url = f"{url}/post_gps?user_id={args.user_id}"
    res = requests.post(post_url, data=body)
    print(f"status_code: {res.status_code}")
    print(f"body: {res.text}")

if __name__ == '__main__':
    main()
