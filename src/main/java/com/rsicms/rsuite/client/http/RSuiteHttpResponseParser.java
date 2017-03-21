package com.rsicms.rsuite.client.http;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.RSuiteException;


public class RSuiteHttpResponseParser {

  private static XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

  public static String parseResponse(String response, String elementName) throws Exception {

    try (StringReader stringReader = new StringReader(response)) {

      XMLEventReader reader = xmlInputFactory.createXMLEventReader(stringReader);

      String rootElement = null;
      String currentElement = null;

      Map<String, String> elementValues = new HashMap<>();
      elementValues.put("message", null);
      elementValues.put(elementName, null);

      Set<String> elementNames = elementValues.keySet();

      String elementValue = "";

      boolean isError = true;

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();

        if (event.isStartElement()) {
          StartElement element = event.asStartElement();
          currentElement = element.getName().getLocalPart();

          if (rootElement == null && !"error".equalsIgnoreCase(currentElement)) {
            isError = false;
          }

          if (rootElement == null) {
            rootElement = currentElement;
          }

          if ("faultType".equalsIgnoreCase(currentElement)) {
            isError = true;
          }
        }

        if (event.isCharacters() && elementNames.contains(currentElement)) {
          elementValue += event.asCharacters().getData();
        }

        if (event.isEndElement()) {
          EndElement endElement = event.asEndElement();
          String endElementName = endElement.getName().getLocalPart();
          if (elementNames.contains(endElementName)) {
            elementValues.put(endElementName, elementValue);
            break;
          }
        }
      }

      if (isError) {
        throw new Exception(elementValues.get("message"));
      }

      return elementValues.get(elementName);

    }
  }

  /**
   * Get the error message out of the provided response, if there is one.
   * 
   * @param docBuilder
   * @param response
   * @return Blank when there isn't an error; else an error.
   * @throws IOException
   * @throws SAXException
   * @throws RSuiteException
   * @throws XPathExpressionException
   * @throws XPathFactoryConfigurationException
   */
  public static String getError(DocumentBuilder docBuilder, String response)
      throws IOException, SAXException, XPathExpressionException,
      XPathFactoryConfigurationException {

    // XMLEventReader was taking too long (over a minute) with the link integrity report test.

    XPathFactory xpf = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI,
        "net.sf.saxon.xpath.XPathFactoryImpl", ClassLoader.getSystemClassLoader());

    String msg = xpf.newXPath().evaluate(
        "/RestResult/actions/map/arguments[type = 'ERROR']/message", docBuilder.parse(IOUtils
            .toInputStream(response, "UTF-8")));

    return msg;

  }
}
