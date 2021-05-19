<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                xmlns:v="http://www.ivoa.net/xml/VOTable/v1.0"
                version="1.0">

   <xsl:output method="xml" encoding="UTF-8" indent="yes"
               omit-xml-declaration="yes" />

   <!--
     -  the type of query being tested.  Allowed values include:
     -  <pre>
     -    cone   normal cone search query
     -    sr0    metadata query triggered by SR=0
     -    error  an erroneous query meant to result in an error response
     -  </pre>
     -->
   <xsl:param name="queryType">cone</xsl:param>

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
     -  the name to give to the root element of the output results document
     -->
   <xsl:param name="resultsRootElement">ConeSearch</xsl:param>

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
         <xsl:attribute name="votable-version">1.0 (xsd)</xsl:attribute>
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
            <xsl:value-of 
                 select="count(//v:RESOURCE[1]/v:TABLE//v:TABLEDATA/v:TR)"/>
         </xsl:attribute>
         <xsl:apply-templates select="/" mode="byquery"/>
      </xsl:element>
   </xsl:template>

   <xsl:template match="/" mode="byquery">
      <xsl:choose>
         <xsl:when test="$queryType='error'">
            <xsl:apply-templates select="/" mode="error"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates select="/" mode="query"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template match="/" mode="query">
      <xsl:call-template name="T2.1a"/>
      <xsl:call-template name="T2.1b"/>
      <xsl:apply-templates select="//v:RESOURCE[1]/v:TABLE[1]" mode="tests"/>
      <xsl:call-template name="T2.3"/>
   </xsl:template>

   <xsl:template match="/" mode="error">
      <xsl:call-template name="T3"/>
   </xsl:template>

   <!--
     -  check that there is only one RESOURCE in the VOTable
     -  (item 2.1a)   
     -->
   <xsl:template name="T2.1a">
      <xsl:variable name="stat">
         <xsl:copy-of select="count(//v:RESOURCE)=1"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">2.1a</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>VOTable must contain only one RESOURCE.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>
   
   <!--
     -  check that there is only one TABLE in the VOTable
     -->
   <xsl:template name="T2.1b">
      <xsl:variable name="stat">
         <xsl:copy-of select="count(//v:RESOURCE/v:TABLE)=1"/>
      </xsl:variable>
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">2.1b</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>The RESOURCE element must contain exactly one TABLE.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <xsl:template match="v:TABLE" mode="tests">
      <xsl:call-template name="T2.2.1"/>
      <xsl:call-template name="T2.2.1d"/>
      <xsl:call-template name="T2.2.2"/>
      <xsl:call-template name="T2.2.3"/>
      <xsl:call-template name="T2.2.4"/>
      <xsl:apply-templates select="v:FIELD|/v:VOTABLE/v:DEFINITIONS/v:PARAM" 
                           mode="checkFieldAnnot"/>
   </xsl:template>
   
   <!--
     -  check that there a column with a given UCD and the proper definition.
     -  This does 3 specific checks
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
     -  @context TABLE
     -->
   <xsl:template name="hasUCD">
      <xsl:param name="item">??</xsl:param>
      <xsl:param name="ucd"/>
      <xsl:param name="unit"/>
      <xsl:param name="arraysize"/>
      <xsl:param name="type">char</xsl:param>

      <!-- the required arraysize attribute value -->
      <xsl:variable name="array">
         <xsl:choose>
            <xsl:when test="$arraysize='' and $type='char'">**</xsl:when>
            <xsl:otherwise><xsl:value-of select="$arraysize"/></xsl:otherwise>
         </xsl:choose>
      </xsl:variable>

      <!-- the proper base message for the arraysize test -->
      <xsl:variable name="msgbase">
         <xsl:text>The </xsl:text>
         <xsl:value-of select="$ucd"/>
         <xsl:text> column must </xsl:text>
      </xsl:variable>

      <!-- the proper message for the arraysize test -->
      <xsl:variable name="arraysizemsg">
         <xsl:choose>
            <xsl:when test="$array='**' and $type='char'">
              <xsl:value-of select="$msgbase"/>
              <xsl:text>have a string-appropriate arraysize </xsl:text>
              <xsl:text>(recommend '*').</xsl:text>
            </xsl:when>
            <xsl:when test="$array='**'">
              <xsl:value-of select="$msgbase"/>
              <xsl:text>have a non-empty arraysize.</xsl:text>
            </xsl:when>
            <xsl:when test="$array=''">
              <xsl:value-of select="$msgbase"/>
              <xsl:text>not provide arraysize (non-empty) attribute.</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$msgbase"/>
              <xsl:text>have arraysize='</xsl:text>
              <xsl:value-of select="$arraysize"/>
              <xsl:text>'</xsl:text>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>

      <!-- a) has one FIELD w/UCD? -->
      <xsl:variable name="stat">
         <xsl:copy-of select="count(v:FIELD[@ucd=$ucd])=1"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item" select="concat($item,'a')"/>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>The TABLE must contain exactly one FIELD with ucd='</xsl:text>
            <xsl:value-of select="$ucd"/>
            <xsl:text>'.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>

      <xsl:for-each select="v:FIELD[@ucd=$ucd]">

         <!-- b) has correct type?  -->
         <xsl:variable name="tpstat">
           <xsl:copy-of select="@datatype=$type"/>
         </xsl:variable>

         <xsl:call-template name="reportResult">
            <xsl:with-param name="item" select="concat($item,'b')"/>
            <xsl:with-param name="status" select="$tpstat"/>
            <xsl:with-param name="desc">
               <xsl:text>The </xsl:text>
               <xsl:value-of select="$ucd"/>
               <xsl:text> column must have datatype='</xsl:text>
               <xsl:value-of select="$type"/>
               <xsl:text>'.</xsl:text>
            </xsl:with-param>
         </xsl:call-template>

         <!-- c) has correct arraysize -->
         <xsl:variable name="arraystat">
            <xsl:choose>
               <xsl:when test="$array=''">
                  <!-- should not have an arraysize -->
                  <xsl:value-of select="not(@arraysize) or @arraysize=''"/>
               </xsl:when>
               <xsl:when test="$array='**'">
                  <!-- arbitrary arraysize needed -->
                  <xsl:value-of select="@arraysize!=''"/>
               </xsl:when>
               <xsl:otherwise>
                  <!-- arraysize must match exactly -->
                  <xsl:value-of select="@arraysize = $array"/>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:variable>

         <xsl:call-template name="reportResult">
            <xsl:with-param name="item" select="concat($item,'c')"/>
            <xsl:with-param name="status" select="$arraystat"/>
            <xsl:with-param name="desc" select="$arraysizemsg"/>
         </xsl:call-template>

      </xsl:for-each>
   </xsl:template>

   <xsl:template name="T2.2.1">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">2.2.1</xsl:with-param>
         <xsl:with-param name="ucd">ID_MAIN</xsl:with-param>
      </xsl:call-template>
   </xsl:template>   

   <xsl:template name="T2.2.2">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">2.2.2</xsl:with-param>
         <xsl:with-param name="ucd">POS_EQ_RA_MAIN</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
      </xsl:call-template>

      <xsl:apply-templates select="v:FIELD[@ucd='POS_EQ_RA_MAIN']" 
                           mode="checkDegUnit">
         <xsl:with-param name="item">2.2.2c</xsl:with-param>
         <xsl:with-param name="ucd">POS_EQ_RA_MAIN</xsl:with-param>
      </xsl:apply-templates>
   </xsl:template>   

   <xsl:template name="T2.2.3">
      <xsl:call-template name="hasUCD">
         <xsl:with-param name="item">2.2.3</xsl:with-param>
         <xsl:with-param name="ucd">POS_EQ_DEC_MAIN</xsl:with-param>
         <xsl:with-param name="type">double</xsl:with-param>
      </xsl:call-template>

      <xsl:apply-templates select="v:FIELD[@ucd='POS_EQ_DEC_MAIN']" 
                           mode="checkDegUnit">
         <xsl:with-param name="item">2.2.3c</xsl:with-param>
         <xsl:with-param name="ucd">POS_EQ_DEC_MAIN</xsl:with-param>
      </xsl:apply-templates>
   </xsl:template>   

   <xsl:template match="v:FIELD" mode="checkDegUnit">
      <xsl:param name="item">??</xsl:param>
      <xsl:param name="ucd">??</xsl:param>
      <xsl:param name="el">FIELD</xsl:param>

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
     -  check OBS_ANG-SIZE.  Warn against the existance of values as both 
     -  a PARAM and a column.
     -  @context TABLE
     -->
   <xsl:template name="T2.2.4">
      <xsl:variable name="stat">
         <xsl:copy-of select="not(v:FIELD[@ucd='OBS_ANG-SIZE'] and 
                       /v:VOTABLE/v:DEFINITIONS/v:PARAM[@ucd='OBS_ANG-SIZE'])"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">2.2.4a</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>VOTABLE can not contain both a PARAM and a FIELD with ucd='OBS_ANG-SIZE'.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>

      <xsl:for-each select="v:FIELD[@name='PositionalError'] |
                    /v:VOTABLE/v:DEFINITIONS/v:PARAM[@name='PositionalError']"> 
         <xsl:variable name="pestat">
            <xsl:copy-of select="@ucd='OBS_ANG-SIZE'"/>
         </xsl:variable>

         <xsl:call-template name="reportResult">
            <xsl:with-param name="item">2.2.4b</xsl:with-param>
            <xsl:with-param name="status" select="$pestat"/>
            <xsl:with-param name="type">warn</xsl:with-param>
            <xsl:with-param name="desc">
               <xsl:text>Use of </xsl:text>
               <xsl:value-of select="local-name()"/>
               <xsl:text> with name='PositionalError' deprecated; </xsl:text>
               <xsl:text>add ucd='OBS_ANG-SIZE'.</xsl:text>
            </xsl:with-param>
         </xsl:call-template>
      </xsl:for-each>

      <xsl:for-each select="v:FIELD[@ucd='OBS_ANG-SIZE'] |
                        /v:VOTABLE/v:DEFINITIONS/v:PARAM[@ucd='OBS_ANG-SIZE']">

         <!--
           -  Spec is ambiguous as to the value of datatype
           -->
         <xsl:variable name="el" select="local-name()"/>
         <xsl:variable name="tstat">
            <xsl:copy-of select="@datatype='double' or @datatype='float' or
                                 @datatype='int' or @datatype='long' or 
                                 @datatype='short'"/>
         </xsl:variable>

         <xsl:call-template name="reportResult">
            <xsl:with-param name="item">2.2.4c</xsl:with-param>
            <xsl:with-param name="status" select="$tstat"/>
            <xsl:with-param name="desc">
               <xsl:value-of select="$el"/>
               <xsl:text>with ucd='OBS_ANG-SIZE' must be of decimal </xsl:text>
               <xsl:text>type (double, float, int, long, short)</xsl:text>
            </xsl:with-param>
         </xsl:call-template>

         <xsl:variable name="dstat">
            <xsl:copy-of select="@datatype='double'"/>
         </xsl:variable>

         <xsl:call-template name="reportResult">
            <xsl:with-param name="item">2.2.4d</xsl:with-param>
            <xsl:with-param name="status" select="$tstat"/>
            <xsl:with-param name="type">warn</xsl:with-param>
            <xsl:with-param name="desc">
               <xsl:text>Recommend datatype='double' for </xsl:text>
               <xsl:value-of select="$el"/>
               <xsl:text>with ucd='OBS_ANG-SIZE'.</xsl:text>
            </xsl:with-param>
         </xsl:call-template>

         <xsl:apply-templates select="." mode="checkDegUnit">
            <xsl:with-param name="item">2.2.4e</xsl:with-param>
            <xsl:with-param name="ucd">OBS_ANG-SIZE</xsl:with-param>
            <xsl:with-param name="el" select="$el"/>
         </xsl:apply-templates>
      </xsl:for-each>
   </xsl:template>

   <!--
     -  make sure the ID column values are unique
     -  @context TABLE
     -->
   <xsl:template name="T2.2.1d">
      <xsl:variable name="colnum">
         <xsl:for-each select="v:FIELD">
            <xsl:if test="@ucd='ID_MAIN'">
               <xsl:copy-of select="position()"/>
            </xsl:if>
         </xsl:for-each>
      </xsl:variable>

      <xsl:variable name="match">
         <xsl:for-each 
              select="v:DATA/v:TABLEDATA/v:TR">
            <xsl:variable name="row">
               <xsl:value-of select="position()"/>
            </xsl:variable>
            <xsl:variable name="val" select="v:TD[position()=number($colnum)]"/>
            <xsl:choose>
               <xsl:when test="../v:TR[position()!=number($row)]/v:TD[position()=number($colnum) and .=$val]">0</xsl:when>
               <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
         </xsl:for-each>
      </xsl:variable>

      <xsl:variable name="stat">
         <xsl:value-of select="not(contains($match,'0'))"/>
      </xsl:variable>
      
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">2.2.1d</xsl:with-param>
         <xsl:with-param name="status" select="$stat"/>
         <xsl:with-param name="desc">
            <xsl:text>Values in the ID_MAIN column must be unique.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!--
     -  Recommend full annotation of a FIELD
     -->
   <xsl:template match="v:FIELD|v:PARAM" mode="checkFieldAnnot">
      <xsl:variable name="name">
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
      </xsl:variable>

      <xsl:variable name="stat">
         <xsl:value-of select="@datatype and @ucd and v:DESCRIPTION"/>
      </xsl:variable>
      
      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">2b</xsl:with-param>
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
            <xsl:with-param name="item">2c</xsl:with-param>
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
     -  handle some miscellaneous recommendations
     -->
   <xsl:template name="T2.3">
      <xsl:variable name="stata">
         <xsl:copy-of select="count(/v:VOTABLE/v:DESCRIPTION)>0"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">2.3a</xsl:with-param>
         <xsl:with-param name="status" select="$stata"/>
         <xsl:with-param name="type">rec</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Recommend including DESCRIPTION under VOTABLE </xsl:text>
            <xsl:text>indicating catalog name and that it is a Cone </xsl:text>
            <xsl:text>Search response.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>

      <xsl:variable name="statb">
         <xsl:copy-of 
              select="count(/v:VOTABLE/v:RESOURCE/v:TABLE/v:DESCRIPTION)>0"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">2.3b</xsl:with-param>
         <xsl:with-param name="status" select="$statb"/>
         <xsl:with-param name="type">rec</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Recommend including DESCRIPTION under TABLE </xsl:text>
            <xsl:text>that describes what each row in the table represents.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>

      <xsl:if test="$queryType='sr0'">
         <xsl:variable name="statc">
           <xsl:copy-of 
           select="not(/v:VOTABLE/v:RESOURCE/v:TABLE/v:DATA/v:TABLEDATA/v:TR)"/>
         </xsl:variable>

         <xsl:call-template name="reportResult">
            <xsl:with-param name="item">2.3c</xsl:with-param>
            <xsl:with-param name="status" select="$statc"/>
            <xsl:with-param name="type">rec</xsl:with-param>
            <xsl:with-param name="desc">
               <xsl:text>Recommend an empty table (no TR elements) </xsl:text>
               <xsl:text>when SR=0.</xsl:text>
            </xsl:with-param>
         </xsl:call-template>
      </xsl:if>
   </xsl:template>

   <xsl:template name="T3">
      <xsl:variable name="stata">
         <xsl:copy-of 
              select="count(/v:VOTABLE/v:INFO[@name='Error'] |
                            /v:VOTABLE/v:RESOURCE/v:INFO[@name='Error'] |
                            /v:VOTABLE/v:DEFINITIONS/v:PARAM[@name='Error']  |
                            /v:VOTABLE/v:RESOURCE/v:PARAM[@name='Error'] )=1"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">3a</xsl:with-param>
         <xsl:with-param name="status" select="$stata"/>
         <xsl:with-param name="desc">
            <xsl:text>Error message must appear in an INFO or PARAM </xsl:text>
            <xsl:text>with name='Error' in required location.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>

      <xsl:variable name="statb">
         <xsl:copy-of select="count(//v:PARAM | //v:INFO)=1"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">3b</xsl:with-param>
         <xsl:with-param name="status" select="$statb"/>
         <xsl:with-param name="type">warn</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Error response should only contain one PARAM.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>

<!--
  - Spec is too ambiguous about location of error PARAM, and therefore is too
  -  ambiguous about whether there should be a RESOURCE element.
  -
      <xsl:variable name="statc">
         <xsl:copy-of select="count(/v:VOTABLE/v:RESOURCE)=0"/>
      </xsl:variable>

      <xsl:call-template name="reportResult">
         <xsl:with-param name="item">3c</xsl:with-param>
         <xsl:with-param name="status" select="$statc"/>
         <xsl:with-param name="type">rec</xsl:with-param>
         <xsl:with-param name="desc">
            <xsl:text>Recommend not including a RESOURCE in an Error </xsl:text>
            <xsl:text>response.</xsl:text>
         </xsl:with-param>
      </xsl:call-template>
  -->

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

</xsl:stylesheet>
