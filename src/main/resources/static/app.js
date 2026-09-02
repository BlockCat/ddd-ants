/* Ant Farm renderer.
   1. fetch the terrain, refresh every few seconds (workers are digging!),
   2. subscribe to /api/sim/stream (SSE), polling /api/sim/state as fallback,
   3. draw every snapshot: sand, obstacles, the burrow (tunnels, chambers,
      entrance/exit holes), food piles inside chambers, birds, and ants —
      underground ants get a halo and move visibly faster. Laden foragers
      leave fading pheromone trails. */
"use strict";

const CELL = 6; // px per world cell
const KIND = { SAND: 0, BRANCH: 1, PEBBLE: 2, HOLE: 3, TUNNEL: 4, CHAMBER: 5 };

const COLORS = {
  worker: "#8a5a2b",
  forager: "#e0702a",
  food: "#3f9b3f",
  bird: "#23262b",
  sand: "#e6d2a1",
  trail: "224,112,42",
};

const terrain = { loaded: false, width: 0, height: 0, rows: [], tunnels: 0, chambers: [], holes: 0 };
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

function baseColor(kind) {
  switch (kind) {
    case KIND.SAND:
    case KIND.HOLE: return COLORS.sand;
    case KIND.BRANCH: return "#6f4a24";
    case KIND.PEBBLE: return "#d8d2c2";
    case KIND.TUNNEL: return "#5d3d20";
    case KIND.CHAMBER: return "#8a6a3d";
    default: return "#000";
  }
}

function renderTerrain() {
  const w = terrain.width, h = terrain.height, c = CELL;
  offCtx.clearRect(0, 0, off.width, off.height);

  // pass 1: base colours
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const kind = terrain.rows[y][x];
      offCtx.fillStyle = baseColor(kind);
      offCtx.fillRect(x * c, y * c, c, c);
    }
  }

  // pass 2: detail overlays
  terrain.chambers = [];
  terrain.tunnels = 0;
  terrain.holes = 0;
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const kind = terrain.rows[y][x];
      const px = x * c, py = y * c;
      if (kind === KIND.BRANCH) {
        offCtx.strokeStyle = "#4a2f14";
        offCtx.lineWidth = 1.6;
        offCtx.beginPath();
        offCtx.moveTo(px + 1, py + c - 2);
        offCtx.lineTo(px + c - 2, py + 1);
        offCtx.stroke();
        offCtx.beginPath();
        offCtx.moveTo(px + c - 4, py + 3);
        offCtx.lineTo(px + c - 6, py + 6);
        offCtx.stroke();
      } else if (kind === KIND.PEBBLE) {
        offCtx.fillStyle = "#9b9381";
        offCtx.beginPath();
        offCtx.arc(px + c / 2, py + c / 2, c * 0.34, 0, Math.PI * 2);
        offCtx.fill();
        offCtx.fillStyle = "rgba(255,255,255,0.25)";
        offCtx.beginPath();
        offCtx.arc(px + c * 0.38, py + c * 0.36, c * 0.14, 0, Math.PI * 2);
        offCtx.fill();
      } else if (kind === KIND.HOLE) {
        terrain.holes++;
        // entrance / exit shaft: dark hole with a sandy rim
        const g = offCtx.createRadialGradient(px + c / 2, py + c / 2, c * 0.1, px + c / 2, py + c / 2, c * 0.75);
        g.addColorStop(0, "#241708");
        g.addColorStop(0.55, "#2f1f0e");
        g.addColorStop(1, "#4a3418");
        offCtx.fillStyle = g;
        offCtx.beginPath();
        offCtx.arc(px + c / 2, py + c / 2, c * 0.66, 0, Math.PI * 2);
        offCtx.fill();
      } else if (kind === KIND.TUNNEL) {
        terrain.tunnels++;
        // rounded dark corridor
        roundRect(offCtx, px + c * 0.12, py + c * 0.12, c * 0.76, c * 0.76, c * 0.3);
        offCtx.fillStyle = "#4a2e15";
        offCtx.fill();
        roundRect(offCtx, px + c * 0.26, py + c * 0.26, c * 0.48, c * 0.48, c * 0.16);
        offCtx.fillStyle = "rgba(120,86,45,0.35)";
        offCtx.fill();
      } else if (kind === KIND.CHAMBER) {
        terrain.chambers.push({ x, y });
        // a roomy chamber dug into the sand
        roundRect(offCtx, px + c * 0.05, py + c * 0.05, c * 0.9, c * 0.9, c * 0.28);
        offCtx.fillStyle = "#7c5a2e";
        offCtx.fill();
        roundRect(offCtx, px + c * 0.16, py + c * 0.16, c * 0.68, c * 0.68, c * 0.2);
        offCtx.fillStyle = "#9a7640";
        offCtx.fill();
        offCtx.strokeStyle = "rgba(58,36,14,0.55)";
        offCtx.lineWidth = 1;
        roundRect(offCtx, px + c * 0.12, py + c * 0.12, c * 0.76, c * 0.76, c * 0.24);
        offCtx.stroke();
      }
    }
  }
}

function roundRect(g, x, y, w, h, r) {
  g.beginPath();
  g.moveTo(x + r, y);
  g.arcTo(x + w, y, x + w, y + h, r);
  g.arcTo(x + w, y + h, x, y + h, r);
  g.arcTo(x, y + h, x, y, r);
  g.arcTo(x, y, x + w, y, r);
  g.closePath();
}

function cellKind(px, py) {
  if (py < 0 || py >= terrain.rows.length) return KIND.SAND;
  const row = terrain.rows[py];
  if (!row || px < 0 || px >= row.length) return KIND.SAND;
  return row[px];
}

// ---------------------------------------------------------------------
// pheromone trails (client-side fading of laden-forager paths)
// ---------------------------------------------------------------------

const TRAIL_TTL = 4500; // ms before a trail segment fades out (server scent lasts minutes)
const trails = new Map(); // antId -> [{x, y, t}]

function recordTrails(nowMs) {
  const seen = new Set();
  for (const ant of snapshot.ants) {
    if (ant.carrying > 0 && !isUnderground(ant)) {
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

function isUnderground(ant) {
  const kind = cellKind(ant.x, ant.y);
  return kind === KIND.TUNNEL || kind === KIND.CHAMBER;
}

// ---------------------------------------------------------------------
// frame
// ---------------------------------------------------------------------

function draw(nowMs) {
  requestAnimationFrame(draw);
  if (!terrain.loaded || !snapshot) return;
  const c = CELL;

  ctx.drawImage(off, 0, 0);

  // food piles inside chambers reflect the colony food store
  if (terrain.chambers.length > 0 && snapshot.colonyFood > 0.5) {
    const perChamber = Math.min(6, snapshot.colonyFood / (terrain.chambers.length * 4));
    ctx.fillStyle = COLORS.food;
    for (const ch of terrain.chambers) {
      const px = (ch.x + 0.5) * c, py = (ch.y + 0.5) * c;
      const r = c * Math.min(0.85, 0.4 + perChamber * 0.12);
      ctx.beginPath();
      ctx.arc(px, py, r, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "rgba(255,255,255,0.18)";
      ctx.beginPath();
      ctx.arc(px - r * 0.3, py - r * 0.3, r * 0.3, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = COLORS.food;
    }
  }

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

  // ants — colour by role; underground ants get a dark halo and no trail
  for (const ant of snapshot.ants) {
    const px = (ant.x + 0.5) * c, py = (ant.y + 0.5) * c;
    const energy = Math.max(0.15, Math.min(1, ant.energy / 100));
    const under = isUnderground(ant);
    ctx.globalAlpha = 0.5 + 0.5 * energy;
    ctx.fillStyle = roleColor(ant.role);
    ctx.beginPath(); ctx.arc(px, py, Math.max(1.8, c * 0.44), 0, Math.PI * 2); ctx.fill();
    if (ant.carrying > 0) {
      ctx.globalAlpha = 1;
      ctx.fillStyle = COLORS.food;
      ctx.beginPath(); ctx.arc(px, py, Math.max(1.2, c * 0.2), 0, Math.PI * 2); ctx.fill();
    }
    if (under) {
      ctx.globalAlpha = 1;
      ctx.strokeStyle = "#2c1a08";
      ctx.lineWidth = 1.4;
      ctx.beginPath();
      ctx.arc(px, py, Math.max(2.6, c * 0.6), 0, Math.PI * 2);
      ctx.stroke();
    }
  }
  ctx.globalAlpha = 1;

  updateHud();
}

function roleColor(role) {
  return role === "FORAGER" ? COLORS.forager : COLORS.worker;
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
  set("h-birds", snapshot.birds);
  set("h-tunnels", terrain.tunnels);
  set("h-chambers", terrain.chambers.length);
  set("h-holes", terrain.holes);
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
  // workers are digging — refresh terrain often to watch the burrow grow
  setInterval(async () => { if (running) await loadTerrain(); }, 2500);
  requestAnimationFrame(draw);
}

main();
