from concurrent.futures import ThreadPoolExecutor
import argparse
import functools
import os
import shutil
import subprocess
import sys

def get_runtime_args():
    deploy_region = os.environ.get("DEPLOY_REGION", None)
    if deploy_region == None:
        raise Exception("Missing environment key DEPLOY_REGION used for deploying gcloud function")
    return [
        "--trigger-http",
        "--region", deploy_region,
        "--runtime", "python310",
        "--gen2",
        "--memory", "128Mi",
        "--cpu", "0.083",
    ]

def add_source_args(name):
    abs_path = os.path.abspath(name)
    if not os.path.exists(abs_path):
        raise Exception(f"Directory for {name} doesn't exist: {abs_path}")
    return [
        "--source", abs_path,
        "--entry-point", name,
        name,
    ]

def create_environment_args(keys):
    args = []
    for key in keys:
        value = os.environ.get(key, None)
        if value == None:
            raise Exception(f"Missing environment key: {key}")
        args.extend(["--update-env-vars", f"{key}={value}"])
    return args

REGISTERED_ENDPOINTS = {}

def register_endpoint(name):
    def decorator(endpoint):
        REGISTERED_ENDPOINTS[name] = endpoint
        return endpoint
    return decorator

def allow_unauthenticated(endpoint):
    @functools.wraps(endpoint)
    def wrapper():
        args = endpoint()
        args.append("--allow-unauthenticated")
        return args
    return wrapper

@register_endpoint("oauth2_id_token")
@allow_unauthenticated
def endpoint_oauth2_id_token():
    return create_environment_args(["OAUTH2_CLIENT_ID", "OAUTH2_CLIENT_SECRET"])

@register_endpoint("oauth2_login")
@allow_unauthenticated
def endpoint_oauth2_login():
    return create_environment_args(["OAUTH2_CLIENT_ID"])

@register_endpoint("get_gps")
def endpoint_get_gps():
    return create_environment_args(["PROJECT_ID"])

@register_endpoint("get_user_names")
def endpoint_get_user_names():
    return create_environment_args(["PROJECT_ID"])

@register_endpoint("post_gps")
@allow_unauthenticated
def endpoint_post_gps():
    return create_environment_args(["PROJECT_ID"])

@register_endpoint("register_user_name")
@allow_unauthenticated
def endpoint_register_user_name():
    return create_environment_args(["PROJECT_ID"])

@register_endpoint("visualise_tracks")
@allow_unauthenticated
def endpoint_visualise_tracks():
    return []

def launch_endpoint(gcloud_path, name):
    endpoint = REGISTERED_ENDPOINTS.get(name, None)
    if endpoint == None:
        raise Exception(f"Endpoint {name} does not exist")
 
    args = [gcloud_path, "functions", "deploy", "--quiet"]
    args.extend(endpoint())
    args.extend(get_runtime_args())
    args.extend(add_source_args(name))
    process = subprocess.Popen(args, shell=True)
    print(f"Deploying {name}")
    return process

def join_process(name, process):
    process.wait()
    print(f"Finished deploying {name}")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("endpoints", nargs="*", default=[], type=str)
    parser.add_argument("--list", action="store_true", default=False)
    args = parser.parse_args()

    if args.list:
        print("Valid endpoints are: ")
        for name in REGISTERED_ENDPOINTS.keys():
            print(f"- {name}")
        return 0

    endpoints = [e for e in args.endpoints]
    for name in endpoints:
        if not name in REGISTERED_ENDPOINTS:
            raise Exception(f"No endpoint named '{name}'. Use --list to show valid endpoints")
    if len(endpoints) == 0:
        endpoints = REGISTERED_ENDPOINTS.keys()

    gcloud_path = shutil.which("gcloud")
    if gcloud_path == None:
        raise Exception(f"gcloud executable is missing from path")

    processes = []
    print(f"Deploying {len(endpoints)} endpoints")
    for name in endpoints:
        process = launch_endpoint(gcloud_path, name)
        processes.append((name, process))

    total_threads = len(processes)
    with ThreadPoolExecutor(total_threads) as executor:
        for (name, process) in processes:
            executor.submit(join_process, name, process)
    print(f"Finished deploying {len(processes)} endpoints")
    return 0

if __name__ == "__main__":
    try:
        rv = main()
        sys.exit(rv)
    except Exception as ex:
        print(ex)
        sys.exit(1)
