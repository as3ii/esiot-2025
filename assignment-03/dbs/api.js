const cus_state = document.getElementById("cus-state");
const tms_state = document.getElementById("tms-state");
const wcs_state = document.getElementById("wcs-state");
const valve_opening = document.getElementById("valve-opening");
const canvas = document.getElementById("rainwater-level");

const manual_input = document.getElementById("manual");
const automatic_input = document.getElementById("automatic");
const valve_input = document.getElementById("valve");
const form = document.querySelector("main article section form");

const sample_count = 200;

let last_measurement = -1.0;
let data = [];

function updateState(cus, tms, wcs) {
  cus = cus.toUpperCase();
  tms = tms.toUpperCase();
  wcs = wcs.toUpperCase();
  cus_state.value = cus;
  if (cus == "WORKING") {
    cus_state.style.color = "green";
  } else if (cus == "UNKNOWN") {
    cus_state.style.color = "grey";
  } else {
    cus_state.style.color = "red";
  }

  tms_state.value = tms;
  if (tms == "CONNECTED") {
    tms_state.style.color = "green";
  } else if (tms == "UNKNOWN") {
    tms_state.style.color = "grey";
  } else {
    tms_state.style.color = "red";
  }

  wcs_state.value = wcs;
  if (wcs == "AUTOMATIC") {
    wcs_state.style.color = "green";
  } else if (wcs == "UNKNOWN") {
    wcs_state.style.color = "grey";
  } else {
    wcs_state.style.color = "red";
  }
}

// endpoint: endpoint to select (+ optional GET parameters)
// data: key-value pairs to be passed as POST parameters
async function callApi(endpoint, data) {
  if (typeof endpoint != "string" || endpoint.length <= 0) {
    return { status: "error", msg: "Invalid inputs", raw: "" };
  }

  const url = "api/" + endpoint;
  let request;
  if (data != null) {
    request = { method: "POST", body: JSON.stringify(data) };
  } else {
    request = { method: "GET" };
  }
  const response = await fetch(url, request);
  if (!response.ok) {
    return { status: "error", msg: response.statusText, raw: await response.text() };
  }
  const text = await response.clone().text();
  try {
    const json = await response.json();
    return {
      status: "ok",
      msg: json,
      raw: text
    };
  } catch (exception) {
    return { status: "error", msg: exception.message, raw: text };
  }
}

async function getState() {
  const endpoint = "state";
  let response;
  try {
    response = await callApi(endpoint, null);
  } catch (exception) {
    if (exception instanceof TypeError) {
      // Handle: TypeError: NetworkError when attempting to fetch resource
      updateState("NOT AVAILABLE", "UNKNOWN", "UNKNOWN");
    } else {
      console.log("Received exception " + exception);
    }
    return;
  }
  if (response.status != "ok") {
    updateState("NOT AVAILABLE (" + response.msg + ")", "UNKNOWN", "UNKNOWN");
    return;
  }
  updateState("WORKING", response.msg.tmsState, response.msg.wcsState)
  valve_opening.value = response.msg.valveOpening;
  last_measurement = { level: response.msg.lastMeasurement, timestamp: Date.now() };
}

async function getHistory() {
  const endpoint = "history?limit=" + sample_count;
  const response = await callApi(endpoint, null);
  if (response.status != "ok") {
    console.log("Error retrieving history: " + response.msg);
    return;
  }
  if (response.msg.length > 0) {
    data = response.msg;
  } else {
    console.log("No data available");
  }
}

function draw(data) {
  const ctx = canvas.getContext("2d");
  const W = canvas.width, H = canvas.height;
  const pad = { left: 50, right: 8, top: 8, bottom: 60 };
  const plotW = W - pad.left - pad.right;
  const plotH = H - pad.top - pad.bottom;
  const levels = data.map(d => d.level);
  const maxVal = Math.max(...levels, 1.0);
  const times = data.map(d => {
    const t = new Date(d.timestamp);
    return t.toLocaleTimeString("en-US", { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  });

  ctx.clearRect(0, 0, W, H);
  ctx.font = "10px sans-serif";
  ctx.textAlign = "right";
  ctx.textBaseline = "middle";
  ctx.strokeStyle = "blue";
  ctx.lineWidth = 1;

  // Draw axes
  ctx.beginPath();
  ctx.moveTo(pad.left, pad.top); ctx.lineTo(pad.left, H - pad.bottom);
  ctx.moveTo(pad.left, H - pad.bottom); ctx.lineTo(W - pad.right, H - pad.bottom);
  ctx.lineWidth = 1;
  ctx.stroke();

  // Y label
  const yTicks = 5;
  // ctx.fillStyle = "#333";
  for (let i = 0; i <= yTicks; i++) {
    const val = (maxVal / yTicks) * i;
    const y = pad.top + (val / maxVal) * plotH; // 0 on top, max on bottom
    ctx.fillText(val.toFixed(1), pad.left - 4, y);
    // tick mark
    ctx.beginPath();
    ctx.moveTo(pad.left - 3, y);
    ctx.lineTo(pad.left, y);
    ctx.stroke();
  }

  // X label
  const xStep = plotW / (levels.length - 1);
  const labelStep = Math.max(1, Math.floor(times.length / 21));
  times.forEach((t, i) => {
    const x = pad.left + i * xStep;
    if (i % labelStep === 0) {
      // tick
      ctx.beginPath();
      ctx.moveTo(x, H - pad.bottom);
      ctx.lineTo(x, H - pad.bottom + 3);
      ctx.stroke();
      // rotated label
      ctx.save();
      ctx.translate(x, H - pad.bottom + 4);
      ctx.rotate(-Math.PI / 4);
      ctx.fillText(t, 0, 0);
      ctx.restore();
    }
  });

  // Plot line
  ctx.beginPath();
  levels.forEach((v, i) => {
    const x = pad.left + i * xStep;
    const y = pad.top + (v / maxVal) * plotH; // 0 on top, max on bottom
    i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
  });
  ctx.stroke();
}

manual_input.addEventListener("click", () => {
  valve_input.removeAttribute("disabled");
});

automatic_input.addEventListener("click", () => {
  valve_input.setAttribute("disabled", "");
});

valve_input.addEventListener("input", () => {
  valve_input.nextElementSibling.firstElementChild.value = valve_input.value;
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const mode = "mode";
  const m_response = await callApi(mode, {
    mode: manual_input.checked ? "manual" : "automatic"
  });
  if (m_response.status != "ok" && m_response.msg.status != "ok") {
    console.log("Error setting mode: " + m_response.msg);
    return;
  } else {
    console.log("Mode set");
  }

  if (manual_input.checked) {
    const opening = "opening";
    const o_response = await callApi(opening, {
      opening: valve_input.value
    });
    if (o_response.status != "ok" && o_response.msg.status != "ok") {
      console.log("Error setting opening: " + o_response.msg);
      return;
    } else {
      console.log("Valve opening set");
    }
  }
});

// Get initial history
getHistory();

// Run functions every 1000ms
setInterval(async () => {
  await getState();
  if (tms_state.value == "CONNECTED" && cus_state.value == "WORKING") {
    if (data.length >= sample_count) {
      data.shift();
    }
    data.push(last_measurement);
  }
  draw(data);
}, 1000);
