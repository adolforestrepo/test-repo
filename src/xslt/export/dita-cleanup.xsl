<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      xmlns:RSUITE="http://www.reallysi.com"
      xmlns:ditaarch="http://dita.oasis-open.org/architecture/2005/"
      xmlns:r="http://www.rsuitecms.com/rsuite/ns/metadata"
      exclude-result-prefixes="xs RSUITE ditaarch r"
      version="2.0">
  
  <!-- DITA cleanup export transform.
    
       - Removes RSUITE Metadata
       - Strips out class, domains, and DITAArchVersion attributes
       
  -->
  
  <xsl:include href="common-identity-transform.xsl"/>
  
  <!-- NOTE: RSUITE:* is RSuite 3.6 and earlier -->
  <xsl:template match="RSUITE:*" priority="10"/>
  
  <!-- NOTE: @r:rsuiteId is RSuite 3.7 and later -->
  <xsl:template match="@r:rsuiteId" priority="10"/>
  
  <xsl:template match="@class | @domains | ditaarch:DITAArchVersion"/>

</xsl:stylesheet>
