# Introduction
Scripts for testing google cloud function.

## Running commands
This requires setting up the Firebase datastore in Google cloud first.

| Descripton | Command |
| --- | --- |
| Get project id | ```gcloud config get-value project``` |
| Submit gps sample to server | ```python ./post_gps.py --local``` |
| Download and export datastore | ```python ./export_datastore.py``` |
| Convert to track format | ```python ./convert_to_track_format.py``` |
