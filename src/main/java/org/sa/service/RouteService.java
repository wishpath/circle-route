package org.sa.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.shapes.GHPoint;
import org.sa.GeoUtils;
import org.sa.PointDTO;
import org.sa.config.Props;

import java.util.ArrayList;
import java.util.List;

public class RouteService {
  public static String KAUNAS_MAP_DATA_PATH = "src/main/java/org/sa/map-data/planet_22.965,54.513_25.01,55.257.osm";
  public static String ROKISKIS_MAP_DATA_PATH = "src/main/java/org/sa/map-data/rokiskis_25.314_55.724_2c97fadb.osm";
  public static String NAME_PART_FOR_GITIGNORE = "-graph-cache";


  private static GraphHopper hopper;

  static {
    hopper = new GraphHopper()
        .setOSMFile(ROKISKIS_MAP_DATA_PATH)
        .setGraphHopperLocation(Props.CIRCLE_AND_MAP_DATA_NAME + NAME_PART_FOR_GITIGNORE) //for new map data, please change this name, to build new chache
        .setProfiles(new Profile("foot").setVehicle("foot").setWeighting("fastest"))
        .importOrLoad();
  }

  public List<PointDTO> generatePerfectCirclePoints(PointDTO center, double circleLengthKm, double maxDistanceBetweenPointsKm) {
    double earthRadiusKm = 6371.0;
    double radiusKm = circleLengthKm / (2 * Math.PI);
    int pointCount = (int) Math.ceil(circleLengthKm / maxDistanceBetweenPointsKm);
    List<PointDTO> points = new ArrayList<>();

    for (int i = 0; i < pointCount; i++) {
      double angle = 2 * Math.PI * i / pointCount;
      double latOffset = (radiusKm / earthRadiusKm) * Math.sin(angle);
      double lonOffset = (radiusKm / (earthRadiusKm * Math.cos(Math.toRadians(center.latitude)))) * Math.cos(angle);
      double lat = center.latitude + Math.toDegrees(latOffset);
      double lon = center.longitude + Math.toDegrees(lonOffset);
      points.add(new PointDTO(lat, lon));
    }
    return points;
  }

  public List<PointDTO> snapPointsOnRoadGrid(List<PointDTO> perfectCircle) {
    List<PointDTO> snapped = new ArrayList<>();
    for (PointDTO p : perfectCircle) {
      GHRequest req = new GHRequest(p.latitude, p.longitude, p.latitude, p.longitude).setProfile("foot");
      GHResponse rsp = hopper.route(req);

      if (!rsp.hasErrors()) {
        ResponsePath path = rsp.getBest();
        GHPoint snappedPoint = path.getPoints().get(0);
        snapped.add(new PointDTO(snappedPoint.lat, snappedPoint.lon));
      } else snapped.add(p);
    }
    return snapped;
  }

//  public List<PointDTO> connectSnappedPointsWithRoutes(List<PointDTO> snappedPoints) {
//    List<PointDTO> routedPoints = new ArrayList<>();
//    for (int i = 0; i < snappedPoints.size() - 1; i++) {
//      PointDTO from = snappedPoints.get(i);
//      PointDTO to = snappedPoints.get(i + 1);
//
//      GHRequest req = new GHRequest(from.latitude, from.longitude, to.latitude, to.longitude).setProfile("foot");
//      GHResponse rsp = hopper.route(req);
//
//      if (!rsp.hasErrors()) {
//        ResponsePath path = rsp.getBest();
//        path.getPoints().forEach(p -> routedPoints.add(new PointDTO(p.lat, p.lon)));
////        path.getInstructions().forEach(instr -> {
////          GHPoint junction = instr.getPoints().get(0); // first point of this instruction
////          routedPoints.add(new PointDTO(junction.lat, junction.lon));
////        });
//      } else {
//        // fallback: just add straight line
//        routedPoints.add(from);
//        routedPoints.add(to);
//      }
//    }
//    // close the loop: last point back to first
//    PointDTO first = snappedPoints.get(0);
//    PointDTO last = snappedPoints.get(snappedPoints.size() - 1);
//    GHRequest req = new GHRequest(last.latitude, last.longitude, first.latitude, first.longitude).setProfile("foot");
//    GHResponse rsp = hopper.route(req);
//    if (!rsp.hasErrors()) {
//      ResponsePath path = rsp.getBest();
//      path.getPoints().forEach(p -> routedPoints.add(new PointDTO(p.lat, p.lon)));
////      path.getInstructions().forEach(instr -> {
////        GHPoint junction = instr.getPoints().get(0); // first point of this instruction
////        routedPoints.add(new PointDTO(junction.lat, junction.lon));
////      });
//    }
//    return routedPoints;
//  }

  public List<PointDTO> connectSnappedPointsWithRoutes(List<PointDTO> snappedPoints) {
    List<PointDTO> routedPoints = new ArrayList<>();
    for (int i = 0; i < snappedPoints.size() - 1; i++) {
      PointDTO from = snappedPoints.get(i);
      PointDTO to = snappedPoints.get(i + 1);

      GHRequest req = new GHRequest(from.latitude, from.longitude, to.latitude, to.longitude).setProfile("foot");
      GHResponse rsp = hopper.route(req);

      if (!rsp.hasErrors()) {
        ResponsePath path = rsp.getBest();
        path.getPoints().forEach(p -> routedPoints.add(new PointDTO(p.lat, p.lon)));
      } else {
        routedPoints.add(from);
        routedPoints.add(to);
      }
    }

    // close the loop
    PointDTO first = snappedPoints.get(0);
    PointDTO last = snappedPoints.get(snappedPoints.size() - 1);
    GHRequest req = new GHRequest(last.latitude, last.longitude, first.latitude, first.longitude).setProfile("foot");
    GHResponse rsp = hopper.route(req);
    if (!rsp.hasErrors()) {
      ResponsePath path = rsp.getBest();
      path.getPoints().forEach(p -> routedPoints.add(new PointDTO(p.lat, p.lon)));
    }

    return routedPoints;
  }



  public List<PointDTO> removeLoops(List<PointDTO> routedPoints, double indicatorOfLoop_maxDistance_loopStart_loopFinish) {
    List<PointDTO> cleaned = new ArrayList<>();
    if (routedPoints.isEmpty()) return cleaned;
    double routedPointsTotalDistance = GeoUtils.getRouteDistanceKm(routedPoints);
    double loopSizeThreshold = routedPointsTotalDistance * 0.3;

    int i = 0;

    //if loop is found — cut it out!
    for (; i < routedPoints.size(); i++) {
      Integer loopEndingIndex = getLoopEndingIndex(routedPoints, i, indicatorOfLoop_maxDistance_loopStart_loopFinish);

      //no loop found
      if (loopEndingIndex == null) {
        cleaned.add(routedPoints.get(i));
        i++;
      }
      else {
        boolean isLoopBig = GeoUtils.getRouteDistanceKm(routedPoints, i, loopEndingIndex) > loopSizeThreshold; // computationally expensive!!!
        if (isLoopBig) { //if loop is big, still don't cut it
          cleaned.add(routedPoints.get(i));
          i++;
        }
        else { //cut out the loop
          cleaned.add(routedPoints.get(loopEndingIndex));
          i = loopEndingIndex + 1;
        }
      }
    }

    return cleaned;
  }

  private Integer getLoopEndingIndex(List<PointDTO> routedPoints, int i, double indicatorOfLoop_maxDistance_loopStart_loopFinish) {
    //get first index outside loopThreshold
    PointDTO current = routedPoints.get(i);
    Integer startIndex = i;
    while (GeoUtils.getDistanceBetweenLocations(routedPoints.get(startIndex), current) < indicatorOfLoop_maxDistance_loopStart_loopFinish) {
      startIndex++;
      if (startIndex ==routedPoints.size())
        return null;
    }

    //start looking for the biggest possible loop, so start from the end point
    for (int j = routedPoints.size() - 2; j >= startIndex; j--)
      if (GeoUtils.getDistanceBetweenLocations(routedPoints.get(j), current) < indicatorOfLoop_maxDistance_loopStart_loopFinish)
        return (Integer) j;

    return null;
  }

  public List<PointDTO> shiftABtoBA(List<PointDTO> points) {
    if (points == null || points.isEmpty()) return List.of();
    int mid = points.size() / 2;
    List<PointDTO> firstHalf = points.subList(0, mid);
    List<PointDTO> secondHalf = points.subList(mid, points.size());
    List<PointDTO> result = new ArrayList<>(secondHalf);
    result.addAll(firstHalf);
    return result;
  }

  public PointDTO movePoint(PointDTO point, double northKm, double eastKm) {
    double earthKmPerDegree = 111.32;

    double latOffset = northKm / earthKmPerDegree;
    double lonOffset = eastKm / (earthKmPerDegree * Math.cos(Math.toRadians(point.latitude)));

    double newLat = point.latitude + latOffset;
    double newLon = point.longitude + lonOffset; // east is positive

    return new PointDTO(newLat, newLon);
  }
}

