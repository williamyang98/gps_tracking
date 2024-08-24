# Setting up Google cloud project
1. Create project at: ```https://console.cloud.google.com/```.
2. Create Firebase datastore
    - [Datastore](https://console.cloud.google.com/datastore/databases)
    - Make sure ```Database ID``` is ```(default)``` so you can use free tier.

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
- This requires setting up the Firebase datastore in Google cloud first.
- Refer to ```/cloud_functions``` and ```/datastore``` for commands.

# Plotting track
## Download track data
- Option 1: [Download from server (attachment)](https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/get-track?download=gps_visualiser_track.csv&max_rows=128&user_id=0)
- Option 2: Download locally
    1. Download datastore as csv: ```python ./datastore/export_datastore.py```.
    2. Convert to track format: ```python ./datastore/convert_to_track_format.py```.

## Upload track data
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
