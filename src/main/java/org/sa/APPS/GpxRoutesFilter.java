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

    //prep data
    PointDTO mainTownCenterPoint = TownData.townName_townCenterPoint.get(MAIN_TOWN.toLowerCase());
    File[] allIntvlCityEdges = new File(INTVL_EDGES_DIR).listFiles((dir, fileName) -> fileName.toLowerCase().endsWith(".gpx"));
    Arrays.stream(allIntvlCityEdges).filter(f -> !f.getName().matches("^\\d{4}_\\d{2}_\\d{2} [A-Za-z]+ .*\\.gpx$")).findAny().ifPresent(f -> { throw new IllegalArgumentException("Filename: " + f.getName() + ". Must be 'YYYY_MM_DD CityName description.gpx'"); });
    List<PointDTO> mainTownIntvlEdge = GpxParser.parseFromGpxFileToPoints(Arrays.stream(allIntvlCityEdges).filter(f -> f.getName().toLowerCase().contains(" " + MAIN_TOWN.toLowerCase() + " ")).max(Comparator.comparing(File::getName)).orElse(null));

    //write good INTVL edges for each town
    Map<String, List<PointDTO>> filenameOfTownNearbyOrMain_latestIntvlEdge = getTownsLatestIntvlEdge(allIntvlCityEdges, mainTownCenterPoint, mainTownIntvlEdge);
    filenameOfTownNearbyOrMain_latestIntvlEdge.forEach((name, route) ->new GpxOutput().outputPointsAsGPXToDirectory(route, name, OUTPUT_DIR));

    int passedFilterCount = 1;
    nextUnfilteredRouteGpxFile:
    for (File unfilteredRouteGpxFile : new File(SOURCE_DIR_ALL_UNFILTERED_ROUTES).listFiles((d, n) -> n.toLowerCase().endsWith(".gpx"))) {
      passedFilterCount = writeRouteIfPassedFilters(unfilteredRouteGpxFile, mainTownCenterPoint, filenameOfTownNearbyOrMain_latestIntvlEdge, passedFilterCount);
    }
  }

  private static int writeRouteIfPassedFilters(File unfilteredRouteGpxFile, PointDTO mainTownCenterPoint, Map<String, List<PointDTO>> filenameOfTownNearbyOrMain_latestIntvlEdge, int passedFilterCount) {
    //get route points
    List<PointDTO> unfilteredRoutePoints = GpxParser.parseFromGpxFileToPoints(unfilteredRouteGpxFile);

    //filter out: too far
    if (GeoUtils.getDistanceBetweenLocations(mainTownCenterPoint, unfilteredRoutePoints.get(0)) > MAX_RADIUS_ALLOWED_KM)
      return passedFilterCount;

    //filter out: not efficient
    if (new EfficiencyService().getRouteEfficiency(unfilteredRoutePoints).efficiencyPercent < MIN_EFFICIENCY_ALLOWED)
      return passedFilterCount;

    //filter out: already covered by INTVL edges (main town or ones nearby)
    for (List<PointDTO> latestIntvlEdge : filenameOfTownNearbyOrMain_latestIntvlEdge.values()) {
      if (GeoUtils.areMostPointsWithinPolygon(unfilteredRoutePoints, latestIntvlEdge, 95)) {
        System.err.println(unfilteredRouteGpxFile.getName() + " is within INTV edge and was excluded from filter output");
        return passedFilterCount;
      }
    }

    //route is good. write to output
    new GpxOutput().outputPointsAsGPXToDirectory(unfilteredRoutePoints, unfilteredRouteGpxFile.getName().replace(".gpx", "_" + passedFilterCount++ + ".gpx"), OUTPUT_DIR);
    return passedFilterCount;
  }

  private static Map<String, List<PointDTO>> getTownsLatestIntvlEdge(File[] allIntvlCityEdges, PointDTO mainTownCenterPoint, List<PointDTO> mainTownIntvlEdge) {
    //find INTVL edges that are close enough and map them to their town names
    Map<String, List<File>> filenameOfTownNearbyOrMain_intvlEdgesGpxFiles = new HashMap<>();
    for (File intvlCityEdgeFile : allIntvlCityEdges) {
      List<PointDTO> intvlCityEdgePoints = GpxParser.parseFromGpxFileToPoints(intvlCityEdgeFile);

      //filter out ones that are far2025_10_21 kaunas INTVL taken.gpx
      boolean isIntvlEdgeCloseToMainTown = intvlCityEdgePoints.stream().anyMatch(cityEdgePoint -> GeoUtils.getDistanceBetweenLocations(mainTownCenterPoint, cityEdgePoint) < MAX_RADIUS_ALLOWED_KM);
      if (!isIntvlEdgeCloseToMainTown) {
        System.out.println(intvlCityEdgeFile.getName() + " filtered out: too far");
        continue;
      }
      //filter out ones that are covered by main town INTVL edge
      if (GeoUtils.areMostPointsWithinPolygon(intvlCityEdgePoints, mainTownIntvlEdge, 95)) {
        if (intvlCityEdgePoints.equals(mainTownIntvlEdge)) {
          System.out.println("\u001B[32mmain town latest edge: " + intvlCityEdgeFile.getName() + "\u001B[0m");
        }
        else {
          System.err.println(intvlCityEdgeFile.getName() + ": INTVL edge is within " + MAIN_TOWN + " INTVL edge and was excluded from filter output");
          continue;
        }
      }
      filenameOfTownNearbyOrMain_intvlEdgesGpxFiles.computeIfAbsent(intvlCityEdgeFile.getName(), k -> new ArrayList<>()).add(intvlCityEdgeFile);
    }

    //only keep latest single INTVL edge for each town
    Map<String, List<PointDTO>> filenameOfTownNearby_latestIntvlEdge = new HashMap<>();
    filenameOfTownNearbyOrMain_intvlEdgesGpxFiles.forEach((filenameOfTownNearby, intvlEdgesGpxFile) -> {
      File latestIntvlEdgesGpxFile = intvlEdgesGpxFile.stream().max(Comparator.comparing(File::getName)).orElse(null);
      if (latestIntvlEdgesGpxFile == null) {
        System.err.println("latestIntvlEdgesGpxFile should not be null here since it's the point of it being here.");
      }
      else {
        filenameOfTownNearby_latestIntvlEdge.put(filenameOfTownNearby, GpxParser.parseFromGpxFileToPoints(latestIntvlEdgesGpxFile));
      }
    });

    return filenameOfTownNearby_latestIntvlEdge;
  }
}
