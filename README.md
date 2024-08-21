# Setting up Google cloud project
1. Create project at: ```https://console.cloud.google.com/```.
2. Create Firebase datastore
    - [Datastore](https://console.cloud.google.com/datastore/databases)
    - Make sure ```Database ID``` is ```(default)``` so you can use free tier.
3. Create Google cloud function
    - [Cloud functions](https://console.cloud.google.com/functions)
    - Click on ```Create function``` and copy and paste ```test_google_function.py```.
    - Deploy serverless cloud function.

# Local development
## Setup gcloud
1. Install gcloud.
2. Initalise gcloud: ```gcloud init```.
3. Create local authentication credentials for user account: ```gcloud auth application-default login```.

## Setup python environment
1. Setup virtual environment: ```py -m venv venv```.
2. Activate virtual environment: ```source ./venv/*/activate```.
3. Install requirements: ```pip install -r requirements.txt```.

## Running commands
This requires setting up the Firebase datastore in Google cloud first.

| Descripton | Command |
| --- | --- |
| Get project id | ```gcloud config get-value project``` |
| Test serverless function locally | ```functions-framework --target post_gps --source ./test_google_function.py --debug``` |
| Test endpoint locally | ```python ./test_endpoint.py --local``` |
| Download and export datastore | ```python ./export_datastore.py``` |
| Convert to track format | ```python ./convert_to_track_format.py``` |

# Plotting track
1. Enable [Google maps API](https://console.cloud.google.com/marketplace/product/google/maps-backend.googleapis.com).
2. Make sure there are no restrictions to referall URL or usage type.
4. Upload converted track csv to [GPS_Visualiser](https://www.gpsvisualizer.com/map_input?form=html&format=google).
5. (Optional) Copy Google maps api key.
6. Setup options according to screenshot.
7. Press ```Draw the map```.

## Options
![Visualiser options](./docs/gps_visualiser_options.png)

## Viewer
![Visualiser viewer](./docs/gps_visualiser_viewer.png)
