package org.sa.config;

import org.sa.PointDTO;
import java.util.Map;

public class CityCenters {

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
}
