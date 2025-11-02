package org.sa.service;

import org.sa.DTO.PointDTO;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GpxFileToPointsParser {
  public static List<PointDTO> parseFromGpxFileToPoints(File gpxFile) {
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
}
