<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:local="urn:local-functions"
    xmlns:df="http://dita2indesign.org/dita/functions"
    xmlns:e2s="http//dita2indesign.org/functions/element-to-style-mapping"
    exclude-result-prefixes="xs local df e2s" version="2.0">

    <!-- Element-to-style mapper
    
    This module provides the base implementation for
    the "style-map" modes, which map elements in context
    to InDesign style names (paragraph, character, frame,
    object, table).
    
    Copyright (c) 2009 Really Strategies, Inc.
    
    NOTE: This material is intended to be donated to the RSI-sponsored
    DITA2InDesign open-source project.
  -->
    <!-- Required modules:
  <xsl:import href="../../net.sourceforge.dita4publishers.common.xslt/xsl/lib/dita-support-lib.xsl"/>
  <xsl:import href="lib/icml_generation_util.xsl"/>
  
  -->


    <xsl:template match="*[df:class(., 'topic/title')]"
        mode="style-map-pstyle">
        <xsl:sequence select="'ATD TOC'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'TOC_para']"
        mode="style-map-pstyle">
        <xsl:sequence select="'ATD toc paragraph'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/title')][@outputclass = 'Title']"
        mode="style-map-pstyle" priority="5">
        <xsl:sequence select="'Heading 1'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'Chapter_Number']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Heading 2'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'Overview']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Copy'"/>
    </xsl:template>
 
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'Example_Tool']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Copy'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'Example_Tool_Title']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Copy'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'H1']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Heading 1'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'H2']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Heading 2'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'H3']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Heading 3'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = '1st_Paragraph_Example/Tool']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Paragraph Style 1'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'Body_Example/Tool']"
        mode="style-map-pstyle">
        <xsl:sequence select="'Copy'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Index_AHead']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'ATD index-ahead'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Index_Main']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'ATD index-main'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Index_Sub1']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'ATD index-sub1'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Index_Sub2']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'ATD index-sub2'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Index_Sub3']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'ATD index-sub3'"/>
    </xsl:template>
    
<!-- ********************* Start of How We Learn **************************** -->

    <xsl:template match="*[df:class(., 'chapter/chapter')]/*[df:class(., 'topic/title')][@outputclass='Title']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'CH Title'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/topic')][@outputclass='Title_Page']/*[df:class(., 'topic/title')]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'CH Title'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/topic')][@outputclass='Preface']/*[df:class(., 'topic/title')]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'CH Title'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/topic')][@outputclass='Intro']/*[df:class(., 'topic/title')]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'CH Title'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/topic')][@outputclass='Author_Bio']/*[df:class(., 'topic/title')]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'CH Title'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Chapter_Number']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'Ch Num'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='1st_Paragraph']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'1st Paragraph'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='H1']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'H1'"/>
    </xsl:template>
 
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='H2']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'H2'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Subtitle']"
        mode="style-map-pstyle" priority="11">
        <xsl:sequence select="'H3'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/topic')][@outputclass='Title_Page']/*[df:class(., 'topic/body')]/*[df:class(., 'topic/p')]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'H3'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/p')][contains(ancestor::chapter/title, 'References')]"
        mode="style-map-pstyle" priority="2">
        <xsl:sequence select="'reference body'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')]"
        mode="style-map-pstyle" priority="1">
        <xsl:sequence select="'body'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'hi-d/i')]" 
        mode="style-map-cstyle">
        <xsl:sequence select="'italics'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]
        /*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]
        /*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]"
        mode="style-map-pstyle" priority="30">
        <xsl:sequence select="'Bullet_List_3'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]
        /*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]"
        mode="style-map-pstyle" priority="20">
        <xsl:sequence select="'Bullet_List_2'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/ol')]/*[df:class(., 'topic/li')]
        /*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]"
        mode="style-map-pstyle" priority="20">
        <xsl:sequence select="'Bullet List - 2nd Level'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'Bullet List'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/ol')]/*[df:class(., 'topic/li')][count(preceding-sibling::*)=0]"
        mode="style-map-pstyle" priority="11">
        <xsl:sequence select="'Num List_First'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/ol')]/*[df:class(., 'topic/li')]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'Num List'"/>
    </xsl:template>
    
    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Table_Title']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'Figure/Table Title'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][@outputclass='Callout']"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'Callout'"/>
    </xsl:template>

    <xsl:template match="*[df:class(., 'topic/p')][preceding::*[df:class(., 'topic/title')][@outputclass='Copyright']]"
        mode="style-map-pstyle" priority="10">
        <xsl:sequence select="'Copyright'"/>
    </xsl:template>
    
    <!-- ********************* End of How We Learn **************************** -->
    
    <xsl:template match="*"
        mode="style-map-pstyle" priority="-1">
        <xsl:sequence select="'Copy'"/>
    </xsl:template>
    
    <xsl:template match="*" priority="-2" mode="style-map-pstyle">
        <xsl:sequence
            select="
                if (string(@outputclass) != '')
                then
                    e2s:getPStyleForOutputClass(., string(@outputclass))
                else
                ''
                "
        />
    </xsl:template>


    <xsl:template match="*" priority="-1" mode="style-map-cstyle">
        <xsl:sequence
            select="
                if (string(@outputclass) != '')
                then
                    e2s:getCStyleForOutputClass(., string(@outputclass))
                else
                    ''
                "
        />
    </xsl:template>

    <xsl:template match="*" priority="-1" mode="style-map-tstyle">
        <xsl:sequence
            select="
                if (string(@outputclass) != '')
                then
                    e2s:getTStyleForOutputClass(.)
                else
                    '[Basic Table]'
                "
        />
    </xsl:template>

    <xsl:function name="e2s:getPStyleForElement" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <xsl:param name="articleType" as="xs:string"/>
        <xsl:apply-templates select="$context" mode="style-map-pstyle">
            <xsl:with-param name="articleType" as="xs:string" tunnel="yes" select="$articleType"/>
        </xsl:apply-templates>
    </xsl:function>

    <xsl:function name="e2s:getCStyleForElement" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <xsl:choose>
            <xsl:when test="$context/@outputclass">
                <xsl:sequence select="e2s:getCStyleForOutputClass($context)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="$context" mode="style-map-cstyle"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="e2s:getTStyleForElement" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <xsl:apply-templates select="$context" mode="style-map-tstyle"/>
    </xsl:function>

    <xsl:function name="e2s:getTStyleForOutputClass" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <xsl:sequence select="e2s:getTStyleForOutputClass($context, string($context/@outputclass))"
        />
    </xsl:function>

    <xsl:function name="e2s:getTStyleForOutputClass" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <!-- Element that exhibits the outputclass value -->
        <xsl:param name="outputclass" as="xs:string"/>
        <!-- The output class value. This is passed
      so this function can be used where the
      the "outputclass" is provided by means 
      other than an @outputclass attribute. -->
        <!-- Given an @outputclass value, maps it to an InDesign style.
      
      For now, this just returns the outputclass value, but this
      needs to be driven by a configuration file, ideally the
      same one used to map element types to styles.
    -->
        <xsl:sequence select="$outputclass"/>
    </xsl:function>


    <xsl:function name="e2s:getPStyleForOutputClass" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <!-- Element that exhibits the outputclass value -->
        <xsl:param name="outputclass" as="xs:string"/>
        <!-- The output class value. This is passed
      so this function can be used where the
      the "outputclass" is provided by means 
      other than an @outputclass attribute. -->
        <!-- Given an @outputclass value, maps it to an InDesign style.
      
      For now, this just returns the outputclass value, but this
      needs to be driven by a configuration file, ideally the
      same one used to map element types to styles.
    -->
        <xsl:sequence select="$outputclass"/>
    </xsl:function>

    <xsl:function name="e2s:getCStyleForOutputClass" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <xsl:sequence select="e2s:getCStyleForOutputClass($context, string($context/@outputclass))"
        />
    </xsl:function>

    <xsl:function name="e2s:getCStyleForOutputClass" as="xs:string">
        <xsl:param name="context" as="element()"/>
        <!-- Element that exhibits the outputclass value -->
        <xsl:param name="outputclass" as="xs:string"/>
        <!-- The output class value. This is passed
      so this function can be used where the
      the "outputclass" is provided by means 
      other than an @outputclass attribute. -->
        <!-- Given an @outputclass value, maps it to an InDesign style.
      
      For now, this just returns the outputclass value, but this
      needs to be driven by a configuration file, ideally the
      same one used to map element types to styles.
    -->
        <xsl:sequence select="$outputclass"/>
    </xsl:function>

</xsl:stylesheet>
