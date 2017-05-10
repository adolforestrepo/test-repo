<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:local="urn:local-functions"
  xmlns:rsiwp="http://reallysi.com/namespaces/generic-wordprocessing-xml"
  xmlns:stylemap="urn:public:dita4publishers.org:namespaces:word2dita:style2tagmap"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:relpath="http://dita2indesign/functions/relpath"
  exclude-result-prefixes="xs rsiwp stylemap local relpath" version="2.0">

  
<xsl:import href="rsuite:/res/plugin/dita4publishers/toolkit_plugins/org.dita4publishers.word2dita/xsl/docx2dita.xsl"/>
  <xsl:import href="wordml2simple.xsl" />
  <xsl:import href="simple2dita.xsl" />   
  <xsl:param name="filterBr" as="xs:string" select="'true'"/>
  <xsl:param name="filterTabs" as="xs:string" select="'true'"/>
  <xsl:param name="includeWordBackPointers" as="xs:string" select="'false'"/>
  <xsl:param name="path_for_references">/rsuite/rest/v2/content/binary/alias/</xsl:param>
  
  <xsl:template match="*" mode="final-fixup">
    <xsl:variable name="contains_teacher_condition">
      <xsl:choose>
        <xsl:when test="@outputclass and child::ph[@outputclass='TEACHER_CONTENT']">true</xsl:when>
        <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>    
    <xsl:variable name="contains_break_flag">
      <xsl:choose>
        <xsl:when test="@outputclass and child::ph[@outputclass='PAGE_BREAK']">true</xsl:when>
        <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="contains_layout_flag">
      <xsl:choose>
        <xsl:when test="@outputclass and child::ph[@outputclass='LAYOUT']">
          <xsl:value-of select="ph/@outputclass='LAYOUT'"/>
        </xsl:when>
        <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>      
    <xsl:choose>
      <!-- corrects bug in Word2DITA -->
      <xsl:when test="self::mainpubtitle">
        <title outputclass="{parent::topic/@outputclass}">
          <xsl:apply-templates select="@*" mode="block_GoBack"/>
          <xsl:apply-templates mode="final-fixup"/>
        </title>
      </xsl:when>
      <!-- all four of these conditions are used to modify <fn> in the current Word2DITA transform:
				1. <fn> is contained inside <ph> for no reason - remove that containing <ph>
				2. <fn> contains empty <ph> for no reason - remove that nested <ph>
				3. <fn> needs an ID so that it's easier to handle references in output
				4. <fn> is assigend @outputclass of values "footnote" or "endnote" as approprite so that it's easier to process them-->
      <xsl:when test="self::ph[parent::p[@outputclass = 'Footnote']][not(text() or * or @outputclass)]"></xsl:when>
      <xsl:when test="self::ph[parent::p[@outputclass = 'Endnote']][not(text() or * or @outputclass)]"></xsl:when>
      <xsl:when test="self::ph[child::fn]">
        <xsl:apply-templates mode="final-fixup"/>
      </xsl:when>
      <xsl:when test="self::fn">
        <xsl:copy>			
          <xsl:attribute name="id" select="generate-id()"/>
          <xsl:attribute name="outputclass" >
            <xsl:choose>
              <xsl:when test="descendant::*[@outputclass='SIDEBAR']">Sidebar</xsl:when>
              <xsl:when test="p[@outputclass='Footnote']">Footnote</xsl:when>
              <xsl:when test="p[@outputclass='Endnote']">Endnote</xsl:when>                            
              <xsl:otherwise>something wrong with fn outputclass</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:apply-templates mode="final-fixup"/>
        </xsl:copy>
      </xsl:when>    
      <xsl:when test="parent::fn and descendant::*[@outputclass='SIDEBAR']">
       <xsl:copy>
         <xsl:attribute name="outputclass">Sidebar</xsl:attribute>
         <xsl:apply-templates mode="final-fixup"/>
       </xsl:copy>        
      </xsl:when>
    <!-- can't use this condition until we have the XML documents with the standards so the references point at actual content -->    
      <xsl:when test="self::ul[@outputclass='List_Bullet'][descendant::ph[@outputclass='CONTENT_REFERENCE']]">
        <xsl:apply-templates mode="content_reference"/>
      </xsl:when>
<!--      <xsl:when test="self::li and child::ph[@outputclass='CONTENT_REFERENCE']">
        <xsl:variable name="reference_content">
          <xsl:for-each select="child::ph[@outputclass='CONTENT_REFERENCE']">
            <xsl:value-of select="."/>
          </xsl:for-each>
        </xsl:variable>
        <p conref="{concat($path_for_references, 'ela-literacy#CCSS_ELA/', $reference_content)}"/>
      </xsl:when>    -->
     <!-- remove this condition when we have the XML documents for content references to the various standards and use the one above instead -->  
<?remove      <xsl:when test="self::li and child::ph[@outputclass='CONTENT_REFERENCE']">
        <xsl:variable name="reference_content">
          <xsl:for-each select="child::ph[@outputclass='CONTENT_REFERENCE']">
            <xsl:value-of select="."/>
          </xsl:for-each>
        </xsl:variable>
        <li outputclass="CONTENT_REFERENCE">
          <xsl:value-of select="$reference_content"/>
        </li>
      </xsl:when>?>

<!--      <xsl:when test="ph[@outputclass='CONTENT_REFERENCE']">
        <xsl:apply-templates mode="final-fixup"></xsl:apply-templates>
      </xsl:when>-->
<?remove      
        <!-- used these templates originally when outputting actual <dl> element and it's markup;
        but EL ED's content for definitions doesn't fit that data model, so removed this for looser @outputclass-based markup -->
        <xsl:when test="self::dthd">
        <dlhead>
          <dthd>
            <xsl:apply-templates mode="final-fixup"/>
          </dthd>
        </dlhead>
      </xsl:when>      
      <xsl:when test="self::dlentry">       
        <dlentry>
            <xsl:apply-templates select="dd | dt" mode="final-fixup"/>
        </dlentry>
      </xsl:when>     ?> 
      <!-- do not output the content of <ph outputclass="SIDEBAR"> -->
      <xsl:when test="self::ph[@outputclass='SIDEBAR']"/>
      <!-- do not output the content of <ph outputclass="PAGE_BREAK"> -->
      <xsl:when test="self::ph[@outputclass='PAGE_BREAK']"/>
      <!-- do not output the content of <ph outputclass="TEACHER"> -->
      <xsl:when test="self::ph[@outputclass='TEACHER_CONTENT']"/>      
      <!-- do not output the content of <ph outputclass="LAYOUT"> -->
      <xsl:when test="self::ph[@outputclass='LAYOUT']"/>
      <!-- if the paragraph has no text node nor an @outputclass then it's a Word page break paragraph and should be removed -->
      <xsl:when test="self::p[not(@outputclass) and not(text())]"/> 
      <!-- convert the Word table for Box into a DITA container -->
      <xsl:when test="self::table and @outputclass='ELBox'">
        <xsl:variable name="box_container">
          <xsl:choose>
            <xsl:when test="ancestor::section">sectiondiv</xsl:when>
            <xsl:otherwise>bodydiv</xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:element name="{$box_container}">
          <xsl:attribute name="base">
            <xsl:value-of select="descendant::entry/p[@outputclass='SC_Box_Size']"/>
          </xsl:attribute>
          <xsl:apply-templates select="descendant::entry/*[not(@outputclass='SC_Box_Size')]" mode="final-fixup"/>
        </xsl:element>
      </xsl:when>
      <xsl:when test="self::table and ancestor::table">
        <p outputclass="para_wrapper_nested_table">
          <xsl:copy>
            <xsl:for-each select="@*">
              <xsl:copy-of select="."/>
            </xsl:for-each>
            <xsl:apply-templates mode="final-fixup"/>
          </xsl:copy>
        </p>
      </xsl:when>      
      <!-- convert the Word table for Horizontal layout into a DITA container -->
      <xsl:when test="self::table and @outputclass='ELHorizontal'">
        <xsl:variable name="container">
          <xsl:choose>
            <xsl:when test="ancestor::section">sectiondiv</xsl:when>
            <xsl:otherwise>bodydiv</xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:element name="{$container}">
          <xsl:attribute name="outputclass">Horizontal</xsl:attribute>
          <xsl:for-each select="descendant::entry">
            <xsl:element name="{$container}">
              <xsl:attribute name="outputclass">Horizontal_Item</xsl:attribute>
              <xsl:apply-templates mode="final-fixup"/>            
            </xsl:element>
          </xsl:for-each>
        </xsl:element>
      </xsl:when>      
      <xsl:when test="self::section">
        <xsl:copy>
          <xsl:attribute name="outputclass" select="substring-before(p[position()=1]/@outputclass, '^')"></xsl:attribute>
          <xsl:if test="child::p[position()=1][contains(@outputclass, '^Title')]/ph[@outputclass='TEACHER_CONTENT']">
            <xsl:attribute name="audience">teacher</xsl:attribute>
          </xsl:if>
          <xsl:if test="child::p[position()=1][contains(@outputclass, '^Title')]/ph[@outputclass='PAGE_BREAK']">
            <xsl:attribute name="props">page_break</xsl:attribute>
          </xsl:if>
          <xsl:if test="child::p[position()=1][contains(@outputclass, '^Title')]/ph[@outputclass='LAYOUT']">
            <xsl:variable name="layout" select="child::p[position()=1]/ph[@outputclass='LAYOUT']"/>
            <xsl:attribute name="otherprops">
              <xsl:value-of select="$layout"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:apply-templates mode="final-fixup"/>
        </xsl:copy>
      </xsl:when>
      <xsl:when test="self::topic">
          <xsl:apply-templates select="title" mode="final-fixup"/>
      </xsl:when>
      <xsl:when test="self::title and parent::topic">
        <xsl:variable name="parent_element" select="name(parent::*)"/>
        <xsl:element name="{$parent_element}">
          <xsl:apply-templates select="parent::*/@*" mode="block_GoBack"/>          
          <xsl:if test="$contains_teacher_condition = 'true'">
            <xsl:attribute name="audience">teacher</xsl:attribute>
          </xsl:if>          
          <xsl:if test="$contains_break_flag = 'true'">
            <xsl:attribute name="props">page_break</xsl:attribute>
          </xsl:if>
          <xsl:if test="not($contains_layout_flag = 'false')">
            <xsl:attribute name="otherprops">
              <xsl:value-of select="$contains_layout_flag"/>
            </xsl:attribute>
          </xsl:if>
          <title>          
            <xsl:apply-templates select="* | text()" mode="final-fixup"/>
          </title>       
          <xsl:apply-templates select="parent::topic/body" mode="final-fixup"/>
          <xsl:apply-templates select="parent::topic/body/following-sibling::*" mode="final-fixup"/>
        </xsl:element>
      </xsl:when>
      <xsl:when test="self::p and contains(@outputclass, '^Title')">
        <title>          
          <xsl:apply-templates mode="final-fixup"/>
        </title>       
      </xsl:when>
      <xsl:when test="self::ol[@outputclass='Grade' or @outputclass='Standard']">
       <!--<xsl:comment>Grade or Standard-Reading or Standard-Language template</xsl:comment>-->
        <xsl:apply-templates mode="final-fixup"/>
      </xsl:when>
      <xsl:when test="self::li[parent::ol[@outputclass='Grade']]">
       <!-- <xsl:comment>Grade list item</xsl:comment>-->
        <xsl:apply-templates select="*" mode="final-fixup"/>
      </xsl:when>
      <xsl:when test="self::li[@outputclass='Target_Note']">
        <p outputclass="{@outputclass}">
          <xsl:apply-templates mode="final-fixup"/>
        </p>
      </xsl:when>      
      <xsl:when test="self::li[parent::ol[@outputclass='Standard']]">
        <!--<xsl:comment>Reading Foundation list item</xsl:comment>-->
        <sectiondiv audience="{parent::ol/parent::li/text()}" outputclass="Standard_and_Targets">
          <p outputclass="Standard">
            <xsl:apply-templates select="text()" mode="final-fixup"/>
          </p>
          <xsl:apply-templates select="*" mode="final-fixup"/>          
        </sectiondiv>
      </xsl:when>      
      <xsl:when test="self::ol[@outputclass='Targets']">
       <!-- <xsl:comment>Targets list</xsl:comment>-->
        <xsl:if test="*[@outputclass='Long-term_Learning_Target']">
          <sectiondiv outputclass="Long-term_Learning_Target">
            <xsl:for-each select="li[@outputclass='Long-term_Learning_Target']">
              <p>
                <xsl:apply-templates select="text()" mode="final-fixup"/>
              </p>
              <xsl:if test="following-sibling::li[@outputclass='Target_Note']">
                <xsl:apply-templates select="following-sibling::li[@outputclass='Target_Note']" mode="final-fixup"/>
              </xsl:if>
            </xsl:for-each>
          </sectiondiv>
        </xsl:if>
          <xsl:if test="*[@outputclass='Supporting_Targets']">
          <sectiondiv outputclass="Supporting_Targets">
            <xsl:for-each select="li[@outputclass='Supporting_Targets']">
              <p>
                <xsl:apply-templates mode="final-fixup"/>
              </p>
              <xsl:if test="following-sibling::li[@outputclass='Target_Note']">
                <xsl:apply-templates select="following-sibling::li[@outputclass='Target_Note']" mode="final-fixup"/>
              </xsl:if>
            </xsl:for-each>
          </sectiondiv>
          </xsl:if>
        <xsl:if test="*[not(@outputclass='Long-term_Learning_Target') and not(@outputclass='Supporting_Targets') and not(@outputclass='Target_Note')]">
        <!-- <xsl:comment>extra!</xsl:comment>-->
          <sectiondiv outputclass="{li[not(@outputclass='Long-term_Learning_Target') and not(@outputclass='Supporting_Targets') and not(@outputclass='Target_Note')][position()=1]/@outputclass}">
            <xsl:for-each select="li[not(@outputclass='Long-term_Learning_Target') and not(@outputclass='Supporting_Targets') and not(@outputclass='Target_Note')]">
              <p >
                <xsl:apply-templates mode="final-fixup"/>
              </p>
            </xsl:for-each>
          </sectiondiv>
        </xsl:if>
      </xsl:when>        
      <xsl:otherwise>
        <xsl:copy>
          <xsl:apply-templates select="@*" mode="block_GoBack"/>          
          <xsl:if test="$contains_teacher_condition = 'true'">
            <xsl:attribute name="audience">teacher</xsl:attribute>
          </xsl:if>          
          <xsl:if test="$contains_break_flag = 'true'">
            <xsl:attribute name="props">page_break</xsl:attribute>
          </xsl:if>
          <xsl:if test="not($contains_layout_flag = 'false')">
            <xsl:attribute name="otherprops">
              <xsl:value-of select="$contains_layout_flag"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:apply-templates mode="final-fixup"/>
        </xsl:copy>        
      </xsl:otherwise>      
    </xsl:choose>
  </xsl:template>

  <xsl:template match="li" mode="content_reference">
    <xsl:variable name="reference_content">
      <xsl:for-each select="child::ph[@outputclass='CONTENT_REFERENCE']">
        <xsl:value-of select="."/>
      </xsl:for-each>
    </xsl:variable>
    <p conref="{concat($path_for_references, 'ela-literacy-dita#CCSS_ELA/', $reference_content)}"/>
  </xsl:template>

  <xsl:template match="text()" mode="content_reference">
    <xsl:value-of select="."/>
  </xsl:template>


<xsl:template match="text()" mode="final-fixup">
  <xsl:value-of select="."/>
</xsl:template>

  <!-- all <xref> are derived from Word hyperlink mechanism so they should all be external links to either PDF or HTML -->
  <xsl:template match="xref" mode="final-fixup">
    <xsl:variable name="href" select="@href" as="xs:string"/>
    <xref>
      <xsl:apply-templates select="@*" mode="block_GoBack"/>
      <!-- overwrite the @href attribute -->
      <xsl:attribute name="href">
        <xsl:choose>
          <xsl:when test="contains($href, 'unresolvable reference to name ')">
            <xsl:value-of select="encode-for-uri(normalize-space(substring-after($href, 'unresolvable reference to name ')))"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="encode-for-uri(normalize-space($href))"/>            
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="scope">external</xsl:attribute>
      <xsl:attribute name="format">
        <xsl:choose>
          <xsl:when test="ends-with($href, '.pdf')">pdf</xsl:when>
          <xsl:otherwise>html</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xref>
  </xsl:template>
  
  <xsl:template match="image" mode="final-fixup">
    <image>
      <xsl:apply-templates select="@*" mode="block_GoBack"/>
      <xsl:attribute name="href">
        <xsl:value-of select="concat($path_for_references, encode-for-uri(normalize-space(.)))"/>
      </xsl:attribute>
      <xsl:attribute name="placement">
        <xsl:choose>
          <xsl:when test="@outputclass='Inline'">inline</xsl:when>
          <xsl:otherwise>break</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </image>
  </xsl:template>

  <!-- purpose of this template is to block all @id values that have "GoBack" in them; they are trash -->
  <xsl:template match="@*" mode="block_GoBack">
    <xsl:choose>
      <xsl:when test="contains(., 'GoBack')"/>
      <xsl:otherwise>
        <xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
   
</xsl:stylesheet>
 