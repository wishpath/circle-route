package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.EfficiencyService;
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
  private EfficiencyService efficiencyService = new EfficiencyService();

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

  public static final PointDTO KURSENAI_CENTER     = new PointDTO(55.9850, 22.9361);
  public static final PointDTO BIRZAI_CENTER       = new PointDTO(56.2000, 24.7500);
  public static final PointDTO RASEINIAI_CENTER    = new PointDTO(55.3833, 23.1167);
  public static final PointDTO JURBARKAS_CENTER    = new PointDTO(55.0833, 22.7667);
  public static final PointDTO ELEKTRENAI_CENTER   = new PointDTO(54.7833, 24.6667);
  public static final PointDTO ANYKSCIAI_CENTER    = new PointDTO(55.5167, 25.1000);
  public static final PointDTO DRUSKININKAI_CENTER = new PointDTO(54.0167, 23.9667);
  public static final PointDTO VILKAVISKIS_CENTER  = new PointDTO(54.6500, 23.0333);
  public static final PointDTO ZARASAI_CENTER      = new PointDTO(55.7333, 26.2500);
  public static final PointDTO IGNALINA_CENTER     = new PointDTO(55.3500, 26.1667);
  public static final PointDTO MOLETAI_CENTER      = new PointDTO(55.2333, 25.4167);
  public static final PointDTO SVENCIONELIAI_CENTER= new PointDTO(55.1667, 26.0000);
  public static final PointDTO PABRADE_CENTER      = new PointDTO(54.9833, 25.7667);
  public static final PointDTO SIRVINTOS_CENTER    = new PointDTO(55.0500, 24.9500);
  public static final PointDTO NEMENCINE_CENTER    = new PointDTO(54.8500, 25.4833);
  public static final PointDTO KUPISKIS_CENTER     = new PointDTO(55.8333, 24.9833);
  public static final PointDTO VABALNINKAS_CENTER  = new PointDTO(55.9800, 24.7500);
  public static final PointDTO PAKRUOJIS_CENTER    = new PointDTO(55.9667, 23.8667);
  public static final PointDTO KREKENAVA_CENTER    = new PointDTO(55.5167, 24.0833);
  public static final PointDTO VARENA_CENTER       = new PointDTO(54.2167, 24.5667);
  public static final PointDTO SAKIAI_CENTER       = new PointDTO(54.9500, 23.0500);
  public static final PointDTO GARGZDAI_CENTER     = new PointDTO(55.7100, 21.4000);
  public static final PointDTO SALANTAI_CENTER     = new PointDTO(56.0667, 21.5667);
  public static final PointDTO SKUODAS_CENTER      = new PointDTO(56.2667, 21.5333);
  public static final PointDTO VILKIJA_CENTER      = new PointDTO(55.0500, 23.6333);
  public static final PointDTO ARIOGALA_CENTER     = new PointDTO(55.2667, 23.4667);
  public static final PointDTO KAISIADORYS_CENTER  = new PointDTO(54.8667, 24.4667);
  public static final PointDTO PRIENAI_CENTER      = new PointDTO(54.6333, 23.9500);
  public static final PointDTO KAZLU_RUDA_CENTER   = new PointDTO(54.7667, 23.5000);
  public static final PointDTO JONISKIS_CENTER = new PointDTO(56.2389, 23.6139);


  public static final Map<String, PointDTO> city_townCenter = Map.ofEntries(
//      Map.entry("vilnius", VILNIUS_CENTER),
//      Map.entry("kaunas", KAUNAS_CENTER),
//      Map.entry("klaipeda", KLAIPEDA_CENTER),
//      Map.entry("siauliai", SIAULIAI_CENTER),
//      Map.entry("panevezys", PANEVEZYS_CENTER),
//      Map.entry("alytus", ALYTUS_CENTER),
//      Map.entry("marijampole", MARIJAMPOLE_CENTER),
//      Map.entry("mazeikiai", MAZEIKIAI_CENTER),
//      Map.entry("utena", UTENA_CENTER),
//      Map.entry("jonava", JONAVA_CENTER),
//      Map.entry("kedainiai", KEDAINIAI_CENTER),
//      Map.entry("telsiai", TELSIAI_CENTER),
//      Map.entry("taurage", TAURAGE_CENTER),
//      Map.entry("ukmerge", UKMERGE_CENTER),
//      Map.entry("visaginas", VISAGINAS_CENTER),
      Map.entry("palanga", PALANGA_CENTER)//,
//      Map.entry("plunge", PLUNGE_CENTER),
//      Map.entry("kretinga", KRETINGA_CENTER),
//      Map.entry("silute", SILUTE_CENTER),
//      Map.entry("rokiskis", ROKISKIS_CENTER),
//      Map.entry("kursenai", KURSENAI_CENTER),
//      Map.entry("birzai", BIRZAI_CENTER),
//      Map.entry("raseiniai", RASEINIAI_CENTER),
//      Map.entry("jurbarkas", JURBARKAS_CENTER),
//      Map.entry("elektrenai", ELEKTRENAI_CENTER),
//      Map.entry("anyksciai", ANYKSCIAI_CENTER),
//      Map.entry("druskininkai", DRUSKININKAI_CENTER),
//      Map.entry("vilkaviskis", VILKAVISKIS_CENTER),
//      Map.entry("zarasai", ZARASAI_CENTER),
//      Map.entry("ignalina", IGNALINA_CENTER),
//      Map.entry("moletai", MOLETAI_CENTER),
//      Map.entry("svencioneliai", SVENCIONELIAI_CENTER),
//      Map.entry("pabrade", PABRADE_CENTER),
//      Map.entry("sirvintos", SIRVINTOS_CENTER),
//      Map.entry("nemencine", NEMENCINE_CENTER),
//      Map.entry("kupiskis", KUPISKIS_CENTER),
//      Map.entry("vabalninkas", VABALNINKAS_CENTER),
//      Map.entry("pakruojis", PAKRUOJIS_CENTER),
//      Map.entry("krekenava", KREKENAVA_CENTER),
//      Map.entry("varena", VARENA_CENTER),
//      Map.entry("sakiai", SAKIAI_CENTER),
//      Map.entry("gargzdai", GARGZDAI_CENTER),
//      Map.entry("salantai", SALANTAI_CENTER),
//      Map.entry("skuodas", SKUODAS_CENTER),
//      Map.entry("vilkija", VILKIJA_CENTER),
//      Map.entry("ariogala", ARIOGALA_CENTER),
//      Map.entry("kaisiadorys", KAISIADORYS_CENTER),
//      Map.entry("prienai", PRIENAI_CENTER),
//      Map.entry("kazlu_ruda", KAZLU_RUDA_CENTER),
//      Map.entry("joniskis", JONISKIS_CENTER)
  );

  public void traverse() {
    LocalDateTime start = LocalDateTime.now();

    //Total instances: 150, duration: 31 seconds,  instances per second: 4
    //Total instances: 150, duration: 60 seconds, instances per second: 2
    outer:
    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {
      final double finalPerimeter = perimeter;
      System.out.println("Circle length: "  + perimeter);
      city_townCenter.forEach((townName, center) -> {
        List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, finalPerimeter, MAX_DISTANCE_BETWEEN_POINTS_KM); // +0s
        List<PointDTO> snappedCircle = graphHopper.snapPointsOnRoadGrid(perfectCircle, GRASSHOPPER_PROFILE_FOOT_SHORTEST);

        //List<PointDTO> routedClosedCircle = graphHopper.connectSnappedPointsWithRoutes(snappedCircle, GRASSHOPPER_PROFILE_FOOT_SHORTEST);
        //List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routedClosedCircle); // doubled method from LithuaniaTraverse

        EfficiencyDTO efficiencyDTO = efficiencyService.getRouteEfficiency(snappedCircle);
        gpxOutput.outputGPX(snappedCircle, efficiencyDTO.efficiencyPercent + "_"+ (int) efficiencyDTO.routeLength + "_" + townName);
      });
    }


    int instances = (city_townCenter.size() * sizeInstances);
    int timeSeconds = (int) java.time.Duration.between(start, LocalDateTime.now()).toSeconds();
    System.out.println(
        "Total instances: " + instances +
            ", duration: " + timeSeconds + " seconds, " +
            "instances per second: " + ((timeSeconds != 0) ? (instances / timeSeconds) : "ALL_IN_ZERO_SECONDS")
      );
  }

  //doubled method, care!
  private List<PointDTO> removeLoopsByLoopingTheSameActions(List<PointDTO> routePoints) {
    double indicatorOfLoop_maxDistance_loopStart_loopFinish_km = 0.2;
    List<PointDTO> noLoops;
    List<PointDTO> noLoopsRouted;
    List<PointDTO> shifted = routePoints;

    for (int i = 0; i < 4; i++) {
      noLoops = routeGenerator.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
      noLoopsRouted = graphHopper.connectSnappedPointsWithRoutes(noLoops, GRASSHOPPER_PROFILE_FOOT_SHORTEST);
      shifted = routeGenerator.shiftABtoBA_andReverse(noLoopsRouted);
    }
    return shifted;
  }
}
