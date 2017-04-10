<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:RSUITE="http://www.reallysi.com" 
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:df="http://dita2indesign.org/dita/functions"
  exclude-result-prefixes="RSUITE df xs"
  >
  
  <xsl:template match="*[df:class(., 'astd_section-d/chapter-highlights')]">
    <div class="{df:getHtmlClass(.)}"
       style="display: block;
       border: solid blue 1pt;
       padding: 6pt;
       background-color: #FCFC00;
       color: black;
       ">
      <h4>Chapter Highlights</h4>
      <xsl:apply-templates/>
    </div>
       
  </xsl:template>
 
</xsl:stylesheet>
