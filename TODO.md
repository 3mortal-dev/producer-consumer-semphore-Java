- [x] Implement TreatmentRooms
- [x] Implement Hospital class
- [x] Implement Doctor's Overrided run method
- [x] Implement `tryAcquire()` method
- [x] replace `tryAcquire()` with the Producer-consumer **patients and doctors**
- [x] Logs
- [x] Review
- [x] Push on github

## 8-bit GUI
- [x] Event model + thread-safe broadcaster (backend -> EDT)
- [x] Backend hooks in Patient / Doctor / WaitingRoom / TreatmentRooms
- [x] 16-color palette + procedural sprite atlas
- [x] Press Start 2P font loading with fallback
- [x] PixelCanvas double-buffered renderer
- [x] Waiting room seats + outside queue view
- [x] Two treatment rooms view
- [x] Doctor status bar
- [x] Scrolling event log
- [x] Control panel: inputs, START, PAUSE, real speed scaling
- [x] Hospital floor tile map between waiting room and treatment rooms
- [x] Directional sprite walk/face animation (front / left / right, 2-frame cycle)
- [x] Two-frame idle animation for seated patients, doctors, and monitors
- [x] Live simulation smoke test

## Isometric 2.5D game (default GUI)
- [x] GameEventBridge: lock-free backend -> EDT event queue
- [x] WorldState / WorldReducer / slots / routing (EDT-confined, cosmetic)
- [x] 2:1 isometric projection + 3 camera presets with tween, drag-pan, wheel-zoom
- [x] Procedural sprite bank (characters, chairs, beds, IV, monitors, props)
- [x] WorldRenderer: floors, walls, animated glass door, depth sorting, ID tags
- [x] HudRenderer: clickable tabs, live stats, speed/pause/event controls, overlays
- [x] In-canvas title screen with editable capacity/doctors/patients/speed
- [x] gui.Main launcher (default game, `--legacy` for the dashboard GUI)
- [x] Headless + live-window verification (title, 3 views, pause, events-off, completion)

## Readability + pacing settings
- [x] Minimum readable text size (8) in HUD, ID tags, signs, title screen
- [x] FONT setting: PIXEL (Press Start 2P) / READABLE (monospaced), title chip + `F`
- [x] EventPlaybackQueue: OFF / DELAY (adjustable 0.3-2.0s, spaced FIFO) / STEP (`N`)
- [x] Backlog indicator in the bottom strip; completion gated on drained queue
- [x] Headless queue checks + live-window verification of delay/step/font switching

## Bedside choreography (visual only, backend untouched)
- [x] Doctor holds at bedside until patient gets up, then returns to station
- [x] Next patient/doctor wait at staging until the bed visually vacates (pending slot)
- [x] Sent-back patients walk straight back to a chair (or queue), not via intake
- [x] Headless choreography checks (hold, pending promotion, pending-finish race, sent-back)
- [x] Full pipeline re-verified (12/12, 40/40, 60/60) + live-window captures
