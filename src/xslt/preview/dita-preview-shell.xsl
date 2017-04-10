<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      exclude-result-prefixes="xs"
      version="2.0">
      
      <!-- ====================================================
      
          ASTD-specific shell for RSuite HTML preview.
          
          This transform is intended to be the transform configured
          as the DTD and schema HTML preview transforms for all
          DITA document types. It includes the base preview
          implementation provided by the DITA For Publishers
          RSuite plugin as well as Eduneering-specific overrides
          and extensions provided by the Eduneering RSuite
          plugin.
          
          Copyright (c) 2009 ASTD
          ===================================================== -->
  
  <xsl:import href="rsuite:/res/plugin/astd/xslt/preview/dita-previewExtensions.xsl"/>
  <xsl:import href="rsuite:/res/plugin/dita4publishers/xslt/preview/dita-previewImpl.xsl"/>

  <xsl:param name="rsuite.sessionkey" as="xs:string" select="'unset'"/>
  <xsl:param name="rsuite.serverurl" as="xs:string" select="'urn:unset:/dev/null'"/>
  

</xsl:stylesheet>
