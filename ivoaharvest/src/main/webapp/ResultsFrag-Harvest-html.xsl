<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                version="1.0">

   <xsl:output method="html" encoding="UTF-8" />

   <xsl:param name="RIdoc">RegistryInterface4RofR.htm</xsl:param>
   <xsl:param name="OAIdoc">not_available.htm</xsl:param>
   <xsl:param name="VORdoc">not_available.htm</xsl:param>
   <xsl:param name="linkAllResources" select="false()" />

   <xsl:template match="/" xml:space="preserve">
<div id="results">

<xsl:apply-templates select="//OAIValidation" />
<xsl:apply-templates select="//HarvestValidation" />
<xsl:apply-templates select="//VOResourceValidation" />

</div>
   </xsl:template>

   <xsl:template match="*[@showStatus]" mode="tocsummary">
     <xsl:text>(</xsl:text>
     <xsl:if test="contains(@showStatus,'pass')">
        <xsl:value-of 
             select="count(*/test[@status='pass'])"/>
        <xsl:text> Successes</xsl:text>
        <xsl:if test="contains(@showStatus,'warn') or 
                      contains(@showStatus,'rec') or 
                      contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'rec')">
        <xsl:value-of 
             select="count(*/test[@status='warn'])"/>
        <xsl:text> Recommendations</xsl:text>
        <xsl:if test="contains(@showStatus,'warn') or 
                      contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'warn')">
        <xsl:value-of 
             select="count(*/test[@status='warn'])"/>
        <xsl:text> Warnings</xsl:text>
        <xsl:if test="contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'fail')">
        <xsl:value-of 
             select="count(*/test[@status='fail'])"/>
        <xsl:text> Failures</xsl:text>
     </xsl:if>
     <xsl:text>) </xsl:text> 
        
   </xsl:template>

   <xsl:template match="VOResourceValidation" mode="tocvorsummary">
     <xsl:text>(</xsl:text>
     <xsl:value-of select="count(*[number(@nfail)=0 and number(@nwarn)=0 and 
                                   number(@nrec)=0])"/>
     <xsl:text> Successes</xsl:text>
     <xsl:if test="contains(@showStatus,'warn') or 
                   contains(@showStatus,'rec') or 
                   contains(@showStatus,'fail')">
        <xsl:text>, </xsl:text>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'rec')">
        <xsl:value-of 
             select="count(*[number(@nrec)>0])"/>
        <xsl:text> with Recommendations</xsl:text>
        <xsl:if test="contains(@showStatus,'warn') or 
                      contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'warn')">
        <xsl:value-of 
             select="count(*[number(@nwarn)>0])"/>
        <xsl:text> with Warnings</xsl:text>
        <xsl:if test="contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'fail')">
        <xsl:value-of 
             select="count(*[number(@nfail)>0])"/>
        <xsl:text> with Failures</xsl:text>
     </xsl:if>
     <xsl:text>) </xsl:text> 
        
   </xsl:template>

   <xsl:template match="OAIValidation">

<div id="oairesults">

<a name="stdoai">
<h3>Standard OAI-PMH compliance</h3></a>

<p>
This series of queries tests compliance with the 
<a href="http://www.openarchives.org/">OAI-PMH standard</a>.  (This part of
validation is courtesy of the OAI Explorer by Hussein Suleman.)
</p>

      <xsl:if test="contains(@showStatus,'fail')">
<p><strong>Failed Tests</strong></p>

         <xsl:choose>
            <xsl:when test="*[test/@status='fail']">
<ul>
<xsl:apply-templates select="*[test/@status='fail']" mode="OAIstatus">
   <xsl:with-param name="status">fail</xsl:with-param>
</xsl:apply-templates>
</ul>
            </xsl:when>
            <xsl:otherwise><p><em>None found.</em></p></xsl:otherwise>
         </xsl:choose>

      </xsl:if>

      <xsl:if test="contains(@showStatus,'warn')">
<p><strong>Warnings</strong></p>

         <xsl:choose>
            <xsl:when test="*[test/@status='warn']">
<ul>
<xsl:apply-templates select="*[test/@status='warn']" mode="OAIstatus">
   <xsl:with-param name="status">warn</xsl:with-param>
</xsl:apply-templates>
</ul>
            </xsl:when>
            <xsl:otherwise><p><em>None found.</em></p></xsl:otherwise>
         </xsl:choose>

      </xsl:if>

      <xsl:if test="contains(@showStatus,'rec')">
<p><strong>Recommendations</strong></p>

         <xsl:choose>
            <xsl:when test="*[test/@status='rec']">
<ul>
<xsl:apply-templates select="*[test/@status='rec']" mode="OAIstatus">
   <xsl:with-param name="status">rec</xsl:with-param>
</xsl:apply-templates>
</ul>
            </xsl:when>
            <xsl:otherwise><p><em>None found.</em></p></xsl:otherwise>
         </xsl:choose>

      </xsl:if>

      <xsl:if test="contains(@showStatus,'pass')">
<p><strong>Passed Tests</strong></p>

         <xsl:choose>
            <xsl:when test="*[test/@status='pass']">
<ul>
<xsl:apply-templates select="*[test/@status='pass']" mode="OAIstatus">
   <xsl:with-param name="status">pass</xsl:with-param>
</xsl:apply-templates>
</ul>
            </xsl:when>
            <xsl:otherwise><p><em>None found.</em></p></xsl:otherwise>
         </xsl:choose>
      </xsl:if>
</div>
   </xsl:template>

   <xsl:template match="*[test]" mode="OAIstatus">
      <xsl:param name="status">fail</xsl:param>

      <xsl:variable name="key" select="translate(@name,' (),:/&gt;','-------')"/>

   <li> <a name="{$key}">
        <strong><xsl:value-of select="@name"/></strong></a>
        <xsl:text>: </xsl:text>
        <font size="-1">
        <a href="{concat(/*/@baseURL,'?',@options)}">
        <xsl:value-of select="concat(/*/@baseURL,'?',@options)"/></a>
        </font> <br /> 
        <ul>
        <xsl:apply-templates select="test[@status=$status]" />
        </ul> </li>

   </xsl:template>

   <xsl:template match="HarvestValidation">

<div id="ivoaresults">

<a name="ivoaoai">
<h3>IVOA Profile on OAI-PMH compliance</h3></a>

<p>
This series of queries tests compliance with the IVOA profile on the 
<a href="http://www.openarchives.org/">OAI-PMH standard</a> as specified by 
the <a href="http://www.ivoa.net/Documents/latest/RegistryInterfaces.html">
IVOA Registry Interfaces standard</a> for harvesting.  
</p>

<dl>
<xsl:apply-templates select="testQuery" mode="IVOA"/>
</dl>
</div>
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
<xsl:apply-templates select="descendant::test[@status='fail']"/>
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
<xsl:apply-templates select="descendant::test[@status='warn']"/>
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
<xsl:apply-templates select="descendant::test[@status='rec']"/>
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
<xsl:apply-templates select="descendant::test[@status='pass']"/>
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

   <xsl:template match="testQuery[@name='Identify']" mode="ivoaTestDesc">
      <xsl:text>Tests for correct self-identification of Registry, </xsl:text>
      <xsl:text>including the inclusion of the Registry's own </xsl:text>
      <xsl:text>registry.</xsl:text>
   </xsl:template>

   <xsl:template match="testQuery[@name='ListSets']" mode="ivoaTestDesc">
      <xsl:text>Checks to be sure required IVOA-defined sets are </xsl:text>
      <xsl:text>defined.   </xsl:text>
   </xsl:template>

   <xsl:template match="testQuery[@name='ListMetadataFormats']" mode="ivoaTestDesc">
      <xsl:text>Checks to be sure that the VOResource format </xsl:text>
      <xsl:text>(ivo-vor) has been defined.</xsl:text>
   </xsl:template>

   <xsl:template match="testQuery[@name='ListRecords']" mode="ivoaTestDesc">
      <xsl:text>Tests for correct export of VORecords, </xsl:text>
      <xsl:text>This includes checking for required records as well </xsl:text>
      <xsl:text>as checking the validity of each individual record.</xsl:text>
   </xsl:template>

   <xsl:template match="VOResourceValidation">
<div id="vorresults">
<a name="vorstd">
<h3>VOResource Compliance</h3></a>

<p>
Each record returned by ListRecords is checked for compliance with the 
<a href="http://www.ivoa.net/Documents/latest/VOResource.html">VOResource
standard</a> and its recognized extensions.  These have been extracted
from the OAI responses and checked as stand-alone XML documents. 
</p>

<p>
Follow the links to see detailed descriptions of results.  
</p>

<table border="2" cellpadding="2">
  <tr>
    <th align="left">IVOA Identifier</th><th>status</th>
    <xsl:if test="contains(@showStatus,'fail')"><th>Failed Tests</th></xsl:if>
    <xsl:if test="contains(@showStatus,'warn')"><th>Warnings</th></xsl:if>
    <xsl:if test="contains(@showStatus,'rec')"><th>Recommendations</th></xsl:if>
    <xsl:if test="contains(@showStatus,'pass')"><th>Passed Tests</th></xsl:if>
  </tr>
  <xsl:apply-templates select="testQuery[number(@nfail)>0]" mode="VORsummary"/>
  <xsl:apply-templates select="testQuery[number(@nfail)=0 and number(@nwarn)>0]"
                       mode="VORsummary"/>
  <xsl:apply-templates select="testQuery[number(@nfail)=0 and 
                                         number(@nwarn)=0 and number(@nrec)>0]"
                       mode="VORsummary"/>
  <xsl:apply-templates select="testQuery[number(@nfail)=0 and 
                                         number(@nwarn)=0 and number(@nrec)=0]"
                       mode="VORsummary"/>
</table>

<xsl:if test="testQuery[test]">
<h4>Results from a Sampling of Records</h4>

<dl>
<xsl:apply-templates select="testQuery[test]" mode="VOR"/>
</dl>

</xsl:if>
</div>
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

   <dt> <a name="{$key}">
        <strong>IVOA ID: </strong> <xsl:value-of select="@ivo-id"/></a></dt>
   <dd> <strong>Status: </strong> 
        <xsl:value-of select="@status"/><br />
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

</xsl:stylesheet>
