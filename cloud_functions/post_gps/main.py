from collections import namedtuple
from http import HTTPStatus
import flask
import functions_framework
from google.cloud import datastore
import struct

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
def decode_gps_data(encode_data):
    gps_data_list = []
    extension_format = [
        "f",
        "f", "f",
        "f", "f",
        "f", "f",
        "f", "f",
    ]
    buffer = encode_data
    header_size = 8 + 1 + 8 + 8 + 2

    while len(buffer) > 0:
        if len(buffer) < header_size:
            raise Exception(f"Buffer is not large enough to contain header ({header_size} > {len(buffer)})")
        header_data = struct.unpack("<QBddH", buffer[:header_size])
        buffer = buffer[header_size:]
        # mandatory header data
        unix_time_millis, battery_data, latitude, longitude, extension_flags = header_data
        battery_percentage = battery_data & 0x7F
        battery_charging = (battery_data & 0x80) != 0x00
        # determine extension format
        struct_format = []
        extension_field_index = []
        for index, format in enumerate(extension_format):
            is_present = (extension_flags & (1 << index)) != 0x00
            if not is_present:
                continue
            extension_field_index.append(index)
            struct_format.append(format)
        struct_format = "".join(struct_format)
        extension_size = struct.calcsize(struct_format)
        if len(buffer) < extension_size:
            raise Exception(f"Buffer is not large enough to contain extension fields ({extension_size} > {len(buffer)})")
        extension_present_fields = struct.unpack(f"<{struct_format}", buffer[:extension_size])
        buffer = buffer[extension_size:]
        # unzip data
        extension_fields = [None for _ in extension_format]
        for index, field in zip(extension_field_index, extension_present_fields):
            extension_fields[index] = field 
        gps_data = GPS_Data(unix_time_millis, battery_percentage, battery_charging, latitude, longitude, *extension_fields)
        gps_data_list.append(gps_data)
    return gps_data_list

def detect_gps_data_errors(gps_data: GPS_Data):
    errors = []
    if abs(gps_data.latitude) > 90:
        errors.append(f"Latitude must be between -90 and +90 ({gps_data.latitude})")
    if abs(gps_data.longitude) > 180:
        errors.append(f"Longitude must be between -180 and +180 ({gps_data.longitude})")
    if gps_data.bearing != None and (gps_data.bearing < 0 or gps_data.bearing > 360):
        errors.append(f"Bearing must be between 0 and 360 ({gps_data.bearing})")
    if gps_data.bearing_accuracy != None and (gps_data.bearing_accuracy < 0 or gps_data.bearing_accuracy > 360):
        errors.append(f"Bearing accuracy must be between 0 and 360 ({gps_data.bearing_accuracy})")
    return errors

@functions_framework.http
def post_gps(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'POST':
        return "POST request required", HTTPStatus.METHOD_NOT_ALLOWED

    user_id = req.args.get("user_id", None)
    if user_id == None:
        return "user_id must be specified", HTTPStatus.BAD_REQUEST
    try:
        user_id = int(user_id)
    except:
        return "user_id must be an integer", HTTPStatus.BAD_REQUEST

    body = req.get_data()
    try:
        gps_data_list = decode_gps_data(body)
    except struct.error:
        return "invalid binary format", HTTPStatus.BAD_REQUEST
    except Exception as ex:
        return f"error while parsing binary data: {ex}", HTTPStatus.BAD_REQUEST

    all_errors = []
    for index, gps_data in enumerate(gps_data_list):
        errors = detect_gps_data_errors(gps_data)
        if len(errors) > 0:
            all_errors.append({ "index": index, "errors": errors })
    if len(all_errors) > 0:
        return { "type": "validate_gps_data",  "errors": all_errors }, HTTPStatus.UNPROCESSABLE_ENTITY

    client = datastore.Client("gps-tracking-433211")
    with client.transaction():
        key = client.key("gps")
        for gps_data in gps_data_list:
            entry = datastore.Entity(key)
            data = gps_data._asdict()
            data["user_id"] = user_id
            entry.update(data)
            client.put(entry)
    return f"", HTTPStatus.OK
