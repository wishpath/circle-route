package org.sa.config;

import org.sa.PointDTO;

public class Props {
  public static String CIRCLE_AND_MAP_DATA_NAME = "ROKISKIS";
  public static final PointDTO KAUNAS_CENTER = new PointDTO(54.8985, 23.9036);
  public static final PointDTO ROKISKIS_CENTER = new PointDTO(55.9474, 25.5948);
  public static final PointDTO CIRCLE_CENTER = ROKISKIS_CENTER;

  public static String KAUNAS_MAP_DATA_PATH = "src/main/java/org/sa/map-data/planet_22.965,54.513_25.01,55.257.osm";
  public static String ROKISKIS_MAP_DATA_PATH = "src/main/java/org/sa/map-data/rokiskis_25.314_55.724_2c97fadb.osm";
  public static String MAP_DATA_PATH = ROKISKIS_MAP_DATA_PATH;

}
