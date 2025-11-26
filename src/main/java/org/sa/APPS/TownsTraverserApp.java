package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.b_storage.TownData;
import org.sa.service.TraverseService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * testing and experimentation class
 * checks that a limited number of towns produces a limited number of routes
 * provides a quick test compared to lithuania traverser class
 * used for efficiency improvements
 * checks gpx track output on the map
 * sometimes used for visualizing town locations on the map
 */
public class TownsTraverserApp {
  TraverseService service = new TraverseService();

  //variety of circle lengths
  private static final double CIRCLE_LENGTH_MIN = 10.0;
  private static final double CIRCLE_LENGTH_MAX = 10.0;
  private static final double CIRCLE_LENGTH_STEP = 5.0;
  private static final int CIRCLE_COUNT_PER_TOWN = ((int)((CIRCLE_LENGTH_MAX - CIRCLE_LENGTH_MIN) / CIRCLE_LENGTH_STEP)) + 1;


  public void traverse() {
    LocalDateTime start = LocalDateTime.now();

    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {
      final double finalPerimeter = perimeter;
      TownData.townName_townCenterPoint.forEach((townName, center) -> {
        List<PointDTO> noLoopRoutedPoints = service.produceRoute(center, finalPerimeter);
        EfficiencyDTO efficiencyDTO = service.efficiencyService.getRouteEfficiency(noLoopRoutedPoints);
        service.gpxOutput.outputPointsAsGPX(noLoopRoutedPoints, efficiencyDTO.efficiencyPercent + "_"+ (int) efficiencyDTO.routeLength + "_" + townName + ".gpx");
      });
    }

    //print stats
    int totalInstances = (TownData.townName_townCenterPoint.size() * CIRCLE_COUNT_PER_TOWN);
    int timeSeconds = (int) Duration.between(start, LocalDateTime.now()).toSeconds();
    System.out.println(
        "Total instances: " + totalInstances +
            ", duration: " + timeSeconds + " seconds, " +
            "instances per second: " + ((timeSeconds != 0) ? (totalInstances / timeSeconds) : "ALL_IN_ZERO_SECONDS")
      );
  }
}
