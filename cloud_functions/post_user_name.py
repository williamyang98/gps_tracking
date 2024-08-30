from collections import namedtuple
import requests
import argparse

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("user_id", type=int)
    parser.add_argument("user_name", type=str)
    parser.add_argument("--url", default="http://localhost:5000", type=str)
    args = parser.parse_args()

    body = { "user_id": args.user_id, "user_name": args.user_name }
    res = requests.post(f"{args.url}/register_user_name", json=body)
    print(f"status_code: {res.status_code}")
    print(f"body: {res.text}")

if __name__ == '__main__':
    main()
