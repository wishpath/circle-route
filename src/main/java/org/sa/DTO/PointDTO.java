package org.sa.DTO;

import java.util.Objects;

public class PointDTO {
  public final double latitude; // y, vertical
  public final double longitude; // x, horizontal

  public PointDTO(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  @Override
  public boolean equals(Object o) {
    final double MAX_COORDINATE_DIFFERENCE_TOLERATED = 1e-9;
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PointDTO point = (PointDTO) o;
    return Math.abs(latitude - point.latitude) < MAX_COORDINATE_DIFFERENCE_TOLERATED &&
        Math.abs(longitude - point.longitude) < MAX_COORDINATE_DIFFERENCE_TOLERATED;
  }

  @Override
  public int hashCode() {
    return Objects.hash(latitude, longitude);
  }
}
