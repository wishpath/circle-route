package org.sa.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.sa.DTO.PointDTO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TraverseService {
  public static String GRAPHHOPPER_PROFILE_FOOT_SHORTEST = "foot_shortest"; // delete cache when changing to different profile!!!
  private static final int MAX_DISTANCE_BETWEEN_CIRCLE_PERIMETER_POINTS_KM = 2; // distance between ideal circle points on the perimeter
  public RouteService routeGenerator = new RouteService();
  public GraphHopper graphHopper = new GraphHopper(GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
  public PointsWriterToGpxFile gpxOutput = new PointsWriterToGpxFile();
  public EfficiencyService efficiencyService = new EfficiencyService();
  public Polygon lithuaniaContour = getLithuaniaContour();

  public List<PointDTO> removeLoopsByLoopingTheSameActions(List<PointDTO> routePoints) {
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

  public Polygon getLithuaniaContour() {
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

  public List<PointDTO> produceRoute(PointDTO center, double perimeter) {
    List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, perimeter, MAX_DISTANCE_BETWEEN_CIRCLE_PERIMETER_POINTS_KM);
    List<PointDTO> snappedCircle = graphHopper.snapPointsOnRoadGrid(perfectCircle);
    List<PointDTO> routedClosedCircle = graphHopper.connectSnappedPointsWithRoutesAndClose(snappedCircle, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
    List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routedClosedCircle);
    return noLoopRoutedPoints;
  }

  public Polygon getLithuaniaContourInwardOffset(double perimeter) {
    return GeoUtils.offsetPolygonInwards(lithuaniaContour, perimeter / 2);
  }
}
