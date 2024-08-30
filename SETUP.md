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
- Refer to ```/cloud_functions``` and ```/datastore``` for additional initial setup and deployment instructions.
- Refer to ```/android_app``` for instructions on how to install or build Android app.

## Links to gcloud admin console
- [Cloud Functions](https://console.cloud.google.com/functions/list)
- [Datastore](https://console.cloud.google.com/datastore/databases/-default-/)
- [OAuth2 Consent Screen](https://console.cloud.google.com/apis/credentials/consent)
- [OAuth2 Credentials](https://console.cloud.google.com/apis/credentials)

# Plotting track with website
1. Enable [Google maps API](https://console.cloud.google.com/marketplace/product/google/maps-backend.googleapis.com).
2. Copy the api key into the visualiser when it prompts for it.

## Serving website from gcloud 
1. Go to dashboard: [Cloud functions](https://console.cloud.google.com/functions/list)
2. Get URL for ```/visualise_tracks/``` endpoint.

## Deploying website locally
1. Start local test server: ```python ./cloud_functions/test_local.py```.
2. Open visualiser: [Local URL](http://localhost:5000/visualise_tracks/index.html)

![Local visualiser](./docs/local_visualiser.png)
