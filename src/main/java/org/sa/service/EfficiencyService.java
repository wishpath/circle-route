package org.sa.service;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;

import java.util.List;


public class EfficiencyService {
  public EfficiencyDTO getRouteEfficiency(List<PointDTO> routePoints) {
    double routeLength = round2(GeoUtils.autoCloseRouteAndGetLengthKm(routePoints));
    double routeAreaKm = round2(GeoUtils.getRouteAreaKm(routePoints));
    double routeAreaPerKm = round2(routeAreaKm / routeLength);
    double idealAreaKm = round2(GeoUtils.getCircleAreaByLength(routeLength)); // precalculate this and store to a map
    int efficiencyPercent = (int) (routeAreaKm / idealAreaKm * 100);
    return new EfficiencyDTO(routeLength, routeAreaKm, routeAreaPerKm, efficiencyPercent);
  }

  public void printRouteEfficiency(EfficiencyDTO efficiencyDTO, String routeName) {
    final String BLUE = "\u001B[34m";
    final String RED = "\u001B[31m";
    final String RESET = "\u001B[0m";
    System.out.println(
        BLUE + "Route name: " + routeName + RESET +
            "\nLength: " + efficiencyDTO.routeLength + " km" +
            "\nArea: " + efficiencyDTO.routeAreaKm + " sq.km" +
            "\nArea per 1km: " + efficiencyDTO.routeAreaPerKm + " sq.km/1km" +
            "\n" + RED + "Efficiency for this length: " + efficiencyDTO.efficiencyPercent + "%" + RESET + "\n"
    );
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
