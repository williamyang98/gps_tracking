const GPS_API_URL = "https://australia-southeast1-gps-tracking-433211.cloudfunctions.net"

const parse_csv = (data) => {
  return data
    .split("\n")
    .map(row => row.trim())
    .filter(row => row.length > 0)
    .map(row => row
      .split(",")
      .map(col => col.trim())
      .map(col => (col.length === 0) ? null : col)
    )
    .filter(row => row.length > 0);
}

export class GpsPoint {
  constructor(
    user_id,
    unix_time_millis,
    battery_percentage,
    battery_charging,
    latitude, longitude, accuracy,
    altitude, altitude_accuracy,
    msl_altitude, msl_altitude_accuracy,
    speed, speed_accuracy,
    bearing, bearing_accuracy,
  ) {
    this.user_id = user_id;
    this.unix_time_millis = unix_time_millis;
    this.battery_percentage = battery_percentage;
    this.battery_charging = battery_charging;
    this.latitude = latitude;
    this.longitude = longitude;
    this.accuracy = accuracy;
    this.altitude = altitude;
    this.altitude_accuracy = altitude_accuracy;
    this.msl_altitude = msl_altitude;
    this.msl_altitude_accuracy = msl_altitude_accuracy;
    this.speed = speed;
    this.speed_accuracy = speed_accuracy;
    this.bearing = bearing;
    this.bearing_accuracy = bearing_accuracy;
  }
}

const convert_csv_to_objects = (csv, headers, transform) => {
  let csv_header = csv[0];
  let csv_body = csv.slice(1);
  let RECEIVED_HEADERS = new Set(csv_header);
  const EXPECTED_HEADERS = new Set(headers);
  if (!EXPECTED_HEADERS.isSubsetOf(RECEIVED_HEADERS)) {
    console.error(`Csv did not get expected headers (${EXPECTED_HEADERS}) from received headers (${RECEIVED_HEADERS})`);
    return [];
  }
  let indexes = headers.map(header => csv_header.indexOf(header));
  let rows = csv_body.map(row => {
    let cols = indexes.map(index => row[index]);
    let entity = transform(cols);
    return entity;
  });
  return rows;
}

export class User {
  constructor(id, name) {
    this.id = id;
    this.name = name;
  }
}

export class GpsApi {
  static get_gps = async ({ user_id, max_rows=128 }) => {
    let params = [];
    params.push(`user_id=${encodeURIComponent(user_id)}`);
    params.push(`max_rows=${encodeURIComponent(max_rows)}`);
    let base_url = `${GPS_API_URL}/get-gps`;
    let url = `${base_url}?${params.join("&")}`;
    let response = await fetch(url);
    let body = await response.text();
    let csv_data = parse_csv(body);
    let gps_points = convert_csv_to_objects(
      csv_data,
      [
        "user_id",
        "unix_time_millis",
        "battery_percentage",
        "battery_charging",
        "latitude", "longitude", "accuracy",
        "altitude", "altitude_accuracy",
        "msl_altitude", "msl_altitude_accuracy",
        "speed", "speed_accuracy",
        "bearing", "bearing_accuracy",
      ],
      ([
        user_id,
        unix_time_millis,
        battery_percentage,
        battery_charging,
        latitude, longitude, accuracy,
        altitude, altitude_accuracy,
        msl_altitude, msl_altitude_accuracy,
        speed, speed_accuracy,
        bearing, bearing_accuracy,
      ]) => {
        user_id = Number(user_id);
        unix_time_millis = Number(unix_time_millis);
        battery_percentage = Number(battery_percentage);
        battery_charging = Boolean(battery_charging == "True");
        latitude = Number(latitude);
        longitude = Number(longitude);
        accuracy = (accuracy !== null) ? Number(accuracy) : null;
        altitude = (altitude !== null) ? Number(altitude) : null;
        altitude_accuracy = (altitude_accuracy !== null) ? Number(altitude_accuracy) : null;
        msl_altitude = (msl_altitude !== null) ? Number(msl_altitude) : null;
        msl_altitude_accuracy = (msl_altitude_accuracy !== null) ? Number(msl_altitude_accuracy) : null;
        speed = (speed !== null) ? Number(speed) : null;
        speed_accuracy = (speed_accuracy !== null) ? Number(speed_accuracy) : null;
        bearing = (bearing !== null) ? Number(bearing) : null;
        bearing_accuracy = (bearing_accuracy !== null) ? Number(bearing_accuracy) : null;
        return new GpsPoint(
          user_id,
          unix_time_millis,
          battery_percentage,
          battery_charging,
          latitude, longitude, accuracy,
          altitude, altitude_accuracy,
          msl_altitude, msl_altitude_accuracy,
          speed, speed_accuracy,
          bearing, bearing_accuracy,
        );
      },
    );
    return gps_points;
  }

  static get_users = async () => {
    let url = `${GPS_API_URL}/get-user-names`;
    let response = await fetch(url);
    let body = await response.text();
    let csv_data = parse_csv(body);
    let user_names = convert_csv_to_objects(
      csv_data,
      ["user_id", "user_name"],
      ([id, name]) => new User(id, name),
    );
    return user_names;
  }
}

