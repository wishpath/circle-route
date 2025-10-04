package org.sa.APPS;

import org.sa.PointDTO;
import org.sa.service.GeoUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * GPXRouteEfficiencyEvaluator reads gpx files from a given directory.
 * - Considers first and last points of the route — connected.
 * - Uses GeoUtils to count length and area of each route in the folder.
 * - Prints result to console with colors.
 */
public class GPXRouteEfficiencyEvaluator {

  private static final String GPX_FOLDER = "src/main/java/org/sa/APPS/gpx_files_to_evaluate";
//  private static final String GPX_FOLDER = "src/main/java/org/sa/output-gpx";


  ---
  // ANSI escape codes
  private static final String BLUE = "\u001B[34m";
  private static final String RED = "\u001B[31m";
  private static final String RESET = "\u001B[0m";

  public static void main(String[] args) {
    evaluateGPXRoutesInDirectory(GPX_FOLDER);
  }

  public static void evaluateGPXRoutesInDirectory(String path) {
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

    for (File gpxFile : gpxFiles) {
      List<PointDTO> routePoints = parseGpxFile(gpxFile);
      if (routePoints.size() < 2) continue;

      if (!routePoints.get(0).equals(routePoints.get(routePoints.size() - 1))) routePoints.add(routePoints.get(0));

      double routeLength = round2(GeoUtils.autoCloseRouteAndGetLengthKm(routePoints));
      double routeAreaKm = round2(GeoUtils.getRouteAreaKm(routePoints));
      double idealAreaKm = round2(GeoUtils.getCircleAreaByLength(routeLength));
      double perfectSquareAreaKm = round2(routeLength * routeLength / 16);
      double routeAreaPerKm = round2(routeAreaKm / routeLength);
      double idealAreaPerKm = round2(idealAreaKm / routeLength);
      int efficiencyPercent = (int)(routeAreaKm / idealAreaKm * 100);
      int perfectSquareEfficiencyPercent = (int)(perfectSquareAreaKm / idealAreaKm * 100);


      if (efficiencyPercent < 52) continue;
      System.out.println(
          BLUE + "File: " + gpxFile.getName() + RESET +
              "\nLength: " + routeLength + " km" +
              "\nArea: " + routeAreaKm + " sq.km" +
              "\nArea per 1km: " + routeAreaPerKm + " sq.km/1km" +
              "\nIdeal area for this length: " + idealAreaKm + " sq.km" +
              "\nIdeal area per 1km: " + idealAreaPerKm + " sq.km/1km" +
              "\nPerfect square efficiency: " + perfectSquareEfficiencyPercent + "%" +
              "\n" + RED + "Efficiency for this length: " + efficiencyPercent + "%" + RESET + "\n"
      );
    }
  }

  private static List<PointDTO> parseGpxFile(File gpxFile) {
    List<PointDTO> points = new ArrayList<>();
    try {
      var factory = DocumentBuilderFactory.newInstance();
      var builder = factory.newDocumentBuilder();
      var doc = builder.parse(gpxFile);
      var trkptList = doc.getElementsByTagName("trkpt");

      for (int i = 0; i < trkptList.getLength(); i++) {
        var element = (org.w3c.dom.Element) trkptList.item(i);
        double lat = Double.parseDouble(element.getAttribute("lat"));
        double lon = Double.parseDouble(element.getAttribute("lon"));
        points.add(new PointDTO(lat, lon));
      }
    } catch (Exception e) {
      System.err.println("Failed to parse GPX file: " + gpxFile.getName() + " — " + e.getMessage());
    }
    return points;
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
