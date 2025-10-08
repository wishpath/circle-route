package org.sa.DTO;

public class EfficiencyDTO {
  public double routeLength;
  public double routeAreaKm;
  public double routeAreaPerKm;
  public int efficiencyPercent;

  public EfficiencyDTO(double routeLength, double routeAreaKm, double routeAreaPerKm, int efficiencyPercent) {
    this.routeLength = routeLength;
    this.routeAreaKm = routeAreaKm;
    this.routeAreaPerKm = routeAreaPerKm;
    this.efficiencyPercent = efficiencyPercent;
  }
}
