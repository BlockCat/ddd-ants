/* Ant Farm renderer.
   1. fetch the static terrain once (re-fetch periodically: workers dig),
   2. subscribe to /api/sim/stream (SSE), polling /api/sim/state as fallback,
   3. draw every snapshot: terrain, nest, food, birds, ants — laden foragers
      leave a fading pheromone trail so the stigmergy trick is visible. */
"use strict";

const CELL = 6; // px per world cell
const KIND = { SAND: 0, BRANCH: 1, PEBBLE: 2, CHAMBER: 3 };

const COLORS = {
  worker: "#8a5a2b",
  forager: "#e0702a",
  food: "#3f9b3f",
  bird: "#23262b",
  nestRim: "rgba(74,52,24,0.9)",
  nestHole: "rgba(38,26,12,0.95)",
  trail: "224,112,42",
};

const terrain = { loaded: false, width: 0, height: 0, rows: [] };
let snapshot = null;
let streamMode = "connecting";
let running = true;

const canvas = document.getElementById("sim");
const ctx = canvas.getContext("2d");
const off = document.createElement("canvas");
const offCtx = off.getContext("2d");

// ---------------------------------------------------------------------
// terrain
// ---------------------------------------------------------------------

async function loadTerrain() {
  try {
    const res = await fetch("/api/sim/terrain");
    const data = await res.json();
    terrain.width = data.width;
    terrain.height = data.height;
    terrain.rows = data.cells;
    terrain.loaded = true;
    canvas.width = terrain.width * CELL;
    canvas.height = terrain.height * CELL;
    off.width = canvas.width;
    off.height = canvas.height;
    renderTerrain();
  } catch (err) {
    setStatus("could not load terrain: " + err);
  }
}

function kindFill(kind) {
  switch (kind) {
    case KIND.SAND: return "#e6d2a1";
    case KIND.BRANCH: return "#6f4a24";
    case KIND.PEBBLE: return "#a49b8a";
    case KIND.CHAMBER: return "#c8ad7c";
    default: return "#000";
  }
}

function renderTerrain() {
  offCtx.clearRect(0, 0, off.width, off.height);
  for (let y = 0; y < terrain.height; y++) {
    for (let x = 0; x < terrain.width; x++) {
      const kind = terrain.rows[y][x];
      offCtx.fillStyle = kindFill(kind);
      offCtx.fillRect(x * CELL, y * CELL, CELL, CELL);
      if (kind === KIND.BRANCH) {
        offCtx.strokeStyle = "#4a2f14";
        offCtx.lineWidth = 1.6;
        offCtx.beginPath();
        offCtx.moveTo(x * CELL + 1, y * CELL + CELL - 2);
        offCtx.lineTo(x * CELL + CELL - 2, y * CELL + 1);
        offCtx.stroke();
        offCtx.beginPath();
        offCtx.moveTo(x * CELL + CELL - 4, y * CELL + 3);
        offCtx.lineTo(x * CELL + CELL - 6, y * CELL + 6);
        offCtx.stroke();
      } else if (kind === KIND.PEBBLE) {
        offCtx.fillStyle = "#8b8371";
        offCtx.beginPath();
        offCtx.arc(x * CELL + CELL / 2, y * CELL + CELL / 2, CELL * 0.34, 0, Math.PI * 2);
        offCtx.fill();
      }
    }
  }
}

// ---------------------------------------------------------------------
// pheromone trails (client-side fading of laden-forager paths)
// ---------------------------------------------------------------------

const TRAIL_TTL = 4500; // ms before a trail segment fades out (server scent lasts minutes)
const trails = new Map(); // antId -> [{x, y, t}]

function recordTrails(nowMs) {
  const seen = new Set();
  for (const ant of snapshot.ants) {
    if (ant.carrying > 0) {
      seen.add(ant.id);
      let pts = trails.get(ant.id);
      if (!pts) { pts = []; trails.set(ant.id, pts); }
      pts.push({ x: (ant.x + 0.5) * CELL, y: (ant.y + 0.5) * CELL, t: nowMs });
    }
  }
  for (const [id, pts] of trails) {
    while (pts.length && nowMs - pts[0].t > TRAIL_TTL) pts.shift();
    if (!pts.length || !seen.has(id)) trails.delete(id);
  }
}

function drawTrails(nowMs) {
  for (const pts of trails.values()) {
    for (let i = 0; i < pts.length - 1; i++) {
      const age = (nowMs - pts[i].t) / TRAIL_TTL;
      const alpha = 0.55 * (1 - age);
      if (alpha <= 0.02) continue;
      ctx.strokeStyle = `rgba(${COLORS.trail},${alpha.toFixed(3)})`;
      ctx.lineWidth = 2.2;
      ctx.beginPath();
      ctx.moveTo(pts[i].x, pts[i].y);
      ctx.lineTo(pts[i + 1].x, pts[i + 1].y);
      ctx.stroke();
    }
  }
}

// ---------------------------------------------------------------------
// frame
// ---------------------------------------------------------------------

function roleColor(role) {
  return role === "FORAGER" ? COLORS.forager : COLORS.worker;
}

function draw(nowMs) {
  requestAnimationFrame(draw);
  if (!terrain.loaded || !snapshot) return;
  const c = CELL;

  ctx.drawImage(off, 0, 0);

  // nest entrances
  for (const nest of snapshot.nests) {
    const px = (nest.x + 0.5) * c, py = (nest.y + 0.5) * c;
    ctx.fillStyle = COLORS.nestRim;
    ctx.beginPath(); ctx.arc(px, py, c * 0.66, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = COLORS.nestHole;
    ctx.beginPath(); ctx.arc(px, py, c * 0.42, 0, Math.PI * 2); ctx.fill();
  }

  // food sources: size reflects remaining amount
  for (const food of snapshot.foods) {
    const px = (food.x + 0.5) * c, py = (food.y + 0.5) * c;
    const r = c * (0.35 + 0.3 * Math.min(1, food.amount / 30));
    ctx.fillStyle = "rgba(63,155,63,0.25)";
    ctx.beginPath(); ctx.arc(px, py, r + 2, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = COLORS.food;
    ctx.beginPath(); ctx.arc(px, py, r, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = "rgba(255,255,255,0.25)";
    ctx.beginPath(); ctx.arc(px - r * 0.3, py - r * 0.3, r * 0.35, 0, Math.PI * 2); ctx.fill();
  }

  // pheromone trails under everything living
  recordTrails(nowMs);
  drawTrails(nowMs);

  // birds — flapping triangles
  const flap = Math.floor(nowMs / 300) % 2 === 0;
  ctx.fillStyle = COLORS.bird;
  for (const bird of snapshot.birds) {
    const px = (bird.x + 0.5) * c, py = (bird.y + 0.5) * c;
    const w = c * 1.6;
    if (flap) {
      ctx.beginPath();
      ctx.moveTo(px - w / 2, py);
      ctx.quadraticCurveTo(px, py - c * 1.1, px + w / 2, py);
      ctx.quadraticCurveTo(px, py - c * 0.4, px - w / 2, py);
      ctx.fill();
    } else {
      ctx.beginPath();
      ctx.moveTo(px - w / 2, py - c * 0.3);
      ctx.quadraticCurveTo(px, py + c * 0.8, px + w / 2, py - c * 0.3);
      ctx.lineTo(px + w / 2, py);
      ctx.lineTo(px - w / 2, py);
      ctx.closePath();
      ctx.fill();
    }
  }

  // ants — laden ones get a green food dot
  for (const ant of snapshot.ants) {
    const px = (ant.x + 0.5) * c, py = (ant.y + 0.5) * c;
    const energy = Math.max(0.15, Math.min(1, ant.energy / 100));
    ctx.globalAlpha = 0.5 + 0.5 * energy;
    ctx.fillStyle = roleColor(ant.role);
    ctx.beginPath(); ctx.arc(px, py, Math.max(1.8, c * 0.44), 0, Math.PI * 2); ctx.fill();
    if (ant.carrying > 0) {
      ctx.globalAlpha = 1;
      ctx.fillStyle = COLORS.food;
      ctx.beginPath(); ctx.arc(px, py, Math.max(1.2, c * 0.2), 0, Math.PI * 2); ctx.fill();
    }
  }
  ctx.globalAlpha = 1;

  updateHud();
}

function updateHud() {
  if (!snapshot) return;
  const set = (id, v) => { document.getElementById(id).textContent = v; };
  set("h-tick", snapshot.tick.toLocaleString());
  set("h-state", snapshot.running ? "▶ running" : "⏸ paused");
  set("h-alive", snapshot.antsAlive);
  set("h-workers", snapshot.workers);
  set("h-foragers", snapshot.foragers);
  set("h-store", snapshot.colonyFood.toFixed(1));
  set("h-brood", snapshot.brood);
  set("h-food", snapshot.foodSources);
  set("h-birds", snapshot.birdCount);
  set("h-stream", `${streamMode} · ${snapshot.ticksPerSecond.toFixed(1)}/s`);
  const btn = document.getElementById("btn-pause");
  btn.textContent = snapshot.running ? "⏸ Pause" : "▶ Resume";
}

function setStatus(msg) { document.getElementById("status").textContent = msg; }

// ---------------------------------------------------------------------
// transport: SSE with polling fallback
// ---------------------------------------------------------------------

function connectSSE() {
  const es = new EventSource("/api/sim/stream");
  es.addEventListener("snapshot", (ev) => {
    snapshot = JSON.parse(ev.data);
    streamMode = "sse live";
    setStatus("");
  });
  es.onerror = () => {
    es.close();
    streamMode = "polling";
    setStatus("SSE down — polling /api/sim/state");
  };
}

async function pollState() {
  try {
    const res = await fetch("/api/sim/state");
    if (res.ok) snapshot = await res.json();
  } catch (_) { /* backend briefly unreachable */ }
}

// ---------------------------------------------------------------------
// controls
// ---------------------------------------------------------------------

async function post(path) {
  try {
    const res = await fetch(path, { method: "POST" });
    return res.ok;
  } catch (_) {
    return false;
  }
}

function bindControls() {
  const btn = document.getElementById("btn-pause");
  const sel = document.getElementById("sel-speed");

  btn.addEventListener("click", async () => {
    running = !running;
    btn.disabled = true;
    const ok = await post(running ? "/api/sim/resume" : "/api/sim/pause");
    if (!ok) {
      running = !running;
      setStatus("⛔ control failed — is the backend up?");
    } else {
      setStatus(running ? "▶ resumed" : "⏸ paused");
    }
    btn.disabled = false;
  });

  sel.addEventListener("change", async () => {
    const multiplier = sel.value;
    const ok = await post("/api/sim/speed?multiplier=" + encodeURIComponent(multiplier));
    setStatus(ok
      ? `⏩ speed set to ${multiplier}× = ${(multiplier * 10).toFixed(1)} ticks/s`
      : "⛔ speed change failed");
  });
}

// ---------------------------------------------------------------------

async function main() {
  await loadTerrain();
  bindControls();
  connectSSE();
  setInterval(() => { if (streamMode !== "sse live") pollState(); }, 250);
  // workers dig chambers — refresh terrain every few seconds to show them
  setInterval(async () => { if (running) await loadTerrain(); }, 8000);
  requestAnimationFrame(draw);
}

main();
