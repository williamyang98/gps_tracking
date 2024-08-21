from collections import namedtuple
from http import HTTPStatus
import flask
import functions_framework
from google.cloud import datastore
import struct

GPS_Data = namedtuple("GPS_Data", ["user_id", "unix_time", "latitude", "longitude", "altitude"])
def decode_gps_data(data):
    gps_data = struct.unpack("<IIddd", data)
    return GPS_Data(*gps_data)

@functions_framework.http
def post_gps(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'POST':
        return "POST request required", HTTPStatus.METHOD_NOT_ALLOWED
    body = req.get_data()
    try:
        gps_data = decode_gps_data(body)
    except struct.error:
        return "Invalid binary format", HTTPStatus.BAD_REQUEST
    client = datastore.Client("gps-tracking-433211")
    with client.transaction():
        key = client.key("gps")
        entry = datastore.Entity(key, exclude_from_indexes=("unix_time", "latitude", "longitude", "altitude"))
        entry.update(gps_data._asdict())
        client.put(entry)
    return f"", HTTPStatus.OK
