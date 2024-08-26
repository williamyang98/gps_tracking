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

export class App {
  constructor() {
    this.elems = {
      "map": document.getElementById("map"),
      "user_list": document.getElementById("user_list"),
      "user_list_refresh": document.getElementById("user_list_refresh"),
      "gps_points": document.getElementById("gps_points"),
      "gps_points_select_up": document.getElementById("gps_points_select_up"),
      "gps_points_select_down": document.getElementById("gps_points_select_down"),
      "gps_points_show_all": document.getElementById("gps_points_show_all"),
      "gps_points_refresh": document.getElementById("gps_points_refresh"),
      "gps_points_render": document.getElementById("gps_points_render"),
      "settings": {
        "max_rows": document.getElementById("settings_max_rows"),
        "timezone": document.getElementById("settings_timezone"),
        "zoom_level": document.getElementById("settings_zoom_level"),
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
      },
    };
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
    this.load_users();
  }

  bind_controls = () => {
    this.elems.user_list.addEventListener("change", (ev) => {
      let id = Number(ev.target.value);
      let user = this.users.find(user => user.id == id);
      if (user !== undefined) {
        this.selected_user = user;
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
  }

  load_users = async () => {
    let users = await GpsApi.get_users();
    this.users = users;
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
    let max_rows = Number(this.elems.settings.max_rows.value);
    let timezone = Number(this.elems.settings.timezone.value);
    let gps_points = await GpsApi.get_track({ user_id, max_rows, timezone });
    // create html table
    let elem = this.elems.gps_points;
    elem.replaceChildren();
    if (gps_points.length === 0) {
      let row = document.createElement("tr");
      row.innerHTML = "<td colspan=4><div class='text-center w-100'>No entries</div></td>";
      elem.appendChild(row);
      this.table_rows = [];
      return;
    }
    this.table_rows = gps_points.map((row, index) => {
      let row_elem = document.createElement("tr");
      row_elem.innerHTML = `
        <td style='white-space: nowrap'>${row.name}</td>
        <td>${row.latitude.toFixed(4)}</td>
        <td>${row.longitude.toFixed(4)}</td>
        <td>${row.altitude.toFixed(0)}</td>
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
          title: row.name,
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
        return marker;
      })
      .reverse();
  }

  clear_map_path = () => {
    this.map_lines.forEach(line => line.setMap(null));
    this.map_lines = [];
    this.map_markers.forEach(marker => marker.setMap(null));
    this.map_markers = [];
    this.selected_marker_index = 0;
    this.elems.gps_points_select_down.disabled = true;
    this.elems.gps_points_select_up.disabled = true;
    this.table_rows.forEach(row => row.classList.remove("selected"));
  }

  select_marker = (index) => {
    let total = this.map_markers.length;
    if (total === 0) return;
    if (index < 0) return;
    if (index >= total) return;
    this.selected_marker_index = index;
    this.elems.gps_points_select_up.disabled = (index == 0);
    this.elems.gps_points_select_down.disabled = (index >= (total-1));
    let selected_marker = this.map_markers[index];
    let selected_gps_point = this.gps_points[index];
    let selected_map_point = this.map_points[index];
    this.map_info.open({
      anchor: selected_marker,
      map: this.map,
    });
    this.map.setCenter(selected_map_point);
    this.map_info.setContent(`
      <style>
        .gm-ui-hover-effect {
          display: none !important;
        }
      </style>
      <div style="margin: 2px">
        <table>
          <tbody>
            <tr><td>Time</td><td>${selected_gps_point.name}</td></tr>
            <tr><td>Latitude</td><td>${selected_gps_point.latitude.toFixed(6)}</td></tr>
            <tr><td>Longitude</td><td>${selected_gps_point.longitude.toFixed(6)}</td></tr>
            <tr><td>Altitude (m)</td><td>${selected_gps_point.altitude.toFixed(2)}</td></tr>
          </tbody>
        </table>
      </div>
    `);
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
    }
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
      let table_row = this.table_rows[index];
      table_row.classList.remove("selected");
    };
  }
}
