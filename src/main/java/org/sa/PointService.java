package org.sa;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.shapes.GHPoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PointService {
  public static String MAP_DATA_PATH = "src/main/java/org/sa/map-data/planet_22.965,54.513_25.01,55.257.osm";
  public static String GPX_OUTPUT_DIR = "src/main/java/org/sa/output-gpx";

  private static GraphHopper hopper;

  static {
    hopper = new GraphHopper()
        .setOSMFile(MAP_DATA_PATH)
        .setGraphHopperLocation("graph-cache")
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

  public void outputGPX(List<PointDTO> circlePointsSnappedOnRoad) {
    try {
      File dir = new File(GPX_OUTPUT_DIR);
      if (!dir.exists()) dir.mkdirs();

      File gpxFile = new File(dir, "circle-route.gpx");
      try (FileWriter writer = new FileWriter(gpxFile)) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<gpx version=\"1.1\" creator=\"PointService\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        writer.write("  <trk>\n");
        writer.write("    <name>Circle Route</name>\n");
        writer.write("    <trkseg>\n");

        for (PointDTO p : circlePointsSnappedOnRoad) {
          writer.write(String.format("      <trkpt lat=\"%f\" lon=\"%f\"></trkpt>\n", p.latitude, p.longitude));
        }

        writer.write("    </trkseg>\n");
        writer.write("  </trk>\n");
        writer.write("</gpx>\n");
      }

      System.out.println("GPX file written to: " + gpxFile.getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException("Failed to write GPX file", e);
    }
  }

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
        // fallback: just add straight line
        routedPoints.add(from);
        routedPoints.add(to);
      }
    }
    // close the loop: last point back to first
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

  public List<PointDTO> removeLoops(List<PointDTO> routedPoints, double loopThresholdKm) {
    List<PointDTO> cleaned = new ArrayList<>();
    if (routedPoints.isEmpty()) return cleaned;

    int i = 0; //routedPoints index
    //start always get added
    cleaned.add(routedPoints.get(0));

    //if loop is found — cut it out!
    for (; i < routedPoints.size(); i++) {
      Integer loopEndingIndex = getLoopEndingIndex(routedPoints, i, loopThresholdKm);
      if (loopEndingIndex == null) {
        cleaned.add(routedPoints.get(i));
        i++;
      }
      else {
        cleaned.add(routedPoints.get(loopEndingIndex));
        i = loopEndingIndex + 1;
      }
    }

    return cleaned;
  }

  private Integer getLoopEndingIndex(List<PointDTO> routedPoints, int i, double loopThresholdKm) {
    //get first index outside loopThreshold
    PointDTO current = routedPoints.get(i);
    Integer startIndex = i;
    while (GeoUtils.haversine(routedPoints.get(startIndex), current) < loopThresholdKm)
      startIndex++;

    //start looking for the biggest possible loop, so start from the end point
    for (int j = routedPoints.size() - 2; j >= startIndex; j--)
      if (GeoUtils.haversine(routedPoints.get(j), current) < loopThresholdKm)
        return (Integer) j;

    return null;
  }
}

