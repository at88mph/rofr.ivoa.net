# Makefile for OAI Repository Explorer
# July 2005
# Hussein Suleman

# variables
include ../deploy_env.properties

ifeq ($(wildcard /data/rofr/devel/$(USER)/ivy),) 
  IVY_DEFAULT_IVY_USER_DIR =
else 
  IVY_DEFAULT_IVY_USER_DIR = -Divy.default.ivy.user.dir=/data/rofr/devel/$(USER)/ivy
endif

RESOLVER	= local
STATUS		= integration
OVERWRITE	= false
JAVA		= java
CFLAGS		= -O2 -Iexpat/lib -Wno-deprecated
C++		   = g++

LDFLAGS		=

LFLAGS		= -lcurl

# rules
# list the headers to get the dependencies right.
# needs to be refactored if the includes change
# run 'g++ -MM about.c' to get new dependencies after changing the includes
about.o: about.c blocks.h cgi.h attlist.h config.h language.h
	$(C++) $(CFLAGS) -o $*.o -c $<
addarchive.o: addarchive.c blocks.h cgi.h attlist.h config.h language.h
	$(C++) $(CFLAGS) -o $*.o -c $<
attlist.o: attlist.c attlist.h escape.h
	$(C++) $(CFLAGS) -o $*.o -c $<
blocks.o: blocks.c config.h language.h
	$(C++) $(CFLAGS) -o $*.o -c $<
cgi.o: cgi.c cgi.h attlist.h
	$(C++) $(CFLAGS) -o $*.o -c $<
comply.o: comply.c config.h language.h taglist.h attlist.h validate.h \
 xml.h
	$(C++) $(CFLAGS) -o $*.o -c $<
convert.o: convert.c
	$(C++) $(CFLAGS) -o $*.o -c $<
convpredef.o: convpredef.c
	$(C++) $(CFLAGS) -o $*.o -c $<
escape.o: escape.c
	$(C++) $(CFLAGS) -o $*.o -c $<
language.o: language.c attlist.h language.h
	$(C++) $(CFLAGS) -o $*.o -c $<
parser2.o: parser2.c config.h escape.h language.h parser2.h taglist.h \
 attlist.h
	$(C++) $(CFLAGS) -o $*.o -c $<
parser.o: parser.c escape.h language.h parser.h taglist.h attlist.h
	$(C++) $(CFLAGS) -o $*.o -c $<
replace.o: replace.c replace.h
	$(C++) $(CFLAGS) -o $*.o -c $<
sortlist.o: sortlist.c sortlist.h
	$(C++) $(CFLAGS) -o $*.o -c $<
taglist.o: taglist.c taglist.h attlist.h
	$(C++) $(CFLAGS) -o $*.o -c $<
testoai.o: testoai.c blocks.h cgi.h attlist.h config.h language.h \
 parser.h taglist.h parser2.h sortlist.h validate.h xml.h
	$(C++) $(CFLAGS) -o $*.o -c $<
validate.o: validate.c config.h replace.h taglist.h attlist.h validate.h \
 xml.h
	$(C++) $(CFLAGS) -o $*.o -c $<
xml.o: xml.c expat/lib/expat.h expat/lib/expat_external.h taglist.h \
 attlist.h
	$(C++) $(CFLAGS) -o $*.o -c $<

# targets

OBJECTS = xml.o attlist.o taglist.o escape.o language.o \
          validate.o replace.o comply.o blocks.o

OBJECTS2 = cgi.o xml.o attlist.o taglist.o parser.o escape.o language.o \
           validate.o replace.o testoai.o sortlist.o blocks.o parser2.o

OBJECTS3 = cgi.o attlist.o language.o addarchive.o blocks.o escape.o

OBJECTS4 = cgi.o attlist.o language.o blocks.o about.o escape.o

XMLOBJECTS = expat/lib/xmlparse.o \
	  expat/lib/xmltok.o \
	  expat/lib/xmlrole.o

TARGET = comply
TARGET2 = testoai
TARGET3 = addarchive
TARGET4 = about

all: $(TARGET) $(TARGET2) $(TARGET3) $(TARGET4)
	@echo Finis.

dist: oai-repository-explorer.tar.gz

install: publish

config.h: config.h.in
	sed -e "s,@rofr_base_url@,$(rofr.base.url),g; s,@rofr_email@,$(rofr.email),g; s,@rofr_home@,$(rofr.home.dir),g; s,@python_path@,$(python.path),g; s,@http_proxy@,$(http.proxy),g; s,@https_proxy@,$(https.proxy),g;" $< > $@

$(TARGET): config.h $(OBJECTS) $(XMLOBJECTS) 
	$(C++) $(LDFLAGS) -o $(TARGET) $(OBJECTS) $(XMLOBJECTS) $(LFLAGS)

$(TARGET2): config.h $(OBJECTS2) $(XMLOBJECTS) 
	$(C++) $(LDFLAGS) -o $(TARGET2) $(OBJECTS2) $(XMLOBJECTS) $(LFLAGS)

$(TARGET3): config.h $(OBJECTS3) 
	$(C++) $(LDFLAGS) -o $(TARGET3) $(OBJECTS3) $(LFLAGS)

$(TARGET4): config.h $(OBJECTS4) 
	$(C++) $(LDFLAGS) -o $(TARGET4) $(OBJECTS4) $(LFLAGS)

$(XMLOBJECTS):
	cd expat ; make ; cd ..

oai-repository-explorer.tar.gz: comply enus.lan toolkit.xsd lynx.pl resolve
	-rm -fr oai-repository-explorer/
	mkdir -p oai-repository-explorer/bin/
	mkdir -p oai-repository-explorer/lib/
	cd oai-repository-explorer/bin
	ln comply oai-repository-explorer/bin/comply
	ln enus.lan oai-repository-explorer/bin/enus.lan
	ln toolkit.xsd oai-repository-explorer/bin/toolkit.xsd
	ln lynx.pl oai-repository-explorer/bin/lynx.pl
	ln xerces/validate.sh oai-repository-explorer/bin/validate.sh
	ln lib/xercesImpl.jar oai-repository-explorer/lib/
	ln lib/xercesSamples.jar oai-repository-explorer/lib/
	ln lib/xml-apis.jar oai-repository-explorer/lib/
	tar czf oai-repository-explorer.tar.gz oai-repository-explorer/
	-rm -fr oai-repository-explorer/

resolve: ../ivysettings.xml ivy.xml 
	$(JAVA) $(IVY_DEFAULT_IVY_USER_DIR) -jar antlib/ivy.jar -settings ../ivysettings.xml -ivy ivy.xml -retrieve 'lib/[artifact](-[classifier]).[ext]'

publish: dist ../ivysettings.xml ivy.xml 
	if [ "$(STATUS)" == "integration" ]; \
	then revision=`date +%Y%m%d%H%M%S`; \
	else revision=`sed -e '/#define reversion "/!d' -e 's/#define reversion "\([^"]*\)"/\1/' -e q config.h`; \
	fi; \
	$(JAVA) $(IVY_DEFAULT_IVY_USER_DIR) -jar antlib/ivy.jar -settings ../ivysettings.xml -ivy ivy.xml -publish $(RESOLVER) -publishpattern "[module].[ext]" -revision $$revision -status $(STATUS) -overwrite $(OVERWRITE)

clean:
	cd expat ; make clean ; cd ..
	rm -f *.o config.h

distclean: veryclean
	rm -rf oai-repository-explorer.tar.gz ivy-*.xml lib/

veryclean:
	cd expat ; make clean ; cd ..
	rm -f *.o config.h
	rm -f $(TARGET)
	rm -f $(TARGET2)
	rm -f $(TARGET3)
	rm -f $(TARGET4)

