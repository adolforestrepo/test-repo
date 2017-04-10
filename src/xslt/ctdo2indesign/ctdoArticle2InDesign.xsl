<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      xmlns:df="http://dita2indesign.org/dita/functions"
      xmlns:e2s="http//dita2indesign.org/functions/element-to-style-mapping"
      xmlns:relpath="http://dita2indesign/functions/relpath"
      xmlns:local="urn:local-functions"      
      exclude-result-prefixes="xs e2s df relpath local"
      version="2.0">

  <xsl:import href="rsuite:/res/plugin/dita2indesign/xslt/dita2indesign/lib/incx_generation_util.xsl"/>
  <xsl:import href="rsuite:/res/plugin/dita2indesign/xslt/dita2indesign/elem2styleMapper.xsl"/>
  <xsl:import href="rsuite:/res/plugin/dita4publishers/xslt/lib/relpath_util.xsl"/>
  <xsl:import href="rsuite:/res/plugin/dita4publishers/xslt/lib/dita-support-lib.xsl"/>
  
  <xsl:include href="ctdoElem2styleMapper.xsl"/>
  
  <xsl:include href="rsuite:/res/plugin/dita2indesign/xslt/dita2indesign/topic2article.xsl"/>
  
  <xsl:template match="/*[df:class(., 'article/article')]" priority="15">
    <!-- The topicref that points to this topic -->
    <xsl:param name="topicref" as="element()?" tunnel="yes"/>
    <xsl:if test="$debugBoolean">
      <xsl:message> + [DEBUG] topic2article.xsl: Processing root topic...</xsl:message>
    </xsl:if>
    <xsl:message> + [INFO] Processing XML article to generate InCopy article...</xsl:message>
    <!-- Create a new output InCopy article. 
      
      NOTE: This code assumes that all chunking has been performed
      so that each document-root topic maps to one result
      InCopy article and all nested topics are output as
      part of the same story. This behavior can be
      overridden by providing templates that match on
      specific topic types or output classes.
    -->
    <xsl:variable name="astdAuthorElems"
      select="*[df:class(., 'topic/prolog')]/*[df:class(., 'astd-author-d/astd-author')]"
      as="element()*"
    />    

    
    <xsl:variable name="astdDisclaimerElems"
      select="*[df:class(., 'topic/prolog')]/*[df:class(., 'astd-metadata-d/disclaimer')]"
      as="element()*"
    />    
    
    <!--added by  pengli for ticket#55-->
    <xsl:variable name="astdFeatureElems"
      select="*[df:class(., 'topic/prolog')]/*[df:class(., 'astd-metadata-f/featuretype')]"
      as="element()*"
    /> 
    <!--end for ticket#55-->
    <xsl:variable name="articleType" as="xs:string"
      select="local:getArticleType(., $topicref)"
    />
    
    
    <xsl:variable name="bioParagraphs" as="node()*">
      <xsl:if test="$astdAuthorElems">
        <!-- Generate paragraph with author information -->
        <xsl:choose>
          <xsl:when test="$astdAuthorElems/*[df:class(., 'astd-author-d/author-bio-para')]">
            <xsl:for-each select="$astdAuthorElems/*[df:class(., 'astd-author-d/author-bio-para')]">
              <xsl:call-template name="makeBlock-cont">
                <xsl:with-param name="pStyle" tunnel="yes" select="e2s:getPStyleForElement(., $articleType)"/>
                <xsl:with-param name="content" as="node()*"
                  select="."
                />
              </xsl:call-template>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="$astdAuthorElems">
              <xsl:call-template name="makeBlock-cont">
                <xsl:with-param name="pStyle" tunnel="yes" select="e2s:getPStyleForElement(., $articleType)"/>
                <xsl:with-param name="content" as="node()*"
                  select="."
                />
              </xsl:call-template>
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
        
      </xsl:if>
    </xsl:variable>
    
    <!--added by  pengli for ticket#55-->
    <xsl:variable name="featureParas" as="node()*">
      <xsl:if test="$astdFeatureElems !=''">
      <xsl:for-each select="$astdFeatureElems">
        <xsl:call-template name="makeBlock-cont">
          <!-- <xsl:with-param name="pStyle" tunnel="yes" select="e2s:getPStyleForElement(., $articleType)"/>-->
          <xsl:with-param name="pStyle" tunnel="yes" select="'Featuretype'"/>
          <xsl:with-param name="content" as="node()*"
            select="."
          />
        </xsl:call-template>
      </xsl:for-each>
      </xsl:if>
    </xsl:variable>
    
    
    <xsl:variable name="disclaimerParas" as="node()*">
      <!--<xsl:if test="$astdAuthorElems">-->
        <!-- Generate paragraph with author information -->
        <xsl:for-each select="$astdDisclaimerElems">
          <xsl:call-template name="makeBlock-cont">
            <xsl:with-param name="pStyle" tunnel="yes" select="e2s:getPStyleForElement(., $articleType)"/>
            <xsl:with-param name="content" as="node()*"
              select="."
            />
          </xsl:call-template>
        </xsl:for-each>
    <!--  </xsl:if>-->
    </xsl:variable>
    
    <!--added by pengli for ticket#55
      <xsl:variable name="trailingParagraphs" as="node()*"
      select="$bioParagraphs, $disclaimerParas"
      />
    -->
    <xsl:variable name="trailingParagraphs" as="node()*"
      select="$bioParagraphs, $featureParas, $disclaimerParas"
    />
    
    <xsl:message> + [INFO] Article type is "<xsl:sequence select="$articleType"/>"</xsl:message>
    
    <xsl:call-template name="makeInCopyArticle">
      <xsl:with-param name="styleCatalog" select="$styleCatalog"/>
      <xsl:with-param name="trailingParagraphs" select="$trailingParagraphs"/>
      <xsl:with-param name="articleType" tunnel="yes" as="xs:string" select="$articleType"/>
    </xsl:call-template> 
    <xsl:message> + [INFO] Article processed.</xsl:message>
  </xsl:template>
  
  <xsl:template priority="10"
    match="
    *[df:class(., 'astd-author-d/author-affiliation') ]/*[df:class(., 'hi-d/i')]
    " 
    mode="cont">
    <xsl:apply-templates mode="cont">
      <xsl:with-param name="cStyle" select="'roman'" tunnel="yes" as="xs:string"/>
    </xsl:apply-templates> 
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-author-d/author-bio-para')]"
    mode="block-children">
    <xsl:apply-templates mode="block-children"/>        
  </xsl:template>
  
  <!--added by pengli to fix ticket#23
    <xsl:template match="*[df:class(., 'astd-author-d/author-desc-italic')]"
    mode="block-children">
    <xsl:apply-templates mode="block-children"/>        
    </xsl:template> -->
  <!--added by pengli to fix ticket#23 20110602-->
    <xsl:template match="*[df:class(., 'astd-author-d/author-desc-italic')]"
    mode="block-children">
      <xsl:apply-templates mode="block-children">
        <xsl:with-param name="cStyle" select="'roman'" tunnel="yes" as="xs:string"/>
      </xsl:apply-templates>  
    </xsl:template> 
  <!-- End -->
  <!--end ticket#23-->
  
  
  
  
   <xsl:template match="*[df:class(., 'astd-author-d/author-desc')]"
    mode="block-children">
      <xsl:apply-templates mode="block-children">
        <xsl:with-param name="cStyle" select="'authorDesc'" tunnel="yes" as="xs:string"/>
      </xsl:apply-templates>  
    </xsl:template> 
    
    
    
    
  
  
  <!--added by pengli to fix ticket#55-->
  <xsl:template match="*[df:class(., 'topic/prolog')]/*[df:class(., 'astd-metadata-f/featuretype')]"
    mode="block-children">
    <xsl:apply-templates mode="block-children">
      <xsl:with-param name="pStyle" tunnel="yes" select="'Featuretype'"/>
    </xsl:apply-templates>  
  </xsl:template> 
  <!--added by pengli to fix ticket#55-->
  
      <xsl:template match="*[df:class(., 'topic/p')]/*[@outputclass = 'Hyperlink']"
    mode="block-children">
      <xsl:apply-templates mode="block-children">
        <xsl:with-param name="cStyle" select="'Hyperlink'" tunnel="yes" as="xs:string"/>
      </xsl:apply-templates>  
    </xsl:template> 
  
    <!--added by adolfo to fix ticket#93 20130227-->
<xsl:template match="*[df:class(., 'topic/p')]/*[@outputclass = 'Sub2Char']"
    mode="block-children">
      <xsl:apply-templates mode="block-children">
        <xsl:with-param name="cStyle" select="'SubheadInText'" tunnel="yes" as="xs:string"/>
      </xsl:apply-templates>  
    </xsl:template> 
    
	
	
	<xsl:template match="*[df:class(., 'topic/ul')]/*[df:class(., 'topic/li')]/*[@outputclass = 'Sub2Char']"
    mode="block-children">
      <xsl:apply-templates mode="block-children">
        <xsl:with-param name="cStyle" select="'SubheadInText'" tunnel="yes" as="xs:string"/>
      </xsl:apply-templates>  
    </xsl:template> 
  <!-- End -->
  <!--end ticket#93-->
  
  
  
  <xsl:template match="/*[df:class(., 'article/article')]/*[df:class(.,'topic/title')]" priority="10">
    <xsl:param name="articleType" as="xs:string" tunnel="yes"/>
    <xsl:if test="$articleType != 'BX'">
      <xsl:call-template name="makeBlock-cont">
        <xsl:with-param name="pStyle" tunnel="yes" select="e2s:getPStyleForElement(., $articleType)"/>
      </xsl:call-template>
   <!--  <xsl:if test="../*[df:class(.,'topic/prolog')]/*[df:class(.,'astd-author-d/astd-author')]">
        <xsl:call-template name="makeBlock-cont">
          <xsl:with-param name="pStyle" tunnel="yes" select="'ByLine'"/>
          <xsl:with-param name="content" as="node()*">
            <xsl:value-of select="."/>
            <xsl:for-each select="../*[df:class(.,'topic/prolog')]/*[df:class(.,'astd-author-d/astd-author')]/*[df:class(.,'astd-author-d/author-name')]">
              <xsl:choose>
                <xsl:when test="position() > 1 and position() = last()">
                  <xsl:text> and </xsl:text>
                </xsl:when>
                <xsl:when test="position() > 1">
                  <xsl:text>, </xsl:text>
                </xsl:when>
                <xsl:otherwise/>
              </xsl:choose>          
              <xsl:value-of select="."/>
            </xsl:for-each>
          </xsl:with-param>
        </xsl:call-template>    
      </xsl:if> -->
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[df:class(., 'astd-bodydiv/executive-summary')]"  priority="10">
    <xsl:variable name="filenameBase" as="xs:string"
      select="relpath:getNamePart(document-uri(root(.)))"
    />
    <xsl:variable name="execSumUri" as="xs:string"
      select="relpath:newFile(relpath:getParent(document-uri(root(.))), concat($filenameBase, '-execsum.incx'))"
    />
    <xsl:result-document href="{$execSumUri}">
      <xsl:call-template name="makeInCopyArticle">
        <xsl:with-param name="styleCatalog" select="$styleCatalog"/>     
        <xsl:with-param name="content" select="ancestor::*[df:class(., 'topic/topic')]/*[df:class(., 'topic/title')] | ./node()"/>
      </xsl:call-template>       
    </xsl:result-document>
  </xsl:template>
  
  
  <xsl:template match="*[df:class(., 'astd-author-d/astd-author')]" mode="block-children">
    <xsl:if test="$debugBoolean">
      <xsl:message> + [DEBUG] author-d/astd-author, mode 'block-children' </xsl:message>
    </xsl:if>
    <xsl:apply-templates mode="block-children"/>
  </xsl:template>
  
  <xsl:template 
    match="
    *[df:class(., 'astd-author-d/author-name')] |
    *[df:class(., 'astd-author-d/author-title')] |
    *[df:class(., 'astd-author-d/author-affiliation')] |
    *[df:class(., 'astd-author-d/author-email')] |
    *[df:class(., 'astd-author-d/author-desc-italic')] |
   
    *[df:class(., 'astd-author-d/author-bio-para')]/*[df:class(., 'astd-author-d/author-desc')]
    " 
    priority="10"
    mode="block-children">
    <xsl:if test="$debugBoolean">
      <xsl:message> + [DEBUG} <xsl:sequence select="string(@class)"/>: mod block-children </xsl:message>
    </xsl:if>
    
    <xsl:variable name="cStyle" select="e2s:getCStyleForElement(.)"/>
    <xsl:if test="$debugBoolean">
      <xsl:message> + [DEBUG] (mode block-children): <xsl:sequence select="string(@class)"/>: cStyle="<xsl:sequence select="$cStyle"/>"</xsl:message>
    </xsl:if>
    <xsl:apply-templates mode="cont">
      <xsl:with-param name="cStyle" select="$cStyle" tunnel="yes" as="xs:string"/>
    </xsl:apply-templates>   
  </xsl:template>
  
  <xsl:template 
    match="*[df:class(., 'topic/author')]/*[df:class(., 'astd-author-d/author-desc')]
    " 
    priority="10"
    mode="block-children">
    <xsl:if test="$debugBoolean">
      <xsl:message> + [DEBUG] astd-author-d/author-desc: mod block-children </xsl:message>
    </xsl:if>
    <xsl:variable name="cStyle" select="e2s:getCStyleForElement(.)"/>
    <xsl:call-template name="makeTxsr">
      <xsl:with-param name="text" as="xs:string" select="' is '"/>
      <xsl:with-param name="cStyle" select="'[No character style]'"/>
    </xsl:call-template>
    <xsl:apply-templates mode="cont">
      <xsl:with-param name="cStyle" select="$cStyle" tunnel="yes" as="xs:string"/>
    </xsl:apply-templates>   
  </xsl:template>
  
  <xsl:template match="*[df:class(., 'astd-metadata-d/disclaimer')]" mode="block-children">
    <xsl:if test="$debugBoolean">
      <xsl:message> + [DEBUG] astd-metadata-d/disclaimer, mode 'block-children' </xsl:message>
    </xsl:if>
    <xsl:apply-templates mode="block-children"/>
  </xsl:template>
  
  
  
  <xsl:function name="local:getArticleType" as="xs:string">
    <!-- Determines the article type based on whatever information is in the XML -->
    <xsl:param name="context" as="element()"/>
    <xsl:param name="topicref" as="element()?"/>
    
    <xsl:variable name="docUri" select="string(document-uri(root($context)))" as="xs:string"/>
    <xsl:variable name="fileName" select="relpath:getName($docUri)" as="xs:string"/>
    <xsl:variable name="typeCode" as="xs:string*">
      <xsl:analyze-string select="$fileName" regex="^..(..).+$">
        <xsl:matching-substring>
          <xsl:sequence select="upper-case(regex-group(1))"/>
        </xsl:matching-substring>
        <xsl:non-matching-substring>
          <xsl:message> - [WARNING] Document filename did not match "^..(..).+$", article type may not be correct.</xsl:message>
        </xsl:non-matching-substring>
      </xsl:analyze-string>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$typeCode = 'BX'">
        <xsl:sequence select="'BX'"/>
      </xsl:when>
      <xsl:when test="$typeCode = 'BK' or $context//*[df:class(., 'topic/p') and @outputclass = 'BookTitle']">
        <xsl:sequence select="'BK'"/>
      </xsl:when>      
      <xsl:otherwise>
        <xsl:sequence select="$typeCode"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
</xsl:stylesheet>
