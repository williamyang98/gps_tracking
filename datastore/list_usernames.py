from collections import namedtuple
from google.cloud import datastore
from tabulate import tabulate
import os

User = namedtuple("User", ["user_id", "user_name"])

def datastore_to_user(entry):
    user_id = int(entry["user_id"])
    user_name = entry["user_name"]
    return User(user_id, user_name)

def main():
    client = datastore.Client(os.environ["PROJECT_ID"])
    query = client.query(kind="username")
    query.order = ["user_id"]
    results = query.fetch()
    users = list(map(lambda x: datastore_to_user(x), results))
    print(tabulate(users, headers=User._fields))

if __name__ == "__main__":
    main()
