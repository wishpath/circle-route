package org.sa.APPS;

import org.sa.DTO.PointDTO;
import org.sa.service.GpxOutput;
import org.sa.service.GraphHopper;
import org.sa.service.RouteGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

//check limited amount of routes, improve efficiency for the real scan and also check output on the map if there are some problems!
public class CitiesTraverser {
  //keeping grass hopper profile here in case it will be needed to rotate them here
  public static String GRASSHOPPER_PROFILE_FOOT_SHORTEST = "foot_shortest"; // delete cache when changed
  private RouteGenerator routeGenerator = new RouteGenerator();
  private GraphHopper graphHopper = new GraphHopper(GRASSHOPPER_PROFILE_FOOT_SHORTEST);
  private GpxOutput gpxOutput = new GpxOutput();

  private static final double CIRCLE_LENGTH_MIN = 20.0;
  private static final double CIRCLE_LENGTH_MAX = 30.0;
  private static final double CIRCLE_LENGTH_STEP = 5.0;
  private static final int sizeInstances = ((int)((CIRCLE_LENGTH_MAX - CIRCLE_LENGTH_MIN) / CIRCLE_LENGTH_STEP)) + 1;

  private static final int MAX_DISTANCE_BETWEEN_POINTS_KM = 2; // distance between ideal circle points

  public static final PointDTO VILNIUS_CENTER   = new PointDTO(54.6872, 25.2797);
  public static final PointDTO KAUNAS_CENTER    = new PointDTO(54.8985, 23.9036);
  public static final PointDTO KLAIPEDA_CENTER  = new PointDTO(55.7033, 21.1443);
  public static final PointDTO SIAULIAI_CENTER  = new PointDTO(55.9333, 23.3167);
  public static final PointDTO PANEVEZYS_CENTER = new PointDTO(55.7333, 24.3500);
  public static final PointDTO ALYTUS_CENTER    = new PointDTO(54.4014, 24.0491);
  public static final PointDTO MARIJAMPOLE_CENTER = new PointDTO(54.5599, 23.3541);
  public static final PointDTO MAZEIKIAI_CENTER   = new PointDTO(56.3167, 22.3333);
  public static final PointDTO UTENA_CENTER       = new PointDTO(55.5000, 25.6000);
  public static final PointDTO JONAVA_CENTER      = new PointDTO(55.0833, 24.2833);
  public static final PointDTO KEDAINIAI_CENTER   = new PointDTO(55.2883, 23.9744);
  public static final PointDTO TELSIAI_CENTER     = new PointDTO(55.9833, 22.2500);
  public static final PointDTO TAURAGE_CENTER     = new PointDTO(55.2522, 22.2897);
  public static final PointDTO UKMERGE_CENTER     = new PointDTO(55.2500, 24.7500);
  public static final PointDTO VISAGINAS_CENTER   = new PointDTO(55.6000, 26.4333);
  public static final PointDTO PALANGA_CENTER     = new PointDTO(55.9167, 21.0667);
  public static final PointDTO PLUNGE_CENTER      = new PointDTO(55.9167, 21.8500);
  public static final PointDTO KRETINGA_CENTER    = new PointDTO(55.8833, 21.2500);
  public static final PointDTO SILUTE_CENTER      = new PointDTO(55.3500, 21.4833);
  public static final PointDTO ROKISKIS_CENTER    = new PointDTO(55.9474, 25.5948);

  public static final Map<String, PointDTO> city_townCenter = Map.ofEntries(
      Map.entry("vilnius", VILNIUS_CENTER),
      Map.entry("kaunas", KAUNAS_CENTER),
      Map.entry("klaipeda", KLAIPEDA_CENTER),
      Map.entry("siauliai", SIAULIAI_CENTER),
      Map.entry("panevezys", PANEVEZYS_CENTER),
      Map.entry("alytus", ALYTUS_CENTER),
      Map.entry("marijampole", MARIJAMPOLE_CENTER),
      Map.entry("mazeikiai", MAZEIKIAI_CENTER),
      Map.entry("utena", UTENA_CENTER),
      Map.entry("jonava", JONAVA_CENTER),
      Map.entry("kedainiai", KEDAINIAI_CENTER),
      Map.entry("telsiai", TELSIAI_CENTER),
      Map.entry("taurage", TAURAGE_CENTER),
      Map.entry("ukmerge", UKMERGE_CENTER),
      Map.entry("visaginas", VISAGINAS_CENTER),
      Map.entry("palanga", PALANGA_CENTER),
      Map.entry("plunge", PLUNGE_CENTER),
      Map.entry("kretinga", KRETINGA_CENTER),
      Map.entry("silute", SILUTE_CENTER),
      Map.entry("rokiskis", ROKISKIS_CENTER)
  );

  public void traverse() {
    LocalDateTime start = LocalDateTime.now();

    outer:
    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {
      final double finalPerimeter = perimeter;
      System.out.println("Circle length: "  + perimeter);
      city_townCenter.forEach((townName, center) -> {
        System.out.println("     " + townName);
        List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, finalPerimeter, MAX_DISTANCE_BETWEEN_POINTS_KM); // +0s
        List<PointDTO> snappedCircle = graphHopper.snapPointsOnRoadGrid(perfectCircle, GRASSHOPPER_PROFILE_FOOT_SHORTEST);
        List<PointDTO> routedClosedCircle = graphHopper.connectSnappedPointsWithRoutes(snappedCircle, GRASSHOPPER_PROFILE_FOOT_SHORTEST);
        List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routedClosedCircle); // doubled method from LithuaniaTraverse
      });
    }

    System.out.println(
        "Total points: " + (city_townCenter.size() * sizeInstances) +
            ", duration: " + (int) java.time.Duration.between(start, LocalDateTime.now()).toSeconds() + " seconds");
  }

  //doubled method, care!
  private List<PointDTO> removeLoopsByLoopingTheSameActions(List<PointDTO> routePoints) {
    double indicatorOfLoop_maxDistance_loopStart_loopFinish_km = 0.2;
    List<PointDTO> noLoops;
    List<PointDTO> noLoopsRouted;
    List<PointDTO> shifted = routePoints;

    for (int i = 0; i < 3; i++) {
      noLoops = routeGenerator.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
      noLoopsRouted = graphHopper.connectSnappedPointsWithRoutes(noLoops, GRASSHOPPER_PROFILE_FOOT_SHORTEST);
      shifted = routeGenerator.shiftABtoBA_andReverse(noLoopsRouted);
    }
    return shifted;
  }
}
