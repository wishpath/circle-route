package org.sa.APPS;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.map_data.TownData;
import org.sa.service.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * scans lithuania for the best circle routes
 * */
public class LithuaniaTraverser {

  public static String GRAPHHOPPER_PROFILE_FOOT_SHORTEST = "foot_shortest"; // delete cache when changed!!!
  private RouteService routeGenerator = new RouteService();
  private GraphHopper graphHopper = new GraphHopper(GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
  private PointsWriterToGpxFile gpxOutput = new PointsWriterToGpxFile();
  private EfficiencyService efficiencyService = new EfficiencyService();

  private static final double LITHUANIA_MIN_LAT = 53.88; // y, vertical
  private static final double LITHUANIA_MAX_LAT = 56.45; // y, vertical
  private static final double LITHUANIA_MIN_LON = 20.93; // x, horizontal
  private static final double LITHUANIA_MAX_LON = 26.84; // x, horizontal
  private static final double LAT_STEP_1000_M = 1000.0 / 111_320.0; // ≈ 0.00898 degrees ≈ 1 km
  private static final double LON_STEP_1000_M = 1000.0 / (111_320.0 * Math.cos(Math.toRadians(55.0))); // ≈ 0.0156 degrees ≈ 1 km at ~55°N private static final double STEP_KOEF = 10.0;
  private static final double LT_GRID_STEP_KM = 1.0;
  private static final double CIRCLE_LENGTH_MIN = 50.0;
  private static final double CIRCLE_LENGTH_MAX = 50.0;
  private static final double CIRCLE_LENGTH_STEP = 5.0;

  private static final int MAX_DISTANCE_BETWEEN_POINTS_KM = 2; // distance between ideal circle points

  public void traverse() {
    long totalInstances = 0;
    long lithuaniaInstances = 0;
    long okInstances = 0;
    int maxEfficiency = -1;

    Polygon lithuaniaContour = getLithuaniaContour();
    LocalDateTime start = LocalDateTime.now();
    outer:
    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {

      Polygon ltOffset = GeoUtils.offsetPolygonInwards(lithuaniaContour, perimeter / 2); // so the route does not cross the border zone
      for (double latitude = LITHUANIA_MIN_LAT; latitude <= LITHUANIA_MAX_LAT; latitude += LT_GRID_STEP_KM * LAT_STEP_1000_M) {
        for (double longitude = LITHUANIA_MIN_LON; longitude <= LITHUANIA_MAX_LON; longitude += LT_GRID_STEP_KM * LON_STEP_1000_M) {
          totalInstances++;
          if (GeoUtils.isWithinPolygon(ltOffset, latitude, longitude)) {
            lithuaniaInstances++;

            //pruduce route
            PointDTO center = new PointDTO(latitude, longitude); //+1 s
            List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, perimeter, MAX_DISTANCE_BETWEEN_POINTS_KM); // +0s
            List<PointDTO> snappedCircle = graphHopper.snapPointsOnRoadGrid(perfectCircle);
            List<PointDTO> routedClosedCircle = graphHopper.connectSnappedPointsWithRoutesAndClose(snappedCircle, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
            List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routedClosedCircle);

            //if efficient — write to storage
            EfficiencyDTO efficiency = efficiencyService.getRouteEfficiency(noLoopRoutedPoints);
            if (efficiency.efficiencyPercent >= 67) {
              okInstances++;
              maxEfficiency = Math.max(maxEfficiency, efficiency.efficiencyPercent);

              //name route/file by city name
              String closestCity = TownData.townName_townCenterPoint.entrySet().stream()
                  .min(Comparator.comparingDouble(e -> GeoUtils.getDistanceBetweenLocations(e.getValue(), noLoopRoutedPoints.get(0))))
                  .map(java.util.Map.Entry::getKey)
                  .orElse("Unknown");
              System.out.println(closestCity + " " +  efficiency);

              gpxOutput.outputPointsAsGPX(noLoopRoutedPoints,closestCity + okInstances + ".gpx");
            }

            //print progress pseudo percentage
            if (lithuaniaInstances % 500 == 0)
              System.out.println(((lithuaniaInstances * 100) / 47456) + "%");
          }
        }
      }
    }

    //end result console info
    System.out.println(
        "Total points: " + totalInstances +
        ", inside Lithuania: " + lithuaniaInstances +
        ", duration: " + (int) java.time.Duration.between(start, LocalDateTime.now()).toSeconds() + " seconds, " +
        "OK routes: " + okInstances + " , " +
        "max efficiency: " + maxEfficiency);
  }

  private static Polygon getLithuaniaContour() {
    List<PointDTO> lithuaniaContour = GpxFileToPointsParser.parseFromGpxFileToPoints(new File("src/main/java/org/sa/map_data/lithuania_super_rough_closed_contour.gpx"));
    if (lithuaniaContour.size() < 3) throw new RuntimeException("LITHUANIA CONTOUR HAS LESS THAN 3 POINTS");
    List<PointDTO> lithuaniaContourClosed = new ArrayList<>(lithuaniaContour);
    if (!lithuaniaContourClosed.get(0).equals(lithuaniaContourClosed.get(lithuaniaContourClosed.size() - 1)))
      lithuaniaContourClosed.add(lithuaniaContourClosed.get(0));
    Coordinate[] coordinates = lithuaniaContourClosed.stream()
        .map(p -> new Coordinate(p.longitude, p.latitude))
        .toArray(Coordinate[]::new);
    GeometryFactory geometryFactory = new GeometryFactory();
    LinearRing shell = geometryFactory.createLinearRing(coordinates);
    Polygon jtsPolygon = geometryFactory.createPolygon(shell, null);
    return jtsPolygon;
  }

  private List<PointDTO> removeLoopsByLoopingTheSameActions(List<PointDTO> routePoints) {
    double indicatorOfLoop_maxDistance_loopStart_loopFinish_km = 0.2;
    List<PointDTO> noLoops;
    List<PointDTO> noLoopsRouted;
    List<PointDTO> shifted = routePoints;

    for (int i = 0; i < 2; i++) {
      noLoops = routeGenerator.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km, graphHopper, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
      noLoopsRouted = graphHopper.connectSnappedPointsWithRoutesAndClose(noLoops, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
      shifted = routeGenerator.shiftABtoBA_andReverse(noLoopsRouted);
      shifted.add(shifted.get(0));
    }
    return shifted;
  }
}
