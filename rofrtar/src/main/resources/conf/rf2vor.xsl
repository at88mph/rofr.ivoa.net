<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
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

<VODescription
          xmlns="http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOResource"
          xmlns:vor="http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOResource"
          xmlns:vos="http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOStdService"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOResource
            http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOResource.xsd 
            http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOStdService
            http://rai.ncsa.uiuc.edu/~rplante/VO/schemas/VOStdService.xsd"
>
	    <xsl:apply-templates select="/*"  />
</VODescription>
</xsl:template>

<xsl:template match="/*" >
    <xsl:for-each select="node()">
        <xsl:if test="name()!='reserved'">
            <xsl:copy-of select="."/>
        </xsl:if>
    </xsl:for-each>
</xsl:template>
</xsl:stylesheet>
