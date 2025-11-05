package org.sa.APPS;

import org.locationtech.jts.geom.Polygon;
import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.GeoUtils;
import org.sa.service.TownTool;
import org.sa.service.TraverseService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * scans lithuania for the best circle routes
 * */
public class LithuaniaTraverser {

  TraverseService service = new TraverseService();

  //extream coordinates of Lithuania - most northern, southern, eastern and western points
  private static final double LITHUANIA_MIN_LAT = 53.88; // y, vertical
  private static final double LITHUANIA_MAX_LAT = 56.45; // y, vertical
  private static final double LITHUANIA_MIN_LON = 20.93; // x, horizontal
  private static final double LITHUANIA_MAX_LON = 26.84; // x, horizontal

  //approximate how much coordinate change per 1km travel
  private static final double LAT_CHANGE_PER_KM = 1000.0 / 111_320.0; // ≈ 0.00898 degrees ≈ 1 km
  private static final double LON_CHANGE_PER_KM = 1000.0 / (111_320.0 * Math.cos(Math.toRadians(55.0))); // ≈ 0.0156 degrees ≈ 1 km at ~55°N private static final double STEP_KOEF = 10.0;

  //defines grid points density. grid are fixed points that will get scanned.
  private static final double LT_GRID_STEP_KM = 1.0;
  
  //defines variety of circle sizes that will be checked
  private static final double CIRCLE_LENGTH_MIN = 50.0;
  private static final double CIRCLE_LENGTH_MAX = 50.0;
  private static final double CIRCLE_LENGTH_STEP = 5.0;

  public void traverse() {
    long totalInstances = 0;
    long lithuaniaInstances = 0;
    long okInstances = 0; //good efficiency route counter
    int maxEfficiency = -1;

    
    LocalDateTime start = LocalDateTime.now();
    outer:
    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {
      Polygon lithuaniaContourInwardOffset = service.getLithuaniaContourInwardOffset(perimeter); // so the route does not cross the border zone
      
      for (double latitude = LITHUANIA_MIN_LAT; latitude <= LITHUANIA_MAX_LAT; latitude += LT_GRID_STEP_KM * LAT_CHANGE_PER_KM) {
        for (double longitude = LITHUANIA_MIN_LON; longitude <= LITHUANIA_MAX_LON; longitude += LT_GRID_STEP_KM * LON_CHANGE_PER_KM) {
          totalInstances++;

          // filter out non Lithuanian points
          if (!GeoUtils.isWithinPolygon(lithuaniaContourInwardOffset, latitude, longitude)) continue;

          lithuaniaInstances++;
          List<PointDTO> noLoopRoutedPoints = service.produceRoute(new PointDTO(latitude, longitude), perimeter);

          // filter out not efficient routes
          EfficiencyDTO efficiency = service.efficiencyService.getRouteEfficiency(noLoopRoutedPoints);
          if (efficiency.efficiencyPercent < 67) continue;

          okInstances++;
          maxEfficiency = Math.max(maxEfficiency, efficiency.efficiencyPercent);

          //name file by city name
          String closestTown = TownTool.getRouteClosestTownName(noLoopRoutedPoints);
          System.out.println(closestTown + " " +  efficiency);
          service.gpxOutput.outputPointsAsGPX(noLoopRoutedPoints,closestTown + okInstances + ".gpx");

          //print progress pseudo percentage
          int lithuaniaTotalInstances = 47456;
          if (lithuaniaInstances % 500 == 0)
            System.out.println(((lithuaniaInstances * 100) / lithuaniaTotalInstances) + "%");
        }
      }
    }

    //end result console info
    System.out.println(
        "Total points: " + totalInstances +
        ", inside Lithuania: " + lithuaniaInstances +
        ", duration: " + (int) java.time.Duration.between(start, LocalDateTime.now()).toSeconds() + " seconds, " +
        "OK routes: " + okInstances + " , " +
        "max efficiency: " + maxEfficiency);
  }
}
