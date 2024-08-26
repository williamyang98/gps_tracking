const GPS_API_URL = "https://australia-southeast1-gps-tracking-433211.cloudfunctions.net"

const parse_csv = (data) => {
  return data
  	.split("\n")
    .map(row => row.trim())
    .map(row => row
    	.split(",")
    	.map(col => col.trim())
      .filter(col => col.length > 0)
    )
    .filter(row => row.length > 0);
}

export class TrackPoint {
  constructor(name, latitude, longitude, altitude) {
    this.name = name
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
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
  static get_track = async ({ user_id, max_rows=128, timezone=10 }) => {
    let params = [];
    params.push(`user_id=${encodeURIComponent(user_id)}`);
    params.push(`max_rows=${encodeURIComponent(max_rows)}`);
    params.push(`timezone=${encodeURIComponent(timezone)}`);
    let base_url = `${GPS_API_URL}/get-track`;
    let url = `${base_url}?${params.join("&")}`;
    let response = await fetch(url);
    let body = await response.text();
    let csv_data = parse_csv(body);
    let gps_points = convert_csv_to_objects(
      csv_data,
      ["name", "latitude", "longitude", "alt"],
      ([name, latitude, longitude, altitude]) => {
        latitude = Number(latitude);
        longitude = Number(longitude);
        altitude = Number(altitude);
        return new TrackPoint(name, latitude, longitude, altitude);
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

