from google.auth.transport import requests as oauth2_requests_api
from google.oauth2 import id_token as oauth2_id_token_api
from http import HTTPStatus
import argparse
import flask
import functools
import importlib.util
import os

ROOT_PATH = os.path.dirname(os.path.abspath(__file__))

def import_endpoint(name):
    mod_name = f"module_import_{name}"
    path = os.path.join(ROOT_PATH, name, "main.py")
    spec = importlib.util.spec_from_file_location(mod_name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return getattr(module, name)

# We emulate authorization on our authenticated gcloud function endpoint
# https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
def emulate_authorization(endpoint):
    @functools.wraps(endpoint)
    def wrapper(req: flask.Request, *args, **kwargs) -> flask.typing.ResponseReturnValue:
        id_token = req.headers.get("Authorization", None)
        if id_token == None:
            res = { "error": "missing authorization token" }
            return flask.make_response(res, HTTPStatus.UNAUTHORIZED)
        if not id_token.startswith("Bearer "):
            res = { "error": "authorization header should begin with 'Bearer '" }
            return flask.make_response(res, HTTPStatus.UNAUTHORIZED)
        id_token = id_token.removeprefix("Bearer ")
        client_id = os.environ["OAUTH2_CLIENT_ID"]

        try:
            id_info = oauth2_id_token_api.verify_oauth2_token(id_token, oauth2_requests_api.Request(), client_id)
        except ValueError as ex:
            res = {
                "error": "failed to verify oauth2 token",
                "info": str(ex),
            }
            return flask.make_response(res, HTTPStatus.UNAUTHORIZED)
        return endpoint(req, *args, **kwargs)
    return wrapper

def provide_request(endpoint):
    @functools.wraps(endpoint)
    def wrapper(*args, **kwargs) -> flask.typing.ResponseReturnValue:
        req = flask.request
        return endpoint(req, *args, **kwargs)
    return wrapper

def init_app(app):
    oauth2_id_token_func = import_endpoint("oauth2_id_token")
    oauth2_login_func = import_endpoint("oauth2_login")
    get_gps_func = import_endpoint("get_gps")
    get_user_names_func = import_endpoint("get_user_names")
    post_gps_func = import_endpoint("post_gps")
    register_user_name_func = import_endpoint("register_user_name")
    visualise_tracks_func = import_endpoint("visualise_tracks")

    @app.route("/oauth2_id_token", methods=["POST"])
    @provide_request
    def oauth2_id_token(req):
        return oauth2_id_token_func(req)

    @app.route("/oauth2_login", methods=["GET", "POST"])
    @provide_request
    def oauth2_login(req):
        return oauth2_login_func(req)

    @app.route("/get_gps", methods=["GET"])
    @provide_request
    @emulate_authorization
    def get_gps(req):
        return get_gps_func(req)

    @app.route("/get_user_names", methods=["GET"])
    @provide_request
    @emulate_authorization
    def get_user_names(req):
        return get_user_names_func(req)

    @app.route("/post_gps", methods=["POST"])
    @provide_request
    def post_gps(req):
        return post_gps_func(req)

    @app.route("/register_user_name")
    @provide_request
    def register_user_name(req):
        return register_user_name_func(req)

    @app.route("/visualise_tracks/", methods=["GET"])
    @app.route("/visualise_tracks/<path:subpath>", methods=["GET"])
    @provide_request
    def visualise_tracks(req, *args, **kwargs):
        req.path = req.path.removeprefix("/visualise_tracks")
        return visualise_tracks_func(req)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--debug", action="store_true", default=False)
    parser.add_argument("--port", default=5000, type=int)
    parser.add_argument("--host", default="localhost", type=str)
    args = parser.parse_args()

    app = flask.Flask(__name__)
    init_app(app)
    app.run(debug=args.debug, port=args.port, host=args.host)

if __name__ == "__main__":
    main()
