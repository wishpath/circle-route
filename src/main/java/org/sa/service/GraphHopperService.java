package org.sa.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.shapes.GHPoint;
import org.sa.PointDTO;
import org.sa.config.Props;

import java.util.ArrayList;
import java.util.List;

public class GraphHopperService {
  private static GraphHopper hopper;

  static {
    hopper = new GraphHopper()
        .setOSMFile("src/main/java/org/sa/map-data/lithuania-250930.osm.pbf")
        .setGraphHopperLocation(Props.CACHE_FOLDER_NAME) //for new map data, please change this name, to build new chache
        .setProfiles(
            new Profile(Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST).setVehicle("foot").setWeighting("shortest"),
            new Profile(Props.GRASSHOPPER_PROFILE2_BIKE_SHORTEST).setVehicle("bike").setWeighting("shortest"),
            new Profile("motorbike_shortest").setVehicle("motorcycle").setWeighting("shortest"),
            new Profile("car_shortest").setVehicle("car").setWeighting("shortest")
        )
        .importOrLoad();
  }

  public List<PointDTO> snapPointsOnRoadGrid(List<PointDTO> perfectCircle, String grassHopperProfile) {
    List<PointDTO> snapped = new ArrayList<>();
    for (PointDTO p : perfectCircle) {
      GHRequest req = new GHRequest(p.latitude, p.longitude, p.latitude, p.longitude).setProfile(grassHopperProfile);
      GHResponse rsp = hopper.route(req);

      if (!rsp.hasErrors()) {
        ResponsePath path = rsp.getBest();
        GHPoint snappedPoint = path.getPoints().get(0);
        snapped.add(new PointDTO(snappedPoint.lat, snappedPoint.lon));
      } else snapped.add(p);
    }
    return snapped;
  }

  public List<PointDTO> connectSnappedPointsWithRoutes(List<PointDTO> snappedPoints, String grassHopperProfile) {
    List<PointDTO> routedPoints = new ArrayList<>();
    for (int i = 0; i < snappedPoints.size() - 1; i++) {
      PointDTO from = snappedPoints.get(i);
      PointDTO to = snappedPoints.get(i + 1);

      GHRequest req = new GHRequest(from.latitude, from.longitude, to.latitude, to.longitude).setProfile(grassHopperProfile);
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
    GHRequest req = new GHRequest(last.latitude, last.longitude, first.latitude, first.longitude).setProfile(Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST);
    GHResponse rsp = hopper.route(req);
    if (!rsp.hasErrors()) {
      ResponsePath path = rsp.getBest();
      path.getPoints().forEach(p -> routedPoints.add(new PointDTO(p.lat, p.lon)));
    }

    return routedPoints;
  }
}
