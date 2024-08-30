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

# Plotting track with GPS Visualiser
## Download track data
1. Download datastore as csv: ```python ./export_datastore.py```.
2. Convert to track format: ```python ./convert_to_track_format.py```.

## Upload track data
1. Enable [Google maps API](https://console.cloud.google.com/marketplace/product/google/maps-backend.googleapis.com).
2. Make sure there are no restrictions to referall URL or usage type.
4. Upload converted track csv to [GPS_Visualiser](https://www.gpsvisualizer.com/map_input?form=html&format=google).
5. (Optional) Copy Google maps api key.
6. Setup options according to screenshot.
7. Press ```Draw the map```.

### Options
![Visualiser options](../docs/gps_visualiser_options.png)

### Viewer
![Visualiser viewer](../docs/gps_visualiser_viewer.png)
