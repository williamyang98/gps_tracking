# Introduction
- Cloud functions for serverless endpoints. 
- This requires setting up the Firebase datastore in Google cloud first.

## Commands
| Description | Command |
| --- | --- |
| Deploy function/s to gcloud | ```./deploy_function.sh [folder_name]``` |
| Test functions locally | ```python ./test_local.py``` |
| Submit gps sample to server | ```python ./post_gps.py``` |
| Register username | ```python ./post_user_name.py <id> <name>``` |

## Performining initial setup
1. Enable cloud functions api in dashboard: [Cloud functions](https://console.cloud.google.com/functions/list).
2. Deploy all functions to gcloud: ```./deploy_function.sh```.
