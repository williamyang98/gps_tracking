import { GpsApi } from "./api.js";

const hsv_to_rgb = (h, s, v) => {
  let i = Math.floor(h * 6);
  let f = h*6-i;
  let p = v*(1-s);
  let q = v*(1-f*s);
  let t = v*(1-(1-f)*s);
  var r, g, b;
  switch (i % 6) {
    case 0: r = v, g = t, b = p; break;
    case 1: r = q, g = v, b = p; break;
    case 2: r = p, g = v, b = t; break;
    case 3: r = p, g = q, b = v; break;
    case 4: r = t, g = p, b = v; break;
    case 5: r = v, g = p, b = q; break;
  }
  return [
    Math.round(r * 255),
    Math.round(g * 255),
    Math.round(b * 255)
  ];
}

const get_battery_symbol = (percentage, is_charging) => {
  if (is_charging) {
    return "⚡";
  } else if (percentage > 50) {
    return "🔋";
  } else {
    return "🪫";
  }
}

const is_status_code_unauthorized = (status_code) => {
  switch (status_code) {
    case 401: return true;
    case 403: return true;
    default: return false;
  }
}

export const GOOGLE_MAP_API_KEY_INDEX = "google_maps_api_key";

const create_timeline_control = () => {
  let elem = document.createElement("input");
  elem.classList.add("w-100");
  elem.id = "timeline_control";
  elem.type = "range";
  elem.min = "0";
  elem.max = "1";
  elem.value = "0";
  elem.step = "1";
  return elem;
}

export class App {
  constructor() {
    this.elems = {
      "login_button": document.getElementById("login_button"),
      "map": document.getElementById("map"),
      "user_count": document.getElementById("user_count"),
      "user_list": document.getElementById("user_list"),
      "user_list_refresh": document.getElementById("user_list_refresh"),
      "gps_count": document.getElementById("gps_count"),
      "gps_points": document.getElementById("gps_points"),
      "gps_points_select_up": document.getElementById("gps_points_select_up"),
      "gps_points_select_down": document.getElementById("gps_points_select_down"),
      "gps_points_show_all": document.getElementById("gps_points_show_all"),
      "gps_points_refresh": document.getElementById("gps_points_refresh"),
      "gps_points_render": document.getElementById("gps_points_render"),
      "timeline_control": create_timeline_control(),
      "settings": {
        "max_rows": document.getElementById("settings_max_rows"),
        "before_datetime": document.getElementById("settings_before_datetime"),
        "api_key": document.getElementById("settings_api_key"),
        "zoom_level": document.getElementById("settings_zoom_level"),
        "stroke_coloured": document.getElementById("settings_stroke_coloured"),
        "stroke_hue": document.getElementById("settings_stroke_hue"),
        "stroke_weight": document.getElementById("settings_stroke_weight"),
        "stroke_weight_falloff": document.getElementById("settings_stroke_weight_falloff"),
        "stroke_opacity": document.getElementById("settings_stroke_opacity"),
        "stroke_opacity_falloff": document.getElementById("settings_stroke_opacity_falloff"),
        "marker_size": document.getElementById("settings_marker_size"),
        "marker_size_falloff": document.getElementById("settings_marker_size_falloff"),
        "marker_opacity": document.getElementById("settings_marker_opacity"),
        "marker_opacity_falloff": document.getElementById("settings_marker_opacity_falloff"),
        "marker_outline": document.getElementById("settings_marker_outline"),
        "show_info_popup": document.getElementById("settings_show_info_popup"),
        "show_info_close": document.getElementById("settings_show_info_close"),
      },
    };
    this.datetime_format = new Intl.DateTimeFormat(undefined, {
      day: '2-digit',
      month: '2-digit',
      year: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    });
    this.map = new google.maps.Map(this.elems.map, {
      zoom: 4,
      center: { lat: 0, lng: 0 },
      mapTypeId: "roadmap",
    })
    this.users = [];
    this.selected_user = null;
    this.gps_points = [];
    this.map_points = [];
    this.map_lines = [];
    this.map_markers = [];
    this.map_info = new google.maps.InfoWindow();
    this.table_rows = [];
    this.selected_marker_index = null;
    this.bind_controls();
    this.persist_settings();
    this.create_map_controls();
    this.attempt_login();
    this.load_users();
  }

  create_map_controls = () => {
    let control = this.elems.timeline_control;
    let parent_div = document.createElement("div");
    parent_div.classList.add("timeline-controls");
    let label = document.createElement("label");
    label.innerText = "Timeline";
    parent_div.appendChild(label);
    parent_div.appendChild(control);
    this.map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push(parent_div);
  }

  log_out = () => {
    localStorage.removeItem("id_token");
    this.elems.login_button.innerText = "Log in";
    this.elems.user_list_refresh.disabled = true;
    this.elems.gps_points_refresh.disabled = true;
  }

  set_logged_in = () => {
    this.elems.login_button.innerText = "Log out";
    this.elems.user_list_refresh.disabled = false;
  }

  attempt_login = () => {
    if (this.is_logged_in()) {
      this.set_logged_in();
    } else {
      this.log_out();
    }

    let params = new URLSearchParams(window.location.search);
    let auth_code = params.get("code");
    if (auth_code === null) return;
    let res = GpsApi.get_id_token(auth_code);
    res
      .then((id_token) => {
        localStorage.setItem("id_token", id_token);
        this.set_logged_in();
        // Redirect to same page but without authentication code
        // This way we cant accidentally refresh and end up using a already used authorization code
        const REDIRECT_URL = `${window.location.protocol}//${window.location.host}${window.location.pathname}`;
        window.location.href = REDIRECT_URL;
      })
      .catch((ex) => {
        console.error(`Failed to get id_token: ${ex}`);
        this.log_out();
        alert("Authorization token was invalid");
      })
  }

  format_unix_time = (unix_time_millis) => {
    let date = new Date(unix_time_millis);
    let parts = this.datetime_format.formatToParts(date);
    parts = parts.reduce((parts, part) => {
      if (part.type !== "literal" || part.value.trim() !== ",") {
        parts[part.type] = part.value;
      }
      return parts;
    }, {});
    return `${parts.year}/${parts.month}/${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`;
  }

  persist_settings = () => {
    let get_value = (key, default_value) => {
      let value = localStorage.getItem(key);
      return (value === null) ? default_value : value;
    };
    let set_value = (key, value) => {
      localStorage.setItem(key, value);
    };
    let persist_number_input = (elem, key, default_value) => {
      elem.value = Number(get_value(key, default_value));
      elem.addEventListener("change", (ev) => {
        let value = Number(ev.target.value);
        if (value !== null && value !== undefined) {
          set_value(key, value);
        }
      });
    };
    let persist_string_input = (elem, key, default_value) => {
      elem.value = get_value(key, default_value);
      elem.addEventListener("change", (ev) => {
        let value = ev.target.value;
        if (value !== null && value !== undefined) {
          set_value(key, value);
        }
      });
    };
    let persist_checked_input = (elem, key, default_value) => {
      let value = get_value(key, default_value);
      elem.checked = (value === "true" || value === true);
      elem.addEventListener("change", (ev) => {
        let value = Boolean(ev.target.checked);
        if (value !== null && value !== undefined) {
          set_value(key, value);
        }
      });
    };
    let elems = this.elems.settings;
    persist_number_input(elems.max_rows, "max_rows", 25);
    persist_string_input(elems.api_key, GOOGLE_MAP_API_KEY_INDEX, "");
    persist_number_input(elems.zoom_level, "zoom_level", 20);
    persist_checked_input(elems.stroke_coloured, "stroke_coloured", true);
    persist_number_input(elems.stroke_hue, "stroke_hue", 0.7);
    persist_number_input(elems.stroke_weight, "stroke_weight", 6);
    persist_number_input(elems.stroke_weight_falloff, "stroke_weight_falloff", 0.25);
    persist_number_input(elems.stroke_opacity, "stroke_opacity", 1);
    persist_number_input(elems.stroke_opacity_falloff, "stroke_opacity_falloff", 0.75);
    persist_number_input(elems.marker_size, "marker_size", 8);
    persist_number_input(elems.marker_size_falloff, "marker_size_falloff", 0.25);
    persist_number_input(elems.marker_opacity, "marker_opacity", 1);
    persist_number_input(elems.marker_opacity_falloff, "marker_opacity_falloff", 0.75);
    persist_number_input(elems.marker_outline, "marker_outline", 0.5);
    persist_checked_input(elems.show_info_popup, "show_info_popup", true);
    persist_checked_input(elems.show_info_close, "show_info_close", false);
  }

  is_logged_in = () => {
    return localStorage.getItem("id_token") !== null;
  }

  bind_controls = () => {
    this.elems.login_button.addEventListener("click", (ev) => {
      ev.preventDefault();
      if (this.is_logged_in()) {
        let is_confirm = confirm("Are you sure you want to log out?");
        if (is_confirm) {
          this.log_out();
        }
      } else {
        let login_url = GpsApi.get_login_url();
        window.location.href = login_url;
      }
    });
    // When website is loading this button is disabled
    this.elems.login_button.disabled = false;
    this.elems.user_list.addEventListener("change", (ev) => {
      let id = Number(ev.target.value);
      let user = this.users.find(user => user.id == id);
      if (user !== undefined) {
        this.selected_user = user;
        this.elems.gps_points_refresh.disabled = false;
        this.load_user(user.id);
      }
    });
    this.elems.user_list_refresh.addEventListener("click", (ev) => {
      ev.preventDefault();
      this.load_users();
    });
    this.elems.gps_points_refresh.addEventListener("click", (ev) => {
      ev.preventDefault();
      if (this.selected_user !== null) {
        this.load_user(this.selected_user.id);
      }
    });
    this.elems.gps_points_render.addEventListener("click", (ev) => {
      ev.preventDefault();
      this.render_track();
    });
    this.elems.gps_points_show_all.addEventListener("click", (ev) => {
      ev.preventDefault();
      this.show_all_markers();
    });
    this.elems.gps_points_select_up.addEventListener("click", (ev) => {
      ev.preventDefault();
      if (this.selected_marker_index === null) return;
      if (this.selected_marker_index === 0) return;
      this.select_marker(this.selected_marker_index-1);
    });
    this.elems.gps_points_select_down.addEventListener("click", (ev) => {
      ev.preventDefault();
      if (this.selected_marker_index === null) return;
      if (this.selected_marker_index >= (this.map_markers.length - 1)) return;
      this.select_marker(this.selected_marker_index+1);
    });
    this.elems.timeline_control.addEventListener("input", (ev) => {
      let index = Number(ev.target.value);
      if (index != this.selected_marker_index) {
        this.select_marker(index);
      }
    });
  }

  load_users = async() => {
    this.elems.user_list_refresh.disabled = true;
    try {
      await this._load_users();
    } catch (ex) {
      console.error(`Failed to load users: ${ex}`);
    }
    this.elems.user_list_refresh.disabled = false;
  }

  _load_users = async () => {
    let id_token = localStorage.getItem("id_token");
    if (id_token === null) {
      console.error("Missing id token");
      return;
    }
    var users = null;
    try {
      users = await GpsApi.get_users(id_token);
    } catch (response) {
      if (is_status_code_unauthorized(response.status)) {
        this.log_out();
        alert("Failed to fetch list of users because current login session was unauthorized");
      } else {
        console.error("GpsApi.get_users(...) failed with unhandled error");
        console.error(response);
      }
      return;
    }
    this.users = users;
    this.elems.user_count.innerText = `Users (${users.length})`
    let option_elems = users.map(user => {
      let elem = document.createElement("option");
      elem.value = user.id;
      elem.innerHTML = `${user.id}: ${user.name}`;
      elem.selected = false;
      return elem;
    });
    this.elems.user_list.replaceChildren();

    let default_option = document.createElement("option");
    default_option.value = "";
    default_option.innerHTML = "None selected";
    default_option.disabled = true;
    default_option.selected = true;
    default_option.hidden = true;
    this.elems.user_list.appendChild(default_option);
    option_elems.forEach(elem => this.elems.user_list.appendChild(elem));
    if (this.selected_user !== null) {
      let index = this.users.findIndex(user => user.id == this.selected_user.id);
      if (index !== -1) {
        option_elems[index].selected = true;
      }
    }
  }

  load_user = async (user_id) => {
    this.elems.gps_points_refresh.disabled = true;
    try {
      await this._load_user(user_id);
    } catch (ex) {
      console.error(`Failed to refresh gps points: ${ex}`);
    } 
    this.elems.gps_points_refresh.disabled = false;
  }

  _load_user = async (user_id) => {
    let id_token = localStorage.getItem("id_token");
    if (id_token === null) {
      console.error("Missing id token");
      return;
    }
    let max_rows = Number(this.elems.settings.max_rows.value);
    let before_datetime_string = this.elems.settings.before_datetime.value;
    var older_than_millis = null;
    if (before_datetime_string !== '') {
      try {
        let date = new Date(before_datetime_string);
        older_than_millis = date.getTime();
      } catch (ex) {
        console.error(`Failed to read before_datetime string: ${ex}`);
      }
    }
    var gps_points = null;
    try {
      gps_points = await GpsApi.get_gps({ user_id, id_token, older_than_millis, max_rows });
    } catch (response) {
      if (is_status_code_unauthorized(response.status)) {
        this.log_out();
        alert("Failed to fetch gps data because current login session was unauthorized");
      } else {
        console.error("GpsApi.get_gps(...) failed with unhandled error");
        console.error(response);
      }
      return;
    }
    // create html table
    this.elems.gps_count.innerText = `GPS Points (${gps_points.length})`;
    let elem = this.elems.gps_points;
    elem.replaceChildren();
    if (gps_points.length === 0) {
      let row = document.createElement("tr");
      row.innerHTML = "<td colspan=2><div class='text-center w-100'>No entries</div></td>";
      elem.appendChild(row);
      this.table_rows = [];
      return;
    }
    this.table_rows = gps_points.map((row, index) => {
      let row_elem = document.createElement("tr");
      let date_string = this.format_unix_time(row.unix_time_millis);
      var battery_symbol = get_battery_symbol(row.battery_percentage, row.battery_charging);
      row_elem.innerHTML = `
        <td style='white-space: nowrap'>${date_string}</td>
        <td>${row.battery_percentage}% ${battery_symbol}</td>
      `;
      row_elem.addEventListener("click", (ev) => {
        ev.preventDefault();
        this.select_marker(index);
      });
      elem.appendChild(row_elem);
      return row_elem;
    });
    this.gps_points = gps_points;
    this.map_points = gps_points.map(row => { return { lat: row.latitude, lng: row.longitude }; });
    // render track on map
    this.elems.timeline_control.min = 0;
    this.elems.timeline_control.max = Math.max(gps_points.length-1, 0);
    this.elems.timeline_control.value = 0;
    this.render_track();
  }

  render_track = () => {
    let gps_points = this.gps_points;
    let map_points = this.map_points;
    let map = this.map;
    if (gps_points.length === 0) return;
    this.clear_map_path();

    let total_points = gps_points.length;
    let step = 1 / total_points;
    let to_hex = (v) => {
      let x = Number(v).toString(16);
      let padded = `00${x}`;
      let n = padded.length - 2;
      return padded.substring(n);
    };

    let zoom_level = Number(this.elems.settings.zoom_level.value);
    let stroke_coloured = this.elems.settings.stroke_coloured.checked;
    let stroke_hue = Number(this.elems.settings.stroke_hue.value);
    let stroke_weight = Number(this.elems.settings.stroke_weight.value);
    let stroke_weight_falloff = Number(this.elems.settings.stroke_weight_falloff.value);
    let stroke_opacity = Number(this.elems.settings.stroke_opacity.value);
    let stroke_opacity_falloff = Number(this.elems.settings.stroke_opacity_falloff.value);
    let marker_size = Number(this.elems.settings.marker_size.value);
    let marker_size_falloff = Number(this.elems.settings.marker_size_falloff.value);
    let marker_opacity = Number(this.elems.settings.marker_opacity.value);
    let marker_opacity_falloff = Number(this.elems.settings.marker_opacity_falloff.value);
    let marker_outline = Number(this.elems.settings.marker_outline.value);

    map.setCenter(map_points[0])
    map.setZoom(zoom_level)

    let colours = gps_points.map((_row, index) => {
      let amount = step*index;
      let hue = amount*stroke_hue;
      let [r,g,b] = hsv_to_rgb(hue, 1, 1);
      let rgb = [r,g,b].map(c => to_hex(c)).join("");
      let colour = `#${rgb}`;
      return colour;
    });

    if (stroke_coloured) {
      this.map_lines = map_points
        .slice(0,-1)
        .map((_, index) => {
          index = map_points.length-index-2;
          let point = map_points[index];
          let other_point = map_points[index+1];
          let amount = step*index;
          let weight = stroke_weight*(1-amount*stroke_weight_falloff);
          let opacity = stroke_opacity*(1-amount*stroke_opacity_falloff);
          let line = new google.maps.Polyline({
            path: [point, other_point],
            geodesic: true,
            strokeColor: colours[index],
            strokeOpacity: opacity,
            strokeWeight: weight,
          });
          line.setMap(map);
          return line;
        })
        .reverse();
    } else {
      let line = new google.maps.Polyline({
        path: map_points,
        geodesic: true,
        strokeColor: "#444444",
        strokeWeight: stroke_weight,
        strokeOpacity: stroke_opacity,
      });
      line.setMap(map);
      this.map_lines = [line];
    }

    this.map_markers = gps_points
      .map((_, index) => {
        index = gps_points.length-index-1;
        let row = gps_points[index];
        let amount = step*index;
        let fill_color = colours[index];
        let scale = marker_size*(1-amount*marker_size_falloff);
        let fill_opacity = marker_opacity*(1-amount*marker_opacity_falloff);
        let marker = new google.maps.Marker({
          map: map,
          position: { lat: row.latitude, lng: row.longitude },
          title: this.format_unix_time(row.unix_time_millis),
          icon: {
            path: google.maps.SymbolPath.CIRCLE,
            scale: scale,
            fillColor: fill_color,
            fillOpacity: fill_opacity,
            strokeWeight: 1,
            strokeOpacity: marker_outline,
          },
          optimized: true,
        });
        marker.addListener("click", () => this.select_marker(index));
        return marker;
      })
      .reverse();
  }

  clear_map_path = () => {
    this.map_lines.forEach(line => line.setMap(null));
    this.map_lines = [];
    this.map_markers.forEach(marker => marker.setMap(null));
    this.map_markers = [];
    this.selected_marker_index = null;
    this.elems.gps_points_select_down.disabled = true;
    this.elems.gps_points_select_up.disabled = true;
    this.table_rows.forEach(row => row.classList.remove("selected"));
  }

  select_marker = (index) => {
    let total = this.map_markers.length;
    if (total === 0) return;
    if (index < 0) return;
    if (index >= total) return;
    if (index === this.selected_marker_index) {
      this.show_all_markers();
      return;
    }
    this.elems.timeline_control.min = 0;
    this.elems.timeline_control.max = total-1;
    this.elems.timeline_control.value = index;
    this.selected_marker_index = index;
    this.elems.gps_points_select_up.disabled = (index == 0);
    this.elems.gps_points_select_down.disabled = (index >= (total-1));
    let marker = this.map_markers[index];
    let gps_point = this.gps_points[index];
    let map_point = this.map_points[index];
    this.map.setCenter(map_point);
    // show only selected marker
    for (let marker_index = 0; marker_index < total; marker_index++) {
      let marker = this.map_markers[marker_index];
      let table_row = this.table_rows[marker_index];
      let is_selected = marker_index == index;
      if (is_selected) {
        table_row.classList.add("selected");
      } else {
        table_row.classList.remove("selected");
      }
      marker.setVisible(is_selected);
      // marker.setOpacity(is_selected ? 1 : 0.2);
    }
    // show info popup
    if (!this.elems.settings.show_info_popup.checked) {
      this.map_info.close();
      return;
    }
    this.map_info.open({
      anchor: marker,
      map: this.map,
    });
    let date_string = this.format_unix_time(gps_point.unix_time_millis);
    let rows = [];
    let push_row = (label, body) => {
      rows.push(`<tr><td>${label}</td><td>${body}</td></tr>`);
    };
    push_row("Time", `${date_string}`);
    let battery_symbol = get_battery_symbol(gps_point.battery_percentage, gps_point.battery_charging);
    push_row("Battery", `${gps_point.battery_percentage}% ${battery_symbol}`);
    push_row("Latitude", gps_point.latitude.toFixed(6));
    push_row("Longitude", gps_point.longitude.toFixed(6));
    if (gps_point.accuracy !== null) {
      push_row("Accuracy", `± ${gps_point.accuracy.toFixed(2)} m`);
    }
    if (gps_point.altitude !== null) {
      let body = gps_point.altitude.toFixed(2);
      if (gps_point.altitude_accuracy !== null) {
        body += ` ± ${gps_point.altitude_accuracy.toFixed(2)}`;
      }
      push_row("Altitude", `${body} m`);
    }
    if (gps_point.msl_altitude !== null) {
      let body = gps_point.msl_altitude.toFixed(2);
      if (gps_point.msl_altitude_accuracy !== null) {
        body += ` ± ${gps_point.msl_altitude_accuracy.toFixed(2)}`;
      }
      push_row("MSL Altitude", `${body} m`);
    }
    if (gps_point.speed !== null) {
      let body = gps_point.speed.toFixed(2);
      if (gps_point.speed_accuracy !== null) {
        body += ` ± ${gps_point.speed_accuracy.toFixed(2)}`;
      }
      push_row("Speed", `${body} m/s`);
    }
    if (gps_point.bearing !== null) {
      let body = gps_point.bearing.toFixed(2);
      if (gps_point.bearing_accuracy !== null) {
        body += ` ± ${gps_point.bearing_accuracy.toFixed(2)}`;
      }
      push_row("Bearing", `${body} °`);
    }

    var hide_close_button_html = "";
    if (!this.elems.settings.show_info_close.checked) {
      hide_close_button_html = `
        <style>
          .gm-ui-hover-effect {
            display: none !important;
          }
        </style>
      `;
    }
    this.map_info.setContent(`
      ${hide_close_button_html}
      <div style="margin: 2px">
        <table>
          <tbody>
            ${rows.join("\n")}
          </tbody>
        </table>
      </div>
    `);
  }

  show_all_markers = () => {
    if (this.map_markers.length === 0) return;
    this.selected_marker_index = null;
    this.map_info.close();
    this.elems.gps_points_select_up.disabled = true;
    this.elems.gps_points_select_down.disabled = true;
    let total = this.map_markers.length;
    for (let index = 0; index < total; index++) {
      let marker = this.map_markers[index];
      marker.setVisible(true);
      // marker.setOpacity(1);
      let table_row = this.table_rows[index];
      table_row.classList.remove("selected");
    };
  }
}
