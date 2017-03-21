package test.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DomUtils {

  public static Document convertStringToDocument(InputStream inputStream)
      throws SAXException, IOException, ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(
        inputStream));
  }

  // Credit to
  // http://www.journaldev.com/1237/java-convert-string-to-xml-document-and-xml-document-to-string
  public static Document convertStringToDocument(String xmlStr)
      throws SAXException, IOException, ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(
        new StringReader(xmlStr)));
  }


}
