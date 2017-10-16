<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      xmlns:local="urn:local-functions"
      xmlns:df="http://dita2indesign.org/dita/functions"
      xmlns:e2s="http//dita2indesign.org/functions/element-to-style-mapping"
      exclude-result-prefixes="xs local df e2s"
      version="2.0">
  
  <!-- Element-to-style mapper
    
    Maps DITA elements to T+D-specific InDesign styles

    Copyright (c) 2009 ASTD
    
  -->
  
  <xsl:template match="/*[df:class(., 'topic/topic')]/*[df:class(., 'topic/title')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'HeadlineDept'"/>
  </xsl:template>
  
 
  <!-- WEK: Heading 2 is a really big heading -->
  <xsl:template match="/*[df:class(., 'topic/topic')]/*[df:class(., 'topic/topic')]/*[df:class(., 'topic/title')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'Subhead'"/>
  </xsl:template>
  
  <xsl:template match="/*[df:class(., 'topic/topic')]/*[df:class(., 'topic/topic')]/*[df:class(., 'topic/topic')]/*[df:class(., 'topic/title')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'Heading 3'"/>
  </xsl:template>

  <xsl:template match="*[df:class(., 'sidebar/sidebar')]/*[df:class(., 'topic/title')]" 
    mode="style-map-pstyle" priority="10">
    <xsl:sequence select="'SidebarHead'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/p')][@outputclass != '']" priority="7" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with non-empty outputclass: <xsl:value-of select="@outputclass"/></xsl:message>
    <xsl:sequence select="string(./@outputclass)"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'BookTitle']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='BookTitle': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="string(./@outputclass)"/>
  </xsl:template>
  
 <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'BookSpecs']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='BookSpecs': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'BookInfo'"/>
  </xsl:template>
  
  <!-- New TD styles 2017  -->
    
 <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'InFocusHeadDept']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='InFocusHeadDept': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'InFocusHeadDept'"/>
  </xsl:template>
  
   <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'InFocusHead']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='InFocusHead': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'InFocusHead'"/>
  </xsl:template>
  
     <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'BookQuote']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='BookQuote': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'BookQuote'"/>
  </xsl:template>
  
  
 <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'BigNumber']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='BigNumber': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'BigNumber'"/>
  </xsl:template>
  
  
   <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'Tag']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='Tag': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'Tag'"/>
  </xsl:template>
  
     <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'Tag']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='Tag': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'Tag'"/>
  </xsl:template>
  
       <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'SidebarHeadDept']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='SidebarHeadDept': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'SidebarHeadDept'"/>
  </xsl:template>
  
     <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'SmallSans']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='SmallSans': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'SmallSans'"/>
  </xsl:template>
  
       <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'CopySansNumbered']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='CopySansNumbered': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'CopySansNumbered'"/>
  </xsl:template>
  
       <xsl:template match="*[df:class(., 'topic/p')][@outputclass = 'CopySansUpsized']" priority="20" 
    mode="style-map-pstyle">
    <xsl:message> + [DEBUG] p with outputclass='CopySansUpsized': <xsl:value-of select="@outputclass"/></xsl:message>
      <xsl:sequence select="'CopySansUpsized'"/>
  </xsl:template>
  
  
  <!-- END NEW STYLES -->
  
  <xsl:template match="*[df:class(., 'sidebar/sidebar')]/*[df:class(., 'topic/body')]//*[df:class(., 'topic/p')]" priority="10"
    mode="style-map-pstyle">
    <xsl:sequence select="'CopySans'"/>
  </xsl:template>
  
  <!-- #93 process as copysandNoindent if output calss is present -->
    <xsl:template match="*[df:class(., 'sidebar/sidebar')]/*[df:class(., 'topic/body')]//*[df:class(., 'topic/p') and @outputclass = 'CopySansNoIndent']" priority="15"
    mode="style-map-pstyle">
    <xsl:sequence select="'CopySansNoIndent'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/p')]" priority="5"
    mode="style-map-pstyle">
    <xsl:param name="articleType" as="xs:string" tunnel="yes" select="'article'"/>
<!--    <xsl:message> + [DEBUG] (style-map-pstyle) topic/p: Article type is "<xsl:sequence select="$articleType"/>"</xsl:message>
-->    
    <xsl:choose>
      <xsl:when test="$articleType = 'BK' or $articleType = 'BX'">
        <xsl:sequence select="'CopySans'"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="'Copy'"/>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd_p-d/copy-block')]"
    priority="10"
    mode="style-map-pstyle">
    <xsl:sequence select="'CopyBlock'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/p') and @outputclass != '']"
    priority="5"
    mode="style-map-pstyle">
    <!-- By default, if there is an output classs on 'p', assume it's
         the InDesign style.
      -->
    <xsl:sequence select="string(@outputclass)"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/lq')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'Long Quote Single Para'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/lq')]/*[df:class(., 'topic/p')]" priority="10" 
    mode="style-map-pstyle">
    <xsl:variable name="styleModifier"
      select="
      if (count(preceding-sibling::*[df:isBlock(.)]) = 0)
      then if (count(following-sibling::*) = 0)
      then ' Only'
      else ' First'
      else if (count(following-sibling::*) = 0)
      then ' Last'
      else ''
      "
      as="xs:string"
    />
    <xsl:sequence select="concat('Long Quote Para', $styleModifier)"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'article/deck')]" 
    mode="style-map-pstyle" priority="10">
    <xsl:sequence select="'DeckDept'"/>
  </xsl:template>
  
  <!-- Depreciated since #93
  <xsl:template match="/*[df:class(., 'article/article')]/*[df:class(., 'topic/body')]/*[df:class(., 'topic/p')][count(preceding-sibling::*[df:class(.,'topic/p')]) = 0]" 
    mode="style-map-pstyle" priority="15">
    <xsl:sequence select="'Copy Intro'"/>
  </xsl:template>
  -->
  <xsl:template match="*[df:class(., 'astd-bodydiv/callout')]" mode="style-map-pstyle">
    <xsl:sequence select="'CallOutDept'"/>
  </xsl:template> 
  
  <xsl:template match="*[df:class(., 'topic/shortdesc')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'Copy'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'CopyBullet'"/>
  </xsl:template>
  
    <!-- maps the boldface style in incopy config styles -->
  <xsl:template match="*[df:class(., 'topic/ph')][@outputclass = 'boldface']" 
    mode="style-map-cstyle">
    <xsl:sequence select="'boldface'"/>
  </xsl:template>
  
  <!-- maps the italics style in incopy config styles -->
  <xsl:template match="*[df:class(., 'topic/ph')][@outputclass = 'italics']" 
    mode="style-map-cstyle">
    <xsl:sequence select="'italics'"/>
  </xsl:template>
  
  <!-- added forticket #93 -->
  <xsl:template match="*[df:class(., 'sidebar/sidebar')]/*[df:class(., 'topic/body')]//*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]" priority="12"
    mode="style-map-pstyle">
    <xsl:sequence select="'CopySansBullet'"/>
  </xsl:template>
  
  <xsl:template 
    match="*[df:class(., 'topic/ol')]/
    *[df:class(., 'topic/li')][count(preceding-sibling::*[df:class(.,'topic/li')]) = 0]"
    priority="10" 
    mode="style-map-pstyle"
    >
    <xsl:sequence select="'CopyNumbered'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/dt')]" 
    mode="style-map-cstyle">
    <xsl:sequence select="'SubheadInText'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/dd')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'Copy'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/ol')]/*[df:class(., 'topic/li')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'CopyNumbered'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/fig')]/*[df:class(., 'topic/title')]" 
    mode="style-map-pstyle">
    <xsl:sequence select="'Caption'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/section')]" 
    mode="style-map-pstyle">
    <!-- FIXME: account for outputclass -->
    <xsl:sequence select="'Heading 3'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'topic/cite')]" 
    mode="style-map-cstyle" priority="10">
    <xsl:sequence select="'CopyItalic'"/>
  </xsl:template>
    
  <xsl:template match="*[df:class(., 'hi-d/i')]" 
    mode="style-map-cstyle" priority="10">
    <xsl:sequence select="'CopyItalic'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-author-d/astd-author')]" 
    mode="style-map-pstyle" priority="10">
    <xsl:sequence select="'Bio'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-author-d/author-bio-para')]" 
    mode="style-map-pstyle" priority="10">
    <xsl:sequence select="'Bio'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-author-d/author-name')]" 
    mode="style-map-cstyle" priority="10">
    <xsl:sequence select="'BioName'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-author-d/author-affiliation')]" 
    mode="style-map-cstyle" priority="10">
    <xsl:sequence select="'[No character style]'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-author-d/author-desc')]" 
    mode="style-map-cstyle" priority="10">
    <xsl:sequence select="'[No character style]'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-author-d/author-email')]" 
    mode="style-map-cstyle" priority="10">
    <xsl:sequence select="'[No character style]'"/>
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-metadata-d/disclaimer')]" 
    mode="style-map-pstyle" priority="10">
    <xsl:sequence select="'SmallSans'"/>
  </xsl:template>
  
  <!--added by pengli to fix ticket#23 moidified for #93-->
  <xsl:template match="*[df:class(., 'astd-author-d/author-desc-italic')]" 
    mode="style-map-cstyle" priority="10">
    <xsl:sequence select="'BioItalic'"/>
  </xsl:template>
  
    <xsl:template match="/*[df:class(., 'topic/topic')]/*[df:class(., 'interview-question/interview-question')]/*[df:class(., 'topic/title')]" 
    mode="style-map-pstyle" priority="10">
    <xsl:sequence select="'Question'"/>
  </xsl:template>
  <!--ended ticket#23-->
</xsl:stylesheet>
