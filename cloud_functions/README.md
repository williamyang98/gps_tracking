# Introduction
- Cloud functions for serverless endpoints. 
- This requires setting up the Firebase datastore in Google cloud first.

## Commands
| Description | Command |
| --- | --- |
| Deploy function to gcloud | ```./deploy_function.sh <folder_name> <id>``` |
| Test function locally | ```./test_function.sh <folder_name>``` |
| Submit gps sample to server | ```python ./post_gps.py --local``` |
| Register username | ```python ./post_user_name.py <id> <name> --local``` |

## Urls
- [get_gps](https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/get-gps)
- [get_track](https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/get-track)
- [get_user_names](https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/get-user-names)
- [get_gps (attachment)](https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/get-gps?download=gps_data.csv&max_rows=128)
- [get_track (attachment)](https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/get-track?download=gps_visualiser_track.csv&max_rows=128&user_id=0)
