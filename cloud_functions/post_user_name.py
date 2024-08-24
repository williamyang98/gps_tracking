from collections import namedtuple
import requests
import argparse

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("user_id", type=int)
    parser.add_argument("user_name", type=str)
    parser.add_argument("--local", action="store_true")
    parser.add_argument("--url", default="https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/register-user-name", type=str)
    args = parser.parse_args()

    if args.local:
        url = "http://localhost:8080"
    else:
        url = args.url

    body = { "user_id": args.user_id, "user_name": args.user_name }
    res = requests.post(url, json=body)
    print(f"status_code: {res.status_code}")
    print(f"body: {res.text}")

if __name__ == '__main__':
    main()
