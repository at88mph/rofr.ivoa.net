ivoaharvest -- a reference software package for harvesting from IVOA
     Registries 

Raymond Plante
National Center for Supercomputing Applications
National Virtual Observatory

INTRODUCTION

The intention of this package is two-fold:

   * to demonstrate recommended methods for harvesting from IVOA
     Registries using the IVOA Recommendation for Registry Interfaces
     (version 1.0).  

   * to provide a toolkit for validating the compliance of a
     registry's harvesting interface to the IVOA standard.  

This toolkit can not only harvest from a single registry, but also
scan the entire VO by consulting the Registry of Registries.  

While this package is intended to be a reference implementation, it
should a practical tool for harvesting IVOA resource descriptions and
loading them into a local database.

This package is currently in a pre-release state.  Not all
functionality is completed.  Currectly available features include:

  o  Harvest all local VOResource records into a directory using the
       OAI Harvesting interface
  o  Iterate through VOResource records, loading them into DOM
       structures in memory.
  o  Validates compliance to the IVOA Registry Interfaces standard 
       (http://www.ivoa.net/Documents/latest/RI.html)

BUILDING IVOAHARVEST

To build this package you will need:
  1.  Java SDK 1.4.2 or later (available from https://java.sun.com)
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

It is not necessary to set your Java CLASSPATH environment variable to
build this library.  (The Ant User's Manual discourages setting the
CLASSPATH.) 

To build, type "ant".  The result will be a JAR file called
lib/ivoaharvest.jar that is ready for use.  

USING IVOAHARVEST

To use the ivoaharvest library, you can set your CLASSPATH variable to
include lib/ivoaharvest.jar.  Currently, you must also include
lib/junx.jar in your path as well.  

A stand-alone application for harvesting is available via the
Harvester class; it has the following usage:

   java net.ivoa.registry.harvest.Harvester baseOAIurl dir [basename]

where 

   baseOAIurl -- the base URL for the registry's OAI harvesting interface
   dir        -- a directory to write the VOResource records into
   basename   -- a filename prefix to use for the output records 
                   (default: "vor").  The files will be called basename_n.xml
                   where n is an integer.

API DOCUMENTATION

The Java API documentation is built by default and installed into the
docs/japi directory.  To view, open doc/japi/index.html into a web
browser.  

