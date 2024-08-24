from google.cloud import datastore
import argparse
import time

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--user-id", default=None, type=int, help="Id of user to delete from")
    parser.add_argument("--older-than-days", default=None, type=int, help="Delete entries old than this")
    args = parser.parse_args()

    client = datastore.Client("gps-tracking-433211")
    query = client.query(kind="gps")
    if args.user_id != None:
        print(f"Filtering for user_id={args.user_id}")
        query.add_filter(filter=datastore.query.PropertyFilter("user_id", "=", args.user_id))
    if args.older_than_days != None:
        print(f"Ignoring entries older than {args.older_than_days} days")
        unix_time = int(time.time())
        seconds_in_day = 60*60*24
        cutoff_time = unix_time - args.older_than_days*seconds_in_day
        query.add_filter(filter=datastore.query.PropertyFilter("unix_time", "<", cutoff_time))
    query.order = ["-unix_time"]
    results = query.fetch()

    del_keys = [row.key for row in results]
    if len(del_keys) == 0:
        print("No rows to delete")
        return

    print(f"Preparing to delete {len(del_keys)} rows")

    while True:
        response = input("Do you want to continue [Y/n]? ")
        if response == "Y":
            break
        elif response == "n":
            print("Abandoning delete operation")
            return
        else:
            print("Must be either [Y/n]")

    print("Continuing with delete operation")
    with client.transaction():
        client.delete_multi(del_keys)
    print("Finished delete operation")

if __name__ == "__main__":
    main()
