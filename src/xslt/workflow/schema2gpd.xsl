<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      exclude-result-prefixes="xs"
      version="2.0">
  
  <!-- Converts an RSuite Workflow Designer .schema.xml document into an GPD 
       geometry document.
       
    -->

  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>
  
  <xsl:template match="WorkflowProcess">
    <xsl:variable name="name" as="xs:string"
      select="replace(tokenize(substring-before(document-uri(root(.)), '.schema.xml'), '/')[last()], '%20', ' ')"
    />
    <xsl:message> + [DEBUG] $name="<xsl:sequence select="$name"/>"</xsl:message>  
    
    <root-container name="{$name}" width="1000" height="1000">
      <xsl:apply-templates/>
    </root-container>
  </xsl:template>
  
  <xsl:template match="Activities">
    <xsl:apply-templates/>
  </xsl:template>
  
  <xsl:template match="Activity">
    <xsl:variable name="id" select="@id"/>
    <node name="{@name}" x="{round(@xCoordinate * 1.0)}" y="{round(@yCoordinate * 0.8)}" width="{@width}" height="{@height}">
      <xsl:apply-templates select="/*/Transitions/Transition[@from = $id]"/>
    </node>
  </xsl:template>
  
  <xsl:template match="Transitions"/>
  
  <xsl:template match="Transition">
    <edge>
      <label x="5" y="-10"/>
    </edge>
  </xsl:template>
  
  <xsl:template match="*" priority="-1" mode="#default">
    <xsl:message> + [WARNING] Default mode: Unhandled element <xsl:sequence select="name(..)"/>/<xsl:sequence select="name(.)"/></xsl:message>
  </xsl:template>
  
  
</xsl:stylesheet>
