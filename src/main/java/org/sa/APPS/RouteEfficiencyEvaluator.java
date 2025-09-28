package org.sa.APPS;

import org.sa.service.GeoUtils;

public class RouteEfficiencyEvaluator {

  private static final double LENGTH_OF_ROUTE_KM = 30.98;  // km
  private static final double AREA_OF_ROUTE_SQ_KM = 48.55;     // sq.km

  // ANSI escape codes
  private static final String BLUE = "\u001B[34m";
  private static final String RED = "\u001B[31m";
  private static final String RESET = "\u001B[0m";

  public static void main(String[] args) {
    double routeLength = Math.round(LENGTH_OF_ROUTE_KM * 100.0) / 100.0;
    double routeAreaKm = Math.round(AREA_OF_ROUTE_SQ_KM * 100.0) / 100.0;
    double idealAreaKm = Math.round(GeoUtils.getCircleAreaByLength(routeLength) * 100.0) / 100.0;
    double perfectSquareAreaKm = Math.round((routeLength * routeLength / 16) * 100.0) / 100.0;
    double routeAreaPerKm = Math.round((routeAreaKm / routeLength) * 100.0) / 100.0;
    double ideaAreaPerKm = Math.round((idealAreaKm / routeLength) * 100.0) / 100.0;
    int efficiencyPercent = (int)(routeAreaKm / idealAreaKm * 100);
    int perfectSquareEfficiencyPercent = (int)(perfectSquareAreaKm / idealAreaKm * 100);

    System.out.println(
        BLUE + "Constant Route Evaluation" + RESET +
            "\nLength: " + routeLength + " km" +
            "\nArea: " + routeAreaKm + " sq.km" +
            "\nArea per 1km: " + routeAreaPerKm + " sq.km/1km" +
            "\nIdeal area for this length: " + idealAreaKm + " sq.km" +
            "\nIdeal area per 1km: " + ideaAreaPerKm + " sq.km/1km" +
            "\nPerfect square efficiency: " + perfectSquareEfficiencyPercent + "%" +
            "\n" + RED + "Efficiency for this length: " + efficiencyPercent + "%" + RESET + "\n"
    );
  }
}

