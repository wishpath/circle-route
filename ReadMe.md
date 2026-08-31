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
I recommend to list your map data file in the .gitignore file. 

---

### 2. Define the Region Boundary
1. Open https://gpx.studio/
2. Draw the outer boundary of your region.
3. Export as GPX.
4. Save it into the **b_storage directory**.
   1. update path in method getLithuaniaContour() or create a new method.

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
- passable efficiency for output routes.

Then scan and evaluate routes.

# CALCULATE-ROUTE-EFFICIENCY

To standardize existing GPX routes and calculate efficiency metrics:
1. Open `GpxFilesStandardizerApp.java`.
2. Set `GPX_FILES_TO_STANDARDIZE_DIRECTORY` to your input folder path.
3. Place your `.gpx` files in that input folder.
4. Set `STANDARDIZED_OUTPUT_DIRECTORY` to your desired output folder path.
5. Run `GpxFilesStandardizerApp`.
6. The processed files will be saved with calculated efficiency details directly in the filename 
   - (`[Town]_[Length]km_[Efficiency]eff_[Area]sqkm_[Index].gpx`)
     - **Closest Town Name** (calculated from route coordinates)
     - **Route Length** (total distance in km)
     - **Efficiency Percentage** (how closely the route matches an ideal circle)
     - **Enclosed Area** (surface area in sq km)
     - **Sequence Counter** (file index)