<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      xmlns:ditaarch="http://dita.oasis-open.org/architecture/2005/"
      xmlns:r="http://www.rsuitecms.com/rsuite/ns/metadata"      
      exclude-result-prefixes="xs ditaarch r"
      version="2.0">

  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>
  
  <xsl:template match="*" priority="2">
    <xsl:element name="{name(.)}">
      <xsl:apply-templates select="@* | node()"/>
    </xsl:element>
  </xsl:template>
  
  <xsl:template match="*[namespace-uri() != '']" priority="10">
    <xsl:element name="{name(.)}" namespace="{namespace-uri()}">
      <xsl:apply-templates select="@* | node()"/>
    </xsl:element>
  </xsl:template>
  
  
  <xsl:template match="@class | 
    @domains | 
    *[contains(@class, ' topic/ph ')]/@format | 
    *[contains(@class, ' topic/ph ')]/@scope | 
    @ditaarch:DITAArchVersion" 
    priority="10"/>
  
  <xsl:template match="@* | node()">
    <xsl:sequence select="."/>
  </xsl:template>
  
  
</xsl:stylesheet>
