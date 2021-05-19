<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:oaidc='http://www.openarchives.org/OAI/2.0/oai_dc/'
				xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:vor="http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOResource" 
				xmlns:vos="http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOStdService"
				xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				version="1.0">
                
<!-- Stylesheet for the Metadata Portion of OAI-DC GetRecord verb -->

<xsl:output
	method="xml"
	encoding="UTF-8"
	indent="yes"
	omit-xml-declaration="yes"
	/>


<xsl:template match="/">

<oaidc:dc
	xmlns:oaidc="http://www.openarchives.org/OAI/2.0/oai_dc/"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
	    <xsl:apply-templates select="ResourceForm/*"  />
	    </oaidc:dc>
</xsl:template>

<xsl:template match="ResourceForm/*"  >
    <xsl:apply-templates select="vor:Title" />
    <xsl:apply-templates select="vor:Curation/vor:Creator/vor:CreatorName" />
    <xsl:apply-templates select="vor:Content/vor:Subject/vor:item" />
    <xsl:apply-templates select="vor:Content/vor:Description" />
    <xsl:apply-templates select="vor:Curation/vor:Publisher" />
    <xsl:apply-templates select="vor:Curation/vor:Contributor" />
    <xsl:apply-templates select="vor:Content/vor:Date" />
    <xsl:apply-templates select="vor:Type/vor:item" />
    <xsl:apply-templates select="vor:Content/vor:Format/vor:item" />
    <xsl:apply-templates select="vor:Identifier" />
    <xsl:apply-templates select="vor:Coverage/vor:Spatial" />
    <xsl:apply-templates select="vor:Coverage/vor:Spectral/vor:SpecDesc" />
    <xsl:apply-templates select="vor:Coverage/vor:Spectral/vor:Bandpass/vor:item" />
    <xsl:apply-templates select="vor:Coverage/vor:Temporal" />
<!--    <xsl:apply-templates select="vor:ContentLevel/Item" /> -->
<!--    <xsl:apply-templates select="vor:Facility" /> -->
   <xsl:apply-templates select="vor:Content/vor:Rights" />
</xsl:template>

<xsl:template match="vor:Title">
    <dc:title>
        <xsl:apply-templates/>
    </dc:title>
</xsl:template>
<xsl:template match="vor:Curation/vor:Creator/vor:CreatorName">
    <dc:creator>
        <xsl:apply-templates/>
    </dc:creator>
</xsl:template>
<xsl:template match="vor:Content/vor:Subject/vor:item">
    <dc:subject>
        <xsl:apply-templates/>
    </dc:subject>
</xsl:template>
<xsl:template match="vor:Content/vor:Description">
    <dc:description>
        <xsl:apply-templates/>
    </dc:description>
</xsl:template>
<xsl:template match="vor:Curation/vor:Publisher">
    <dc:publisher>
        <xsl:apply-templates/>
    </dc:publisher>
</xsl:template>
<xsl:template match="vor:Contributor">
    <dc:contributor>
        <xsl:apply-templates/>
    </dc:contributor>
</xsl:template>
<xsl:template match="vor:Date">
    <dc:date>
        <xsl:apply-templates/>
    </dc:date>
</xsl:template>
<xsl:template match="vor:Type/vor:item">
    <dc:type>
        <xsl:apply-templates/>
    </dc:type>
</xsl:template>
<xsl:template match="vor:Content/vor:Format/vor:item">
    <dc:type>
        <xsl:apply-templates/>
    </dc:type>
</xsl:template>
<xsl:template match="vor:Identifier">
    <dc:identifier>
        <xsl:apply-templates/>
    </dc:identifier>
</xsl:template>
<xsl:template match="vor:Coverage/vor:Spatial">
    <dc:coverage>
		<xsl:text>Spatial: </xsl:text>
        <xsl:apply-templates/>
    </dc:coverage>
</xsl:template>
<xsl:template match="vor:Coverage/vor:Spectral/vor:SpecDesc">
    <dc:coverage>
		<xsl:text>Spectral: </xsl:text>
        <xsl:apply-templates/>
    </dc:coverage>
</xsl:template>
<xsl:template match="vor:Coverage/vor:Spectral/vor:Bandpass/vor:item">
    <dc:coverage>
		<xsl:text>:Spectral Bandpass: </xsl:text>
        <xsl:apply-templates/>
    </dc:coverage>
</xsl:template>
<xsl:template match="vor:Coverage/vor:Temporal">
    <dc:coverage>
		<xsl:text>Temporal: </xsl:text>
           <xsl:value-of select="vor:Begin"/>
          <xsl:text> - </xsl:text>
		<xsl:if test="vor:End">
		      <xsl:value-of select="vor:End"/>
		</xsl:if>
    </dc:coverage>
</xsl:template>
<xsl:template match="vor:Coverage/vor:Spectral/vor:Bandpass/vor:item">
    <dc:coverage>
		<xsl:text>Spectral Bandpass: </xsl:text>
        <xsl:apply-templates/>
    </dc:coverage>
</xsl:template>
<xsl:template match="vor:Content/vor:Rights">
    <dc:rights>
        <xsl:apply-templates/>
    </dc:rights>
</xsl:template>

<!-- These do not seem to fit in Dublin Core
<xsl:template match="vor:ContentLevel/Item">
    <dc:contentlevel>
        <xsl:apply-templates/>
    </dc:contentlevel>
</xsl:template>
<xsl:template match="vor:Facility">
    <dc:facility>
        <xsl:apply-templates/>
    </dc:facility>
</xsl:template>
-->

</xsl:stylesheet>
