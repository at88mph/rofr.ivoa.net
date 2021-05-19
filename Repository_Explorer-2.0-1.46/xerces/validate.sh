#!/bin/sh

bindir="$(dirname $0)"
libdir="$bindir/../lib"

classpath="$libdir/xercesImpl.jar:$libdir/xercesSamples.jar:$libdir/xml-apis.jar"

java -classpath $classpath dom.Counter -S "$1"
 
