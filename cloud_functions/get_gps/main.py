from collections import namedtuple
from http import HTTPStatus
import flask
import functions_framework
from google.cloud import datastore
import struct

GPS_Data = namedtuple("GPS_Data", ["user_id", "unix_time", "latitude", "longitude", "altitude"])

def datastore_to_gps_data(entry):
    user_id = int(entry["user_id"])
    unix_time = int(entry["unix_time"])
    latitude = float(entry["latitude"])
    longitude = float(entry["longitude"])
    altitude = float(entry["altitude"])
    return GPS_Data(user_id, unix_time, latitude, longitude, altitude)

@functions_framework.http
def get_gps(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'GET':
        return "GET request required", HTTPStatus.METHOD_NOT_ALLOWED

    user_id = req.args.get("user_id")
    if user_id != None:
        try:
            user_id = int(user_id)
        except:
            return "User_id must be an integer", HTTPStatus.BAD_REQUEST

    download_name = req.args.get("download", None)
    MAX_DOWNLOAD_NAME = 128
    if download_name != None and len(download_name) > MAX_DOWNLOAD_NAME:
        return f"Download name must be less than {MAX_DOWNLOAD_NAME} characters", HTTPStatus.BAD_REQUEST

    max_rows = req.args.get("max_rows", None)
    if max_rows != None:
        try:
            max_rows = int(max_rows)
        except:
            return "Max rows must be an integer", HTTPStatus.BAD_REQUEST

    client = datastore.Client("gps-tracking-433211")
    query = client.query(kind="gps")
    if user_id != None:
        query.add_filter(filter=datastore.query.PropertyFilter("user_id", "=", user_id))
    query.order = ["-unix_time"]
    results = query.fetch(limit=max_rows)

    def create_csv(results):
        yield ','.join(GPS_Data._fields)
        yield '\n'
        for row in results:
            data = datastore_to_gps_data(row)
            yield ','.join(map(str, data))
            yield '\n'

    res = flask.make_response((create_csv(results), HTTPStatus.OK))
    if download_name != None:
        res.headers["Content-Type"] = "text/csv; charset=utf8-8"
        res.headers["Content-Disposition"] = f"attachment;filename={download_name}"
    else:
        res.headers["Content-Type"] = "text/plain; charset=utf8-8"
        res.headers["Content-Disposition"] = "inline"
    return res
