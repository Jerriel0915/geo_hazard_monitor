# Design: Map Editor Followup Fixes (4 issues)

| Field | Value |
|---|---|
| Author | Claude (brainstorming session) |
| Date | 2026-06-11 |
| Status | Approved |
| Prior spec | [2026-06-11-map-editor-composable-design.md](./2026-06-11-map-editor-composable-design.md) |
| Code areas | `web/src/composables/useMapEditor.ts`, `web/src/components/map/MapBoundaryEditor.vue`, `web/src/views/basic/HazardPoint.vue` |

## 1. Summary

Fix four UX/data issues reported after the first map-editor refactor landed:

1. **Center not persisted**: edits to the map's center point don't reach the database
2. **Per-element delete**: "clear all" exists, but individual strike/aux lines can't be deleted easily
3. **No drawing visual feedback**: after first click on strike/aux, user doesn't know if the start was placed
4. **Strike endpoints not draggable**: once a strike is placed, endpoints can't be modified

All four live in the existing useMapEditor composable and its single parent component. Single spec, single implementation plan.

## 2. Problems

| # | Problem | Root cause | Location |
|---|---|---|---|
| P1 | Center edits don't persist to DB | `onMapDone` writes to `formCenter` but not to `formData.longitude/latitude`. The save payload reads from `formData` | `HazardPoint.vue:2161` |
| P2 | Strike/aux lines can't be individually deleted | "× 删除选中" button hides unless something is selected; user may not realize they need to click the line first | `MapBoundaryEditor.vue:22` |
| P3 | No visual feedback when drawing strike/aux | After 1st click on strike, `strikeLine = [p, p]` (degenerate, doesn't render). After 1st click on aux, line has 1 vertex (Leaflet polylines with 1 point don't render). No cursor follower | `useMapEditor.ts:393` (DRAW-strike branch) |
| P4 | Strike endpoints can't be dragged | `moveStrikeEndpoint` action exists, but no endpoint markers are rendered. Similarly for aux points (`moveAuxPoint` exists, no per-vertex markers) | `useMapEditor.ts:277` (strike render block) |

## 3. Decisions

| # | Decision | Choice |
|---|---|---|
| 1 | Visual feedback for in-progress drawing | **A**: ghost dot at cursor + dashed preview line + pulse on placed start |
| 2 | Per-element modification (strike endpoints, aux points) | **A**: drag handles, consistent with polygon vertices |
| 3 | Per-element delete | Keep "× 删除选中" (whole line). Make it always-visible in edit mode (disabled when nothing selected). YAGNI on per-point delete |
| 4 | Data flow for center | Map → formData.lat/lng on done (formData is source of truth; map dialog is a transient editor) |

## 4. Architecture

```
useMapEditor.ts (composable)
├── existing: state, actions, render()
├── NEW: mouseLatLng ref
├── NEW: ghostGroup ref (L.LayerGroup)
├── NEW: clearGhost(), renderGhost() helpers
├── NEW: map.on('mousemove' / 'mouseout') registration
├── NEW: watch [mouseLatLng, tool, strikeLine, auxiliaryLines, mode] → renderGhost
├── MODIFIED render(): add 2 strike endpoint markers (red, draggable in edit)
├── MODIFIED render(): add N aux vertex markers per line (orange, draggable in edit)
├── NEW: draggingStrikeIndex ref
├── NEW: draggingAuxKey ref
└── MODIFIED render(): skip recreating markers currently being dragged

MapBoundaryEditor.vue
├── MODIFIED toolbar: "× 删除选中" always visible in edit mode (was conditional)
└── MODIFIED hintText: more precise per-state text

HazardPoint.vue
├── MODIFIED onMapDone: sync formData.longitude/latitude from center
└── (no other change)
```

Component layer stays thin — all new behavior in composable. The component only updates hint text and the always-visible delete button.

## 5. Behavior changes

### 5.1 P1: Center persistence

**Before:** `onMapDone(value, center)` writes to `formData.boundaryCoords` and a separate `formCenter` ref; `formData.longitude/latitude` untouched. Payload reads from `formData.lat/lng`. Saved lat/lng is whatever was in the form inputs at open time, regardless of what the user did in the map.

**After:** No `formCenter` ref. The map editor's center IS `formData.longitude` / `formData.latitude`. `:initial-center` reads `{ lat: formData.latitude, lng: formData.longitude }` directly. `onMapDone` writes back to formData on commit.

**Code change in HazardPoint.vue:**

```ts
// before
:initial-center="formCenter"
const onMapDone = (value, center) => {
  formData.boundaryCoords = value
  if (center) formCenter.value = center  // separate ref, never reaches formData
  mapDialogVisible.value = false
}

// after
:initial-center="{ lat: formData.latitude, lng: formData.longitude }"
const onMapDone = (value, center) => {
  formData.boundaryCoords = value
  if (center) {
    formData.longitude = center.lng   // ← directly into form data
    formData.latitude = center.lat
  }
  mapDialogVisible.value = false
}
```

`handleEdit` row-load no longer needs to set `formCenter` (it sets `formData.latitude/longitude` directly from the row, which is what the map will pick up as initial-center).

Invariant: after `done` with a non-null center, `formData.longitude === center.lng && formData.latitude === center.lat`.

Edge case: if user opens dialog but doesn't change anything, `center` is still set (centroid or initial value). On save, formData.lat/lng gets the same value (no-op). Safe.

### 5.2 P3: Visual feedback during drawing

**New composable state:**
```ts
const mouseLatLng = ref<LatLng | null>(null)
const ghostGroup = shallowRef<L.LayerGroup | null>(null)
```

**New map events (registered in the existing leaflet.map watch):**
```ts
map.on('mousemove', e => { mouseLatLng = { lat: e.latlng.lat, lng: e.latlng.lng } })
map.on('mouseout',  () => { mouseLatLng = null })
```

**Render rule (called from a new watch on `[mouseLatLng, tool, strikeLine, auxiliaryLines, mode]`):**

| State | Contents of `ghostGroup` |
|---|---|
| view, edit (no tool) | empty (group exists but no children) |
| DRAW-strike, 0 points placed | blue ghost circleMarker at `mouseLatLng` only |
| DRAW-strike, 1 point (start) placed | pulse marker at start (red 18px div with pulse animation + "起点 ✓" label); blue ghost circleMarker at cursor; dashed red polyline from start to cursor (opacity 0.6) |
| DRAW-strike, 2 points placed | tool auto-exits; permanent endpoint markers take over (see 5.3) |
| DRAW-aux, 0 points | blue ghost circleMarker at cursor |
| DRAW-aux, ≥ 1 points | all placed vertices get permanent orange square markers (last one with pulse); blue ghost circleMarker at cursor; dashed orange polyline through all placed vertices → cursor |
| DRAW-polygon | unchanged (clicks add vertex, polygon grows naturally) |

**Cleanup:** when tool exits (or `done` fires, or mouse leaves map), `mouseLatLng = null` and `ghostGroup = null`. No stale layers.

### 5.3 P4: Strike endpoint + aux point markers (draggable in edit mode)

**Strike endpoints (2 per strike):**
- Red divIcon marker, 18px, white border, similar visual to polygon vertices
- Numbered: endpoint 0 has "1", endpoint 1 has "2"
- `draggable: mode.value === 'edit' && canEdit.value`
- `drag` event → `moveStrikeEndpoint(idx, latlng)`
- `dragstart` sets `draggingStrikeIndex = idx`; `dragend` clears it
- `click` event → `select({ kind: 'strike-endpoint', index: idx })`
- Selected: red pulse ring (same as polygon vertex selected state)

**Aux points (N per line):**
- Orange divIcon marker, 16px square, white border
- `draggable: mode.value === 'edit' && canEdit.value`
- `drag` event → `moveAuxPoint(lineIdx, pointIdx, latlng)`
- `dragstart` sets `draggingAuxKey = { line: lineIdx, point: pointIdx }`; `dragend` clears it
- `click` event → `select({ kind: 'aux-line', index: lineIdx })` (selecting the line, not the point)
- Selected: red pulse ring

**Why whole-line select for aux points:** consistent with how strike endpoint select works. Individual point delete is YAGNI.

**Drag-during-render race fix (same pattern as polygon vertex):**
- Track `draggingStrikeIndex` and `draggingAuxKey`
- In `render()`, skip recreating the marker being dragged; existing markers get `setLatLng` + `setIcon` in place
- Avoids the "drag stops after a few pixels" bug from removing the marker mid-drag

**Visual consistency table:**

| Element | Color | Shape | Size | Selected ring |
|---|---|---|---|---|
| Polygon vertex | green | circle | 28px | red pulse + number |
| Strike endpoint | red | circle | 18px | red pulse + number |
| Aux point | orange | square | 16px | red pulse + number |
| Center | blue | circle (★) | 30px | highlight when dragging |

### 5.4 P2: Always-visible delete button

**Before:** "× 删除选中" button has `v-if="editor.mode.value === 'edit' && editor.selectedId.value"`. Hidden until something is selected.

**After:** Same button, but `v-show` instead of `v-if`, plus `:disabled="!editor.selectedId.value"`. Button stays in the toolbar; disabled state communicates "select something first". Add a tooltip via `el-tooltip`: "先点击线段，再删除".

Tradeoff: slightly more visual noise in toolbar (an always-disabled button). Acceptable because it teaches the workflow.

## 6. Data flow

```
User edits in map
  │
  ▼
composable state updates (polygon / strikeLine / aux / center)
  │
  ├─▶ watch fires (existing)
  │     └─▶ render() redraws layers (existing, plus new endpoint/aux markers)
  │
  └─▶ watch fires (new) on mouseLatLng/tool/data
        └─▶ renderGhost() updates preview

User clicks 完成
  │
  ▼
MapBoundaryEditor.emitDone(value, center)
  │
  ▼
HazardPoint.onMapDone(value, center)
  │
  ├─ formData.boundaryCoords = value (existing)
  ├─ formCenter.value = center (existing)
  └─ formData.longitude/latitude = center.lng/lat (NEW: Issue 1 fix)
  │
  ▼
buildHazardPointPayload()
  │
  ▼
backend POST /hazard-point (formData.lat/lng reflects map edits)
```

## 7. Error handling

| Scenario | Behavior |
|---|---|
| User opens dialog, no edits, clicks 完成 | `formData.lat/lng` gets same value (no-op). No backend call differs |
| User draws polygon, center auto-derives to centroid, clicks 完成 | `formData.lat/lng` gets the centroid. Save reflects current visual center |
| User drags center, clicks 完成 | `formData.lat/lng` gets the dragged position. Save reflects manual override |
| User clicks 完成 with empty polygon (canSave=false) | `emitDone` early-returns with `ElMessage.warning`. No emit, no save |
| Map click during DRAW-polygon | Same as before; new vertex added (no ghost needed) |
| Mouse leaves map mid-DRAW | `mouseout` → `mouseLatLng = null` → ghost cleared. Cursor off-map = no preview |
| Strike endpoint dragged past another point | Move only updates that endpoint. Strike line redraws from new position to the other endpoint |
| Aux point dragged onto another aux point | Both remain (no auto-merge). User can manually delete one |

## 8. Testing

8-10 new unit tests in `useMapEditor.test.ts`:

| Test | What it asserts |
|---|---|
| mouseLatLng updates on map mousemove | new ref behavior |
| mouseLatLng null on mouseout | cleanup |
| ghostGroup null in view mode | no pollution |
| ghostGroup empty in DRAW-strike with 0 points | only cursor dot |
| ghostGroup has dashed line in DRAW-strike with 1 point | A scheme visual |
| ghostGroup has dashed polyline in DRAW-aux with ≥ 1 points | A scheme visual |
| moveStrikeEndpoint replaces the right endpoint | Issue 4 logic |
| moveAuxPoint replaces the right (line, point) | Issue 5 logic |
| Remove aux line doesn't shift indices of others | regression guard |
| Manual smoke for HazardPoint center persistence | end-to-end via mounted test |

Component test: verify "× 删除选中" is always visible in edit mode (not gated by `v-if`).

No new E2E (consistent with prior phase). Manual smoke checklist:

1. Open hazard point editor, draw 4-vertex polygon
2. Center auto-derives to centroid (already worked)
3. Drag center manually — center moves, polygon unchanged
4. Click 完成 — reopen dialog, verify center is at dragged position
5. Click 走向 tool — first click shows pulse + ghost line to cursor; second click completes
6. Drag a strike endpoint — line reshapes from new endpoint
7. Click 辅助线 tool — add 3 points, press Enter — line commits
8. Drag an aux point — line reshapes
9. Click an aux line → "× 删除选中" button enabled → click → line removed
10. Click "完成" — backend receives all changes

## 9. Out of scope

- Touch/mobile drawing experience (separate concern)
- Multi-select for batch delete
- Strike angle as numeric input (drag handle is enough)
- Per-point aux delete (YAGNI — drag or whole-line delete)
- Auto-snap of strike/aux to existing vertices
- Undo/redo for drawing actions
- Dashboard toolbar migration (still out of scope, see prior spec)

## 10. Migration

No data migration. No schema change. No dependency change. All changes are within `web/`.

## 11. Risk

| Risk | Mitigation |
|---|---|
| Adding markers for every strike endpoint + aux point could be slow with many lines | 8-20 markers is typical; not a concern at expected scale. If needed, future optimization: only render markers for visible lines |
| Drag of strike endpoint accidentally creates a very long line that violates some geological constraint | No constraint exists in current schema. YAGNI on validation |
| `done` event mutation of formData might race with backend save if user double-clicks 完成 | `el-button :disabled="!editor.canSave.value"` prevents double-click. Plus `emitDone` early-returns with ElMessage if not canSave. Safe |

## 12. Open questions

None. All design choices approved in brainstorming round.

## 13. Changelog

| Date | Change |
|---|---|
| 2026-06-11 | First version (brainstorming output) |
