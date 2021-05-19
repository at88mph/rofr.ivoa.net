<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                version="1.0">

   <xsl:output method="xml" encoding="UTF-8" />

   <xsl:param name="RIdoc">RegistryInterface4RofR.htm</xsl:param>
   <xsl:param name="OAIdoc">not_available.htm</xsl:param>
   <xsl:param name="VORdoc">not_available.htm</xsl:param>
   <xsl:param name="linkAllResources" select="false()" />

   <xsl:template match="/" xml:space="preserve">
<html> <head>
  <title>Registry Harvesting Validater</title>

  <style type="text/css">
<xsl:comment>
.tiny {FONT-SIZE: 7pt;}
</xsl:comment>
  </style>
  <link href="ivoa_rofr.css" rel="stylesheet" type="text/css"/>
  <link href="tp.css" rel="stylesheet" type="text/css"/>
</head>
<body>

<center>
<table width="100%">
  <tr>
    <td>
      <font class="titleText"><b>I</b>nternational
      <span style="visibility: hidden">i</span>
      <b>V</b>irtual 
      <span style="visibility: hidden">i</span>
      <b>O</b>bservatory 
      <span style="visibility: hidden">i</span><b>A</b>lliance</font><br />
      <font class="titleText" style="font-size: 18pt; font-weight: 600">
      <a name="rofr" title="IVOA Registry of Registries" 
         class="titleText">IVOA Registry of Registries</a>
      </font><br /><br />

      <table cellspacing="0" cellpadding="0" border="0" width="100%">
        <tr>
          <!-- the local links -->
          <td class="rollcall"><a href="http://www.ivoa.net/Documents/latest/RegistryInterface.html">Registry Interfaces Spec.</a></td>
          <td class="rollcall"><a href="http://www.openarchives.org/OAI/openarchivesprotocol.html">OAI-PMH Spec.</a></td>
          <td class="rollcall"><a href="http://www.ivoa.net/Documents/latest/VOResource.html">VOResource Spec.</a></td>
        </tr>
      </table>
    </td>
    <td>
      <a href="/"><img src="IVOA_wb_300.jpg" width="150" 
         height="85" border="0" alt="ivoa.net" /></a>
    </td>
  </tr>
</table>
</center>

<xsl:comment> =======================================================================
  -  Page Content
  -  ======================================================================= </xsl:comment>

<div id="results">

    <xsl:apply-templates select="//VOResourceValidation"/>

</div>
<xsl:comment> =======================================================================
  -  End Page Content
  -  ======================================================================= </xsl:comment>

</body>
</html>
   </xsl:template>

   <xsl:template match="VOResourceValidation" xml:space="preserve">

<h1>VOResource Record Validation Results</h1>

<xsl:apply-templates select="testQuery" mode="VOR"/>
     
   </xsl:template>

   <xsl:template match="testQuery" mode="IVOA">
   <dt> <a name="{@name}">
        <strong><xsl:value-of select="@name"/></strong></a> </dt>
   <dd> <strong>URL: </strong> 
        <a href="{concat(../@baseURL,'?',@options)}">
        <xsl:value-of select="concat(../@baseURL,'?',@options)"/></a> <br />
        <strong>Description: </strong> 
        <xsl:apply-templates select="." mode="ivoaTestDesc"/> <br />
        <xsl:if test="contains(../@showStatus,'fail')">
        <strong>Failed Tests: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='fail']">
<ul>
<xsl:apply-templates select="test[@status='fail']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(../@showStatus,'warn')">
        <strong>Warnings: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='warn']">
<ul>
<xsl:apply-templates select="test[@status='warn']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(../@showStatus,'rec')">
        <strong>Recommendations: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='rec']">
<ul>
<xsl:apply-templates select="test[@status='rec']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(../@showStatus,'pass')">
        <strong>Passed Tests: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='pass']">
<ul>
<xsl:apply-templates select="test[@status='pass']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
   <br />
</dd>   
   </xsl:template>

   <xsl:template match="test">
      <xsl:param name="stddoc">
         <xsl:choose>
            <xsl:when test="starts-with(@item,'RI')">
               <xsl:value-of select="$RIdoc"/>
            </xsl:when>
            <xsl:when test="starts-with(@item,'VR')">
               <xsl:value-of select="$VORdoc"/>
            </xsl:when>
            <xsl:when test="starts-with(@item,'OAI')">
               <xsl:value-of select="$OAIdoc"/>
            </xsl:when>
            <xsl:otherwise>not_available.htm</xsl:otherwise>
         </xsl:choose>
      </xsl:param>

   <li> <a href="{concat($stddoc,'#',translate(@item,'.','_'))}"><xsl:value-of select="@item"/>:</a>
        <xsl:text> </xsl:text><xsl:value-of select="."/></li>
   </xsl:template>

   <xsl:template match="testQuery" mode="VORsummary">
      <xsl:variable name="key" 
                    select="translate(@ivo-id,' (),:/&gt;','-------')"/>
   <tr>
     <td align="left">
       <xsl:choose>
         <xsl:when test="test">
           <a href="#{$key}"><xsl:value-of select="@ivo-id"/></a>
         </xsl:when>
         <xsl:otherwise><xsl:value-of select="@ivo-id"/></xsl:otherwise>
       </xsl:choose>
     </td>
     <td><xsl:value-of select="@status"/></td>
     <xsl:if test="contains(../@showStatus,'fail')">
     <td><xsl:value-of select="@nfail"/></td>
     </xsl:if>
     <xsl:if test="contains(../@showStatus,'warn')">
     <td><xsl:value-of select="@nwarn"/></td>
     </xsl:if>
     <xsl:if test="contains(../@showStatus,'rec')">
     <td><xsl:value-of select="@nrec"/></td>
     </xsl:if>
     <xsl:if test="contains(../@showStatus,'pass')">
     <td><xsl:if test="test[@status='pass']">
         <xsl:value-of select="count(test[@status='pass'])"/>
         </xsl:if></td>
     </xsl:if>
   </tr>
   </xsl:template>

   <xsl:template match="testQuery" mode="VOR">
      <xsl:variable name="key" 
                    select="translate(@ivo-id,' (),:/&gt;','-------')"/>
      <xsl:variable name="recname">
      </xsl:variable>

   <xsl:text>
</xsl:text>
   <h3>
     <xsl:choose>
        <xsl:when test="starts-with(@recordName,'http://')">
           <xsl:text>URL: </xsl:text>
           <a href="{@recordName}"><xsl:value-of select="@recordName"/></a>
        </xsl:when>
        <xsl:when test="@recordName">
           <xsl:text>File: </xsl:text>
           <xsl:value-of select="@recordName"/>
        </xsl:when>
        <xsl:otherwise>
           <xsl:text>ID: </xsl:text>
           <xsl:value-of select="@ivo-id"/>
        </xsl:otherwise>
     </xsl:choose>
   </h3><xsl:text>
</xsl:text>

   <dt> <a name="{$key}">
        <strong>IVOA ID: </strong> <xsl:value-of select="@ivo-id"/></a></dt>
   <dd> <strong>Status: </strong> 
        <xsl:value-of select="@status"/><br />
        <xsl:if test="contains(../@showStatus,'fail')"><xsl:text>
</xsl:text>
        <strong>Failed Tests: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='fail']">
<ul>
<xsl:apply-templates select="test[@status='fail']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(../@showStatus,'warn')"><xsl:text>
</xsl:text>
        <strong>Warnings: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='warn']">
<ul>
<xsl:apply-templates select="test[@status='warn']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(../@showStatus,'rec')"><xsl:text>
</xsl:text>
        <strong>Recommendations: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='rec']">
<ul>
<xsl:apply-templates select="test[@status='rec']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(../@showStatus,'pass')"><xsl:text>
</xsl:text>
        <strong>Passed Tests: </strong> 
           <xsl:choose>
              <xsl:when test="descendant::test[@status='pass']">
<ul>
<xsl:apply-templates select="test[@status='pass']"/>
</ul>
              </xsl:when>
              <xsl:otherwise><em>None found.</em><br /></xsl:otherwise>
           </xsl:choose>
        </xsl:if>
   <br />
</dd>   
   </xsl:template>

</xsl:stylesheet>
