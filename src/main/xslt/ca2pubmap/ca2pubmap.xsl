<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:RSUITE="http://www.reallysi.com"
  xmlns:rsf="http://www.reallysi.com/xslt/functions"
  xmlns:df="http://dita2indesign.org/dita/functions"
  exclude-result-prefixes="xs RSUITE rsf df"
  version="2.0">
  <!-- RSuite content assembly to DITA publication map.
    
       Transforms a content assembly and its children into
       a DITA map that references each of the MOs
       in the content assembly.
       
  -->
  
  
  <xsl:import href="rsuite:/res/plugin/rsuite-dita-support/canode2map/canode2mapImpl.xsl"/>
  
  <!-- Set this when generating output outside of RSuite so
       references will resolve.
    -->
  <xsl:param name="rsuiteServerUrl" select="'rsuite:/res/'" as="xs:string"/>
  
  <xsl:output
    method="xml"
    indent="yes"
    doctype-public="urn:pubid:astd.com/doctypes/dita/bookpub"
    doctype-system="bookpub"
  />
  
  <xsl:template match="/RSUITE:CANODE | /RSUITE:CONTENT-ASSEMBLY" priority="15">
    <bookpub>
      <pubtitle>
        <mainpubtitle><xsl:value-of select="@RSUITE:Name"/></mainpubtitle>
      </pubtitle>
      <pubmeta>
        <pubid>
          <isbn>978-1-56286-nnn-n</isbn>
        </pubid>
        <pubrights>
          <copyrfirst>
            <year>2010</year>
          </copyrfirst>
          <pubowner>
            <organization>The American Society for Training &amp; Development</organization>
          </pubowner>
        </pubrights>        
        <data name="source-ca-id" value="{rsf:getMoId(.)}"/>
      </pubmeta>
      <keydefs/>
      <astd-book>
        <frontmatter>
          <toc>
            <topicmeta>
              <navtitle>Contents</navtitle>
            </topicmeta>
          </toc>
        </frontmatter>
        <pubbody>
          <xsl:apply-templates/>        
        </pubbody>
      </astd-book>
    </bookpub>    
  </xsl:template>
  
  <xsl:template match="/*/RSUITE:CANODE" priority="10">
    <chapter>
      <topicmeta>
        <navtitle><xsl:value-of select="@RSUITE:Name"/></navtitle>
      </topicmeta>
      <xsl:apply-templates/>
    </chapter>
  </xsl:template>
  
  <xsl:template match="RSUITE:CANODE" priority="5">
    <subsection>
      <topicmeta>
        <navtitle><xsl:value-of select="@RSUITE:Name"/></navtitle>
      </topicmeta>
      <xsl:apply-templates/>
    </subsection>
  </xsl:template>
  
  <xsl:template match="/*/RSUITE:*[RSUITE:METADATA/RSUITE:SYSTEM/RSUITE:SOURCEID]" priority="10">    
    <!-- Must be an MO reference -->
    <xsl:call-template name="constructTopicrefToTopic">
      <xsl:with-param name="topicrefTagname" select="'chapter'"/>
    </xsl:call-template>  
  </xsl:template>
  
  
</xsl:stylesheet>
