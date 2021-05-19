<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                xmlns:v="http://www.ivoa.net/xml/VOTable/v1.1"
                version="1.0">

   <xsl:output method="xml" encoding="UTF-8" indent="yes"
               omit-xml-declaration="yes" />

   <!--
     -  the type of query being tested.  Allowed values include:
     -  <pre>
     -    region  normal region search query
     -    meta    metadata query triggered by FORMAT=METADATA
     -    error   an erroneous query meant to result in an error response
     -  </pre>
     -->
   <xsl:param name="queryType">region</xsl:param>

   <!--
     -  the name for the query being tested.  (For diagnostic purposes)
     -->
   <xsl:param name="queryName">generic</xsl:param>

   <!--
     -  a description of the query being sent.  
     -->
   <xsl:param name="queryDesc"></xsl:param>

   <!--
     -  the input parameters (for diagnostic purposes)
     -->
   <xsl:param name="inputs"/>

   <!--
     -  the status values to show in the output.  The value should be 
     -  a space-delimited list of status values.  The default is to show
     -  all status types, e.g. "fail warn rec pass".  
     -->
   <xsl:param name="showStatus">fail warn rec pass</xsl:param>

   <!--
     -  the test codes to ignore.  The value should be a space-delimited
     -  list of test codes (i.e. "items") whose results should not be 
     -  returned.
     -->
   <xsl:param name="ignoreTests"/>

   <!-- 
     - the title of the SIA resource
     -->
   <xsl:param name="title" select="''"/>

   <!-- 
     - the short name of the SIA resource
     -->
   <xsl:param name="shortName" select="''"/>

   <!-- 
     - the value of the service's "ImageServiceType" metadatum
     -->
   <xsl:param name="imageServiceType" select="'pointed'"/>

   <!--
     -  the name to give to the root element of the output results document
     -->
   <xsl:param name="resultsRootElement">SimpleImageAccess</xsl:param>

   <!--
     -  if true, UCD names should be considered case-sensitive
     -->
   <xsl:param name="ucdCaseSensitive" select="false()"/>

   <!--
     -  this is the value of the showStatus parameter with spaces prepended 
     -  and appended to add processing by reportResult
     -->
   <xsl:variable name="verbosity">
      <xsl:value-of select="concat(' ',$showStatus,' ')"/>
   </xsl:variable>

   <!--
     -  this is the value of the show parameter with spaces prepended and 
     -  appended to add processing by reportResult
     -->
   <xsl:variable name="ignore">
      <xsl:value-of select="concat(' ',$ignoreTests,' ')"/>
   </xsl:variable>

   <!--
     -  begin testing.  This template should be overridden for the appropriate
     -    output format (plain text, html, etc.).  It should provide the 
     -    overall document envelope to contain the results of the various
     -    tests (via reportResult).  Within that envelope, it should apply the 
     -    mode="byquery" template on "/".  This implementation returns XML 
     -    encoding.
     -->
   <xsl:template match="/">
      <xsl:element name="{$resultsRootElement}">
         <xsl:attribute name="votable-version">1.1 (xsd)</xsl:attribute>
         <xsl:attribute name="name">
            <xsl:value-of select="$queryName"/>
         </xsl:attribute>
         <xsl:attribute name="role">
            <xsl:value-of select="$queryType"/>
         </xsl:attribute>
         <xsl:if test="string-length(normalize-space($queryDesc))&gt;0">
            <xsl:attribute name="description">
               <xsl:value-of select="$queryDesc"/>
            </xsl:attribute>
         </xsl:if>
         <xsl:attribute name="showStatus">
            <xsl:value-of select="$showStatus"/>
         </xsl:attribute>
         <xsl:if test="$inputs!=''">
            <xsl:attribute name="options">
               <xsl:value-of select="$inputs"/>
            </xsl:attribute>
         </xsl:if>
         <xsl:attribute name="recordCount">
            <xsl:value-of select="count(//v:RESOURCE[1]/v:TABLE//v:TABLEDATA/v:TR)"/>
         </xsl:attribute>
         <xsl:apply-templates select="/" mode="byquery"/>
      </xsl:element>
   </xsl:template>

   <xsl:template match="/" mode="byquery">
      <xsl:choose>
         <xsl:when test="$queryType='error'">
            <xsl:apply-templates select="/" mode="error"/>
         </xsl:when>
         <xsl:when test="$queryType='metadata'">
            <xsl:apply-templates select="/" mode="metadata"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates select="/" mode="query"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!--
     -  run tests on the output from a legal query that should return at 
     -  least one row.
     -->
   <xsl:template match="/" mode="query">

      <!-- ensure there is a RESOURCE with type=results -->
      <xsl:call-template name="T4.2.1a"/>

      <!-- recommend that the results be in the first RESOURCE -->
      <xsl:call-template name="T4.2.1c"/>

      <xsl:choose>
         <xsl:when test="//v:RESOURCE[@type='results']">
            <xsl:apply-templates select="//v:RESOURCE[@type='results']" 
                                 mode="tests"/>
         </xsl:when>
         <xsl:when test="//v:RESOURCE[1]">
            <xsl:apply-templates select="//v:RESOURCE[1]" mode="tests"/>
         </xsl:when>
      </xsl:choose>
   </xsl:template>

   <!--
     -  apply tests to the resource containing the query results
     -->
   <xsl:template match="v:RESOURCE" mode="tests">

      <!-- make sure there is only one TABLE in your RESOURCE of results -->
      <xsl:call-template name="T4.2.1b"/>

      <!-- is there an INFO element? -->
      <xsl:call-template name="T4.2.2a"/>

      <xsl:for-each select="v:INFO[@name='QUERY_STATUS']">

         <!-- is the QUERY_STATUS value legal? -->
         <xsl:call-template name="T4.3d"/>

         <!-- is a legal query supported? -->
         <xsl:call-template name="T4.1"/>

         <!-- Does an error result contain a message? -->
         <xsl:call-template name="T4.3c"/>
      </xsl:for-each>

      <!-- check for required columns -->
      <xsl:call-template name="checkFields"/>
   </xsl:template>

   <!--
     -  ensure there is a RESOURCE with type=results
     -->
   <xsl:template name="T4.2.1a">
      <xsl:variable name="stat">
         <xsl:copy-of select="count(//v:RESOURCE[@type='results'])=1"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.2.1a</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>VOTable must contain exactly one RESOURCE with type='results'.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     -  recommend that the results be in the first RESOURCE
     -->
   <xsl:template name="T4.2.1c">
      <xsl:variable name="stat">
         <xsl:copy-of select="boolean(//v:RESOURCE[position()=1 and 
                                                 @type='results'])"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.2.1c</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="type">warn</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Query results (with type='results') should be the first RESOURCE.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     - make sure there is only one TABLE in your RESOURCE of results
     - @context RESOURCE
     -->
   <xsl:template name="T4.2.1b">
      <xsl:variable name="stat">
         <xsl:copy-of select="count(v:TABLE)=1"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.2.1b</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>There must be exactly one TABLE in the RESOURCE containing the results.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     - is there an INFO element?
     - @context RESOURCE
     -->
   <xsl:template name="T4.2.2a">
      <xsl:variable name="stat">
         <xsl:copy-of select="count(v:INFO[@name='QUERY_STATUS'])=1"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.2.2a</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="type">warn</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>RESOURCE should include one INFO element with name="QUERY_STATUS".</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     - is the QUERY_STATUS value legal?
     - @context INFO[@name='QUERY_STATUS']
     -->
   <xsl:template name="T4.3d">
      <xsl:variable name="stat">
         <xsl:copy-of select="@value='OK' or @value='ERROR' or @value='OVERFLOW'"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.3d</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>Unrecognized QUERY_STATUS: </xsl:text>
            <xsl:value-of select="@value"/>
            <xsl:text>; must be either OK, ERROR, or OVERFLOW.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     - did our legal query produce an error?
     - @context INFO[@name='QUERY_STATUS']
     -->
   <xsl:template name="T4.1">
      <xsl:param name="qtype">Legal</xsl:param>
      <xsl:param name="ref">4.1</xsl:param>

      <xsl:variable name="stat">
         <xsl:copy-of select="@value!='ERROR'"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item" select="$ref"/>
         <xsl:with-param name="status" select="$stat"/>
      <!-- <xsl:with-param name="type">warn</xsl:with-param> -->
         <xsl:with-param name="desc">
            <xsl:value-of select="$qtype"/>
            <xsl:text> query produced an ERROR result.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     -  check to see if required and recommended columns are present
     -->
   <xsl:template name="checkFields">
      <xsl:call-template name="T4.2.4a"/>
      <xsl:call-template name="T4.2.4b"/>
      <xsl:call-template name="T4.2.4c"/>
      <xsl:call-template name="T4.2.4d"/>
      <xsl:call-template name="T4.2.4e"/>
      <xsl:call-template name="T4.2.4f"/>
      <xsl:call-template name="T4.2.4g"/>
      <xsl:call-template name="T4.2.4h"/>
      <xsl:call-template name="T4.2.4i"/>
      <xsl:call-template name="T4.2.4j"/>
      <xsl:call-template name="T4.2.4l"/>
      <xsl:call-template name="T4.2.4m"/>
      <xsl:call-template name="T4.2.4n"/>
      <xsl:call-template name="T4.2.4o"/>
      <xsl:call-template name="T4.2.4p"/>
      <xsl:call-template name="T4.2.4q"/>
      <xsl:call-template name="T4.2.4r"/>
      <xsl:call-template name="T4.2.4s"/>
      <xsl:call-template name="T4.2.4t"/>
      <xsl:call-template name="T4.2.4u"/>
      <xsl:call-template name="T4.2.4v"/>
      <xsl:call-template name="T4.2.4x"/>
      <xsl:call-template name="T4.2.4y"/>
   </xsl:template>

   <!-- Image Metadata -->

   <xsl:template name="T4.2.4a">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4a</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_Title</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4b">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4b</xsl:with-param>
         <xsl:with-param name="ucd">INST_ID</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4c">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4c</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_MJDateObs</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4d">
      <xsl:variable name="ucd">POS_EQ_RA_MAIN</xsl:variable>

      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4d</xsl:with-param>
         <xsl:with-param name="ucd" select="$ucd"/>
         <xsl:with-param name="type">double</xsl:with-param>
      </xsl:call-template>

      <xsl:apply-templates select="v:PARAM[@ucd=$ucd]|
                                   v:TABLE/v:FIELD[@ucd=$ucd]" 
                           mode="checkDegUnit">
         <xsl:with-param name="item">4.2.4d</xsl:with-param>
         <xsl:with-param name="ucd" select="$ucd"/>
      </xsl:apply-templates>
   </xsl:template>

   <xsl:template name="T4.2.4e">
      <xsl:variable name="ucd">POS_EQ_DEC_MAIN</xsl:variable>

      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4e</xsl:with-param>
         <xsl:with-param name="ucd" select="$ucd"/>
         <xsl:with-param name="type">double</xsl:with-param>
      </xsl:call-template>

      <xsl:apply-templates select="v:PARAM[@ucd=$ucd]|
                                   v:TABLE/v:FIELD[@ucd=$ucd]" 
                           mode="checkDegUnit">
         <xsl:with-param name="item">4.2.4e</xsl:with-param>
         <xsl:with-param name="ucd" select="$ucd"/>
      </xsl:apply-templates>
   </xsl:template>

   <xsl:template name="T4.2.4f">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4f</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_Naxes</xsl:with-param>
         <xsl:with-param name="type">int</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4g">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4g</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_Naxis</xsl:with-param>
         <xsl:with-param name="type">int</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4h">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4h</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_Scale</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4i">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4i</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_Format</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!-- Coordinate Metadata -->

   <xsl:template name="T4.2.4j">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4j</xsl:with-param>
         <xsl:with-param name="ucd">VOX:STC_CoordRefFrame</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4l">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4l</xsl:with-param>
         <xsl:with-param name="ucd">VOX:WCS_CoordProjection</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">3</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4m">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4m</xsl:with-param>
         <xsl:with-param name="ucd">VOX:WCS_CoordRefPixel</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4n">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4n</xsl:with-param>
         <xsl:with-param name="ucd">VOX:WCS_CoordRefValue</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4o">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4o</xsl:with-param>
         <xsl:with-param name="ucd">VOX:WCS_CDMatrix</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!-- Spectral Bandpass Metadata -->

   <xsl:template name="T4.2.4p">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4p</xsl:with-param>
         <xsl:with-param name="ucd">VOX:BandPass_ID</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4q">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4q</xsl:with-param>
         <xsl:with-param name="ucd">VOX:BandPass_Unit</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4r">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4r</xsl:with-param>
         <xsl:with-param name="ucd">VOX:BandPass_RefValue</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4s">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4s</xsl:with-param>
         <xsl:with-param name="ucd">VOX:BandPass_HiLimit</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4t">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4t</xsl:with-param>
         <xsl:with-param name="ucd">VOX:BandPass_LoLimit</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!-- Processing Metadata -->

   <xsl:template name="T4.2.4u">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4u</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_PixFlags</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!-- Access Metadata -->

   <xsl:template name="T4.2.4v">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4v</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_AccessReference</xsl:with-param>
         <xsl:with-param name="arraysize">*</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template name="T4.2.4x">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">4.2.4x</xsl:with-param>
         <xsl:with-param name="ucd">VOX:Image_FileSize</xsl:with-param>
         <xsl:with-param name="type">int</xsl:with-param>
         <xsl:with-param name="level">rec</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     - check tagging of all columns and parameters
     -->
   <xsl:template name="T4.2.4y">
      <xsl:apply-templates select="v:PARAM[@name!='QUERY_STATUS' and 
                                         not(starts-with(@name,'INPUT:'))] |
                                   v:TABLE/v:FIELD" 
                           mode="flddesc" />
   </xsl:template>

   <xsl:template match="v:FIELD|v:PARAM" mode="flddesc">
      <xsl:variable name="name">
         <xsl:call-template name="fieldlabel"/>
      </xsl:variable>

      <xsl:variable name="stat">
         <xsl:value-of select="@datatype and @ucd and v:DESCRIPTION"/>
      </xsl:variable>
      
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.2.4y</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="type">rec</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Recommend specifying datatype, ucd, and </xsl:text>
            <xsl:text>DESCRIPTION for </xsl:text>
            <xsl:value-of select="local-name()"/>
            <xsl:text> with </xsl:text>
            <xsl:value-of select="$name"/>
         </xsl:with-param>
      </xsl:call-template>

      <xsl:if test="@datatype='double' or @datatype='float' or 
                    @datatype='complex'">
         <xsl:variable name="ustat">
            <xsl:copy-of select="count(@unit)=1"/>
         </xsl:variable>

         <xsl:call-template name="reportResult">
            <xsl:with-param name="item">4.2.4y/b</xsl:with-param>
            <xsl:with-param name="status" select="$ustat"/>
            <xsl:with-param name="type">rec</xsl:with-param>
            <xsl:with-param name="desc">
               <xsl:text>Recommend specifying unit for </xsl:text>
               <xsl:value-of select="local-name()"/>
               <xsl:text> with </xsl:text>
               <xsl:value-of select="$name"/>
               <xsl:text> when datatype is floating-point.</xsl:text>
            </xsl:with-param>
         </xsl:call-template>
      </xsl:if>
   </xsl:template>

   <!--
     -  run tests on the output from an illegal query that should
     -  return an error.
     -->
   <xsl:template match="/" mode="error">
      <xsl:call-template name="T4.2.1a"/>
      <xsl:call-template name="T4.2.1c"/>
      <xsl:choose>
         <xsl:when test="//v:RESOURCE[@type='results']">
            <xsl:apply-templates select="//v:RESOURCE[@type='results']" 
                                 mode="error"/>
         </xsl:when>
         <xsl:when test="//v:RESOURCE[1]">
            <xsl:apply-templates select="//v:RESOURCE[1]" mode="error"/>
         </xsl:when>
      </xsl:choose>
   </xsl:template>

   <!--
     -  apply tests on the error reporting inside the results RESOURCE
     -->
   <xsl:template match="v:RESOURCE" mode="error">
      <xsl:call-template name="T4.3a"/>
      <xsl:call-template name="T4.3.1a"/>
      <xsl:for-each select="v:INFO[@name='QUERY_STATUS']">
          <xsl:call-template name="T4.3b"/>
          <xsl:call-template name="T4.3c"/>
      </xsl:for-each>
   </xsl:template>

   <!--
     - is there an INFO element indicating an error?
     - @context RESOURCE
     -->
   <xsl:template name="T4.3a">
      <xsl:variable name="stat">
         <xsl:copy-of select="count(v:INFO[@name='QUERY_STATUS'])=1"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.3a</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>On error, RESOURCE must include one INFO element with name="QUERY_STATUS".</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     - is there an INFO element indicating an error?
     - @context RESOURCE
     -->
   <xsl:template name="T4.3.1a">
      <xsl:variable name="stat">
         <xsl:copy-of select="count(v:INFO[@name='QUERY_STATUS' and 
                                           @value='ERROR'])=1"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.3.1a</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>Service should report syntax errors (such as bad position formats)</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     - do we have an ERROR status and is there a message
     - @context INFO[@name='QUERY_STATUS']
     -->
   <xsl:template name="T4.3b">
      <xsl:variable name="stat">
         <xsl:copy-of select="@value='ERROR'"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.3b</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>Errors must be reported using INFO element </xsl:text>
            <xsl:text>with name=QUERY_STATUS and value='ERROR'.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>   

   <!--
     -  Does an error result contain a message?
     -->
   <xsl:template name="T4.3c">
      <xsl:variable name="stat">
         <xsl:copy-of select="@value='OK' or .!=''"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">4.3c</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="type">warn</xsl:with-param>
         <xsl:with-param name="desc">
          <xsl:text>INFO element with QUERY_STATUS not equal to 'OK' </xsl:text>
          <xsl:text>should include an error message as content.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     -  run tests on the output from a metadata query.
     -->
   <xsl:template match="/" mode="metadata">
      <xsl:call-template name="T4.2.1a"/>
      <xsl:call-template name="T4.2.1c"/>
      <xsl:choose>
         <xsl:when test="//v:RESOURCE[@type='results']">
            <xsl:apply-templates select="//v:RESOURCE[@type='results']" 
                                 mode="metadata"/>
         </xsl:when>
         <xsl:when test="//v:RESOURCE[1]">
            <xsl:apply-templates select="//v:RESOURCE[1]" mode="metadata"/>
         </xsl:when>
      </xsl:choose>
   </xsl:template>

   <!--
     -  apply tests on the metadata reporting inside the results RESOURCE
     -->
   <xsl:template match="v:RESOURCE" mode="metadata">

      <!-- make sure there is only one TABLE in your RESOURCE of results -->
      <xsl:call-template name="T4.2.1b"/>

      <!-- is there an INFO element? -->
      <xsl:call-template name="T4.2.2a"/>

      <xsl:for-each select="v:INFO[@name='QUERY_STATUS']">

         <!-- is the QUERY_STATUS value legal? -->
         <xsl:call-template name="T4.3d"/>

         <!-- is a legal query supported? -->
         <xsl:call-template name="T4.1">
            <xsl:with-param name="qtype">Metadata</xsl:with-param>
            <xsl:with-param name="ref">7.1a</xsl:with-param>
         </xsl:call-template>

         <!-- Does an error result contain a message? -->
         <xsl:call-template name="T4.3c"/>
      </xsl:for-each>

      <!-- check for required columns -->
      <xsl:call-template name="checkFields"/>

      <!-- check for input parameter descriptions -->
      <xsl:call-template name="checkInputParams"/>
   </xsl:template>

   <!--
     -  check for input parameter descriptions
     -  @context RESOURCE
     -->
   <xsl:template name="checkInputParams">
      <!-- check for required input PARAMs (POS, SIZE, FORMAT) -->
      <xsl:call-template name="T7.1b"/>

      <!-- recommend documenting other input PARAMs -->
      <xsl:call-template name="T7.1c"/>

      <!-- recommend descriptions for input PARAMs -->
      <xsl:call-template name="T7.1d"/>

      <!-- recommend listing support formats -->
      <xsl:call-template name="T7.1e"/>
   </xsl:template>

   <!-- 
     -  check for required input PARAMs (POS, SIZE, FORMAT)
     -  @context RESOURCE
     -->
   <xsl:template name="T7.1b">
      <xsl:call-template name="checkReqInputParam">
         <xsl:with-param name="inp">POS</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="checkReqInputParam">
         <xsl:with-param name="inp">SIZE</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="checkReqInputParam">
         <xsl:with-param name="inp">FORMAT</xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     -  check for required input PARAM
     -  @context RESOURCE
     -->
   <xsl:template name="checkReqInputParam">
      <xsl:param name="inp" select="'??'"/>

      <xsl:variable name="stat">
         <xsl:copy-of select="v:PARAM[@name=concat('INPUT:',$inp)]"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">7.1b</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
          <xsl:text>Metadata query must include PARAM for required </xsl:text>
          <xsl:text>input parameter, </xsl:text>
          <xsl:value-of select="$inp"/>
          <xsl:text>.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!-- 
     -  recommend descriptions for input PARAMs
     -  @context RESOURCE
     -->
   <xsl:template name="T7.1d">
      <xsl:apply-templates select="v:PARAM[starts-with(@name,'INPUT:')]" 
                           mode="desc"/>
   </xsl:template>  

   <xsl:template match="v:FIELD|v:PARAM" mode="desc">
      <xsl:variable name="name">
         <xsl:call-template name="fieldname"/>
      </xsl:variable>

      <xsl:variable name="stat">
         <xsl:value-of select="v:DESCRIPTION"/>
      </xsl:variable>
      
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">7.1d</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="type">rec</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Recommend DESCRIPTION for </xsl:text>
            <xsl:value-of select="local-name()"/>
            <xsl:text> with </xsl:text>
            <xsl:value-of select="$name"/><xsl:text>.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     -  recommend documenting other input PARAMs
     -->
   <xsl:template name="T7.1c">
      <xsl:variable name="stat">
         <xsl:value-of select="v:PARAM[starts-with(@name,'INPUT:') and 
                                     @name != 'INPUT:POS' and 
                                     @name != 'INPUT:SIZE' and 
                                     @name != 'INPUT:FORMAT']"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">7.1c</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="type">rec</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Are there any supported optional or </xsl:text>
            <xsl:text>service-specific input parameters that should </xsl:text>
            <xsl:text>be described?</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!-- 
     -  recommend listing support formats
     -->
   <xsl:template name="T7.1e">
      <xsl:variable name="stat">
         <xsl:value-of select="v:PARAM[@name='INPUT:FORMAT' and v:VALUES]"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">7.1e</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="type">rec</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Recommend listing supported formats using </xsl:text>
            <xsl:text>VALUES within PARAM with name='INPUT:FORMAT'.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>


<!-- ========================================================== -->

   <!--
     -  return the label for a FIELD or PARAM we can use for a user
     -  message, depending on how it is labeled.
     -  @context FIELD|PARAM
     -->
   <xsl:template name="fieldlabel">
      <xsl:choose>
         <xsl:when test="@name">
            <xsl:text>name='</xsl:text>
            <xsl:value-of select="@name"/>
            <xsl:text>'</xsl:text>
         </xsl:when>
         <xsl:when test="@ID">
            <xsl:text>id='</xsl:text>
            <xsl:value-of select="@ID"/>
            <xsl:text>'</xsl:text>
         </xsl:when>
         <xsl:when test="@ucd">
            <xsl:text>ucd='</xsl:text>
            <xsl:value-of select="@ucd"/>
            <xsl:text>'</xsl:text>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>column position=</xsl:text>
            <xsl:value-of select="position()"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!--
     -  check that there is one and only one column with a given UCD
     -  This does 3 specific checks:
     -  a) that there is one and only one column with given UCD
     -  b) that it has the required datatype 
     -  c) that it has the required arraysize. 
     -  @param item       the base test code 
     -  @param ucd        the required UCD 
     -  @param type       the required type; default: char
     -  @param arraysize  the required arraysize.  If this value='**', then
     -                       the response may have any non-empty value.  If
     -                       it is any other non-empty value, the response's
     -                       @arraysize must match exactly.  If not provided, 
     -                       a default of '**' will be assumed when type=char;
     -                       otherwise, the arraysize should be 
     -                       be empty/not present in the response.
     -  @context RESOURCE
     -->
   <xsl:template name="hasUCD">
      <xsl:param name="item">??</xsl:param>
      <xsl:param name="ucd"/>
      <xsl:param name="unit"/>
      <xsl:param name="type">char</xsl:param>
      <xsl:param name="arraysize">
         <xsl:choose>
            <xsl:when test="$type='char'">**</xsl:when>
            <xsl:otherwise></xsl:otherwise>
         </xsl:choose>
      </xsl:param>
      <xsl:param name="level">fail</xsl:param>
      <xsl:param name="casesensitive" select="$ucdCaseSensitive"/>

      <!-- the proper base message for the arraysize test -->
      <xsl:variable name="arraysizemsgbase">
         <xsl:text>The </xsl:text>
         <xsl:value-of select="$ucd"/>
         <xsl:text> column must </xsl:text>
      </xsl:variable>

      <!-- the proper message for the arraysize test -->
      <xsl:variable name="arraysizemsg">
         <xsl:choose>
            <xsl:when test="$arraysize='**' and $type='char'">
              <xsl:value-of select="$arraysizemsgbase"/>
              <xsl:text>have a string-appropriate arraysize </xsl:text>
              <xsl:text>(recommend '*').</xsl:text>
            </xsl:when>
            <xsl:when test="$arraysize='**'">
              <xsl:value-of select="$arraysizemsgbase"/>
              <xsl:text>have a non-empty arraysize.</xsl:text>
            </xsl:when>
            <xsl:when test="$arraysize=''">
              <xsl:value-of select="$arraysizemsgbase"/>
              <xsl:text>be defined as a scalar--i.e. arraysize="1",</xsl:text>
              <xsl:text> arraysize="", or not provided (preferred).</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$arraysizemsgbase"/>
              <xsl:text>have arraysize='</xsl:text>
              <xsl:value-of select="$arraysize"/>
              <xsl:text>'</xsl:text>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>

      <xsl:variable name="msg">
         <xsl:choose>
            <xsl:when test="$level='warn'">The TABLE should contain </xsl:when>
            <xsl:when test="$level='rec'">The TABLE should contain </xsl:when>
            <xsl:otherwise>The TABLE must contain </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>

      <xsl:variable name="upucd">
         <xsl:call-template name="uppercase">
            <xsl:with-param name="in" select="$ucd"/>
         </xsl:call-template>
      </xsl:variable>

      <!-- a) has one FIELD w/UCD? -->
      <xsl:variable name="exactCase">
         <xsl:copy-of select="count(v:PARAM[@ucd=$ucd and 
                                            not(starts-with(@name, 'INPUT:'))] |
                                    v:TABLE/v:PARAM[@ucd=$ucd] |
                                    v:TABLE/v:FIELD[@ucd=$ucd])=1"/>
      </xsl:variable>

      <xsl:variable name="anyCase">
        <xsl:copy-of select="count(v:PARAM[translate(@ucd,
                                    'abcdefghijklmnopqrstuvwxyz',
                                    'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=$upucd and 
                                           not(starts-with(@name, 'INPUT:'))] |
                                   v:TABLE/v:PARAM[translate(@ucd,
                                    'abcdefghijklmnopqrstuvwxyz',
                                    'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=$upucd] |
                                   v:TABLE/v:FIELD[translate(@ucd,
                                    'abcdefghijklmnopqrstuvwxyz',
                                    'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=$upucd])=1"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item" select="concat($item,'/a')"/>
         <xsl:with-param name="status" 
                         select="$exactCase='true' or
                                ($casesensitive!='true' and $anyCase='true')"/>
         <xsl:with-param name="type" select="$level"/>
         <xsl:with-param name="desc">
            <xsl:value-of select="$msg"/>
            <xsl:text>exactly one FIELD or PARAM with ucd='</xsl:text>
            <xsl:value-of select="$ucd"/>
            <xsl:text>'.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>

      <xsl:for-each select="v:PARAM[translate(@ucd,
                                    'abcdefghijklmnopqrstuvwxyz',
                                    'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=$upucd and 
                                    not(starts-with(@name, 'INPUT:'))] |
                            v:TABLE/v:PARAM[translate(@ucd,
                                    'abcdefghijklmnopqrstuvwxyz',
                                    'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=$upucd] |
                            v:TABLE/v:FIELD[translate(@ucd,
                                    'abcdefghijklmnopqrstuvwxyz',
                                    'ABCDEFGHIJKLMNOPQRSTUVWXYZ')=$upucd]">

         <xsl:if test="$exactCase = 'true' or 
                       (not($casesensitive) and $anyCase='true')">
            <xsl:if test="not($casesensitive)">
               <xsl:call-template name="reportResult">
                  <xsl:with-param name="item" select="concat($item,'/a')"/>
                  <xsl:with-param name="status" select="$exactCase"/>
                  <xsl:with-param name="type">warn</xsl:with-param>
                  <xsl:with-param name="desc">
                    <xsl:text>UCD </xsl:text>
                    <xsl:value-of select="@ucd"/>
                    <xsl:text> should match documented case (</xsl:text>
                    <xsl:value-of select="$ucd"/>
                    <xsl:text>); </xsl:text>
                    <xsl:text>otherwise, some clients may fail to </xsl:text>
                    <xsl:text>recognize them.</xsl:text>
                  </xsl:with-param>
               </xsl:call-template>
            </xsl:if>

            <!-- b) has correct type?  -->
            <xsl:variable name="tpstat">
              <xsl:copy-of select="@datatype=$type"/>
            </xsl:variable>

            <xsl:call-template name="reportResult">
               <xsl:with-param name="item" select="concat($item,'/b')"/>
               <xsl:with-param name="status" select="$tpstat"/>
               <xsl:with-param name="desc">
                  <xsl:text>The </xsl:text>
                  <xsl:value-of select="@ucd"/>
                  <xsl:text> column must have datatype='</xsl:text>
                  <xsl:value-of select="$type"/>
                  <xsl:text>'.</xsl:text>
               </xsl:with-param>
            </xsl:call-template>

            <!-- c) has correct arraysize -->
            <xsl:variable name="asstat">
              <xsl:choose>
                 <xsl:when test="$arraysize=''">
                    <!-- should not have an arraysize -->
                    <xsl:value-of select="not(@arraysize) or @arraysize='' or
                                          @arraysize='1'"/>
                 </xsl:when>
                 <xsl:when test="$arraysize='**'">
                    <!-- arbitrary arraysize needed -->
                    <xsl:value-of select="@arraysize!=''"/>
                 </xsl:when>
                 <xsl:otherwise>
                    <!-- arraysize must match exactly -->
                    <xsl:value-of select="@arraysize = $arraysize"/>
                 </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>

            <xsl:call-template name="reportResult">
               <xsl:with-param name="item" select="concat($item,'/c')"/>
               <xsl:with-param name="status" select="$asstat"/>
               <xsl:with-param name="desc" select="$arraysizemsg"/>
            </xsl:call-template>
         </xsl:if>
         
      </xsl:for-each>
   </xsl:template>

   <!-- 
     -  convert a string to upper case letters.  This also converts / to _
     -  @param in   the input string
     -  @return string  the translated string.
     -->
   <xsl:template name="uppercase">
      <xsl:param name="in"/>
      <xsl:value-of select="translate($in,'abcdefghijklmnopqrstuvwxyz',
                                      'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
   </xsl:template>

   <!--
     -  make sure the field containing has a unit equal to "degrees"
     -->
   <xsl:template match="v:FIELD|v:PARAM" mode="checkDegUnit">
      <xsl:param name="item">??</xsl:param>
      <xsl:param name="ucd">??</xsl:param>
      <xsl:param name="el"><xsl:value-of select="local-name()"/></xsl:param>

      <xsl:variable name="unit">
         <xsl:value-of select="translate(@unit,'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
                                         'abcdefghijklmnopqrstuvwxyz')"/>
      </xsl:variable>

      <xsl:variable name="ustat">
         <xsl:copy-of select="starts-with('degrees',$unit)"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item" select="$item"/>
         <xsl:with-param name="status" select="$ustat"/>
         <xsl:with-param name="type">warn</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Recommend unit='degrees' for </xsl:text>
            <xsl:value-of select="$el"/>
            <xsl:text> with ucd='</xsl:text>
            <xsl:value-of select="$ucd"/>
            <xsl:text>'.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     -  report the result of a test. This template should be overridden
     -    for the appropriate output format (plain text, html, etc.).  This
     -    default implementation returns an XML encoding.  The implementation
     -    should honor the global verbosity parameter (see its documentation). 
     -  @param item    the unique code for the test
     -  @param status  boolean (or string=true or false) indicating whether 
     -                   the test passed.
     -  @param type    the type of test.  This should be the string status code 
     -                   to given if the test did not pass.
     -  @param desc    a human-readable statement of what was tested.
     -  
     -->
   <xsl:template name="reportResult">
      <xsl:param name="item">unspecified</xsl:param>
      <xsl:param name="status" select="true()"/>
      <xsl:param name="type" select="'fail'"/>
      <xsl:param name="desc"/>

      <xsl:if test="not(contains($ignore, concat(' ',$item,' '))) and 
                    ((string($status)='false' and 
                      contains($verbosity, concat(' ',$type,' ')) ) or
                     (string($status)='true' and 
                      contains($verbosity, ' pass ') ))">
         <xsl:variable name="stat">
            <xsl:choose>
               <xsl:when test="string($status)='true'">pass</xsl:when>
               <xsl:otherwise><xsl:value-of select="$type"/></xsl:otherwise>
            </xsl:choose>
         </xsl:variable>

         <test item="{$item}" status="{$stat}">
            <xsl:value-of select="$desc"/>
         </test>
      </xsl:if>
   </xsl:template>

   <xsl:template name="fieldname">
      <xsl:choose>
         <xsl:when test="@name">
            <xsl:text>name=</xsl:text><xsl:value-of select="@name"/>
         </xsl:when>
         <xsl:when test="@ID">
            <xsl:text>ID=</xsl:text><xsl:value-of select="@ID"/>
         </xsl:when>
         <xsl:when test="@ucd">
            <xsl:text>ucd=</xsl:text><xsl:value-of select="@ucd"/>
         </xsl:when>
         <xsl:otherwise>an unspecified name</xsl:otherwise>
      </xsl:choose>
   </xsl:template>

</xsl:stylesheet>
