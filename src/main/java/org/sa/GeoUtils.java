package org.sa;

public class GeoUtils {

  /** Returns distance in km between two points using Haversine formula */
  public static double haversine(PointDTO p1, PointDTO p2) {
    double earthRadiusKm = 6371.0;
    double dLat = Math.toRadians(p2.latitude - p1.latitude);
    double dLon = Math.toRadians(p2.longitude - p1.longitude);
    double lat1 = Math.toRadians(p1.latitude);
    double lat2 = Math.toRadians(p2.latitude);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
  }
}

