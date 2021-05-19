echo off
REM NOTE: this Windows batch script has not been confirmed to work, yet.
REM
REM verify an XML instance document using Apache Xerces
REM
REM validate [ -qh ] [ -S schemaLocFile ] xmlfile ...
REM   -h      print this usage (ignore all other input)
REM   -q      print nothing to standard out; only set the exit code
REM   -s      print nothing to standard out or error; only set the exit code
REM   -S schemaLocFile  set the schema cache via a schema location file
REM Each line in a schemaLocFile gives a namespace, a space, and local file path.
REM The file path is the location of the Schema (.xsd) document for that namespace.
REM

set cp=""
if "%JUNX_JAR%" NEQ "" set cp=%JUNX_JAR%
if "%cp%"=="" (
    if "$JUNX_HOME" NEQ "" set cp=%JUNX_JAR%\lib\junx.jar
)
if "%cp%"=="" (
    if "$CLASSPATH" NEQ "" set cp=%CLASSPATH%
)
if "%cp%"=="" (
    echo Cannot find junx.jar (and no CLASSPATH set)
) else (
REM    echo ${bin}java -cp $cp ncsa.xml.validation.Validate $*
    exec ${bin}java -classpath $cp ncsa.xml.validation.Validate $*
)


