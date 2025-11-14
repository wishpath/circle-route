# CIRCLE-ROUTE

CIRCLE-ROUTE is a Java application for discovering and analyzing the most circle-like running routes inside a geographic region. The goal is to maximize the enclosed area per kilometer of running distance.

---

## Core Idea

For each candidate center point:

1. Generate perfect circle perimeter points.
2. Snap them to real roads (GraphHopper).
3. Build a closed runnable route.
4. Remove loops or distortions.
5. Measure area, length, and efficiency.
6. Save efficient routes as GPX files.

---

## Using CIRCLE-ROUTE for a New Region

### 1. Download Map Data (OSM)
Download an `.osm.pbf` file for your region from:
- https://download.geofabrik.de/
- https://garmin.bbbike.org/

Place the file in your **map_data directory**, then update the GraphHopper file path.

---

### 2. Define the Region Boundary
1. Open https://gpx.studio/
2. Draw the outer boundary of your region.
3. Export as GPX.
4. Save it into the **map_data directory**.
5. Update the code where the Lithuania boundary is loaded.

---

### 3. (Optional) Add Town Centers
Provide a list similar to `TownData` if you want routes labeled by nearest town.

---

### 4. Create Your Traverser
Use `LithuaniaTraverser` as an example and configure:
- region bounds
- grid step
- circle length range
- your boundary GPX

Then scan and evaluate routes.