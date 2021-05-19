# IVOA RofR
This directory contains the IVOA Registry of Registries project.

## Description
The `ivoa-rofr` consists of a number of modules.  These modules are deployed across an HTTP server, a Java Servlet container, and assorted scripts and applications used as CGIs.  The modules are listed below:

### Repository_Explorer-2.0-1.46
C applications which provides OAI validation.

### adql-1.0
Java library which provides ADQL V1.0 parsing functionality.

### dalvalidate
Java servlets and library that provides validation.

### ivoaharvest
Java servlets that provide validation.  Depends upon dalvalidate module.

### ivoaregistry
Java library that provides searchable registry functionality.

### junx-master
Java library which provides XML validation.

### rofrtar
Final packaging of the ivoa-rofr application to be deployed on the HTTP server.

## Configuration
Configuration parameters are listed in the top stanza of ./deploy_env.properties.

rofr.base.url
       Base URL of the ivoa-rofr on the HTTP server.  

rofr.email
       Email contact for ivoa-rofr questions.

rofr.home.dir
       Installation directory of the ivoa-rofr project on the HTTP server.

## Building
After the ivoa-rofr has been configured for the environment in which it will be deployed, run the following:

```shell
$ ant install
```

This will compile, test, package, and install on your local repository the various components of the ivoa-rofr.

## Installing
See below for 
1. Deploy/configure to Apache HTTPD Server
2. Deploy to Apache Tomcat Server

## Deploy/configure to Apache HTTPD Server
1. Copy `ivoa-rofr.tar` from the local Ivy repository, `~/.ivy2/local/nvo/ivoa-rofr/<VERSION>/tars/ivoa-rofr.tar` to the parent directory of `rofr.home.dir` on the HTTP server host.  
1. Untar the `ivoa-rofr.tar`. 
1. Set IVOA_ROFR_HOME environment variable to the value of rofr.home.dir.
1. Edit the Apache HTTP server configuration to support ivoa-rofr requirements.  See `httpd.conf` below.
1. Copy the workers.properties file to the Apache HTTP server configuration directory.
    1. `$ cp ~/.ivy2/local/nvo/ivoa-rofr/1.0/tars/ivoa-rofr.tar /home/rofr/pkgs/`
    1. `$ cd /home/rofr/pkgs`
       `$ gtar xf ivoa-rofr.tar`
    1. `$ setenv IVOA_ROFR_HOME /home/rofr/pkgs`
    1. Edit existing `$HTTPD_HOME/conf/httpd.conf` files as necessary
    1. `$ cp $IVOA_ROFR_HOME/apache_conf/workers.properties $HTTPD_HOME/conf`

### httpd.conf
A sample `http.conf` file is provided in the apache_conf subdirectory.  Alternatively, the modifications to the default httpd.conf 
file required by ivoa-rofr are outlined below:

* Ensure the following LoadModule directives are present and uncommented.
```
LoadModule rewrite_module mod_rewrite.so
LoadModule jk_module mod_jk.so
```
* Following the Module directives add JK Module directives
```
JkWorkersFile conf/workers.properties
JkShmFile logs/mod_jk.shm
JkLogFile logs/mod_jk.log
JkLogLevel info
JkLogStampFormat "[%a %b %d %H:%M:%S %Y] "
```
* Add a VirtualHost directive
```xml 
<VirtualHost *:8090>
   ServerName      newdevel7.cfa.harvard.edu
   ServerAlias     newdevel7.cfa.harvard.edu
   ServerAdmin     mtibbetts@cfa.harvard.edu
   DocumentRoot    ${IVOA_ROFR_HOME}/html
   DirectoryIndex  index.html
   ScriptAlias     /cgi-bin/ "${IVOA_ROFR_HOME}/cgi-bin/"
   ErrorLog        ${IVOA_ROFR_HOME}/var/logs/error_log
   CustomLog       ${IVOA_ROFR_HOME}/var/logs/access_log combined
   JkMount         /regvalidate* rofr
   
   RewriteEngine   On
   RewriteOptions  Inherit
   RewriteRule ^/rofr/(.*) /$1
   RewriteRule ^/cgi-bin/rofr/(.*) /cgi-bin/$1
   RewriteRule ^/VO/services/(cs|sia)validate.html http://newdevel7.cfa.harvard.edu:8090/dalvalidate/$1validate.html [L,R=301]
</VirtualHost>
```
* Add the following Directory directive to make the ivoa-rofr html directory accessible to the httpd server.
```xml
<Directory "${IVOA_ROFR_HOME}/html">
    Options Indexes FollowSymLinks
    AllowOverride All
    Require all granted
</Directory>
```
* Add the following Directory directive to make the ivoa-rofr cgi-bin directory accessible to the httpd server.
```xml 
<Directory "${IVOA_ROFR_HOME}/cgi-bin">
    AllowOverride None
    Options FollowSymLinks
    Require all granted
</Directory>
```

## Deploy to Apache Tomcat Server
`$ scp ~/.ivy2/local/nvo/ivoaharvest/1.0/wars/regvalidate.war $CATALINA_HOME/webapps`
