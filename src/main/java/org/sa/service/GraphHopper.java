package org.sa.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.Snap;
import org.sa.DTO.PointDTO;
import org.sa.config.Props;

import java.util.ArrayList;
import java.util.List;

public class GraphHopper {
  public com.graphhopper.GraphHopper hopper;

  public GraphHopper(String graphHopperProfileFootShortest) {
    hopper = new com.graphhopper.GraphHopper()
        .setOSMFile("src/main/java/org/sa/map_data/lithuania-250930.osm.pbf")
        .setGraphHopperLocation(Props.CACHE_FOLDER_NAME) //for new map data, please change this name, to build new chache
        .setProfiles(
            new Profile(graphHopperProfileFootShortest)
                .setVehicle("foot")
                .setWeighting("shortest")
        )
        .importOrLoad();
  }

  public List<PointDTO> snapPointsOnRoadGrid(List<PointDTO> perfectCircle) {
    List<PointDTO> snapped = new ArrayList<>();
    for (PointDTO p : perfectCircle) {
      Snap snap = hopper.getLocationIndex().findClosest(p.latitude, p.longitude, EdgeFilter.ALL_EDGES);
      if (snap.isValid()) snapped.add(new PointDTO(snap.getSnappedPoint().lat, snap.getSnappedPoint().lon));
      else {
        Graph graph = hopper.getBaseGraph();
        NodeAccess nodeAccess = graph.getNodeAccess();
        int nodes = graph.getNodes();
        double bestDist = Double.MAX_VALUE;
        double bestLat = p.latitude, bestLon = p.longitude;

        for (int i = 0; i < nodes; i++) {
          double lat = nodeAccess.getLat(i), lon = nodeAccess.getLon(i);
          double dist = GeoUtils.measureCircleDistanceHaversine(p.latitude, p.longitude, lat, lon);
          if (dist < bestDist) {
            bestDist = dist;
            bestLat = lat; bestLon = lon;
          }
        }

        // only snap if within 50 km (50000 m)
        if (bestDist <= 50000) snapped.add(new PointDTO(bestLat, bestLon));
      }
    }
    return snapped;
  }


  public List<PointDTO> connectSnappedPointsWithRoutesAndClose(List<PointDTO> snappedPoints, String graphHopperProfile) {
    List<PointDTO> routedPoints = new ArrayList<>();
    for (int i = 0; i < snappedPoints.size() - 1; i++) {
      PointDTO from = snappedPoints.get(i);
      PointDTO to = snappedPoints.get(i + 1);

      GHRequest req = new GHRequest(from.latitude, from.longitude, to.latitude, to.longitude).setProfile(graphHopperProfile);
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
    GHRequest req = new GHRequest(last.latitude, last.longitude, first.latitude, first.longitude).setProfile(graphHopperProfile);
    GHResponse rsp = hopper.route(req);
    if (!rsp.hasErrors()) {
      ResponsePath path = rsp.getBest();
      path.getPoints().forEach(p -> routedPoints.add(new PointDTO(p.lat, p.lon)));
    }

    return routedPoints;
  }

  public List<PointDTO> connectSnappedPointsWithRoutesNoClosing(List<PointDTO> snappedPoints, String graphHopperProfile) {
    List<PointDTO> routedPoints = new ArrayList<>();
    for (int i = 0; i < snappedPoints.size() - 1; i++) {
      PointDTO from = snappedPoints.get(i);
      PointDTO to = snappedPoints.get(i + 1);

      GHRequest req = new GHRequest(from.latitude, from.longitude, to.latitude, to.longitude).setProfile(graphHopperProfile);
      GHResponse rsp = hopper.route(req);

      if (!rsp.hasErrors()) {
        ResponsePath path = rsp.getBest();
        path.getPoints().forEach(p -> routedPoints.add(new PointDTO(p.lat, p.lon)));
      } else {
        routedPoints.add(from);
        routedPoints.add(to);
      }
    }

    return routedPoints;
  }
}
