# Introduction
Google datastore configuration.

## Commands
| Description | Command |
| --- | --- |
| Update index | ```gcloud datastore indexes create ./index.yaml``` |
| Download and export datastore | ```python ./export_datastore.py``` |
| Delete rows | ```python ./delete_datastore.py``` |
| Convert to GPS Visualiser track format | ```python ./convert_to_track_format.py``` |

## Performing initial setup
1. Enable and create Firebase datastore.
    - [Datastore](https://console.cloud.google.com/datastore/databases)
    - Make sure ```Database ID``` is ```(default)``` so you can use free tier.
2. Update database index: ```gcloud datastore indexes create ./index.yaml```

