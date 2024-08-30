#!/bin/bash
# Usage: <folder_name>
deploy_function () {
    echo Deploying endpoint $1
    gcloud functions deploy \
        --trigger-http \
        --region australia-southeast1 \
        --allow-unauthenticated \
        --update-env-vars PROJECT_ID=${PROJECT_ID} \
        --update-env-vars OAUTH2_CLIENT_ID=${OAUTH2_CLIENT_ID} \
        --update-env-vars OAUTH2_CLIENT_SECRET=${OAUTH2_CLIENT_SECRET} \
        --runtime python310 \
        --gen2 \
        --memory 128Mi \
        --cpu 0.083 \
        --source ./$1 \
        --entry-point $1 \
        $1
}

if [[ -z "${PROJECT_ID}" ]]; then
    echo "Missing PROJECT_ID environment variable"
    echo "Make sure that you have run 'source ./setup_env.sh' first"
    exit 1
fi

if [[ $# -eq 0 ]] ; then
    echo "Deploying all functions"
    deploy_function "oauth2_id_token" &
    deploy_function "oauth2_login" &
    deploy_function "get_gps" &
    deploy_function "get_user_names" &
    deploy_function "post_gps" &
    deploy_function "register_user_name" &
    deploy_function "visualise_tracks" &
    wait
else
    deploy_function $1
fi
