# Introduction
- Cloud functions for serverless endpoints. 
- This requires setting up the Firebase datastore in Google cloud first.

## Commands
| Description | Command |
| --- | --- |
| Deploy function/s to gcloud | ```python ./deploy_function.py``` |
| Test functions locally | ```python ./test_local.py``` |
| Submit gps sample to server | ```python ./post_gps.py``` |
| Register username | ```python ./post_user_name.py <id> <name>``` |

## Performining initial setup
1. Enable cloud functions api in dashboard: [Cloud Functions](https://console.cloud.google.com/functions/list).
2. Deploy all functions to gcloud: ```python ./deploy_function.py```.
3. Create oauth2 consent screen: [OAuth2 Consent Screen](https://console.cloud.google.com/apis/credentials/consent)
4. Create oauth2 client id: [OAuth2 Credentials](https://console.cloud.google.com/apis/credentials)
    - Create an OAuth 2.0 client id.
    - Add the URL which all your cloud functions are stored on as an authorized javascript origin and authorized redirect URIs
    - Copy the client id and client secret values into your ```setup_env.sh``` file.

![Oauth2 client credentials](../docs/gcloud/oauth2_credentials.png)
