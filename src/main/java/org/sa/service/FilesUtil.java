package org.sa.service;

import java.io.File;

public class FilesUtil {
  public static File[] listGpxFiles(String folderWithGpxFilesToEvaluate) {
    File folder = new File(folderWithGpxFilesToEvaluate);
    if (!folder.exists() || !folder.isDirectory()) throw new IllegalStateException("Folder not found: " + folderWithGpxFilesToEvaluate);

    File[] gpxFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".gpx"));
    if (gpxFiles == null || gpxFiles.length == 0) {
      System.err.println("No GPX files found in folder: " + folderWithGpxFilesToEvaluate);
    }
    return gpxFiles;
  }
}
