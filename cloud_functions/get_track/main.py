from collections import namedtuple
from datetime import datetime, timedelta, timezone
from google.cloud import datastore
from http import HTTPStatus
import flask
import functions_framework

GPS_Data = namedtuple("GPS_Data", ["user_id", "unix_time", "latitude", "longitude", "altitude"])

def datastore_to_gps_data(entry):
    user_id = int(entry["user_id"])
    unix_time = int(entry["unix_time"])
    latitude = float(entry["latitude"])
    longitude = float(entry["longitude"])
    altitude = float(entry["altitude"])
    return GPS_Data(user_id, unix_time, latitude, longitude, altitude)

TRACK_HEADERS = ("type", "name", "latitude", "longitude", "alt")
def convert_to_trackpoint(d, tz):
    time = datetime.fromtimestamp(d.unix_time, tz=tz)
    # time format: yyyy-mm-dd hh:mm:ss
    time_str = time.strftime("%Y-%m-%d %H:%M:%S")
    return ("T", time_str, d.latitude, d.longitude, d.altitude)

def enable_cors(endpoint):
    # https://cloud.google.com/functions/docs/samples/functions-http-cors
    def cors_endpoint(req: flask.Request) -> flask.typing.ResponseReturnValue:
        # Allows GET requests from any origin with the Content-Type
        # header and caches preflight response for an 3600s
        if req.method == "OPTIONS":
            headers = {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Methods": "GET",
                "Access-Control-Allow-Headers": "Content-Type",
                "Access-Control-Max-Age": "3600",
            }
            return ("", 204, headers)
        res = endpoint(req)
        res.headers["Access-Control-Allow-Origin"] = "*"
        return res
    return cors_endpoint

@functions_framework.http
@enable_cors
def get_track(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'GET':
        return "GET request required", HTTPStatus.METHOD_NOT_ALLOWED

    user_id = req.args.get("user_id", 0)
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

    tz = req.args.get("timezone", 10.0)
    if tz != None:
        try:
            tz = float(tz)
        except Exception as ex:
            return f"Timezone must be a float", HTTPStatus.BAD_REQUEST
    try:
        tz = timezone(timedelta(hours=tz))
    except:
        return f"Invalid timezone: {ex}", HTTPStatus.BAD_REQUEST

    client = datastore.Client("gps-tracking-433211")
    query = client.query(kind="gps")
    query.add_filter(filter=datastore.query.PropertyFilter("user_id", "=", user_id))
    query.order = ["-unix_time"]
    results = query.fetch(limit=max_rows)

    def create_csv(results):
        yield ','.join(TRACK_HEADERS)
        yield '\n'
        for row in results:
            data = datastore_to_gps_data(row)
            track = convert_to_trackpoint(data, tz)
            yield ','.join(map(str, track))
            yield '\n'

    res = flask.make_response((create_csv(results), HTTPStatus.OK))
    if download_name != None:
        res.headers["Content-Type"] = "text/csv; charset=utf8-8"
        res.headers["Content-Disposition"] = f"attachment;filename={download_name}"
    else:
        res.headers["Content-Type"] = "text/plain; charset=utf8-8"
        res.headers["Content-Disposition"] = "inline"
    return res
