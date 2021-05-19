<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:oaidc='http://www.openarchives.org/OAI/2.0/oai_dc/'
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:vor="http://www.ivoa.net/xml/VOResource/v1.0" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="1.0">
                
<!-- Stylesheet for the Metadata Portion of OAI-DC GetRecord verb -->

<xsl:output method="xml" encoding="UTF-8" indent="yes" 
            omit-xml-declaration="yes"/>

<xsl:template match="/">

   <oaidc:dc xmlns:oaidc="http://www.openarchives.org/OAI/2.0/oai_dc/"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/
                               http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
      <xsl:apply-templates select="//*[local-name()='Resource']"  />
   </oaidc:dc>
</xsl:template>

<xsl:template match="*[local-name()='Resource']"  >
    <xsl:apply-templates select="title" />
    <xsl:apply-templates select="identifier" />
    <xsl:apply-templates select="curation/publisher" />
    <xsl:apply-templates select="curation/creator" />
    <xsl:apply-templates select="curation/contributor" />
    <xsl:apply-templates select="content/date" />
    <xsl:apply-templates select="content/subject" />
    <xsl:apply-templates select="content/description" />
    <xsl:apply-templates select="content/type" />
    <xsl:apply-templates select="format" />
    <xsl:apply-templates select="rights" />
</xsl:template>

<xsl:template match="title">
    <dc:title>
       <xsl:value-of select="."/>
    </dc:title>
</xsl:template>

<xsl:template match="creator">
    <dc:creator>
       <xsl:value-of select="name"/>
    </dc:creator>
</xsl:template>

<xsl:template match="subject">
    <dc:subject>
       <xsl:value-of select="."/>
    </dc:subject>
</xsl:template>

<xsl:template match="content/description">
    <dc:description>
       <xsl:value-of select="."/>
    </dc:description>
</xsl:template>

<xsl:template match="curation/publisher">
    <dc:publisher>
       <xsl:value-of select="."/>
    </dc:publisher>
</xsl:template>

<xsl:template match="contributor">
    <dc:contributor>
       <xsl:value-of select="."/>
    </dc:contributor>
</xsl:template>

<xsl:template match="date">
    <dc:date>
       <xsl:if test="@role">
          <xsl:value-of select="@role"/>
          <xsl:text>:  </xsl:text>
       </xsl:if>
       <xsl:value-of select="."/>
    </dc:date>
</xsl:template>

<xsl:template match="type">
    <dc:type>
       <xsl:value-of select="."/>
    </dc:type>
</xsl:template>

<xsl:template match="format">
    <dc:type>
       <xsl:value-of select="."/>
    </dc:type>
</xsl:template>

<xsl:template match="identifier">
    <dc:identifier>
       <xsl:value-of select="."/>
    </dc:identifier>
</xsl:template>

<xsl:template match="rights">
    <dc:rights>
       <xsl:value-of select="."/>
    </dc:rights>
</xsl:template>

</xsl:stylesheet>
