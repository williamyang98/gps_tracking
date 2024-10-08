from collections import namedtuple
from google.cloud import datastore
from http import HTTPStatus
import flask
import functions_framework
import os
import struct

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

@functions_framework.http
def get_gps(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'GET':
        return "GET request required", HTTPStatus.METHOD_NOT_ALLOWED

    user_id = req.args.get("user_id")
    if user_id != None:
        try:
            user_id = int(user_id)
        except:
            return "user_id must be an integer", HTTPStatus.BAD_REQUEST

    download_name = req.args.get("download", None)
    MAX_DOWNLOAD_NAME = 128
    if download_name != None and len(download_name) > MAX_DOWNLOAD_NAME:
        return f"download name must be less than {MAX_DOWNLOAD_NAME} characters", HTTPStatus.BAD_REQUEST

    DEFAULT_MAX_ROWS = 25
    max_rows = req.args.get("max_rows", DEFAULT_MAX_ROWS)
    try:
        max_rows = int(max_rows)
    except:
        return "max_rows must be an integer", HTTPStatus.BAD_REQUEST
    if max_rows <= 0:
        return "max_rows must be a positive non zero integer", HTTPStatus.BAD_REQUEST

    older_than_millis = req.args.get("older_than_millis", None)
    if older_than_millis != None:
        try:
            older_than_millis = int(older_than_millis)
        except:
            return "older_than_millis must be an integer", HTTPStatus.BAD_REQUEST
        if older_than_millis <= 0:
            return "older_than_millis must be a positive non zero integer", HTTPStatus.BAD_REQUEST

    client = datastore.Client(os.environ["PROJECT_ID"])
    query = client.query(kind="gps")
    if user_id != None:
        query.add_filter(filter=datastore.query.PropertyFilter("user_id", "=", user_id))
    query.order = ["-unix_time_millis"]
    if older_than_millis != None:
        query.add_filter(filter=datastore.query.PropertyFilter("unix_time_millis", "<", older_than_millis))
    results = query.fetch(limit=max_rows)

    def create_csv(results):
        yield ','.join(GPS_Data._fields)
        yield '\n'
        for row in results:
            data = datastore_to_gps_data(row)
            yield ','.join(map(lambda x: str(x) if x != None else "", data))
            yield '\n'

    res = flask.make_response((create_csv(results), HTTPStatus.OK))
    if download_name != None:
        res.headers["Content-Type"] = "text/csv; charset=utf8-8"
        res.headers["Content-Disposition"] = f"attachment;filename={download_name}"
    else:
        res.headers["Content-Type"] = "text/plain; charset=utf8-8"
        res.headers["Content-Disposition"] = "inline"
    return res
