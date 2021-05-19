<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                version="1.0">

   <xsl:output method="text" encoding="UTF-8" />

   <xsl:template match="/">
Simple Spectral Access Verification Results
Base URL of Service: <xsl:value-of select="/*/@baseURL"/>

<xsl:choose>
   <xsl:when test="count(SSAValidation/testQuery)=1">
A single test query (<xsl:value-of 
   select="SSAValidation/testQuery/@options"/>) was sent to
the service.  The results are described below.  
   </xsl:when>
   <xsl:otherwise>

The following <xsl:value-of select="count(SSAValidation/testQuery)"/> test queries were sent to the service:

<xsl:for-each select="SSAValidation/testQuery">  o  <xsl:value-of select="@name"/>: <xsl:value-of select="@options"/><xsl:text>
</xsl:text> 
</xsl:for-each>
   </xsl:otherwise>
</xsl:choose>

<xsl:apply-templates select="SSAValidation/testQuery"/>
   </xsl:template>

   <xsl:template match="testQuery">
      <xsl:param name="n"/>
==============================================================================
Test Query Name: <xsl:value-of select="@name"/>
------------------------------------------------------------------------------
      <xsl:choose>
         <xsl:when test="@description">
Description:     <xsl:value-of select="@description"/></xsl:when>
         <xsl:when test="@name='user'">
Description:     test response from the user-provided query (assuming position 
                 is within service's coverage.) </xsl:when>
         <xsl:when test="@name='optional'">
Description:     test response to a query containing many optional parameters.  
                 If any are unsupported, the service should happily ignore 
                 them. </xsl:when>
         <xsl:when test="@role='badpos'">
Description:     test handling of an erroneous query (with a non-decimal 
                 position value). </xsl:when>
         <xsl:when test="@role='error'">
Description:     test handling of an erroneous query. </xsl:when>
      </xsl:choose>
Type:            <xsl:choose>
         <xsl:when test="@role='metadata'">metadata query (FORMAT=METADATA)</xsl:when>
         <xsl:when test="@role='error'">error handling</xsl:when>
         <xsl:otherwise>normal query</xsl:otherwise>
      </xsl:choose>
Arguments:       <xsl:value-of select="@options"/>
URL:  <xsl:value-of select="concat(/*/@baseURL,@options)"/>
VOTable version returned: <xsl:choose>
         <xsl:when test="test[@item='comm']">
            <xsl:text>none </xsl:text>
         </xsl:when>
         <xsl:when test="@votable-version">
            <xsl:value-of select="@votable-version"/>
         </xsl:when>
         <xsl:otherwise><xsl:text>unknown </xsl:text></xsl:otherwise>
      </xsl:choose>
Number of records returned: <xsl:choose>
         <xsl:when test="@recordCount">
            <xsl:value-of select="@recordCount"/>
         </xsl:when>
         <xsl:otherwise>n/a</xsl:otherwise>
      </xsl:choose><xsl:text>
</xsl:text>

      <xsl:if test="contains(@showStatus, 'fail')">
Compliance Errors:  <xsl:choose>
         <xsl:when test="test[@status='fail']">
            <xsl:text>
</xsl:text>
            <xsl:apply-templates select="test[@status='fail']" />
         </xsl:when>
         <xsl:otherwise>None found.</xsl:otherwise>
      </xsl:choose>
      </xsl:if>

      <xsl:if test="contains(@showStatus, 'warn')">
Warnings:  <xsl:choose>
         <xsl:when test="test[@status='warn']">
            <xsl:text>
</xsl:text>
            <xsl:apply-templates select="test[@status='warn']" />
         </xsl:when>
         <xsl:otherwise>None found.</xsl:otherwise>
      </xsl:choose>
      </xsl:if>

      <xsl:if test="contains(@showStatus, 'rec')">
Recommendations:  <xsl:choose>
         <xsl:when test="test[@status='rec']">
            <xsl:text>
</xsl:text>
            <xsl:apply-templates select="test[@status='rec']" />
         </xsl:when>
         <xsl:otherwise>No additional recommendations.</xsl:otherwise>
      </xsl:choose>
      </xsl:if>

      <xsl:if test="contains(@showStatus, 'pass')">
Passed Tests:  <xsl:choose>
         <xsl:when test="test[@status='pass']">
            <xsl:text>
</xsl:text>
            <xsl:apply-templates select="test[@status='pass']" />
         </xsl:when>
         <xsl:otherwise>Sadly, no passing test results detected.</xsl:otherwise>
      </xsl:choose>
      </xsl:if>

      <hr />
            
   </xsl:template>

   <xsl:template match="test">
      <xsl:variable name="text">
         <xsl:call-template name="fmttxt">
            <xsl:with-param name="text" select="."/>
            <xsl:with-param name="pre" select="'            '"/>
         </xsl:call-template>
      </xsl:variable>

      <xsl:text>   </xsl:text>
      <xsl:value-of select="@item"/>
      <xsl:text>.  </xsl:text>
      <xsl:value-of select="substring($text,13)"/>
    </xsl:template>

  <!-- 
    -  format a long string into multiple lines
    -  @param text     the input string
    -  @param width    the maxmimum length of each line
    -  @param pre      an optional string to prepend to each line.
    -->
  <xsl:template name="fmttxt">
     <xsl:param name="text"/>
     <xsl:param name="width" select="75"/>
     <xsl:param name="pre"></xsl:param>

     <xsl:choose>
        <xsl:when test="string-length($text) &gt; $width">

           <!-- input is longer than one line.  First, lop off and print 
                the first line.  -->
           <xsl:variable name="cutpoint">
              <xsl:call-template name="indexOfLast">
                 <xsl:with-param name="text" 
                                 select="substring($text, 1, $width)"/>
              </xsl:call-template>
           </xsl:variable>

           <xsl:value-of select="$pre"/>
           <xsl:value-of select="substring($text, 1, number($cutpoint)-1)"/>
           <xsl:text>
</xsl:text>

           <xsl:if test="number($cutpoint) &lt; string-length($text)">
           <!-- now recurse on the remaining text -->
              <xsl:call-template name="fmttxt">
                 <xsl:with-param name="text" 
                                 select="substring($text,number($cutpoint)+1)"/>
                 <xsl:with-param name="width" select="$width"/>
                 <xsl:with-param name="pre" select="$pre"/>
              </xsl:call-template>
           </xsl:if>
        </xsl:when>

        <xsl:otherwise>
           <!-- input line is less than max width, so just print it -->
           <xsl:value-of select="$pre"/>
           <xsl:value-of select="$text"/>
           <xsl:text>
</xsl:text>
           
        </xsl:otherwise>
     </xsl:choose>
  </xsl:template>

  <!--
    -  return the index of the last occurance of a substring.  If the 
    -  pattern does not occur, the length of the string plus one is returned.
    -  @param text    the input string to search
    -  @param pat     the substring to search for
    -->
  <xsl:template name="indexOfLast">
     <xsl:param name="text"/>
     <xsl:param name="pat" select="' '"/>
     <xsl:param name="sum" select="0"/>
     <xsl:param name="patlen" select="0"/>

     <xsl:choose>
        <xsl:when test="contains($text,$pat)">
           <xsl:variable name="pre" select="substring-before($text, $pat)"/>
           <xsl:variable name="post" select="substring-after($text, $pat)"/>
           <xsl:variable name="newsum" 
                select="$sum + number($patlen) + string-length($pre)"/>
           <xsl:call-template name="indexOfLast">
              <xsl:with-param name="text" select="$post"/>
              <xsl:with-param name="pat" select="$pat"/>
              <xsl:with-param name="sum" select="$newsum"/>
              <xsl:with-param name="patlen" select="string-length($pat)"/>
           </xsl:call-template>
        </xsl:when>
        <xsl:when test="$sum=0">
           <xsl:value-of select="string-length($text)+1"/>
        </xsl:when>
        <xsl:otherwise>
           <xsl:value-of select="$sum+1"/>
        </xsl:otherwise>
     </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
