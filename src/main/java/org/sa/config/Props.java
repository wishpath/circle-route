package org.sa.config;

import org.sa.PointDTO;

public class Props {
  public static String DATA_NAME = "ROKISKIS";

  public static final PointDTO KAUNAS_CENTER = new PointDTO(54.8985, 23.9036);
  public static final PointDTO ROKISKIS_CENTER = new PointDTO(55.9474, 25.5948);
  public static final PointDTO CIRCLE_CENTER = ROKISKIS_CENTER;

  public static String KAUNAS_MAP_DATA_PATH = "src/main/java/org/sa/map-data/planet_22.965,54.513_25.01,55.257.osm";
  public static String ROKISKIS_MAP_DATA_PATH = "src/main/java/org/sa/map-data/rokiskis_25.314_55.724_2c97fadb.osm";
  public static String ROKISKIS_MAP_DATA_PATH_FREE = "src/main/java/org/sa/map-data/" + DATA_NAME + "_free.osm";

  public static String MAP_DATA_PATH = ROKISKIS_MAP_DATA_PATH_FREE; //one in actual use

  //cache folder name
  public static String CACHE_FOLDER_NAME = Props.DATA_NAME + "-graph-cache"; //don't change since it is defined in gitignore

  //routing preferences
  public static String GRASSHOPPER_PROFILE = "free"; //can be "foot" | delete cache when changed

  //output dir
  public static String GPX_OUTPUT_DIR = "src/main/java/org/sa/output-gpx";
}
