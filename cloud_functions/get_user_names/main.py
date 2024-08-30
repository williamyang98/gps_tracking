from collections import namedtuple
from google.cloud import datastore
from http import HTTPStatus
import flask
import functions_framework
import os

User = namedtuple("User", ["user_id", "user_name"])

def datastore_to_user(entry):
    user_id = int(entry["user_id"])
    user_name = entry["user_name"]
    return User(user_id, user_name)

@functions_framework.http
def get_user_names(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'GET':
        return "GET request required", HTTPStatus.METHOD_NOT_ALLOWED

    download_name = req.args.get("download", None)
    MAX_DOWNLOAD_NAME = 128
    if download_name != None and len(download_name) > MAX_DOWNLOAD_NAME:
        return f"Download name must be less than {MAX_DOWNLOAD_NAME} characters", HTTPStatus.BAD_REQUEST

    client = datastore.Client(os.environ["PROJECT_ID"])
    query = client.query(kind="username")
    query.order = ["user_id"]
    results = query.fetch()

    def create_csv(results):
        yield ','.join(User._fields)
        yield '\n'
        for row in results:
            data = datastore_to_user(row)
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
