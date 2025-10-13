package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.EfficiencyService;
import org.sa.service.GpxOutput;
import org.sa.service.GpxParser;

import java.io.File;
import java.util.List;

public class GpxFilesInDirectoryEvaluatorApp {
  private static final String GPX_FOLDER = "src/main/java/org/sa/APPS/gpx_files_to_evaluate";
  //  private static final String GPX_FOLDER = "src/main/java/org/sa/output-gpx";


  public static void main(String[] args) {
    evaluateGPXRoutesInDirectory(GPX_FOLDER);
  }

  public static void evaluateGPXRoutesInDirectory(String path) {
    EfficiencyService efficiencyService = new EfficiencyService();

    File folder = new File(path);
    if (!folder.exists() || !folder.isDirectory()) {
      System.err.println("Folder not found: " + path);
      return;
    }

    File[] gpxFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".gpx"));
    if (gpxFiles == null || gpxFiles.length == 0) {
      System.err.println("No GPX files found in folder: " + path);
      return;
    }
    int counter = 1;
    for (File gpxFile : gpxFiles) {
      List<PointDTO> routePoints = GpxParser.parseGpxFile(gpxFile);
      if (routePoints.size() < 2) continue;
      //close route
      if (!routePoints.get(0).equals(routePoints.get(routePoints.size() - 1))) routePoints.add(routePoints.get(0));

      EfficiencyDTO eff = efficiencyService.getRouteEfficiency(routePoints);
      efficiencyService.printRouteEfficiency(eff, gpxFile.getName());
      new GpxOutput().outputGPXToDir(
          routePoints,
          eff.efficiencyPercent + "eff_" + (int)eff.routeLength + "km_" + gpxFile.getName() + "_" + (int)eff.routeAreaKm + "sqkm_" + counter++,
          "src/main/java/org/sa/APPS/renamed");
    }
  }
}
