from collections import namedtuple
from http import HTTPStatus
import flask
import functions_framework
from google.cloud import datastore

@functions_framework.http
def register_user_name(req: flask.Request) -> flask.typing.ResponseReturnValue:
    if req.method != 'POST':
        return "POST request required", HTTPStatus.METHOD_NOT_ALLOWED

    body = req.get_json()
    user_id = body.get("user_id", None)
    if user_id == None:
        return "Missing user_id field", HTTPStatus.BAD_REQUEST
    user_name = body.get("user_name", None)
    if user_name == None:
        return "Missing user_name field", HTTPStatus.BAD_REQUEST

    try:
        user_id = int(user_id)
    except Exception as ex:
        return f"User id must be an integer: {ex}", HTTPStatus.BAD_REQUEST

    client = datastore.Client("gps-tracking-433211")
    kind = "username"
    query = client.query(kind=kind)
    query.add_filter(filter=datastore.query.PropertyFilter("user_id", "=", user_id))
    results = query.fetch(limit=1)

    entries = list(results)
    if len(entries) == 0:
        print(f"Registering new user_id={user_id}, user_name={user_name}")
        with client.transaction():
            key = client.key(kind)
            entry = datastore.Entity(key)
            entry.update({ "user_id": user_id, "user_name": user_name })
            client.put(entry)
    else:
        entry = entries[0]
        old_user_name = entry.get("user_name", None)
        print(f"Updating user_id={user_id}, old_user_name={old_user_name}, new_user_name={user_name}")
        with client.transaction():
            entry.update({ "user_name": user_name })
            client.put(entry)

    return f"", HTTPStatus.OK
