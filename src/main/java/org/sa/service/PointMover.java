package org.sa.service;

import org.sa.DTO.PointDTO;

public class PointMover {
  public PointDTO movePoint(PointDTO point, double northKm, double eastKm) {
    double earthKmPerDegree = 111.32;

    double latOffset = northKm / earthKmPerDegree;
    double lonOffset = eastKm / (earthKmPerDegree * Math.cos(Math.toRadians(point.latitude)));

    double newLat = point.latitude + latOffset;
    double newLon = point.longitude + lonOffset; // east is positive

    return new PointDTO(newLat, newLon);
  }
}
