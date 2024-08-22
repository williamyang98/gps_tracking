#!/bin/sh
# Usage: <folder_name> <endpoint_name>
gcloud functions deploy \
    --trigger-http \
    --region australia-southeast1 \
    --allow-unauthenticated \
    --runtime python310 \
    --gen2 \
    --memory 128Mi \
    --cpu 0.083 \
    --source ./$1 \
    --entry-point $1 \
    $2
