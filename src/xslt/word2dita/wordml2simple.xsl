<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      xmlns:mv="urn:schemas-microsoft-com:mac:vml"
      xmlns:mo="http://schemas.microsoft.com/office/mac/office/2008/main"
      xmlns:ve="http://schemas.openxmlformats.org/markup-compatibility/2006"
      xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
      xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
      xmlns:v="urn:schemas-microsoft-com:vml"
      xmlns:w10="urn:schemas-microsoft-com:office:word"
      xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
      xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml"
      xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
      xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"
      xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
      xmlns:rels="http://schemas.openxmlformats.org/package/2006/relationships"
      
      xmlns:local="urn:local-functions"
      
      xmlns:saxon="http://saxon.sf.net/"
      xmlns:rsiwp="http://reallysi.com/namespaces/generic-wordprocessing-xml"
      xmlns:stylemap="urn:public:dita4publishers.org:namespaces:word2dita:style2tagmap"
      xmlns:relpath="http://dita2indesign/functions/relpath"
      xmlns="http://reallysi.com/namespaces/generic-wordprocessing-xml"
      
      exclude-result-prefixes="a pic xs mv mo ve o r m v w10 w wne wp local relpath saxon"
      version="2.0">
  
  
<!-- added this override to correct the bug in handling nested <w:smartTag> -->
	<xsl:template name="handlePara">
		<xsl:param name="doDebug" as="xs:boolean" tunnel="yes" select="false()"/>
		<xsl:param name="styleId" as="xs:string"/>
		<xsl:param name="styleData" as="element()"/>
		
		<xsl:if test="$styleData/self::stylemap:characterStyle">
			<xsl:message> - [WARN] Paragraph style "<xsl:value-of select="$styleId"/>" is a paragraph style but 
				is mapped using a <xsl:value-of select="local-name($styleData)"/> element.</xsl:message>
		</xsl:if>
		<!-- If this paragraph generates a topic, then its tagName
           will be the tagname for the topic title, so we want
           that on the paragraph itself.
        -->
		<p style="{$styleId}" wordLocation="{saxon:path()}">
			<xsl:sequence select="local:getTagnameFromNestedProperties($styleData)"/>
			<xsl:for-each select="$styleData/@*">
				<xsl:copy/>
			</xsl:for-each>
			<xsl:if test="not($styleData/@topicZone)">
				<xsl:attribute name="topicZone" 
					select="'body'"
				/>
			</xsl:if>
			<!-- Set attributes on the paragraph indicating if it generates a map, topic, or topicref.
           These attributes make subsequent grouping logic easier. 
        -->
			<xsl:apply-templates select="$styleData/*" mode="set-map-structure-atts"/>
			<!-- Copy any child elements to the paragraph -->
			<xsl:if test="$doDebug">
				<xsl:message> + [DEBUG] handlePara: styleData=<xsl:sequence select="$styleData"/></xsl:message>
			</xsl:if>
			<xsl:sequence select="$styleData/stylemap:*"/>
			<xsl:if test="$doDebug">        
				<xsl:message> + [DEBUG] handlePara: p="<xsl:sequence select="substring(normalize-space(.), 1, 40)"/>"</xsl:message>
			</xsl:if>
			<!-- FIXME: This code is not doing anything specific with smartTag elements, just
                  processing their children. Doing something intelligent with smartTags
                  would require additional logic.
                  
                  WEK: Not sure why I've used this for-each-group logic, but I think it's because Word
                  doesn't require the elements specific to a given kind of thing to occur in sequence.
                  
                  But it seems like it ought to be possible to handle these elements using normal
                  apply-templates.
        -->
			<xsl:for-each-group 
				select="*" 
				group-adjacent="name(.)">
				<xsl:if test="$doDebug">
					<xsl:message> + [DEBUG] handlePara: current-group()[1]=<xsl:sequence select="current-group()[1]"/></xsl:message>
				</xsl:if>
				<xsl:choose>
					<!-- removed footnote/endnote conditions due to dropped-text bug when text() node follows footnote/endnote -->
	<?remove				<xsl:when test="current-group()[1][self::w:r/w:endnoteReference]">
						<xsl:if test="$doDebug">
							<xsl:message> + [DEBUG] handlePara: handling w:r/w:endnoteReference</xsl:message>
						</xsl:if>
						<xsl:call-template name="handleEndNoteRef">
							<xsl:with-param name="runSequence" select="current-group()"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="current-group()[1][self::w:r[w:footnoteReference]]">
						<xsl:if test="$doDebug">
							<xsl:message> + [DEBUG] handlePara: handling w:r/w:footnoteReference</xsl:message>
						</xsl:if>
						<xsl:call-template name="handleFootNoteRef">
							<xsl:with-param name="runSequence" select="current-group()"/>
						</xsl:call-template>
					</xsl:when>?>
					<xsl:when test="current-group()[1][self::w:r]">
						<xsl:for-each-group select="current-group()" group-adjacent="local:getRunStyleId(.)">
							<xsl:call-template name="handleRunSequence">
								<xsl:with-param name="doDebug" as="xs:boolean" tunnel="yes" select="$doDebug"/>
								<xsl:with-param name="runSequence" select="current-group()"/>
							</xsl:call-template>
						</xsl:for-each-group>            
					</xsl:when>
					<xsl:when test="current-group()[1][self::w:smartTag]">
						<xsl:if test="$doDebug">
							<xsl:message> + [DEBUG] handlePara: *** got a w:smartTag. current-group=<xsl:sequence select="current-group()"/></xsl:message>
						</xsl:if>     
						<xsl:for-each select="current-group()">
							<!-- process all <w:r> elements on the decdendant axis in order to make sure nested <w:smartTag> elements are not skipped -->
							<xsl:for-each select="descendant::w:r">
								<xsl:call-template name="handleRunSequence">
									<xsl:with-param name="runSequence" select="."/>
								</xsl:call-template>    
							</xsl:for-each>							
						<!--	<xsl:call-template name="handleRunSequence">
								<xsl:with-param name="doDebug" as="xs:boolean" tunnel="yes" select="$doDebug"/>
								<xsl:with-param name="runSequence" select="w:r"/>
							</xsl:call-template>    -->          
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates select="current-group()"/><!-- default, just handle normally -->
					</xsl:otherwise>
				</xsl:choose>				
			</xsl:for-each-group>
		</p>		
	</xsl:template>	
  
  <?remove
	<xsl:template match="w:hyperlink">
		<xsl:param name="doDebug" as="xs:boolean" tunnel="yes" select="true()"/>
		<xsl:param name="relsDoc" as="document-node()?" tunnel="yes"/>
		
		<xsl:message>--------%%%%%%%%%%%%%%%%%%%%%%%###################################################################</xsl:message>
		<xsl:variable name="runStyle" select="local:getHyperlinkStyle(.)" as="xs:string"/>
		<xsl:message>runStyle: <xsl:value-of select="$runStyle"/></xsl:message>
		<xsl:variable name="styleMapByName" as="element()?"
			select="key('styleMapsByName', lower-case($runStyle), $styleMapDoc)[1]"
		/>
		<xsl:message>styleMapByName: <xsl:value-of select="$styleMapByName"/></xsl:message>
		<xsl:variable name="styleMapById" as="element()?"
			select="key('styleMapsById', $runStyle, $styleMapDoc)[1]"
		/>
		<xsl:message>styleMapById: <xsl:value-of select="$styleMapById"/></xsl:message>
		<xsl:variable name="runStyleMap" as="element()?"
			select="($styleMapByName, $styleMapById)[1]"
		/>
		<xsl:message>runStyleMap: <xsl:value-of select="$runStyleMap"/></xsl:message>
		<xsl:variable name="runStyleData" as="element()">
			<xsl:choose>
				<xsl:when test="$runStyleMap">
					<xsl:sequence select="$runStyleMap"/>
				</xsl:when>
				<xsl:otherwise>
					<stylemap:characterStyle styleId="Hyperlink"
						structureType="xref"
						tagName="xref"
					/>          
					
				</xsl:otherwise>
			</xsl:choose>			
		</xsl:variable>
		<xsl:message>runStyleData: <xsl:value-of select="$runStyleData"/></xsl:message>		
		<xsl:variable name="rel" as="element()?"
			select="key('relsById', @r:id, $relsDoc)"
		/>
		<!-- if there is to @Target, then the
         hyperlink is either to an external URI or
         to an internal bookmark
      -->
		<xsl:variable name="href" as="xs:string"
			select="
			if ($rel)
			then string($rel/@Target)
			else
			if (matches(@href, '^\w+:'))
			then @href
			else string(@w:anchor)
			"  
		/>
		<hyperlink href="{$href}"
			>
			<xsl:for-each select="$runStyleData/@*">
				<xsl:copy/>
			</xsl:for-each>
			<xsl:if test="$doDebug">
				<xsl:message> + [DEBUG] hyperlink: applying templates to first run...</xsl:message>
			</xsl:if>
			<xsl:apply-templates select="w:r[1]"/>
			<xsl:if test="count(w:r) > 1">
				<xsl:if test="$doDebug">
					<xsl:message> + [DEBUG] hyperlink: More runs, calling handleRunSequence on grouped runs...</xsl:message>
				</xsl:if>
				<xsl:for-each-group select="w:r[position() > 1]" group-adjacent="local:getRunStyleId(.)">
					<xsl:if test="$doDebug">
						<xsl:message> + [DEBUG] hyperlink: grouping key="<xsl:value-of select="current-grouping-key()"/>"</xsl:message>
					</xsl:if>
					<xsl:call-template name="handleRunSequence">
						<xsl:with-param name="doDebug" as="xs:boolean" tunnel="yes" select="$doDebug"/>
						<xsl:with-param name="runSequence" select="current-group()"/>
					</xsl:call-template>
				</xsl:for-each-group>            
			</xsl:if>
		</hyperlink>
	</xsl:template>?>
  
	<xsl:function name="local:constructSymbolForCharcode" as="node()*">
		<xsl:param name="charCode" as="xs:string"/>
		<xsl:param name="fontFace" as="xs:string"/>
		<!-- See 17.3.3.30 sym (Symbol Character) in the Office Open XML Part 1 Doc:
      
         Characters with codes starting with "F" have had 0xF000 added to them
         to put them in the private use area.
      -->
		<xsl:variable name="nonPrivateCharCode" as="xs:string"
			select="if (starts-with($charCode, 'F')) 
			then concat('0', substring($charCode, 2))
			else $charCode"
		/>
		<!-- getUnicodeForFont() will return the literal "?" character's code point if
         there is no mapping found for the symbol.
      -->
		<xsl:variable name="unicodeCodePoint" as="xs:string"
			select="local:getUnicodeForFont(string($fontFace), $nonPrivateCharCode)"
		/>
		<xsl:variable name="codePoint" as="xs:integer"
			select="local:hex-to-char($unicodeCodePoint)"
		/>
		<xsl:variable name="character" 
			select="codepoints-to-string($codePoint)" as="xs:string"/>
		<rsiwp:symbol font="{$fontFace}"
			><xsl:sequence select="$character"/></rsiwp:symbol>
		
	</xsl:function>
	
	<xsl:function name="local:getUnicodeForFont" as="xs:string">
		<xsl:param name="fontName" as="xs:string"/>
		<xsl:param name="fontCodePoint" as="xs:string"/>
		<!--    <xsl:message> + [DEBUG] getUnicodeForFont(): fontName="<xsl:value-of select="$fontName"/>"</xsl:message>
    <xsl:message> + [DEBUG] getUnicodeForFont(): fontCodePoint="<xsl:value-of select="$fontCodePoint"/>"</xsl:message>
-->    
		<xsl:variable name="fontCharMap" as="element()?"
			select="$font2UnicodeMaps/*[lower-case(@sourceFont) = lower-case($fontName)]"
		/>
		
		<xsl:choose>
			<xsl:when test="not($fontCharMap)">
				<xsl:message> - [WARN] getUnicodeForFont(): No font-to-character map found for font "<xsl:value-of select="$fontName"/>"</xsl:message>
				<xsl:sequence select="'003F'"></xsl:sequence>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="unicodeCodePoint" as="xs:string?">
					<xsl:variable name="codePointMapping" as="element()?"
						select="$fontCharMap/codePointMapping[@origCodePoint = $fontCodePoint]"
					/>
					
					<xsl:variable name="unicodeCodePoint" select="$codePointMapping/@unicodeCodePoint" as="xs:string"/>
					<!--          <xsl:message> + [DEBUG] getUnicodeForFont():   codePointMapping=<xsl:sequence select="$codePointMapping"/></xsl:message>     -->
					<!--          <xsl:message> + [DEBUG] getUnicodeForFont():   unicodeCodePoint=<xsl:sequence select="$unicodeCodePoint"/></xsl:message>-->
					<xsl:sequence 
						select="$unicodeCodePoint"/>
				</xsl:variable>
				<xsl:sequence select="$unicodeCodePoint"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
  
	<xsl:template match="w:tbl">
		<xsl:param name="doDebug" as="xs:boolean" tunnel="yes" select="false()"/>
		<xsl:variable name="styleData" as="element()">
			<stylemap:paragraphStyle styleId="table"
				structureType="block"
				tagName="table"
				topicZone="body"
			/>                
		</xsl:variable>
		<!--  NOTE: width values are 1/20 of a point -->
		<table>
			<xsl:attribute name="table_style_name" select="w:tblPr/w:tblStyle/@w:val"/>
			<xsl:attribute name="frame" select="local:constructFrameValue(w:tblPr/w:tblBorders)"/>
			<xsl:attribute name="calculatedWidth" select="local:calculateTableActualWidth(w:tblGrid)"/>
			<!-- Construct rowsep and colsep values as appropriate: -->
			<xsl:apply-templates select="w:tblPr/w:tblBorders" mode="table-attributes"/>
			<xsl:for-each select="$styleData/@*">
				<xsl:copy/>
			</xsl:for-each>
			<xsl:apply-templates select="w:tblPr/*"/>
			<xsl:if test="w:tblGrid">
				<cols>
					<xsl:for-each select="w:tblGrid/w:gridCol">
						<xsl:variable name="widthValPoints" as="xs:string"
							select="
							if (string(number(@w:w)) = 'NaN') 
							then @w:w 
							else concat(format-number((number(@w:w) div 20), '######.00'), 'pt')"
						/>
						<col colwidth="{$widthValPoints}"/>
					</xsl:for-each>
				</cols>
			</xsl:if>
			<xsl:apply-templates select="*[not(self::w:tblPr)]"/>
		</table>
	</xsl:template>
  
</xsl:stylesheet>
