<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      xmlns:local="urn:local-functions"
      xmlns:jpdl="urn:jbpm.org:jpdl-3.1"
      exclude-result-prefixes="xs local"
      version="2.0">
  
  <xsl:key name="nodeByName" match="*[@name and (not(self::jpdl:transition) and not(self::jpdl:task))]" use="@name"/>
  
  <!-- Transform GPD workflow diagram geometry files into
       RSuite Workflow Designer geometry files.
  -->
  
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>
  
  <xsl:template match="root-container">
    <xsl:variable name="procDefDoc" as="document-node()"
      select="local:getProcDefDoc(.)"
    />
    <WorkflowProcess>
      <Activities>
        <xsl:apply-templates mode="activities">
          <xsl:with-param name="procDefDoc" select="$procDefDoc" tunnel="yes"/>
        </xsl:apply-templates>
      </Activities>
      <Transitions>
        <xsl:apply-templates mode="transitions">
          <xsl:with-param name="procDefDoc" select="$procDefDoc" tunnel="yes"/>
        </xsl:apply-templates>
      </Transitions>
    </WorkflowProcess>
  </xsl:template>
  
  <xsl:template mode="activities" match="node">
    <xsl:param name="procDefDoc" tunnel="yes" as="document-node()"/>
    <xsl:variable name="xCoord" as="xs:string"
      select="@x"
    />
    <xsl:variable name="yCoord" as="xs:string"
      select="@y"
    />
    <xsl:variable name="width" as="xs:string"
      select="@width"
    />
    <xsl:variable name="height" as="xs:string"
      select="@height"
    />
    <xsl:variable name="nodeId" as="xs:string"
      select="local:getNodeId(., $procDefDoc)"
      />
    <Activity
      id="{$nodeId}"
      name="{@name}"
      type="{local:getNodeType(., $procDefDoc)}"
      xCoordinate="{$xCoord}"
      yCoordinate="{$yCoord}"
      width="{$width}"
      height="{$height}"/>
  </xsl:template>
  
  <xsl:template mode="transitions" match="node">
    <xsl:param name="procDefDoc" tunnel="yes" as="document-node()"/>
    <xsl:variable name="defNode" select="local:getNodeWithName($procDefDoc, string(@name))" as="element()?"/>
    <xsl:apply-templates select="$defNode/jpdl:transition" mode="#current"/>
  </xsl:template>
  
  <xsl:template match="jpdl:transition" mode="transitions">
    <xsl:param name="procDefDoc" tunnel="yes" as="document-node()"/>
    <xsl:variable name="targetNode" as="element()?"
      select="local:getNodeWithName($procDefDoc, string(@to))"
    />
    <Transition
      id="{count(preceding::jpdl:transition) + 1000}"
      name="{@name}"
      from="{local:getNodeId(.., $procDefDoc)}"
      to="{local:getNodeId($targetNode, $procDefDoc)}"
    />
  </xsl:template>
  
  
  <xsl:function name="local:getNodeWithName" as="element()?">
    <xsl:param name="procDefDoc" as="document-node()"/>
    <xsl:param name="name" as="xs:string"/>
    <xsl:variable name="node" select="key('nodeByName', $name, $procDefDoc)"/>
    <xsl:sequence select="$node"/>
  </xsl:function>
  
  <xsl:function name="local:getProcDefDoc" as="document-node()">
    <xsl:param name="context" as="element()"/>
    <xsl:variable name="procName" select="@name" as="xs:string"/>
    <xsl:variable name="procDefDoc" as="document-node()"
      select="document(concat($context/@name, '.xml'), $context)"
    />
    <xsl:sequence select="$procDefDoc"/>
  </xsl:function>
  
  <xsl:function name="local:getNodeId" 
    as="xs:string"
    >
    <xsl:param name="context" as="element()"/>
    <xsl:param name="procDefDoc" as="document-node()"/>
    <xsl:variable name="node" select="local:getNodeWithName($procDefDoc, string($context/@name))" as="element()?"/>
    <xsl:sequence select="string(count($node/preceding-sibling::*) + 1)"/>
  </xsl:function>
  
  <xsl:function name="local:getNodeType" 
    >
    <xsl:param name="context" as="element()"/>
    <xsl:param name="procDefDoc" as="document-node()"/>
    <!-- Look up node in workflow process definition document and get the node type -->
    <xsl:variable name="nodeName" select="string($context/@name)" as="xs:string"/>
    <xsl:variable name="node" as="element()?" select="local:getNodeWithName($procDefDoc, $nodeName)"/>
    <xsl:if test="count($node) = 0">
      <xsl:message terminate="yes"> + [ERROR] Failed to find node with name "<xsl:sequence select="$nodeName"/>"</xsl:message>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="$node/self::jpdl:swimlane">
        <xsl:sequence select="'SWIMLANE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:start-state">
        <xsl:sequence select="'START_NODE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:end-state">
        <xsl:sequence select="'END_NODE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:node">
        <xsl:sequence select="'NODE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:task-node">
        <xsl:sequence select="'TASK_NODE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:join">
        <xsl:sequence select="'JOIN_NODE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:fork">
        <xsl:sequence select="'FORK_NODE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:decision">
        <xsl:sequence select="'DECISION_NODE'"/>
      </xsl:when>
      <xsl:when test="$node/self::jpdl:process-state">
        <xsl:sequence select="'PROCESSSTATE_NODE'"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message> - [ERROR] Unrecognized node type <xsl:sequence select="name($node)"/>: <xsl:sequence select="$node"/></xsl:message>
        <xsl:sequence select="'UNKNOWN'"/>
      </xsl:otherwise>
    </xsl:choose>    
  </xsl:function>
  
  <xsl:template match="*" priority="-1" mode="activities">
    <xsl:message> + [WARNING] Mode activities: Unhandled element <xsl:sequence select="name(..)"/>/<xsl:sequence select="name(.)"/></xsl:message>
  </xsl:template>

  <xsl:template match="*" priority="-1" mode="transitions">
    <xsl:message> + [WARNING] Mode transitions: Unhandled element <xsl:sequence select="name(..)"/>/<xsl:sequence select="name(.)"/></xsl:message>
  </xsl:template>
  
  <xsl:template match="*" priority="-1" mode="#default">
    <xsl:message> + [WARNING] Default mode: Unhandled element <xsl:sequence select="name(..)"/>/<xsl:sequence select="name(.)"/></xsl:message>
  </xsl:template>
  
</xsl:stylesheet>
