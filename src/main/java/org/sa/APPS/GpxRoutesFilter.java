package org.sa.APPS;

import org.sa.DTO.PointDTO;
import org.sa.map_data.TownData;
import org.sa.service.EfficiencyService;
import org.sa.service.GeoUtils;
import org.sa.service.GpxOutput;
import org.sa.service.GpxParser;

import java.io.File;
import java.util.*;

public class GpxRoutesFilter {
  private static final String MAIN_TOWN = "kaunas"; // should be exact name of a town in TownData class
  private static final int MAX_RADIUS_ALLOWED_KM = 66; // will get all the routes withing this radius
  private static final int MIN_EFFICIENCY_ALLOWED = 0; // will only take routes above this efficiency
  private static final String SOURCE_DIR_ALL_UNFILTERED_ROUTES = "src/main/java/org/sa/routes";
  private static final String INTVL_EDGES_DIR = "src/main/java/org/sa/INTVL-taken"; // data about mobile game INTVL (made manually)
  private static final String OUTPUT_DIR = "src/main/java/org/sa/filtered/" + MAIN_TOWN + "_radius" + MAX_RADIUS_ALLOWED_KM + "_efficiency" + MIN_EFFICIENCY_ALLOWED + "/";

  public static void main(String[] args) {

    //prep input data
    File[] allUnfilteredGpxRouteFiles = new File(SOURCE_DIR_ALL_UNFILTERED_ROUTES).listFiles((d, n) -> n.toLowerCase().endsWith(".gpx"));
    if (allUnfilteredGpxRouteFiles == null) {
      System.err.println("allUnfilteredGpxRouteFiles should not be null");
      return;
    }
    PointDTO mainTownCenterPoint = TownData.townName_townCenterPoint.get(MAIN_TOWN.toLowerCase());
    if (mainTownCenterPoint == null) {
      System.err.println("Unknown town center: " + MAIN_TOWN + ". Should be exact name of a town in TownData class");
      return;
    }
    List<PointDTO> mainTownIntvlEdge = getMainTownIntvlEdge();
    if (mainTownIntvlEdge == null) {
      System.err.println("mainTownIntvlEdge should not be null");
      return;
    }

    int counterOfFilteredRoutes = 1;
    for (File unfilteredRouteGpxFile : allUnfilteredGpxRouteFiles) {

      //get route points
      List<PointDTO> unfilteredRoutePoints = GpxParser.parseGpxFile(unfilteredRouteGpxFile);
      if (unfilteredRoutePoints == null || unfilteredRoutePoints.isEmpty()) {
        System.err.println("unfilteredRoutePoints should not be empty: " + unfilteredRouteGpxFile.getName());
        continue;
      }

      //filter out: too far
      if (GeoUtils.getDistanceBetweenLocations(mainTownCenterPoint, unfilteredRoutePoints.get(0)) > MAX_RADIUS_ALLOWED_KM)
        continue;

      //filter out: not efficient
      if (new EfficiencyService().getRouteEfficiency(unfilteredRoutePoints).efficiencyPercent < MIN_EFFICIENCY_ALLOWED)
        continue;

      //filter out: already covered by INTVL
      if (mainTownIntvlEdge != null && GeoUtils.areMostPointsWithinPolygon(unfilteredRoutePoints, mainTownIntvlEdge, 95)) {
        System.err.println(unfilteredRouteGpxFile.getName() + " is within " + MAIN_TOWN + " INTV edge and was excluded from filter output");
        continue;
      }

      //route is good. write to output
      new GpxOutput().outputGPXToDir(unfilteredRoutePoints, unfilteredRouteGpxFile.getName().replace(".gpx", "_" + counterOfFilteredRoutes++ + ".gpx"), OUTPUT_DIR);
    }

    writeIntvlCityEdges(mainTownCenterPoint);
  }

  private static void writeIntvlCityEdges(PointDTO mainTownCenterPoint) {
    //find INTVL edges that are close enough and map them to their town names
    Map<String, List<File>> cityNameNearby_intvlEdgesGpxFiles = new HashMap<>();
    for (File IntvlCityEdgeFile : getAllIntvlCityEdgesFiles()) {
      //filter out ones that are far
      boolean isAnyPointOfIntvlEdgeCloseToMainTown = GpxParser.parseGpxFile(IntvlCityEdgeFile).stream()
          .anyMatch(cityEdgePoint -> GeoUtils.getDistanceBetweenLocations(mainTownCenterPoint, cityEdgePoint) < MAX_RADIUS_ALLOWED_KM);
      if (isAnyPointOfIntvlEdgeCloseToMainTown) {
        String cityName = IntvlCityEdgeFile.getName().split(" ")[1];
        cityNameNearby_intvlEdgesGpxFiles.computeIfAbsent(cityName, k -> new ArrayList<>()).add(IntvlCityEdgeFile);
      }
    }

    //only pick latest INTVL edge for each town and write to output
    cityNameNearby_intvlEdgesGpxFiles.forEach((cityNameNearby, intvlEdgesGpxFile) -> {
      File latestIntvlEdgesGpxFile = intvlEdgesGpxFile.stream().max(Comparator.comparing(File::getName)).orElse(null);
      if (latestIntvlEdgesGpxFile == null) {
        System.err.println("latestIntvlEdgesGpxFile should not be null here since it's the point of it being here");
      }
      new GpxOutput().outputGPXToDir(GpxParser.parseGpxFile(latestIntvlEdgesGpxFile), latestIntvlEdgesGpxFile.getName(), OUTPUT_DIR);
    });
  }

  private static File[] getAllIntvlCityEdgesFiles() {
    File[] allIntvlCityEdges = new File(INTVL_EDGES_DIR).listFiles((dir, fileName) -> fileName.toLowerCase().endsWith(".gpx"));
    if (allIntvlCityEdges == null) {
      System.err.println("No GPX files found in INTVL directory.");
      return null;
    }
    //validate name, important
    for (File cityEdgeFile : allIntvlCityEdges) {
      if (!cityEdgeFile.getName().matches("^\\d{4}_\\d{2}_\\d{2} [A-Za-z]+ .*\\.gpx$"))
        throw new IllegalArgumentException("Filename: " + cityEdgeFile.getName() + ". Must be 'YYYY_MM_DD CityName description.gpx'");
    }
    return allIntvlCityEdges;
  }

  private static List<PointDTO> getMainTownIntvlEdge() {

    //from all INTVL files take ones containing main town name
    File[] all_IntvlCityEdgeFiles = getAllIntvlCityEdgesFiles();
    if (all_IntvlCityEdgeFiles == null || all_IntvlCityEdgeFiles.length == 0) return null;
    List<File> mainTown_IntvlFiles = Arrays.stream(all_IntvlCityEdgeFiles)
        .filter(f -> f.getName().toLowerCase().contains(" " + MAIN_TOWN.toLowerCase() + " "))
        .toList();
    if (mainTown_IntvlFiles.isEmpty()) return null;

    //return the latest one
    File latestMainCityFile = mainTown_IntvlFiles.stream().max(Comparator.comparing(File::getName)).orElse(null);
    return latestMainCityFile == null ? null : GpxParser.parseGpxFile(latestMainCityFile);
  }
}
