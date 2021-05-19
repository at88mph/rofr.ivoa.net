IVOARegistry -- a client tool and library for querying a registry's
                IVOA standard search interface

The primary purpose of this package is to serve as a reference
implementation of a client library that can submit queries to a
registry using the IVOA standard search interface (Registry Interfaces
v1.0, http://www.ivoa.net/Documents/latest/RegistryInterfaces.html).
It also provides a tool, regsearch, that can be used to submit queries
on the command-line.  This includes simple keyword searches as well as
advanced searches using ADQL/s.  See doc/regsearch.txt for
documentation on this tool. 

CONTENTS

   README.txt            this document
   doc/regsearch.txt     command-line tool documentation
   src/java              library java source code
   lib/adql-1.0.jar      the ADQLlib conversion library
   build.xml             an Ant build file

REQUIREMENTS

To build and use this library, Java 1.5 is recommended.
This library has not been tested under Java 1.6 or later but these
should still work.  

To build the library you will need Ant (http://ant.apache.org/),
version 1.5.2 or later.  

This package ships with a precompiled version of the ADQLlib jar file
(in the lib directory).  If you need to rebuild this, you can retrieve
the source distribution via http://trac.us-vo.org/nvo/wiki/ADQLlib.  

This package also ships with precompiled versions of various Axis
(v1.2.1) and J2EE libaries that provide SOAP web service support (see
lib/axis and lib/j2ee respectively for copies of licenses).  If you
need new versions of these, visit the Axis
(http://ws.apache.org/axis/) and Sun (http://java.sun.com) web sites
respectively.  

INSTALLATION

To build the library and install the regsearch command-line tool, type:

  ant

The tools will be installed into the bin subdirectory by default and
will be ready for use.  This will also create a jar file,
ivoaregistry.jar, containing the library and install it into the lib
subdirectory.  The library's API documentation and install it into the
doc/api subdirectory.  

To install the package into another area, provide the "prefix" input
property to ant:

  ant -Dprefix=/usr/local/vo

USING REGSEARCH

Consult the doc directory for information on how to use the regsearch
command to query a registry.  In particular:

  doc/regsearch.txt    a man-page-like reference guide to the
                         command-line syntax 

  doc/tutorial.txt     examples and how-to for using regsearch
                         (adapted from the NVO Summer School).   






