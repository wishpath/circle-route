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

  @Override
  public String toString() {
    return
      "routeLength=" + "\u001B[31m" + routeLength + "\u001B[0m" +
      ", routeAreaKm=" + "\u001B[34m" + routeAreaKm + "\u001B[0m" +
      ", routeAreaPerKm=" + "\u001B[38;5;208m" + routeAreaPerKm + "\u001B[0m" +
      ", efficiencyPercent=" + "\u001B[32m" + efficiencyPercent + "\u001B[0m";
  }
}
