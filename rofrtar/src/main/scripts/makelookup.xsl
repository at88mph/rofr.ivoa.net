<!--
  -  a stylesheet used by pollRegistries that extracts registry access URLs
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:vr="http://www.ivoa.net/xml/VOResource/v1.0" 
                xmlns:ri="http://www.ivoa.net/xml/RegistryInterface/v1.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="1.0">

   <xsl:output method="text"/>

   <xsl:template match="/">
      <xsl:apply-templates 
           select="ri:VOResources/ri:Resource[
                      capability[@standardID='ivo://ivoa.net/std/Registry' and
                                 contains(@xsi:type,':Search')]]" />
   </xsl:template>

   <xsl:template match="ri:Resource">
      <xsl:value-of select="identifier"/>
      <xsl:apply-templates 
           select="capability[@standardID='ivo://ivoa.net/std/Registry' and
                              contains(@xsi:type,':Search')]"/>
      <xsl:text>
</xsl:text>
   </xsl:template>

   <xsl:template match="capability">
      <xsl:for-each select="interface[@role='std']">
         <xsl:if test="position()=1">
            <xsl:text> </xsl:text>
            <xsl:value-of select="accessURL"/>
         </xsl:if>
      </xsl:for-each>      
   </xsl:template>

</xsl:stylesheet>
