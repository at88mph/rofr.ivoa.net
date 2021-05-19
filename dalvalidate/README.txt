DALValidater
http://trac.us-vo.org/project/nvo/wiki/DALValidater

   Designed and initially developed by Raymond Plante (NCSA/NVO)
   Contributors:
      Aurelien Stebe (ESA/Euro-VO):  SSA/SLA validaters

INTRODUCTION

The Java package provides an extensible toolkit for validating the
compliance of DAL services with their corresponding standards.  In
addition to an Java API for validating services programmatically, this
package also provides URL-based web service interfaces that are to be
deployed as Java servlets in a web servlet container (such as Apache
Tomcat).   

Currently, this toolkit provides full implementations for validating
the DAL services of the following types:

   *  Simple Cone Search (SCS)
   *  Simple Image Access (SIA)

In addition, working but not yet complete validation is also supported
for:

   *  Simple Spectral Access (SSA)*
   *  Simple Line Access (SLA)*

These validaters use very similar implementations in which all
validation tests are applied as XSL stylesheet templates against
VOTable responses from the service.  This makes it easier to correct
or add to the tests (provided one knows XSL) without recompiling
code.  However, the use of XSL in the implementation of a validater is
not required; the toolkit provides decomposable classes that can
be combined in different ways (as the IVOAHarvester packages does, 
http://trac.us-vo.org/nvo/wiki/IVOAHarvester) as well as base classes
and interfaces that can be overridden to test a specific type of
service. 

BUILDING DALVALIDATE

To build this package you will need:
  1.  Java SDK 1.5.0 or later (available from https://java.sun.com)
  2.  Apache Ant 1.6.5 or later

If you obtained this package by ckecking it out of the NVO SVN
repository, then you will need to obtain a copy of the Junx library
(http://trac.us-vo.org/nvo/wiki/Junx); place a copy of junx.jar into
this package's lib directory.  

If you plan to install the validater web services, you will need a
servlet container package, such as Apache Tomcat
(http://tomcat.apache.org/).  This release of the servlets is known to
work with Tomcat v. 5.5.  In particular, to build the web service
code, you need a copy of the Java Servlet API (a copy of which is
included with Tomcat).

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

To build the web services, you should also do one of the following:

  *  install Tomcat and set the CATALINA_HOME environment variable (in
     the same way as above) to the directory where Tomcat is
     installed, OR

  *  obtain a copy of the Java Servlet API jar file and edit the
     build.xml file to set its file location.

It is not necessary to set your Java CLASSPATH environment variable to
build this library.  (The Ant User's Manual discourages setting the
CLASSPATH.) 

To build, type "ant".  The result will be a JAR file called
build/dalvalidate.jar that is ready for use.  

A separate command is needed to build the validation servlets; type
"ant war" to build the WAR file, dist/dalvalidate.war, that contains the
servlets.  

INSTALLING AND USING THE DALVALIDATE WEB SERVICES

Deploying the validater services is usually as simple as copying the
WAR file, dalvalidate.war, from the dist subdirectory to the
appropriate directory in the servlet container's web applications
directory.  If you are using Apache Tomcat, you would copy the WAR
file to the webapps subdirectory where Tomcat is installed.  

The WAR file contains a configuration such that web page interfaces to
the DAL services are accessible via:

    <container-baseURL>/dalvalidate/csvalidate.html
    <container-baseURL>/dalvalidate/siavalidate.html
    <container-baseURL>/dalvalidate/ssavalidate.html
    <container-baseURL>/dalvalidate/slavalidate.html

where <container-baseURL> is the base URL for servlets and web
applications for the servlet engine.  For example, for an out-of-the
box installation of Tomcat, the SCS validation interface is accessible
via: 

    http://localhost:8080/dalvalidate/csvalidate.html

The underlying servlets can be accessed directly a CGI service with
arguments for use by non-browser clients.  

The SCS Validater:

The SCS validater web service can be accessed programmatically via the
base URL:

    <container-baseURL>/dalvalidate/ConeSearchValidater?

This base URL is controlled by the web.xml file inside the WAR file.  

This service supports the following arguments:

    [documentation in progress; run browser-based interface for examples] 

The SIA Validater:

The SIA validater web service can be accessed programmatically via the
base URL:

    <container-baseURL>/dalvalidate/SIAValidater?

This base URL is controlled by the web.xml file inside the WAR file.  

This service supports the following arguments:

    [documentation in progress; run browser-based interface for examples] 

The SSA Validater:

The SSA validater web service can be accessed programmatically via the
base URL:

    <container-baseURL>/dalvalidate/SSAValidater?

This base URL is controlled by the web.xml file inside the WAR file.  

This service supports the following arguments:

    [documentation in progress; run browser-based interface for examples] 

The SLA Validater:

The SLA validater web service can be accessed programmatically via the
base URL:

    <container-baseURL>/dalvalidate/SLAValidater?

This base URL is controlled by the web.xml file inside the WAR file.  

This service supports the following arguments:

    [documentation in progress; run browser-based interface for examples] 

CREATING NEW VALIDATERS WITH THIS TOOLKIT

[documentation in progress]  

Creating an XSL-driven validater:

1.  Create a XSL testing stylesheet.  

    This can be modeled after src/xsl/checkSIA.xsl.  Simple DAL
    services return VOTables, so for this type you will have to create
    versions that support the different versions of VOTable, as was
    done for SIA.  Start, however, with the version for simple
    DTD-based VOTables.  

    [more...]

2.  The testing stylesheet produces results in XML format.  To provide
    support for other formats (like HTML and plain text), create a
    presentation stylesheet.  This can be trivially adapted from
    src/xsl/presentation/Results-SIA-*.xsl (e.g. Results-SIA-html.xsl).

3.  Create a configuration file for the test.  This can be modelled
    after conf/WebAppValidateSIA.xml.  

4.  Test the validater stylesheets against a real service using the 
    testing application, TestSimpleIVOAServiceValidater.  

    First be sure the tests are compiled:

       ant compileTests

    Then run TestSimpleIVOAServiceValidater with your configuration
    file:

       java -classpath ./tclasses:./lib/dalvalidate.jar:./lib/junx.jar \
          org.nvo.service.validation.TestSimpleIVOAServiceValidater    \
          my-config-file.xml http://service.net/baseURL/to/test 

    The test results produced by your stylesheet will be sent to
    standard out.  

5.  If you want to deploy the validater as a web service, create
    implementations of the ValidationSession and ValidaterWebApp
    interfaces.  These can be modelled trivially after
    src/java/org/nvo/sia/validate/SIAValidationSession.java and 
    src/java/org/nvo/sia/validate/SIAValidaterWebApp.java,
    respectively.  

    You should then create the web page interface (model after
    web/siavalidate.html). 

    Finally, edit the conf/web.xml file to add your new validater web
    service and edit build.xml to add the instructions for
    incorporating all the required files into the WAR file.  

Other Approaches to Validater Implementation:

To produce a non-XSL-driven validater (or to affect how it is done for
a particular service), you will need to create new extensions of some
of the basic API classes.  For this, you will need to understand how
more about the different classes in the library and how to connect
them together.  An overview of this is provided in the next section.  

HOW THE LIBRARY WORKS

[documentation in progress]

