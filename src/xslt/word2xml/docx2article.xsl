<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:local="urn:local-functions"

	xmlns:rsiwp="http://reallysi.com/namespaces/generic-wordprocessing-xml"
	xmlns:stylemap="http://reallysi.com/namespaces/style-to-tag-map"

	exclude-result-prefixes="xs rsiwp stylemap local" version="2.0">

	<!--========================================== RSI Simple Word Processing 
		XML to ASTD article XML. Copyright (c) 2009 American Society for Training 
		and Development Transforms a DOCX document.xml file into an ASTD Article 
		The input to this transform is the document.xml file within a DOCX package. 
		=========================================== -->

	<!-- NOTE: This is a reference to the transform as provided by the DITA4Publishers 
		RSuite plugin. Within OxygenXML, you can use an XML entity catalog to remap 
		this URI to the location in your local dev environment. -->
	
	<xsl:import href="rsuite:/res/plugin/dita4publishers/toolkit_plugins/org.dita4publishers.word2dita/xsl/docx2dita.xsl"/>


	<xsl:output doctype-public="urn:pubid:astd.com/doctypes/dita/article"
		doctype-system="article.dtd" />

	<xsl:template match="rsiwp:p[@styleId = 'AuthorBio' or @styleId ='bio']"
		priority="10">
		<astd-author>
		<!-- 	<xsl:call-template name="transformParaContent" /> -->
			<author-bio-para>
				<xsl:apply-templates mode="p-content">
					<xsl:with-param name="inTitleContext" as="xs:boolean"
						tunnel="yes" select="false()" />
				</xsl:apply-templates>
			</author-bio-para>
		</astd-author>
	</xsl:template>

	<xsl:template match="rsiwp:run[@styleId = 'authorAffiliationItalic']"
		mode="p-content">
		<xsl:variable name="tagName" as="xs:string"
			select="
      if (@tagName) 
      then string(@tagName)
      else 'ph'
      " />
		<xsl:if test="not(./@tagName)">
			<xsl:message>
				+ [WARNING] No style to tag mapping for character style "
				<xsl:sequence select="string(@style)" />
				"
			</xsl:message>
		</xsl:if>
		<xsl:element name="{$tagName}">
			<xsl:attribute name="xtrc" select="@wordLocation" />
			<xsl:if test="@outputclass">
				<xsl:attribute name="outputclass" select="string(@outputclass)" />
			</xsl:if>
			<i>
				<xsl:apply-templates mode="#current" />
			</i>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>
