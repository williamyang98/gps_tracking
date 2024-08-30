from http import HTTPStatus
import flask
import functions_framework
import os
import urllib

@functions_framework.http
def oauth2_login(req: flask.Request) -> flask.typing.ResponseReturnValue:
    redirect_uri = req.args.get("redirect_uri", None)
    if redirect_uri == None:
        return flask.make_response({ "error": "missing redirect_uri query parameter" }, HTTPStatus.BAD_REQUEST)

    API_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    query_params = {
        "client_id": os.environ["OAUTH2_CLIENT_ID"],
        "response_type": "code",
        "redirect_uri": redirect_uri,
        "scope": "https://www.googleapis.com/auth/userinfo.email openid",
        "include_granted_scopes": "true",
        "state": "",
    }
    query_string = urllib.parse.urlencode(query_params)
    return flask.redirect(f"{API_URL}?{query_string}")
