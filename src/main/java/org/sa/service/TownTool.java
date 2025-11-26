package org.sa.service;

import org.sa.DTO.PointDTO;
import org.sa.b_storage.TownData;

import java.util.Comparator;
import java.util.List;

public class TownTool {
  public static String getRouteClosestTownName(List<PointDTO> route) {
    return TownData.townName_townCenterPoint.entrySet().stream()
        .min(Comparator.comparingDouble(e -> GeoUtils.getDistanceBetweenLocations(e.getValue(), route.get(0))))
        .map(java.util.Map.Entry::getKey)
        .orElse("unknown");
  }
}
