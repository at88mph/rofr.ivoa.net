adqllib:  a library and tools for converting ADQL

NOTE: THIS PACKAGE IS DEPRECATED as it only supports ADQL v1.0 (rather
than the current standard v2.0).  It is made available only in support
of the ivoaregistry client package.  

This package provides a Java library that allows ADQL to be converted
between XML and string formats (i.e. ADQL/x and ADQL/s).  It can also
convert ADLQ/x into any native SQL dialect, and several are currently
supported.  Both version v0.7.4 and v1.0 of the standard are
supported.

This package provides several useful command-line tools:

   convertADQL   full access to ADQL conversion cababilities
   validate      will validate an ADQL/x (or any XML Schema
                   supported) document against the proper schema
   xalan         will apply an XSL stylesheet against an XML document 

This package also provides WSDLs for SOAP Web Service versions of
convertADQL; however, an implementation is not yet provided.  

CONTENTS

   build.xml       ant build file
   src/java        Java source code
   src/scripts     tool wrapper scripts for Unix/Mac and Windows
   samples/v0.7.4  sample ADQL/x and ADLQ/s queries in v0.7.4 format
   samples/v1.0    sample ADQL/x and ADLQ/s queries in v1.0 format
   etc/v0.7.4      schemas, stylesheets, & WSDLs supporting v0.7.4
   etc/v1.0        schemas, stylesheets, & WSDLs supporting v1.0
   lib             library directory containing jar files
   doc/api*        API documentation for the ADQL conversion library
   dist*           (empty)

*after installation

REQUIREMENTS

To build and use this library, Java 1.5 is strongly recommended.
Because the evolving internal support for Xerces (the Apache XML
parser) and Xalan (the Apache XSLT engine), not all of the Java 1.4
releases work properly.  See http://trac.us-vo.org/nvo/wiki/ADQLlib
for additional tips for building and running under Java 1.4.  
This library has not been tested under Java 1.6 or later.  

To build the library you will need Ant (http://ant.apache.org/),
version 1.5.2 or later. 

INSTALLATION

To build the library and install the tools, type:

  ant

The tools will be installed into $NVOSS_HOME/bin and will be ready for
use.  This will also create the library's API documentation and
install it into the doc/api directory.

USING THE TOOLS

convertADLQ

Examples:

convert ADQL/x to ADQL/s:
  convertADQL -x samples/v1.0/adql-simple.xml          # Linux/Mac/Unix
  cat samples/v1.0/adql-simple.xml | convertADQL -X

  convertADQL -x samples/v1.0/adql-simple.xml          # Windows
  type samples\v1.0\adql-simple.xml | convertADQL -X

convert ADQL/s to ADQL/x:
  convertADQL -s samples/v1.0/adql-simple.sql          # Linux/Mac/Unix
  cat samples/v1.0/adql-simple.sql | convertADQL -S

  convertADQL -s samples\v1.0\adql-simple.sql          # Windows
  type samples/v1.0/adql-simple.sql | convertADQL -S

convert version v0.7.4 files
  convertADQL -v v0.7.4 -x samples/v0.7.4/adql-simple.xml   # Linux/Mac/Unix
  convertADQL -v v0.7.4 -s samples/v0.7.4/adql-simple.sql

  convertADQL -v v0.7.4 -x samples\v0.7.4\adql-simple.xml   # Windows
  convertADQL -v v0.7.4 -s samples\v0.7.4\adql-simple.sql

General Usage:

convertADQL -X|-S|-x xmlfile|-s sqlfile [-o outfile] [-t transformer]
              [-c config] [sql...]
Options:
  -X              read and convert XML from standard input
  -x xmlfile      read and convert XML from xmlfile
  -S              read and convert SQL from command line or standard input
  -s sqlfile      read and convert SQL from sqlfile
  -o outfile      write results to output file; if not given, write to
                     standard out
  -v version      the ADQL version to assume (v0.7.4, v1.0; default: v1.0)
  -c config       load customized config file
Arguments:
  sql             ADQL/s string to convert with -S; if not given, read from
                      standard in

validate

Examples:
 validate samples/v1.0/adql-simple.xml    # Linux/Max/Unix
 validate samples\v1.0\adql-simple.xml    # Windows

If all you see is 

 samples/v1.0/adql-simple.xml: 1200 ms (5 elems, 4 attrs, 0 spaces, 26 chars)

then the file is valid; otherwise, the tool will print messages
enumerating the sytax violations.

xalan

Examples:
  xalan -in sample/v1.0/adql-simple.xml \
    -xsl etc/v1.0/stylesheets/ADQLx2MySQL-v1.0.xsl

Usage:
  xalan -xsl stylesheet [-in inputxmlfile] [-out outputfile]


