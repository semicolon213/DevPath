// Guarded single-Enter recovery for an Orca Codex worker whose worker-start prompt was
// pasted but left unsubmitted in the Codex composer (see AGENTS.md §9, "Codex worker
// prompt delivery and submit recovery"). Never resends the prompt, never gates on
// tui-idle, and sends at most one bare Enter per Dispatch, only with --execute.
import { execFileSync } from "node:child_process";
import { closeSync, existsSync, mkdirSync, openSync, readFileSync, writeSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const USAGE = `Usage: node scripts/orca/codex-submit-guard.mjs <worker-start-receipt.json> [--dry-run | --execute]

Checks whether an Orca Codex worker's worker-start prompt is stuck unsubmitted and, only
with --execute, sends exactly one bare Enter to its terminal.

  <worker-start-receipt.json>  JSON output saved from: orca orchestration worker-start ... --json
  --dry-run                    Default. Evaluate every condition; never send input.
  --execute                    Send one Enter when every condition passes, then verify the turn start.
  --help                       Show this message.

Environment:
  ORCA_CLI_COMMAND               Orca executable (default: orca).
  CODEX_SUBMIT_GUARD_STATE_DIR   Per-Dispatch sent-marker directory
                                 (default: <os tmpdir>/devpath-codex-submit-guard).

Verdicts: NO_ACTION, WOULD_SEND (dry run), RECOVERED  -> exit 0
          BLOCKED                                      -> exit 2
          usage error                                  -> exit 1`;

const args = process.argv.slice(2);
if (args.includes("--help") || args.includes("-h")) {
  console.log(USAGE);
  process.exit(0);
}
const unknown = args.filter(arg => arg.startsWith("-") && !["--dry-run", "--execute"].includes(arg));
const receiptPaths = args.filter(arg => !arg.startsWith("-"));
if (unknown.length > 0 || receiptPaths.length !== 1 || (args.includes("--dry-run") && args.includes("--execute"))) {
  console.error(USAGE);
  process.exit(1);
}
const execute = args.includes("--execute");
const orcaCommand = process.env.ORCA_CLI_COMMAND || "orca";
const stateDirectory = process.env.CODEX_SUBMIT_GUARD_STATE_DIR || join(tmpdir(), "devpath-codex-submit-guard");

const MARKERS_ONLY = /^(\[Pasted Content \d+ chars\])+$/;
const RUNNING = /Working|esc to interrupt/;
// Confirmation, selection, and permission UI: numbered options, approval prompts, y/n, trust and plan menus.
const MENU_LINE = /^\s*[›>]?\s*\d+\.\s|\(y\/n\)|\[y\/N\]|allow|approv|permission|press enter to (confirm|continue)|do you trust|implement this plan|select an option|↑\/↓|use arrow keys/i;
const STABILITY_WINDOW_MS = 5000;

const log = (event, detail = {}) => console.log(`${new Date().toISOString()} ${event} ${JSON.stringify(detail)}`);
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const finish = (verdict, reason) => {
  log("VERDICT", { verdict, reason, mode: execute ? "execute" : "dry-run" });
  process.exit(verdict === "BLOCKED" ? 2 : 0);
};

function orca(...commandArgs) {
  try {
    return JSON.parse(execFileSync(orcaCommand, [...commandArgs, "--json"], { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] }));
  } catch (error) {
    try {
      return JSON.parse(error.stdout?.toString() ?? "");
    } catch {
      return { ok: false, error: { message: error.message } };
    }
  }
}

const hasObservedTurn = value => value !== null && typeof value === "object"
  && Object.entries(value).some(([key, nested]) => (/turnstart/i.test(key) && nested === "observed") || hasObservedTurn(nested));
const showsMenu = text => text.split("\n").filter(line => !/^\s*permissions:/i.test(line)).some(line => MENU_LINE.test(line));

let receipt;
try {
  receipt = JSON.parse(readFileSync(receiptPaths[0], "utf8")).result ?? {};
} catch (error) {
  console.error(`Cannot read worker-start receipt: ${error.message}`);
  process.exit(1);
}
const dispatchId = receipt.dispatchId;
const receiptHandle = (receipt.effects ?? []).find(effect => effect.kind === "terminal" && effect.role === "agent")?.id;
log("receipt", { state: receipt.state, stage: receipt.stage, turnStart: receipt.turnStart, dispatchId, receiptHandle });

// Check 1: worker-start reported an unobserved turn start.
if (receipt.state !== "outcome_unknown" || receipt.stage !== "turn_start_unobserved") {
  finish("NO_ACTION", "receipt does not report outcome_unknown / turn_start_unobserved");
}
if (!dispatchId || !/^[A-Za-z0-9_-]+$/.test(dispatchId) || !receiptHandle) finish("BLOCKED", "receipt lacks a valid dispatch id or agent terminal");

// Duplicate protection: an Enter already sent for this Dispatch is never repeated.
const markerPath = join(stateDirectory, `${dispatchId}.enter-sent`);
if (existsSync(markerPath)) finish("BLOCKED", `an Enter was already sent for this Dispatch (${markerPath})`);

// The terminal handle is re-read from the live Dispatch rather than trusted from the receipt.
const handle = orca("orchestration", "worker-show", "--dispatch", dispatchId).result?.dispatch?.assigneeHandle;
log("handle", { fromDispatch: handle, fromReceipt: receiptHandle });
if (!handle || handle !== receiptHandle) finish("BLOCKED", "dispatch terminal handle is missing or differs from the receipt");

function dispatchUnstarted() {
  // Check 2: Dispatch still pending on the same terminal, with no observed turn start.
  const shown = orca("orchestration", "worker-show", "--dispatch", dispatchId).result ?? {};
  const unstarted = shown.dispatch?.status === "pending" && shown.dispatch?.assigneeHandle === handle
    && shown.worker?.stage === "turn_start_unobserved" && !hasObservedTurn(shown);
  log("dispatch", { status: shown.dispatch?.status, stage: shown.worker?.stage, observedTurn: hasObservedTurn(shown), unstarted });
  return unstarted;
}

function readScreen() {
  // Checks 3 and 4: composer holds only paste markers; no running indicator or menu/permission UI.
  const terminal = orca("terminal", "read", "--terminal", handle, "--screen").result?.terminal ?? {};
  const tail = terminal.tail ?? [];
  const composerLine = tail.map(line => line.trim()).filter(line => line.startsWith("› ")).map(line => line.slice(2).trim()).pop() ?? null;
  const pasteOnly = typeof terminal.draft === "string"
    ? MARKERS_ONLY.test(terminal.draft.trim())
    : composerLine !== null && MARKERS_ONLY.test(composerLine);
  const text = tail.join("\n");
  const screen = { source: terminal.source, draft: terminal.draft ?? null, composerLine, pasteOnly, running: RUNNING.test(text), menu: showsMenu(text), tail };
  log("screen", { source: screen.source, draft: screen.draft, composerLine, pasteOnly, running: screen.running, menu: screen.menu });
  return screen;
}
const unsubmitted = screen => screen.source === "screen" && screen.pasteOnly && !screen.running && !screen.menu;
const sameScreen = (left, right) => JSON.stringify(left.tail) === JSON.stringify(right.tail);

if (!dispatchUnstarted()) finish("NO_ACTION", "dispatch is not pending with an unobserved turn start");
const firstScreen = readScreen();
if (!unsubmitted(firstScreen)) finish("NO_ACTION", "screen is not the plain unsubmitted-paste state");

// Check 5: two screen reads STABILITY_WINDOW_MS apart are identical.
await sleep(STABILITY_WINDOW_MS);
const secondScreen = readScreen();
log("stability", { stable: sameScreen(firstScreen, secondScreen), windowMs: STABILITY_WINDOW_MS });
if (!sameScreen(firstScreen, secondScreen) || !unsubmitted(secondScreen)) finish("BLOCKED", "screen changed during the stability window");

// Check 6: re-check Dispatch and screen immediately before sending.
if (!dispatchUnstarted()) finish("NO_ACTION", "dispatch changed before send");
const finalScreen = readScreen();
if (!unsubmitted(finalScreen) || !sameScreen(finalScreen, secondScreen)) finish("BLOCKED", "screen changed immediately before send");

if (!execute) finish("WOULD_SEND", "every condition passed; rerun with --execute to send one Enter");

// Claim the Dispatch atomically before sending so no later or concurrent run can send again.
try {
  mkdirSync(stateDirectory, { recursive: true });
  const marker = openSync(markerPath, "wx");
  writeSync(marker, JSON.stringify({ dispatchId, handle, claimedAt: new Date().toISOString() }));
  closeSync(marker);
} catch (error) {
  finish("BLOCKED", `cannot claim the per-Dispatch Enter marker (${error.code ?? error.message})`);
}

const sent = orca("terminal", "send", "--terminal", handle, "--enter");
log("enter", { sent: 1, ok: sent.ok, accepted: sent.result?.send?.accepted });
if (!sent.ok) finish("BLOCKED", "Enter send was not accepted; not retrying");

// Verify the turn started: marker cleared and Working shown, or an observed turn start / settlement.
// Orca may not record turn_started after a manual Enter, so a matching worker_done remains required.
for (let attempt = 0; attempt < 15; attempt += 1) {
  await sleep(2000);
  const terminal = orca("terminal", "read", "--terminal", handle, "--screen").result?.terminal ?? {};
  const text = (terminal.tail ?? []).join("\n");
  const shown = orca("orchestration", "worker-show", "--dispatch", dispatchId).result ?? {};
  const markerLeft = typeof terminal.draft === "string" || (terminal.tail ?? []).some(line => /^›\s*\[Pasted Content \d+ chars\]/.test(line.trim()));
  const running = RUNNING.test(text);
  log("verify", { attempt, markerLeft, running, menu: showsMenu(text), observedTurn: hasObservedTurn(shown), status: shown.dispatch?.status });
  if (showsMenu(text) && !running) finish("BLOCKED", "unexpected menu or permission UI after Enter; not interacting");
  if (!markerLeft && (running || hasObservedTurn(shown) || shown.dispatch?.status === "completed")) {
    finish("RECOVERED", "turn start observed after one Enter; still require a matching worker_done");
  }
}
finish("BLOCKED", "no turn start observed after one Enter; not retrying");
