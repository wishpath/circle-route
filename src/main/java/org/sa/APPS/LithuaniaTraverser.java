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
  private RouteGenerator routeGenerator = new RouteGenerator();
  private GraphHopper graphHopperService = new GraphHopper();
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

  public void traverse() {
    long totalInstances = 0;
    long lithuaniaInstances = 0;

    Polygon lithuaniaContour = getLithuaniaContour();
    LocalDateTime start = LocalDateTime.now();
    outer:
    for (double circleLength = CIRCLE_LENGTH_MIN; circleLength <= CIRCLE_LENGTH_MAX; circleLength += CIRCLE_LENGTH_STEP) {

      Polygon ltOffset = GeoUtils.offsetPolygonInwards(lithuaniaContour, circleLength / 2);
      for (double latitude = LITHUANIA_MIN_LAT; latitude <= LITHUANIA_MAX_LAT; latitude += LT_GRID_STEP_KM * LAT_STEP_1000_M) {
        for (double longitude = LITHUANIA_MIN_LON; longitude <= LITHUANIA_MAX_LON; longitude += LT_GRID_STEP_KM * LON_STEP_1000_M) {
          totalInstances++; //325 458
          //if (totalInstances > 100_000) break outer;
          if (GeoUtils.isWithinPolygon(ltOffset, latitude, longitude)) {
            lithuaniaInstances++;
            //PointDTO center = new PointDTO(latitude, longitude);
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
}
