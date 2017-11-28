<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:r="http://www.rsuitecms.com/rsuite/ns/metadata"
  exclude-result-prefixes="xs r"
  version="2.0">
  
  <!-- RSuite content assembly to DITA map.
    
    Transforms a content assembly and its children into
    a DITA map that references each of the MOs
    in the content assembly.
    
    This shell XSL serves to simply set the public
    and system ID for the map to be generated.
    
    Copy this shell and modify it to generate maps with
    specific document type public IDs. 
  -->
  
    
  <xsl:import href="canode2mapImpl.xsl"/>
  
  <xsl:output
    doctype-public="urn:pubid:astd.com/doctypes/dita/map"
    doctype-system="map.dtd"
    indent="yes"
    method="xml"
  />
  
</xsl:stylesheet>
