package org.sa.APPS;

import javax.xml.stream.*;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * MapDataModifier ensures that all ways in OSM data are fully accessible:
 * - Removes restrictions such as access=no, foot=no, vehicle=no, bicycle=no.
 * - Sets foot=yes everywhere so all paths are walkable.
 * - Forces all ways to be bidirectional by adding oneway=no.
 *
 * This uses StAX streaming to efficiently handle large OSM files.
 */
public class MapDataModifier {

  public static void main(String[] args) {
    preprocessOsm();
  }

  public static void preprocessOsm() {
    try (FileInputStream fis = new FileInputStream("src/main/java/org/sa/map-data/lithuania-250930.osm.pbf");
         FileOutputStream fos = new FileOutputStream("src/main/java/org/sa/map-data/lithuania-250930_free.osm.pbf")) {

      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLEventReader reader = inputFactory.createXMLEventReader(fis);
      XMLEventWriter writer = outputFactory.createXMLEventWriter(fos);

      XMLEventFactory eventFactory = XMLEventFactory.newInstance();
      int wayCount = 0;

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();

        if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("way")) {
          // Start way element
          StartElement wayStart = event.asStartElement();
          writer.add(wayStart);

          boolean footTagExists = false;

          // Process inner tags of way
          while (reader.hasNext()) {
            XMLEvent innerEvent = reader.nextEvent();

            if (innerEvent.isStartElement() && innerEvent.asStartElement().getName().getLocalPart().equals("tag")) {
              String k = innerEvent.asStartElement().getAttributeByName(new javax.xml.namespace.QName("k")).getValue();
              String v = innerEvent.asStartElement().getAttributeByName(new javax.xml.namespace.QName("v")).getValue();

              // Remove restricted tags
              if (k.equals("oneway") || k.equals("access") || k.equals("vehicle") ||
                  (k.equals("foot") && v.equals("no")) ||
                  (k.equals("bicycle") && v.equals("no"))) {
                // skip this tag
                reader.nextEvent(); // consume the EndElement
                continue;
              }

              // Ensure foot=yes
              if (k.equals("foot")) footTagExists = true;

              // write original tag with foot=yes if foot
              if (k.equals("foot")) {
                writer.add(eventFactory.createStartElement("", null, "tag"));
                writer.add(eventFactory.createAttribute("k", "foot"));
                writer.add(eventFactory.createAttribute("v", "yes"));
                writer.add(eventFactory.createEndElement("", null, "tag"));
                reader.nextEvent(); // consume the EndElement
                continue;
              }

              writer.add(innerEvent);
            } else if (innerEvent.isEndElement() && innerEvent.asEndElement().getName().getLocalPart().equals("way")) {
              // Add foot=yes if missing
              if (!footTagExists) {
                writer.add(eventFactory.createStartElement("", null, "tag"));
                writer.add(eventFactory.createAttribute("k", "foot"));
                writer.add(eventFactory.createAttribute("v", "yes"));
                writer.add(eventFactory.createEndElement("", null, "tag"));
              }

              // Force bidirectional
              writer.add(eventFactory.createStartElement("", null, "tag"));
              writer.add(eventFactory.createAttribute("k", "oneway"));
              writer.add(eventFactory.createAttribute("v", "no"));
              writer.add(eventFactory.createEndElement("", null, "tag"));

              writer.add(innerEvent); // close way
              wayCount++;
              if (wayCount % 1000 == 0)
                System.out.printf("Processed ways: %d%n", wayCount);
              break;
            } else {
              writer.add(innerEvent);
            }
          }

        } else {
          writer.add(event);
        }
      }

      writer.flush();
      writer.close();
      reader.close();

      System.out.println("✅ OSM preprocessing complete");
      System.out.println("Total ways processed: " + wayCount);

    } catch (Exception e) {
      throw new RuntimeException("Failed to preprocess OSM file", e);
    }
  }
}
