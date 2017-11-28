<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:RSUITE="http://www.reallysi.com"
  xmlns:rsf="http://www.reallysi.com/xslt/functions"
  xmlns:df="http://dita2indesign.org/dita/functions"
  xmlns:r="http://www.rsuitecms.com/rsuite/ns/metadata"
  xmlns:relpath="http://dita2indesign/functions/relpath"
  exclude-result-prefixes="xs RSUITE rsf df r relpath"
  version="2.0">
  <!-- RSuite content assembly to DITA map.
    
       Transforms a content assembly and its children into
       a DITA map that references each of the MOs
       in the content assembly.

       This is the base implementation. This module must
       be included or imported from a top-level XSLT that defines
       an xsl:output element that specifies the desired
       result map public and system ID values.
  -->
  
  
  <xsl:import href="rsuite:/res/plugin/dita4publishers/toolkit_plugins/org.dita-community.common.xslt/xsl/relpath_util.xsl"/>
  <xsl:import href="rsuite:/res/plugin/dita4publishers/toolkit_plugins/org.dita-community.common.xslt/xsl/dita-support-lib.xsl"/>
  
  <!-- Set this when generating output outside of RSuite so
       references will resolve.
    -->
  <xsl:param name="isOnRSuiteServer" select="'true'" as="xs:string"/>  
  <!-- If there's no session key, must be on server, otherwise, construct normal REST URLs -->
  <xsl:variable name="isOnRSuiteServerBoolean" as="xs:boolean"
    select="$rsuiteSessionKey = ''"
  />
  <xsl:param name="rsuiteInternalUrl" select="'rsuite:/res/'" as="xs:string"/>
  <xsl:param name="rsuiteServerUrl" select="'/rsuite/rest/v2/content/'" as="xs:string"/>
  <xsl:param name="rsuiteSessionKey" select="'SESSIONKEY'" as="xs:string"/>
  <xsl:param name="rsuiteHost" select="'localhost'" as="xs:string"/>
  <xsl:param name="rsuitePort" select="'8080'" as="xs:string"/>
  
  <xsl:param name="rsuiteRestV1UrlBase" select="'/rsuite/rest/v1/'" as="xs:string"/>
  <xsl:param name="rsuiteRestV2UrlBase" select="'/rsuite/rest/v2/'" as="xs:string"/>
  
  <xsl:variable name="rsuiteUrlScheme" as="xs:string"
    select="if ($isOnRSuiteServerBoolean) 
               then 'rsuite:' 
               else 'http://'
               "
  />
  <xsl:variable name="rsuiteUrlServer" as="xs:string"
    select="if ($isOnRSuiteServerBoolean) 
               then ''
               else concat($rsuiteHost, ':', $rsuitePort)
               "
  />
  
  <xsl:variable name="rsuiteUrlSchemeAndServer"
    select="concat($rsuiteUrlScheme, 
                   $rsuiteUrlServer)"
  />
  
  <xsl:template match="/">
    <xsl:call-template name="report-parameters"/>
    <xsl:apply-templates/>
  </xsl:template>
  
  <xsl:template match="/rs_ca | /rs_canode" priority="15">
    <map>
      <title><xsl:value-of select="rsf:getMoDisplayName(.)"/></title>
      <topicmeta>
        <data name="source-ca-id" value="{rsf:getMoId(.)}"/>
      </topicmeta>
      <topicgroup>
        <xsl:apply-templates/>        
      </topicgroup>
    </map>    
  </xsl:template>
  
  <xsl:template match="rs_canode" priority="5" name="container2topichead">
    <topichead>
      <topicmeta>
        <navtitle><xsl:value-of select="rsf:getMoDisplayName(.)"/></navtitle>
      </topicmeta>
      <xsl:apply-templates/>
    </topichead>
  </xsl:template>
  
  <xsl:template match="rs_caref" priority="10">    
    
    <xsl:variable name="moid" select="rsf:getTargetMoId(.)" as="xs:string"/>
    <xsl:variable name="sourceUri" select="rsf:constructAuthenticatedRSuiteContentUrl($moid)" as="xs:string"/>
    <xsl:variable name="sourceDoc" select="document($sourceUri, .)" as="document-node()?"/>
    <xsl:choose>
      <xsl:when test="not($sourceDoc)">
        <xsl:message> - [WARN] Failed to resolve URI "<xsl:sequence select="$sourceUri"/>" for MO [<xsl:sequence select="$moid"/>] to a document.</xsl:message>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="$sourceDoc/*" mode="subca"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template mode="subca" match="rs_ca">
    <xsl:call-template name="container2topichead"/>
  </xsl:template>
  
  <xsl:template match="rs_moref" priority="10">    
    
    <xsl:call-template name="constructTopicrefToTopic">
      <xsl:with-param name="topicrefTagname" select="'topicref'"/>
      <xsl:with-param name="moid" select="rsf:getTargetMoId(.)"/>
    </xsl:call-template>  
  </xsl:template>
  
  <xsl:template name="constructTopicrefToTopic">    
    <xsl:param name="topicrefTagname" as="xs:string"/>
    <xsl:param name="moid" as="xs:string"/>    
    <xsl:param name="refType" as="xs:string" tunnel="yes" select="'topicref'"/>
    
    <xsl:variable name="moInfo" as="document-node()?"
      select="rsf:getMoInfoForMoId($moid)"
    />
    <xsl:if test="false()">
      <xsl:message> + [DEBUG] mo info="<xsl:sequence select="$moInfo"/>"</xsl:message>
    </xsl:if>
    <!--
      
      FIXME: We should really be generating key definitions for each target MO
      or submap and then generating references to the keys to define the
      navigation structure. Will leave that until the Oxygen applet integration
      has support for keys.
    -->
    
    <xsl:variable name="objectType" as="xs:string"
      select="string($moInfo/rsuite-metadata/objectType)"
    />
    <xsl:choose>
      <xsl:when test="$objectType = 'mo'">
        <xsl:variable name="ditaclass" as="xs:string" select="rsf:getDitaClassForMoId($moid)"/>
        <!-- This code assumes that all XML MOs are either topics or maps. This is not necessarily
             true and probably need to make this check more sophisticated. For example, somebody
             could attach a non-topic subMO element to a container.
          -->
        <xsl:choose>
          <xsl:when test="contains($ditaclass, ' map/map ')">
            <xsl:message> + [INFO] Skipping DITA map managed object.</xsl:message>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="sourceUri" select="rsf:constructAuthenticatedRSuiteContentUrl($moid)" as="xs:string"/>
            <xsl:variable name="sourceDoc" select="document($sourceUri, .)" as="document-node()?"/>
            <xsl:choose>
              <xsl:when test="not($sourceDoc)">
                <xsl:message> - [WARN] Failed to resolve URI "<xsl:sequence select="$sourceUri"/>" for MO [<xsl:sequence select="$moid"/>] to a document.</xsl:message>
              </xsl:when>
              <xsl:otherwise>
                <xsl:variable name="alias" select="rsf:getAliasForManagedObject($moid)" as="xs:string?"/>
                <!-- NOTE: For hrefs from maps, just the resource path part of the URL ("/rsuite/rest/...")
                     will always be resolvable relative to the map's base URI because both the normal REST
                     API and the rsuite: scheme support the same path syntax. -->
                <xsl:element name="{$topicrefTagname}">
                  <xsl:attribute name="href" select="rsf:constructRSuiteContentResourcePath($alias, $moid)"/>
                  <xsl:attribute name="format" select="'dita'"/>
                  <xsl:apply-templates/>
                </xsl:element> 
              </xsl:otherwise>
            </xsl:choose>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$objectType = 'mononxml'">
        <xsl:message> + [INFO] Skipping non-XML managed object [<xsl:sequence select="$moid"/>]</xsl:message>
      </xsl:when>
    </xsl:choose>
    
  </xsl:template>
  
  <xsl:template match="text()"/>
  
  <xsl:template match="*" priority="-1">
    <xsl:if test="$debugBoolean">
      <xsl:message> + [DEBUG] default mode: catch-all: <xsl:sequence select="name(..)"/>/<xsl:sequence select="name(.)"/></xsl:message>
    </xsl:if>
  </xsl:template>
  
  <xsl:function name="rsf:getMoId" as="xs:string">
    <xsl:param name="mo" as="element()"/>
    <xsl:variable name="result" select="string($mo/@r:rsuiteId)" as="xs:string"/>
    <xsl:sequence select="$result"/>
  </xsl:function>
  
  <xsl:function name="rsf:getAliasForManagedObject" as="xs:string?">
    <xsl:param name="moid" as="xs:string"/>
    <xsl:variable name="moInfo" as="node()*" select="rsf:getMoInfoForMoId($moid)"/>
    <xsl:variable name="aliases" select="$moInfo/aliases/*" as="element()*"/>
    <xsl:variable name="alias" 
      select="($moInfo/rsuite-metadata/aliases/alias[@type = 'filename'], 
               $moInfo/rsuite-metadata/aliases/alias[not(@type = 'filename')])[1]"
      as="element()?"/>
    <xsl:sequence select="if ($alias) then string($alias) else ()"/>
  </xsl:function>
  
  <xsl:function name="rsf:getTargetMoId" as="xs:string">
    <xsl:param name="moref" as="element()"/>
    <xsl:variable name="result" select="string($moref/@href)" as="xs:string"/>
    <xsl:sequence select="$result"/>
  </xsl:function>
  
  <xsl:function name="rsf:constructAuthenticatedRSuiteContentUrl" as="xs:string">
    <!-- NOTE: Constructs a V1 URL so references to CAs will work (they don't 
               resolve in the V2 REST API
      -->
    <xsl:param name="moid" as="xs:string"/>
    <xsl:variable name="url" as="xs:string"
      select="if ($isOnRSuiteServerBoolean)
                 then rsf:constructAbsoluteContentUrl($moid)
                 else concat(rsf:constructAbsoluteContentUrl($moid), '?', 
                             'skey=', $rsuiteSessionKey)"
    />      
    <xsl:sequence select="$url"/>    
  </xsl:function>
  
  <xsl:function name="rsf:constructAbsoluteContentUrl">
     <xsl:param name="moid" as="xs:string"/>
     <xsl:variable name="url" as="xs:string"
       select="concat($rsuiteUrlSchemeAndServer,                          
                      rsf:constructRSuiteV1ContentResourcePath('', $moid)
       )"
     />
    <xsl:sequence select="$url"/>
  </xsl:function>
  
  <xsl:function name="rsf:constructRSuiteContentResourcePath" as="xs:string">
    <xsl:param name="alias" as="xs:string?"/>
    <xsl:param name="moid" as="xs:string"/>
    
    <xsl:variable name="path" as="xs:string"
      select="concat($rsuiteRestV2UrlBase,
                     'content/binary/',
                     if ($alias != '') 
                        then concat('alias/', $alias)
                        else concat('id/', $moid))
      "
    />
    <xsl:variable name="encodedUri" as="xs:string"
      select="relpath:encodeUri($path)"
    />
    <xsl:sequence select="$encodedUri"/>
  </xsl:function>

  <xsl:function name="rsf:constructRSuiteV1ContentResourcePath" as="xs:string">
    <xsl:param name="alias" as="xs:string?"/>
    <xsl:param name="moid" as="xs:string"/>
    
    <xsl:variable name="path" as="xs:string"
      select="concat($rsuiteRestV1UrlBase,
                     'content/',
                     if ($alias != '') 
                        then concat('alias/', $alias)
                        else $moid)
      "
    />
    <xsl:sequence select="$path"/>
  </xsl:function>

  <xsl:function name="rsf:getMoDisplayName" as="xs:string">
    <xsl:param name="moElement" as="element()"/>
    <xsl:variable name="moInfo" as="node()*"
      select="rsf:getMoInfo($moElement)"
    />
    <xsl:variable name="displayName" as="xs:string"
      select="string($moInfo/rsuite-metadata/displayName)"
    />
    <xsl:sequence select="$displayName"/>
  </xsl:function>
  
  <xsl:function name="rsf:getMoInfoForMoId" as="document-node()?">
    <xsl:param name="moid" as="xs:string"/>
    <!--       'content/info/id/',
      
      NOTE: See RS-157
      
      The content/id method returns the list of children as well
      as the other data returned by content/info/id
      
      -->
    <xsl:variable name="rsuiteUrl" as="xs:string"
      select="concat($rsuiteUrlSchemeAndServer, 
                     $rsuiteRestV2UrlBase,
                     'content/info/id/',
                     $moid,
                     if ($isOnRSuiteServerBoolean)
                        then ''
                        else concat('?', 
                             'skey=', $rsuiteSessionKey)
                    )"
    />
  <xsl:if test="true()">
    <xsl:message> + [DEBUG] rsf:getMoInfoForMoId: rsuiteUrl=
<xsl:sequence select="$rsuiteUrl"/></xsl:message>
  </xsl:if>    
    <xsl:variable name="result" as="node()*"
      select="document($rsuiteUrl)"
    />
<xsl:if test="false()">
  <xsl:message> + [DEBUG] rsf:getMoInfoForMoId: result=
<xsl:sequence select="$result"/></xsl:message>
</xsl:if>

    <xsl:sequence select="$result"/>
  </xsl:function>
  
  <xsl:function name="rsf:getMoFullInfoForMoId" as="document-node()?">
    <!-- Get the full info for the MO, that is, the details
         returned by the base content/id/{moid} REST call
      -->
    <xsl:param name="moid" as="xs:string"/>
    
    <xsl:variable name="rsuiteUrl" as="xs:string"
      select="concat($rsuiteUrlSchemeAndServer, 
                     $rsuiteRestV2UrlBase,
                     'content/id/',
                     $moid,
                     if ($isOnRSuiteServerBoolean)
                        then ''
                        else concat('?', 
                             'skey=', $rsuiteSessionKey)
                    )"
    />
  <xsl:if test="true()">
    <xsl:message> + [DEBUG] rsf:getMoInfoForMoId: rsuiteUrl=
<xsl:sequence select="$rsuiteUrl"/></xsl:message>
  </xsl:if>    
    <xsl:variable name="result" as="node()*"
      select="document($rsuiteUrl)"
    />
<xsl:if test="false()">
  <xsl:message> + [DEBUG] rsf:getMoFullInfoForMoId: result=
<xsl:sequence select="$result"/></xsl:message>
</xsl:if>

    <xsl:sequence select="$result"/>
  </xsl:function>
  
  <xsl:function name="rsf:getMoXmlForMoId" as="document-node()?">
    <xsl:param name="moid" as="xs:string"/>
    <xsl:variable name="rsuiteURL" as="xs:string"
      select="rsf:constructAuthenticatedRSuiteContentUrl($moid)"
    />
    <xsl:variable name="result" as="document-node()?"
      select="document($rsuiteURL)"
    />
    <xsl:sequence select="$result"/>
  </xsl:function>
  
  <xsl:function name="rsf:getMoInfo" as="document-node()?">
    <xsl:param name="moElement" as="element()"/>
    
    <xsl:variable name="moid" select="string($moElement/@r:rsuiteId)" as="xs:string"/>
    <xsl:sequence select="rsf:getMoInfoForMoId($moid)"/>
  </xsl:function>
  
  <xsl:function name="rsf:getMoFullInfo" as="document-node()?">
    <xsl:param name="moElement" as="element()"/>
    
    <xsl:variable name="moid" select="string($moElement/@r:rsuiteId)" as="xs:string"/>
    <xsl:sequence select="rsf:getMoFullInfoForMoId($moid)"/>
  </xsl:function>
  
  <xsl:function name="rsf:getDitaClassForMoId" as="xs:string">
    <xsl:param name="moid" as="xs:string"/>
    
    <xsl:variable name="moDoc" select="rsf:getMoXmlForMoId($moid)" as="document-node()?"/>
    <xsl:variable name="result" as="xs:string"
      select="$moDoc/*/@class"
    />
    <xsl:sequence select="$result"/>
  </xsl:function>
  
  <xsl:template name="report-parameters">
    <xsl:param name="isOnRSuiteServer" select="'true'" as="xs:string"/>
    <xsl:variable name="isOnRSuiteServerBoolean" as="xs:boolean"
      select="$isOnRSuiteServer != 'false'"
    />
    <xsl:message> + [INFO] Parameters
 + [INFO]   isOnRSuiteServer = '<xsl:sequence select="$isOnRSuiteServer"/>'
 + [INFO]   rsuiteServerUrl = '<xsl:sequence select="$rsuiteServerUrl"/>'
 + [INFO]   rsuiteSessionKey = '<xsl:sequence select="$rsuiteSessionKey"/>'
 + [INFO]   rsuiteHost = '<xsl:sequence select="$rsuiteHost"/>'
 + [INFO]   rsuitePort = '<xsl:sequence select="$rsuitePort"/>'
 + [INFO]   debug           = '<xsl:sequence select="$debug"/>'
    </xsl:message>
  </xsl:template>
  
</xsl:stylesheet>
