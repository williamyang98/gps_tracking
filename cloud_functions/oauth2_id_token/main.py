from http import HTTPStatus
import flask
import functions_framework
import os
import requests

@functions_framework.http
def oauth2_id_token(req: flask.Request) -> flask.typing.ResponseReturnValue:
    BASE_URL = "https://www.googleapis.com/oauth2/v4/token"

    req_data = req.json
    auth_code = req_data.get("auth_code", None)
    redirect_uri = req_data.get("redirect_uri", None)
    if auth_code == None:
        return flask.make_response({ "error": "missing auth_code parameter in request" }, HTTPStatus.BAD_REQUEST)
    if redirect_uri == None:
        return flask.make_response({ "error": "missing redirect_uri parameter in request" }, HTTPStatus.BAD_REQUEST)

    query_params = {
        "code": auth_code,
        "grant_type": "authorization_code",
        "client_id": os.environ["OAUTH2_CLIENT_ID"],
        "client_secret": os.environ["OAUTH2_CLIENT_SECRET"],
        "redirect_uri": redirect_uri,
    }
    res = requests.post(BASE_URL, data=query_params)

    res_data = res.json()
    if res.status_code != 200:
        data = {
            "error": "failed to get oauth2 id token from authorization code",
            "status_code": res.status_code,
            "response_body": res_data,
        }
        return flask.make_response(data , HTTPStatus.UNAUTHORIZED)

    id_token = res_data.get("id_token", None)
    if id_token == None:
        return flask.make_response({ "error": "missing oauth2 id token" }, HTTPStatus.BAD_REQUEST)
    return flask.make_response({ "id_token": id_token }, HTTPStatus.OK)
