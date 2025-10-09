package org.sa.config;

import org.sa.DTO.PointDTO;

public class Props {
  public static String DATA_NAME = "LT";

  public static final PointDTO KAUNAS_CENTER = new PointDTO(54.8985, 23.9036);
  public static final PointDTO ROKISKIS_CENTER = new PointDTO(55.9474, 25.5948);
  public static final PointDTO CIRCLE_CENTER = ROKISKIS_CENTER;

  //cache folder name
  public static String CACHE_FOLDER_NAME = Props.DATA_NAME + "-graph-cache"; //don't change since it is defined in gitignore

  //output dir
  public static String GPX_OUTPUT_DIR = "src/main/java/org/sa/output-gpx";
}
