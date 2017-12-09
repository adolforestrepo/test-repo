<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:local="urn:local-functions"
    xmlns:df="http://dita2indesign.org/dita/functions"
    xmlns:relpath="http://dita2indesign/functions/relpath"
    xmlns:e2s="http//dita2indesign.org/functions/element-to-style-mapping"
    xmlns:RSUITE="http://www.reallysi.com"
    xmlns:idsc="http://www.reallysi.com/namespaces/indesign_style_catalog"
    xmlns:incxgen="http//dita2indesign.org/functions/incx-generation"
    exclude-result-prefixes="xs local df relpath e2s RSUITE idsc incxgen" version="2.0">

    <!-- Topic to ICML Transformation.
    
       Into one or more InCopy (ICML) articles.
       
       This module handles the base (topic.mod) types. 
       Specialization modules should add their own
       XSL modules as necessary.
       
       Copyright (c) 2011 DITA2InDesign Project
       
  -->
    
    <xsl:template match="
        *[df:class(., 'topic/li')][*[df:isBlock(.)]]
        ">
        <!-- Correctly handle paragraphs that contain mixed content with block-creating elements.
      -->
        <xsl:param name="articleType" as="xs:string" tunnel="yes"/>
        
        <xsl:variable name="pStyle" select="e2s:getPStyleForElement(., $articleType)" as="xs:string"/>
        <xsl:variable name="cStyle" select="e2s:getCStyleForElement(.)" as="xs:string"/>
        <xsl:for-each-group select="* | text()"
            group-adjacent="
            if (self::*) then
            if (df:isBlock(.)) then
            'block'
            else
            'text'
            else
            'text'">
            <xsl:choose>
                <xsl:when test="self::* and df:isBlock(.)">
                    <xsl:apply-templates select="current-group()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="makeBlock-cont">
                        <xsl:with-param name="pStyle" select="$pStyle" as="xs:string" tunnel="yes"/>
                        <xsl:with-param name="cStyle" select="'$ID/[No character style]'" as="xs:string" tunnel="yes"/>
                        <xsl:with-param name="content" as="node()*" select="current-group()"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each-group>
    </xsl:template>
    
    
</xsl:stylesheet>
