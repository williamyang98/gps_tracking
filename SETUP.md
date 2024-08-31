# Local development
## 1. Setup gcloud
1. Create project at: ```https://console.cloud.google.com/```.
2. Install gcloud.
3. Initalise gcloud: ```gcloud init```.
4. Create local authentication credentials for user account: ```gcloud auth application-default login```.

## 2. Setup python environment
1. Setup virtual environment: ```py -m venv venv```.
2. Activate virtual environment: ```source ./venv/*/activate```.
3. Install requirements: ```pip install -r requirements.txt```.

## 3. Performining initial setup
- **(IMPORTANT)** Setup environment variables: ```./setup_env.sh```.
    - Refer to ```example_setup_env.sh``` for variables used by the project.
    - Set ```PROJECT_ID``` to the output of the command ```gcloud config get-value project```.
    - ```/cloud_functions``` also requires additional variables to be added to ```setup_env.sh```.
    - Every time you are developing or editing this project make sure to run this script to setup environment variables
    - NOTE: This requires a bash shell (or git-bash shell): ```source ./setup_env.sh```
- Refer to the following sections and their ```README.md``` to setup each part of the project in the correct order.
    1. ```/datastore```: Setup database for storing GPS data points.
    2. ```/cloud_functions```: Setup server endpoints for receiving and serving GPS data points.
    3. ```/android_app```: Installation instructions and optional build instructions for Android app.

## Useful links to gcloud admin console
- [Cloud Functions](https://console.cloud.google.com/functions/list)
- [Datastore](https://console.cloud.google.com/datastore/databases/-default-/)
- [OAuth2 Consent Screen](https://console.cloud.google.com/apis/credentials/consent)
- [OAuth2 Credentials](https://console.cloud.google.com/apis/credentials)
- [Google Maps Javascript API](https://console.cloud.google.com/marketplace/product/google/maps-backend.googleapis.com)
