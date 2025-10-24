package org.sa.APPS;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LithuaniaTraverser {

  public static String GRAPHHOPPER_PROFILE_FOOT_SHORTEST = "foot_shortest"; // delete cache when changed!!!
  private RouteGenerator routeGenerator = new RouteGenerator();
  private GraphHopper graphHopper = new GraphHopper(GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
  private GpxOutput gpxOutput = new GpxOutput();
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

      Polygon ltOffset = GeoUtils.offsetPolygonInwards(lithuaniaContour, perimeter / 2);
      for (double latitude = LITHUANIA_MIN_LAT; latitude <= LITHUANIA_MAX_LAT; latitude += LT_GRID_STEP_KM * LAT_STEP_1000_M) {
        for (double longitude = LITHUANIA_MIN_LON; longitude <= LITHUANIA_MAX_LON; longitude += LT_GRID_STEP_KM * LON_STEP_1000_M) {
          totalInstances++; //325 458
          //if (lithuaniaInstances >= 1_000) break outer;
          if (GeoUtils.isWithinPolygon(ltOffset, latitude, longitude)) {
            lithuaniaInstances++;



            //Total points: 108486, inside Lithuania: 47456, duration: 7 seconds
            PointDTO center = new PointDTO(latitude, longitude); //+1 s
            List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, perimeter, MAX_DISTANCE_BETWEEN_POINTS_KM); // +0s

            //Total points: 325458, inside Lithuania: 148305, duration: 447 seconds
            //Snapping 332 routes per second
            //Total points: 38843, inside Lithuania: 10000, duration: 30 seconds
            //Snapping 333 routes per second
            //Total points: 27752, inside Lithuania: 5000, duration: 16 seconds
            //Snapping 312 routes per second
            List<PointDTO> snappedCircle = graphHopper.snapPointsOnRoadGrid(perfectCircle);

            //Total points: 27752, inside Lithuania: 5000, duration: 38 seconds
            //Routing 131 routes per second
            //Total points: 21325, inside Lithuania: 2500, duration: 21 seconds
            //Routing 119 routes per second
            List<PointDTO> routedClosedCircle = graphHopper.connectSnappedPointsWithRoutesAndClose(snappedCircle, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
            //do i need both to snap and route? or this can be done in one?

            //first try: Total points: 21325, inside Lithuania: 2500, duration: 1805 seconds
            //second try: Total points: 15357, inside Lithuania: 1000, duration: 272 seconds
            List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routedClosedCircle);

            EfficiencyDTO eff = efficiencyService.getRouteEfficiency(noLoopRoutedPoints);
            //Total points: 11537, inside Lithuania: 100, duration: 30 seconds, OK routes: 24
            //Total points: 11537, inside Lithuania: 100, duration: 30 seconds, OK routes: 5
            //Total points: 13409, inside Lithuania: 300, duration: 91 seconds, OK routes: 4
            //Total points: 17588, inside Lithuania: 1000, duration: 327 seconds, OK routes: 7 , max efficiency: 70
            if (eff.efficiencyPercent >= 67) {
              okInstances++;
              maxEfficiency = Math.max(maxEfficiency, eff.efficiencyPercent);

              //Total points: 17588, inside Lithuania: 1000, duration: 402 seconds, OK routes: 7 , max efficiency: 70
              //Total points: 108486, inside Lithuania: 47456, duration: 13455 seconds, OK routes: 70 , max efficiency: 80
              //Total points: 216972, inside Lithuania: 93030, duration: 24414 seconds, OK routes: 47 , max efficiency: 84
              //15km: Total points: 108486, inside Lithuania: 53453, duration: 5612 seconds, OK routes: 84 , max efficiency: 85
              String city = CitiesTraverser.city_townCenter.entrySet().stream()
                  .min(Comparator.comparingDouble(e -> GeoUtils.getDistanceBetweenLocations(e.getValue(), noLoopRoutedPoints.get(0))))
                  .map(java.util.Map.Entry::getKey)
                  .orElse("Unknown");

              System.out.println(city + " " +  eff);

              gpxOutput.outputGPX(
                  noLoopRoutedPoints,
                  //eff.efficiencyPercent + "eff_" + (int)eff.routeLength + "km_" + "_" + (int)eff.routeAreaKm + "sqkm_" + okInstances
                  city + okInstances + ".gpx"
              );
            }
            if (lithuaniaInstances % 500 == 0) System.out.println(((lithuaniaInstances * 100) / 47456) + "%");
          }
        }
      }
    }

    System.out.println(
        "Total points: " + totalInstances +
        ", inside Lithuania: " + lithuaniaInstances +
        ", duration: " + (int) java.time.Duration.between(start, LocalDateTime.now()).toSeconds() + " seconds, " +
        "OK routes: " + okInstances + " , " +
        "max efficiency: " + maxEfficiency);
  }

  private static Polygon getLithuaniaContour() {
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
