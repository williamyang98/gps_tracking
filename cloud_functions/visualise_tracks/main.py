from http import HTTPStatus
import flask
import functions_framework
import os

STATIC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static")

@functions_framework.http
def visualise_tracks(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'GET':
        return "GET request required", HTTPStatus.METHOD_NOT_ALLOWED
    file_path = req.path
    if file_path.startswith("/"):
        file_path = file_path[1:]
    if file_path == "":
        return flask.redirect(req.url + "visualise_tracks/index.html")
    return flask.send_from_directory(STATIC_DIR, file_path)
