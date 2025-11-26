package org.sa.APPS;

import org.locationtech.jts.geom.Polygon;
import org.sa.DTO.PointDTO;
import org.sa.b_storage.TownData;
import org.sa.service.EfficiencyService;
import org.sa.service.GeoUtils;
import org.sa.service.PointsWriterToGpxFile;
import org.sa.service.GpxFileToPointsParser;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class GpxRoutesFilterApp {
  private static final String MAIN_TOWN = "kaunas"; // should be exact name of a town in TownData class
  private static final int MAX_CENTER_RADIUS_ALLOWED_KM = 72; // will get all the routes withing this radius
  private static final int MAX_EDGE_OFFSET_KM = 35; // will get all the routes withing this offset from current main town intvl edge
  private static final int MIN_EFFICIENCY_ALLOWED = 0; // will only take routes above this efficiency
  private static final String SOURCE_DIR_ALL_UNFILTERED_ROUTES = "src/main/java/org/sa/b_storage/routes";
  private static final String INTVL_EDGES_DIR = "src/main/java/org/sa/b_storage/INTVL-taken"; // data about mobile game INTVL (made manually)
  private static final String OUTPUT_DIR =
      "src/main/java/org/sa/b_storage/filtered/" +
      MAIN_TOWN +
      LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("_yyyy_MM_dd")) +
      "_radius" + MAX_CENTER_RADIUS_ALLOWED_KM +
      "_efficiency" + MIN_EFFICIENCY_ALLOWED +
      "_offset" + MAX_EDGE_OFFSET_KM + "/";

  public static void main(String[] args) {

    //prep data
    PointDTO mainTownCenterPoint = TownData.townName_townCenterPoint.get(MAIN_TOWN.toLowerCase());
    File[] allIntvlCityEdges = new File(INTVL_EDGES_DIR).listFiles((dir, fileName) -> fileName.toLowerCase().endsWith(".gpx"));
    Arrays.stream(allIntvlCityEdges).filter(f -> !f.getName().matches("^\\d{4}_\\d{2}_\\d{2} [A-Za-z]+ .*\\.gpx$")).findAny().ifPresent(f -> {
      throw new IllegalArgumentException("Filename: " + f.getName() + ". Must be 'YYYY_MM_DD CityName description.gpx'");
    });
    List<PointDTO> mainTownIntvlEdge = GpxFileToPointsParser.parseFromGpxFileToPoints(Arrays.stream(allIntvlCityEdges).filter(f -> f.getName().toLowerCase().contains(" " + MAIN_TOWN.toLowerCase() + " ")).max(Comparator.comparing(File::getName)).orElse(null));
    Polygon mainTownEdgeOffset = GeoUtils.offsetPolygonOutwards(mainTownIntvlEdge, MAX_EDGE_OFFSET_KM);

    //write good INTVL edges for each town
    Map<String, List<PointDTO>> intvlTownFilename_points = getGoodIntvlTownEdges(allIntvlCityEdges, mainTownCenterPoint, mainTownEdgeOffset, mainTownIntvlEdge);
    intvlTownFilename_points.forEach((name, route) -> new PointsWriterToGpxFile().outputPointsAsGPX(route, name, OUTPUT_DIR));

    //write good circle-routes
    int passedFilterCount = 1;
    for (File unfilteredRouteGpxFile : new File(SOURCE_DIR_ALL_UNFILTERED_ROUTES).listFiles((d, n) -> n.toLowerCase().endsWith(".gpx"))) {
      passedFilterCount = writeRouteIfPassedFilters(unfilteredRouteGpxFile, mainTownCenterPoint, intvlTownFilename_points, passedFilterCount, mainTownEdgeOffset);
    }
  }

  private static int writeRouteIfPassedFilters(File unfilteredRouteGpxFile, PointDTO mainTownCenterPoint, Map<String, List<PointDTO>> filenameOfTownNearbyOrMain_latestIntvlEdge, int passedFilterCount, Polygon mainTownEdgeOffset) {
    //get route points
    List<PointDTO> unfilteredRoutePoints = GpxFileToPointsParser.parseFromGpxFileToPoints(unfilteredRouteGpxFile);

    //filter out: too far
    boolean isFarByRadius = GeoUtils.getDistanceBetweenLocations(mainTownCenterPoint, unfilteredRoutePoints.get(0)) > MAX_CENTER_RADIUS_ALLOWED_KM;
    boolean isFarByOffset = !GeoUtils.isWithinPolygon(mainTownEdgeOffset, unfilteredRoutePoints.get(0));
    if (isFarByRadius && isFarByOffset)
      return passedFilterCount;

    //filter out: not efficient
    if (new EfficiencyService().getRouteEfficiency(unfilteredRoutePoints).efficiencyPercent < MIN_EFFICIENCY_ALLOWED)
      return passedFilterCount;

    //filter out: already covered by INTVL edges (main town or ones nearby)
    for (List<PointDTO> latestIntvlEdge : filenameOfTownNearbyOrMain_latestIntvlEdge.values()) {
      if (GeoUtils.areMostPointsWithinPolygon(unfilteredRoutePoints, latestIntvlEdge, 80)) {
        System.err.println(unfilteredRouteGpxFile.getName() + " is within INTV edge and was excluded from filter output: already covered by INTVL edge");
        return passedFilterCount;
      }
    }

    //route is good. write to output
    new PointsWriterToGpxFile().outputPointsAsGPX(unfilteredRoutePoints, unfilteredRouteGpxFile.getName().replace(".gpx", "_" + passedFilterCount++ + ".gpx"), OUTPUT_DIR);
    return passedFilterCount;
  }

  private static Map<String, List<PointDTO>> getGoodIntvlTownEdges(File[] allIntvlCityEdges, PointDTO mainTownCenterPoint, Polygon mainTownEdgeOffset, List<PointDTO> mainTownIntvlEdge) {
    Map<String, List<PointDTO>> intvlTownFilename_points = new HashMap<>();

    for (File intvlCityEdgeFile : allIntvlCityEdges) {
      List<PointDTO> intvlTownEdgePoints = GpxFileToPointsParser.parseFromGpxFileToPoints(intvlCityEdgeFile);

      //filter out ones that are far
      boolean isIntvlEdgeCloseToMainTown_byCenterRadius = intvlTownEdgePoints.stream().anyMatch(cityEdgePoint -> GeoUtils.getDistanceBetweenLocations(mainTownCenterPoint, cityEdgePoint) < MAX_CENTER_RADIUS_ALLOWED_KM);
      boolean isIntvlEdgeCloseToMainTown_byEdgeOffset = intvlTownEdgePoints.stream().anyMatch(pointDTO -> GeoUtils.isWithinPolygon(mainTownEdgeOffset, pointDTO));
      if (!isIntvlEdgeCloseToMainTown_byCenterRadius && !isIntvlEdgeCloseToMainTown_byEdgeOffset) {
        System.out.println(intvlCityEdgeFile.getName() + " filtered out: too far");
        continue;
      }

      //filter out ones that are covered by main town INTVL edge (except main town INTVL edge itself)
      if (GeoUtils.areMostPointsWithinPolygon(intvlTownEdgePoints, mainTownIntvlEdge, 95)) {
        if (intvlTownEdgePoints.equals(mainTownIntvlEdge)) {
          System.out.println("\u001B[32mmain town latest edge: " + intvlCityEdgeFile.getName() + "\u001B[0m");
        } else {
          System.err.println(intvlCityEdgeFile.getName() + ": INTVL edge is within " + MAIN_TOWN + " INTVL edge and was excluded from filter output");
          continue;
        }
      }

      intvlTownFilename_points.put(intvlCityEdgeFile.getName(), intvlTownEdgePoints);
    }

    return intvlTownFilename_points;
  }
}
