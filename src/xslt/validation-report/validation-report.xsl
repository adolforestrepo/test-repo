<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:ditaarch="http://dita.oasis-open.org/architecture/2005/"
  exclude-result-prefixes="xs"
  version="2.0">
  
  <xsl:output method="text"/>
  
  <xsl:strip-space elements="*"/>
  
  <xsl:template match="/">
    <xsl:if test="*/error">
      <xsl:apply-templates/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="validationReport">
    <xsl:apply-templates>
      <xsl:sort select="concat(string(@line), '^',string(@col))"/>
    </xsl:apply-templates>
  </xsl:template>
  
  <xsl:template match="error">
    <xsl:if test="string(@docuri) != string(preceding-sibling::error[1]/@docuri)">
      <xsl:text>&#x0a;</xsl:text>
      <xsl:text>Document </xsl:text>
      <xsl:value-of select="@docuri"/>
      <xsl:text>:&#x0a;</xsl:text>
    </xsl:if>
    <xsl:text>&#x0a;</xsl:text>
    <xsl:text> - [Error] Line </xsl:text>
    <xsl:value-of select="@line"/>
    <xsl:text>, column </xsl:text>
    <xsl:value-of select="@col"/>
    <xsl:text> - </xsl:text>
    <xsl:value-of select="@message"/>
    <xsl:apply-templates/>
    
  </xsl:template>
  
  <xsl:template match="context">
    <xsl:text>&#x0a;</xsl:text>
    <xsl:text>   Context:</xsl:text>
    <xsl:text>&#x0a;</xsl:text>
    <xsl:text>&#x0a;</xsl:text>
    <xsl:apply-templates mode="serialize">
      <xsl:with-param name="indent" select="'  '" as="xs:string"/>
    </xsl:apply-templates>
  </xsl:template>
  
  <xsl:template mode="serialize" match="*">
    <xsl:param name="indent" as="xs:string"/>
    <xsl:sequence select="$indent"/>
    <xsl:sequence select="concat('&lt;',name(.))"/>
    <xsl:apply-templates select="@*" mode="serialize">
      <xsl:with-param name="indent" select="$indent"/>
    </xsl:apply-templates>
    <xsl:text>&gt;</xsl:text>
    <xsl:if test="*">
      <xsl:text>&#x0a;</xsl:text>
    </xsl:if>
    <xsl:apply-templates mode="serialize">
      <xsl:with-param name="indent" select="concat($indent, '  ')"/>
    </xsl:apply-templates>
    <xsl:if test="*">
      <xsl:sequence select="$indent"/>
    </xsl:if>
    <xsl:sequence select="concat('&lt;','/',name(), '&gt;')"/>
    <xsl:text>&#x0a;</xsl:text>
  </xsl:template>
  
  <xsl:template match="@class | @domains | @ditaarch:DITAArchVersion | DITAArchVersion" mode="serialize" priority="5">
    <!-- Suppressed -->
  </xsl:template>
  
  <xsl:template match="@xtrf | @xtrc" mode="serialize" priority="5">
    <!-- Suppressed for now -->
  </xsl:template>
  
  <xsl:template match="@*" mode="serialize">
    <xsl:param name="indent" as="xs:string"/>
    <xsl:if test="position() > 1">
      <xsl:text>&#x0a;</xsl:text>
      <xsl:sequence select="concat($indent, '  ')"/>
    </xsl:if>
    <xsl:sequence select="concat(name(), '=&quot;',string(.), '&quot;')"/>    
  </xsl:template>
  
  <xsl:template match="text()" mode="serialize">
    <xsl:copy/>
  </xsl:template>
  
  <xsl:template match="text()"/>
</xsl:stylesheet>
