import argparse
import flask
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

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--debug", action="store_true", default=False)
    parser.add_argument("--port", default=5000, type=int)
    parser.add_argument("--host", default="localhost", type=str)
    args = parser.parse_args()

    app = flask.Flask(__name__)

    get_gps_func = import_endpoint("get_gps")
    get_user_names_func = import_endpoint("get_user_names")
    post_gps_func = import_endpoint("post_gps")
    register_user_name_func = import_endpoint("register_user_name")
    visualise_tracks_func = import_endpoint("visualise_tracks")

    @app.route("/get_gps")
    def get_gps():
        return get_gps_func(flask.request)

    @app.route("/get_user_names")
    def get_user_names():
        return get_user_names_func(flask.request)

    @app.route("/post_gps")
    def post_gps():
        return post_gps_func(flask.request)

    @app.route("/register_user_name")
    def register_user_name():
        return register_user_name_func(flask.request)

    @app.route("/visualise_tracks/")
    @app.route("/visualise_tracks/<path:subpath>")
    def visualise_tracks(*args, **kwargs):
        req = flask.request
        req.path = req.path.removeprefix("/visualise_tracks")
        return visualise_tracks_func(req)

    app.run(debug=args.debug, port=args.port, host=args.host)

if __name__ == "__main__":
    main()
