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

<div id="resultSummary">
<h3>Summary of Results </h3>

A series of test queries were sent to the service:
<dl>
      <xsl:for-each select="//OAIValidation[1]">
   <dt> <strong>Standard OAI-PMH compliance</strong> </dt>
   <dd> <xsl:apply-templates select="." mode="tocsummary"/> </dd>
      </xsl:for-each>

      <xsl:for-each select="//HarvestValidation[1]">
   <dt> <strong>IVOA Profile on OAI-PMH compliance</strong> </dt>
   <dd> <xsl:apply-templates select="." mode="tocsummary"/> </dd>
      </xsl:for-each>
      <xsl:for-each select="//VOResourceValidation[1]">
   <dt> <strong>VOResource compliance</strong> </dt>
   <dd> <xsl:apply-templates select="." mode="tocvorsummary"/> </dd>
      </xsl:for-each>
</dl>

   <!-- choose seems to be giving XSLTProcessor some bellyache -->
   <xsl:if test="*/@status='pass' and
                 */@nfail='0' and */@nwarn='0' and */@nrec='0'">
      <p>Congratulations! Your registry is perfect!</p>
   </xsl:if>
   <xsl:if test="not(*/@nfail='0' and */@nwarn='0' and */@nrec='0')
                 and */@status='pass'">
      <p>Congratulations! Your registry is minimally compliant.  Please 
      review the results below for warnings and recommendations. </p>
   </xsl:if>
   <xsl:if test="*/@status and */@status!='pass'">
      <p>Some non-compliance has been detected.  Please review
      the results below for detected problems.  Failures must be
      corrected before you can register your registry.  </p>
   </xsl:if>

<!--
<xsl:choose>
   <xsl:when test="*/@status='pass' and
                   */@nfail='0' and */@nwarn='0' and */@nrec='0'">
      <p>Congratulations! You're registry is perfect!</p>
   </xsl:when>
   <xsl:when test="*/@status='pass'">
      <p>Congratulations! You're registry is minimally compliant.  Please 
      review the results below for warnings and recommendations. </p>
   </xsl:when>
   <xsl:when test="*/@status">
      <p>Some non-compliance has been detected.  Please review the results 
      below for detected problems.</p>
   </xsl:when>
</xsl:choose>
  -->

</div>
   </xsl:template>

   <xsl:template match="*[@showStatus]" mode="tocsummary">
     <xsl:text>(</xsl:text>
     <xsl:if test="contains(@showStatus,'pass')">
        <xsl:value-of 
             select="count(*//test[@status='pass'])"/>
        <xsl:text> Successes</xsl:text>
        <xsl:if test="contains(@showStatus,'warn') or 
                      contains(@showStatus,'rec') or 
                      contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'rec')">
        <xsl:value-of 
             select="count(*//test[@status='rec'])"/>
        <xsl:text> Recommendations</xsl:text>
        <xsl:if test="contains(@showStatus,'warn') or 
                      contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'warn')">
        <xsl:variable name="n" select="count(*//test[@status='warn'])"/>
        <xsl:choose>
           <xsl:when test="$n > 0">
              <strong><xsl:value-of select="$n"/> Warnings</strong>
           </xsl:when>
           <xsl:otherwise><xsl:value-of select="$n"/> Warnings</xsl:otherwise>
        </xsl:choose>
        <xsl:if test="contains(@showStatus,'fail')">
           <xsl:text>, </xsl:text>
        </xsl:if>
     </xsl:if>
     <xsl:if test="contains(@showStatus,'fail')">
        <xsl:variable name="n" select="count(*//test[@status='fail'])"/>
        <xsl:choose>
           <xsl:when test="$n > 0">
              <strong><xsl:value-of select="$n"/> Failures</strong>
           </xsl:when>
           <xsl:otherwise><xsl:value-of select="$n"/> Failures</xsl:otherwise>
        </xsl:choose>
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

</xsl:stylesheet>
