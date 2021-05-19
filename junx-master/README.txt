Junx.  It stands for "Java XML Utilities from NCSA".  

Raymond Plante
National Center for Supercomputing Applications
National Virtual Observatory

INTRODUCTION

The genesis of this utilites package comes from work supporting the
NCSA BIMA Archive and the National Virtual Observatory (NVO).  It
contains general purpose XML utilities that I have found useful on
multiple occasions.  These include

Validate (ncsa.xml.validation):
   an application that can be run from the command line to validate
   XML documents against their XML Schemas.

SchemaLocation (ncsa.xml.validation): 
   A class that provides a mechanism for maintaining a local cache of 
   XML Schema documents (xsd) that a validating parser can pull a
   schema from in lieu of downloading it from the document-specified 
   location.  The ValidationUtils class is used to load the schemas
   into a JAXP parser.  

ExtractingParser (ncsa.xml.extractor):  
   an XML parser that can be used to split nodes of an XML document
   out into separate documents.  In particular, this parser will
   ensure that all of the namespace prefixes needed by the output
   parts are carried over, using the original prefixes, regardless of
   where in the document they have beed defined.  

SAXFilteredReader (ncsa.xml.saxfilter):
   This reader sends an XML stream through a SAX parser that can,
   though a user-provided content handler, alter the stream in near-
   arbitrary ways, such as insert text, skip nodes, or substitute
   elements with data from another file.  The text that reaches the
   client through the Reader interface, however, is not a
   re-serialized version, but the original character stream (apart
   from the changes made by the content handler), including the
   original spacing, namespace prefixes, etc.  

NamespaceMap (ncsa.xml.sax):
   A class that can keep track of the current set of namespace-prefix
   mappings in scope while parsing an XML document (e.g. using SAX).  

SAX2XML (ncsa.xml.sax):
   A class that translates SAX events back into an XML character
   stream.  


BUILDING JUNX

To build this package you will need:

  1.  Java SDK (available from https://java.sun.com):

      a.  If you want to use Validate to validate XML documents or 
          SchemaLocation to manage a local cache of XML Schema 
          documents (xsd) with a validating (JAXP) parser, then you
          must have:

              Java SDK 1.5 or later 

          (See also USING JUNX below).

      b.  Otherwise, you can use:

              Java SDK 1.4.2 or later

  2.  Apache Ant 1.6.5 or later

Once you have installed these, you need to set up your environment to
use them by setting up some environment variables to point to where
these packages are installed.  Choose and modifying the appropriate
example below [1]:

   On Linux/Unix/MacOS (bash), use these commands:

      export ANT_HOME=/usr/local/ant
      export JAVA_HOME=/usr/local/jdk-1.5.0.05
      export PATH=${PATH}:${ANT_HOME}/bin

   On Linux/Unix/MacOS (csh/tcsh):

      setenv ANT_HOME /usr/local/ant
      setenv JAVA_HOME /usr/local/jdk/jdk-1.5.0.05
      set path=( $path $ANT_HOME/bin )

   On Windows:

      set ANT_HOME=c:\ant
      set JAVA_HOME=c:\jdk-1.5.0.05
      set PATH=%PATH%;%ANT_HOME%\bin

[1] From the Ant User's Manual.  

To build, type "ant".  The result will be a JAR file called
lib/junx.jar that is ready for use.  

USING JUNX

To use the Junx library, you can set your CLASSPATH variable to
include lib/junx.jar.  

If you want to use the ValidationUtils class to maintain a local cache
of XML Schema documents to validate documents, then you should use Java
1.5 or later.  Java 1.4.2 will not work out of the box.  With the
proper version of Xerces installed, it is possible to get it to work.  

API DOCUMENTATION

The Java API documentation is built by default and installed into the
docs/japi directory.  To view, open doc/japi/index.html into a web
browser.  

