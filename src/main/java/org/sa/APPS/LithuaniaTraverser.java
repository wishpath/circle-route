package org.sa.APPS;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.sa.DTO.PointDTO;
import org.sa.service.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LithuaniaTraverser {
  //routing preferences
  public static String GRASSHOPPER_PROFILE_FOOT_SHORTEST = "foot_shortest"; // delete cache when changed
  private RouteGenerator routeGenerator = new RouteGenerator();
  private GraphHopper graphHopperService = new GraphHopper(GRASSHOPPER_PROFILE_FOOT_SHORTEST);
  private GpxOutput gpxOutput = new GpxOutput();

  private static final double LITHUANIA_MIN_LAT = 53.88; // y, vertical
  private static final double LITHUANIA_MAX_LAT = 56.45; // y, vertical
  private static final double LITHUANIA_MIN_LON = 20.93; // x, horizontal
  private static final double LITHUANIA_MAX_LON = 26.84; // x, horizontal
  private static final double LAT_STEP_1000_M = 1000.0 / 111_320.0; // ≈ 0.00898 degrees ≈ 1 km
  private static final double LON_STEP_1000_M = 1000.0 / (111_320.0 * Math.cos(Math.toRadians(55.0))); // ≈ 0.0156 degrees ≈ 1 km at ~55°N private static final double STEP_KOEF = 10.0;
  private static final double LT_GRID_STEP_KM = 1.0;
  private static final double CIRCLE_LENGTH_MIN = 20.0;
  private static final double CIRCLE_LENGTH_MAX = 30.0;
  private static final double CIRCLE_LENGTH_STEP = 5.0;

  private static final int MAX_DISTANCE_BETWEEN_POINTS_KM = 2;

  public void traverse() {
    long totalInstances = 0;
    long lithuaniaInstances = 0;

    Polygon lithuaniaContour = getLithuaniaContour();
    LocalDateTime start = LocalDateTime.now();
    outer:
    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {

      Polygon ltOffset = GeoUtils.offsetPolygonInwards(lithuaniaContour, perimeter / 2);
      for (double latitude = LITHUANIA_MIN_LAT; latitude <= LITHUANIA_MAX_LAT; latitude += LT_GRID_STEP_KM * LAT_STEP_1000_M) {
        for (double longitude = LITHUANIA_MIN_LON; longitude <= LITHUANIA_MAX_LON; longitude += LT_GRID_STEP_KM * LON_STEP_1000_M) {
          totalInstances++; //325 458
          if (lithuaniaInstances >= 5_000) break outer;
          if (GeoUtils.isWithinPolygon(ltOffset, latitude, longitude)) {
            lithuaniaInstances++;
            PointDTO center = new PointDTO(latitude, longitude); //+1 s
            List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, perimeter, MAX_DISTANCE_BETWEEN_POINTS_KM); // +0s

            //Total points: 325458, inside Lithuania: 148305, duration: 447 seconds
            //Snapping 332 routes per second
            //Total points: 38843, inside Lithuania: 10000, duration: 30 seconds
            //Snapping 333 routes per second
            //Total points: 27752, inside Lithuania: 5000, duration: 16 seconds
            //Snapping 312 routes per second
            List<PointDTO> snappedCircle = graphHopperService.snapPointsOnRoadGrid(perfectCircle, GRASSHOPPER_PROFILE_FOOT_SHORTEST);

            //do i need both to snap and route? or this can be done in one?
          }
        }
      }
    }

    System.out.println(
        "Total points: " + totalInstances +
        ", inside Lithuania: " + lithuaniaInstances +
        ", duration: " + (int) java.time.Duration.between(start, LocalDateTime.now()).toSeconds() + " seconds");
  }

  private static Polygon getLithuaniaContour() {
    // validate lithuania contour
    List<PointDTO> lithuaniaContour = GpxParser.parseGpxFile(new File("src/main/java/org/sa/map-data/lithuania_super_rough_closed_contour.gpx"));
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

  private static void generateRoute(RouteGenerator routeService, PointDTO routeCenterPoint, GraphHopper graphHopperService, GpxOutput outputService, int circleCounter) {
    List<PointDTO> perfectCirclePoints = routeService.generatePerfectCirclePoints(routeCenterPoint, 25, 2);
    List<PointDTO> circlePointsSnappedOnRoad = graphHopperService.snapPointsOnRoadGrid(perfectCirclePoints, GRASSHOPPER_PROFILE_FOOT_SHORTEST);


    List<PointDTO> routed = graphHopperService.connectSnappedPointsWithRoutes(circlePointsSnappedOnRoad, GRASSHOPPER_PROFILE_FOOT_SHORTEST);
    //System.out.println("routed points: " + routed.size());

    List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(graphHopperService, routeService, routed);

    List<PointDTO> smoothRoutePoints = new ArrayList<>(noLoopRoutedPoints);
    double maxCheckWindowSize = GeoUtils.autoCloseRouteAndGetLengthKm(noLoopRoutedPoints) / 6;
    String profile = GRASSHOPPER_PROFILE_FOOT_SHORTEST;

    //output
    //outputService.outputGPX(smoothRoutePoints, circleCounter);
    //outputService.outputGpxWaypoints(smoothRoutePoints);

    //printout efficiency parameters
    //GPXRouteEfficiencyEvaluator.evaluateGPXRoutesInDirectory(Props.GPX_OUTPUT_DIR);
  }

  private static List<PointDTO> removeLoopsByLoopingTheSameActions(GraphHopper graphHopperService, RouteGenerator routeService, List<PointDTO> routePoints) {
    double indicatorOfLoop_maxDistance_loopStart_loopFinish_km = 0.2;
    List<PointDTO> noLoops = routeService.removeLoops(routePoints, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
    //System.out.println("1 loops cut: " + noLoops.size());
    List<PointDTO> noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops, GRASSHOPPER_PROFILE_FOOT_SHORTEST); //reroute to fix loop cuts
    //System.out.println("1 rerout: " + noLoopsRouted.size());
    List<PointDTO> shifted = routeService.shiftABtoBA_andReverse(noLoopsRouted);
    //System.out.println("1 shifted: " + shifted.size());

    for (int i = 2; i < 8; i++, indicatorOfLoop_maxDistance_loopStart_loopFinish_km /= 2) {
      //System.out.println();
      noLoops = routeService.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
      //System.out.println(i + " loops cut: " + noLoops.size());
      noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops, GRASSHOPPER_PROFILE_FOOT_SHORTEST);
      //System.out.println(i + " rerout: " + noLoopsRouted.size());
      shifted = routeService.shiftABtoBA_andReverse(noLoopsRouted);
      //System.out.println(i + " shifted: " + shifted.size());
    }
    return shifted;
  }
}
