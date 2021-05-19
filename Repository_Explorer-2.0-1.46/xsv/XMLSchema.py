# Copyright (C) 2000 LTG -- See accompanying COPYRIGHT and COPYING files
# Implementation of XML Schema on top of layer.py
# $Id: XMLSchema.py,v 1.196 2001/06/16 11:13:16 ht Exp $
import layer
import types
import string
import sys
import copy
import os
from urlparse import urljoin
import PyLTXML
import XML
import re
import xpath

vss=string.split("$Revision: 1.196 $ of $Date: 2001/06/16 11:13:16 $")
versionString="%s of %s %s"%(vss[1],vss[5],vss[6])

# For the time being, I force every schema document to be validated by the
# DTD for schemas, so there is no need to check for required daughters,
# attributes, etc.

# There are _3_ layers of representation here:
# 1) The XML elements themselves, instances of 'element',
#    usually held in a variable/property called 'elt'
# 2) Their normalised internal form, instances of e.g. complexTypeElt or anyElt
#    usually held in a variable/property called 'xrpr'
# 3) The corresponding schema component, instances of e.g. complexType or any,
#    usually held in a variable/property called 'component'

# The paradigm is that xxxElt.__init__ plugs in the schema and provides empty
# lists/dicts for daughters/attrs to go in, xxxElt.init (called once
# all daughters/attrs have been processed) creates and attaches component(s),
# component.init copies literal properties and attaches sub-components where
# possible

# Most components have properties with names based on those in the REC
# If a property is component-valued, it will have a clause in the
# component's class's __getattr_, which attempts to dereference the
# corresponding <prop>Name property, c.f. basetype/basetypeName for simpleType

# I've innovated one component not in the REC, namely attributeUse,
# parallel to particle

# Thinking about the interaction between lazy chasing of references and
# SVC constraints -- I think the REC isn't capable of a consistent
# interpretation here.  My inclination is to implement SVCs on an
# as-used basis.  I could try putting a 'checked' attribute on every component,
# dividing the computed attributes into literals and non-literals, and
# testing 'checked' before returning _any_ non-literals.
# Note that anything along this line means that schema errors may occur
# in the midst of instance validation :-(  What's the desired interaction
# with lax validation/validation outcome?

checkFacetTable={"minInclusive":'checkMin',
		 "minExclusive":'checkMin',
		 "maxInclusive":'checkMax',
		 "maxExclusive":'checkMax',
		 "totalDigits":'checkPS',
		 "fractionDigits":'checkPS',
		 "enumeration":'checkEnum',
		 "length":'vacuousCheck',
		 "minLength":'vacuousCheck',
		 "maxLength":'vacuousCheck',
		 "pattern":'vacuousCheck'}

XMLSchemaNS="http://www.w3.org/2001/XMLSchema"
XMLSchemaInstanceNS = "http://www.w3.org/2001/XMLSchema-instance"
syspat=re.compile("^[^[]* (SYSTEM|PUBLIC) ")
intpat=re.compile("^[^[]*\[([^\001]*)\]")

def newFactory():
  factory=layer.factory()
  cwd=string.replace(os.getcwd(),'\\','/')+'/'
  if cwd[1]==':':
    cwd='file:///'+cwd
  factory.fileNames=[cwd]
  factory.prepared=0
  factory.schemaStack=[]
  factory.eltStack=[]
  factory.schemas={}
  factory.schema=None
  factory.unprocessedImports={}
  factory.targetNS=None
  factory.processingInclude=0
  return factory

def fromFile(filename=None,factory=None,targetNS=None,fromInclude=0):
  if not factory:
    factory=newFactory()
    factory.targetNS=targetNS
  elif not factory.schema:
    factory.targetNS=targetNS
  else:
    # we save the defaults in case an <include>d schema changes them
    factory.schemaStack[0:0]=[(factory.schema,factory.processingInclude,
                               factory.schema.elementFormDefault,
                               factory.schema.attributeFormDefault,
                               factory.schema.blockDefault,
                               factory.schema.finalDefault)]
    if targetNS!=factory.schema.targetNS:
      if factory.schemas.has_key(targetNS):
	factory.schema=factory.schemas[targetNS]
      else:
	factory.schema=None
      factory.targetNS=targetNS
    factory.processingInclude=fromInclude
  fullFile=filename
  layErr=None
  if fullFile in factory.fileNames:
    res=None
    sys.stderr.write("%s attempts to include/import/redefine itself, ignored"%fullFile)
  else:
    factory.fileNames[0:0]=[fullFile]
    try:
      f=PyLTXML.Open(fullFile,PyLTXML.NSL_read|PyLTXML.NSL_read_namespaces)
      if f:
        dts=f.doctype.doctypeStatement
        if dts and syspat.match(dts):
          # let them use their own doctype
          ff=None
          fake=None
        else:
          intDecls=""
          if dts:
            mres=intpat.match(dts)
            if mres:
              intDecls=mres.group(1)
          prefs=[]
          scpFound=0
          b=PyLTXML.GetNextBit(f)
          while (b and b.type not in ('start','empty')):
            if b.type=='bad':
              b=None
              break
            b=PyLTXML.GetNextBit(f)
          if not b:
            sys.stderr.write("%s has no elements???\n"%fullFile)
            raise PyLTXML.error
          for (key,val) in b.item.nsdict.items():
            if val==XMLSchemaNS:
              scpFound=1
              scp=key
            elif key=='xml' and val=='http://www.w3.org/XML/1998/namespace':
              continue
            else:
              prefs.append(key)
          if not scpFound:
            sys.stderr.write("document element of %s is not in namespace %s\n"%(fullFile,XMLSchemaNS))
            raise PyLTXML.error
          if scp!='xs':
            if scp:
              pref="%s:"%scp
              pds="\n<!ENTITY %"+" s ':%s'>\n<!ENTITY %% p '%s:'>"%(scp,scp)
            else:
              pref=""
              pds="\n<!ENTITY % s ''>\n<!ENTITY % p ''>"
          else:
            pds=""
            pref="xs:"
          ns=""
          for p in prefs:
            if p:
              ns=ns+"\n<!ATTLIST %sschema xmlns:%s CDATA #IMPLIED>"%(pref,p)
          home='.'
          for p in sys.path:
            if os.path.isfile("%s/%s"%(p or '.','XMLSchema.dtd')):
              home=p or '.'
              break
          home = os.path.abspath(home)
          home=string.replace(home,"\\","/")
          if home[1]==':':
            home='file:///'+home
          if pds or ns or intDecls:
            fake1="<!DOCTYPE %sschema SYSTEM '%s/XMLSchema.dtd'"%(pref, home)
            fake2=" [%s"%pds;
            fake3="%s\n%s]>\n"%(ns, intDecls)
            fake=fake1+fake2+fake3
          else:
            fake="<!DOCTYPE %sschema SYSTEM '%s/XMLSchema.dtd'>\n"%(pref,home)
          fake=fake+"<%sschema/>"%pref
          ff=PyLTXML.OpenString(fake,
                                PyLTXML.NSL_read|PyLTXML.NSL_read_namespaces)
        if ff or not fake:
          try:
            res=factory.fromFile(schemaEltDispatch,
                                 {"finalDefault":"ignore",
                                  "blockDefault":"ignore",
                                  "elementFormDefault":"ignore",
                                  "attributeFormDefault":"ignore",
                                  "nillable":"nullable"},
                                 lookup,"instance","variable",
                                 XMLSchemaNS,
                                 fullFile,
                                 (XMLSchemaNS,"schema"),ff)
          except layer.LayerError,layErr:
            res=None
        else:
          res=None
      else:
        res=None
    except PyLTXML.error:
      res=None
  if not isinstance(res,schemaElt):
    ne=XML.Element("notASchema")
    ne.addAttr('filename',fullFile)
    if layErr:
      ne.addAttr('lowLevelErrorMsg',layErr)
    factory.resElt.children.append(ne)
    schema=None
  else:
    schema=res.component
    schema.locations.append(fullFile)
  # schema is a an instance of schema, if present
  factory.fileNames=factory.fileNames[1:]
  if factory.schemaStack:
    factory.schema=factory.schemaStack[0][0]
    (factory.schema,factory.processingInclude,
     factory.schema.elementFormDefault,
     factory.schema.attributeFormDefault,
     factory.schema.blockDefault,
     factory.schema.finalDefault) = factory.schemaStack[0]
    factory.targetNS=factory.schema.targetNS
    factory.schemaStack=factory.schemaStack[1:]
  else:
    factory.schema=factory.targetNS=None
  return schema

def lookup(eltName):
  if eltClasses.has_key(eltName):
    return eltClasses[eltName]
  else:
    return userClass

class userClass:
  def __init__(self,factory,elt):
    self.elt=elt
    self.factory=factory

def prepare(factory):
  # before we do anything serious, check if we need to
  # bootstrap the schema for schema
  if factory.schemas.has_key(XMLSchemaNS):
    # we're validating a schema with a (purported) schema for schemas
    sfors=factory.schemas[XMLSchemaNS]
    factory.sfors=sfors
    if ((not sfors.typeTable.has_key('string')) or
        sfors.typeTable['string'].basetypeName.local=='anySimpleType'):
      # need the ab-initio types
      sfors.targetNS=XMLSchemaNS
      factory.schema=sfors
      factory.targetNS=XMLSchemaNS
      sfors.doBuiltIns(factory)
  else:
    sfors=Schema(factory,None)
    factory.schemas[XMLSchemaNS]=sfors
    factory.sfors=sfors
    sfors.targetNS=XMLSchemaNS
    factory.schema=sfors
    factory.targetNS=XMLSchemaNS
    sfors.doBuiltIns(factory)
  sforsi=Schema(factory,None)
  factory.schemas[XMLSchemaInstanceNS]=sforsi
  factory.sforsi=sforsi
  sforsi.targetNS=XMLSchemaInstanceNS
  factory.schema=sforsi
  factory.targetNS=XMLSchemaInstanceNS
  sforsi.installInstanceAttrs(factory)
  factory.schema=sfors
  factory.targetNS=XMLSchemaNS
  ec=0
  for sch in factory.schemas.values():
    ec=ec+sch.errors
  factory.prepared=1
  return ec

class Schema:
  annotations=[]
  def __init__(self,factory,xrpr):
    # note that unlike other components, this one is built _before_ processing
    # the children of the elt it corresponds to
    self.xrpr=xrpr
    self.factory=factory
    self.errors=0
    self.locations=[]
    if xrpr:
      # these are needed during schema accumulation
      self.maybeSetVar('targetNS','targetNamespace',None)
      self.maybeSetVar('elementFormDefault','elementFormDefault','unqualified')
      self.maybeSetVar('attributeFormDefault','attributeFormDefault',
                       'unqualified')
      self.maybeSetVar('finalDefault','finalDefault','')
      self.maybeSetVar('blockDefault','blockDefault','')
      if self.targetNS=="":
        self.error("Empty string is not allowed as value of targetNamespace",
                   xrpr.elt)
        self.targetNS=None
      if self.factory.targetNS:
        if self.targetNS!=self.factory.targetNS:
          if ((not self.targetNS) and factory.processingInclude):
            # chameleon include, OK
            self.targetNS=self.factory.targetNS
            factory.processingInclude=2
          else:
            self.error("targetNamespace mismatch: %s expected, %s found" % (self.factory.targetNS, self.targetNS),xrpr.elt)
      else:
        self.factory.targetNS=self.targetNS
      if factory.schemas.has_key(self.targetNS):
        oldSchema=factory.schemas[self.targetNS]
        # use real tables, we're ephemeral
        factory.schema=oldSchema
        self.typeTable=oldSchema.typeTable
        self.elementTable=oldSchema.elementTable
        self.attributeTable=oldSchema.attributeTable
        self.groupTable=oldSchema.groupTable
        self.attributeGroupTable=oldSchema.attributeGroupTable
        self.vTypeTable=oldSchema.vTypeTable
        self.vElementTable=oldSchema.vElementTable
        self.vAttributeTable=oldSchema.vAttributeTable
        self.vGroupTable=oldSchema.vGroupTable
        self.vAttributeGroupTable=oldSchema.vAttributeGroupTable
        # copy defaults (they've been saved, will be restored)
        oldSchema.elementFormDefault=self.elementFormDefault
        oldSchema.attributeFormDefault=self.attributeFormDefault
        oldSchema.blockDefault=self.blockDefault
        oldSchema.finalDefault=self.finalDefault
        return
      else:
        factory.schemas[self.targetNS]=self      
        factory.schema=self
    # either we're the first for this NS, or we're the FIRST
    self.typeTable={}
    self.elementTable={}
    self.attributeTable={}
    self.groupTable={}
    self.attributeGroupTable={}
    self.vTypeTable=VMapping(self, "typeTable")
    self.vElementTable=VMapping(self, "elementTable")
    self.vAttributeTable=VMapping(self, "attributeTable")
    self.vGroupTable=VMapping(self, "groupTable")
    self.vAttributeGroupTable=VMapping(self, "attributeGroupTable")
  
  def __str__(self):
    types=map(str,self.typeTable.values())
    groups=map(str, self.groupTable.values())
    attributeGroups=map(str, self.attributeGroupTable.values())
    elts=map(str,self.elementTable.values())
    attrs=map(str,self.attributeTable.values())
    return "{Target:%s}{Types:%s}{Groups:%s}{AttrGroups:%s}{Elements:%s}{Attributes:%s}"%(self.targetNS,string.join(types,''),string.join(groups,''),string.join(attributeGroups,''),string.join(elts,''),string.join(attrs,''))

  def maybeSetVar(self,varName,attrName,default):
    if self.xrpr.elt.hasAttrVal(attrName):
      setattr(self,varName,self.xrpr.elt.attrVal(attrName))
    else:
      setattr(self,varName,default)

  def doBuiltIns(self,factory):
    self.doAbInitios(factory)
    # TODO: implement fixed facets
    for (bitn,basen,facets) in builtinTypeNames:
      fake=simpleTypeElt(factory,None)
      fake.name=bitn
      fake.restriction=restrictionElt(factory,None)
      fake.restriction.init(None)
      fake.init(None)
      fake.component.basetypeName=QName(None,basen,XMLSchemaNS)
      fake.component.variety='atomic'
      bit=fake.restriction.component
      bit.rootName=fake.component.basetype.rootName
      for (fc,fv) in facets:
        nf=fc(factory,None)
        bit.primitiveType.facets[fc.name]=nf
        nf.value=fv
      self.typeTable[bitn]=fake.component
    for (bitn,basen) in builtinLists:
      fake=simpleTypeElt(factory,None)
      fake.name=bitn
      fake.list=listElt(factory,None)
      fake.list.init(None)
      fake.init(None)
      fake.component.basetype=urType
      fake.component.variety='list'
      fake.list.component.itemtypeName=QName(None,basen,XMLSchemaNS)
      self.typeTable[bitn]=fake.component
    ap=Particle(factory,None,AnyAny(factory,None),None)
    ap.term.processContents='lax'
    ap.occurs=(0,None)
    urType.model.term.particles.append(ap)
    self.typeTable['anyType']=urType
    urSimpleType.factory=urType.factory=factory
    self.typeTable['anySimpleType']=urSimpleType

  def doAbInitios(self,factory):
    wsf1=Whitespace(factory,None)
    wsf1.value="collapse"
    wsf1.fixed="true"
    wsf2=Whitespace(factory,None)
    wsf2.value="preserve"
    for (ain,ait) in abInitioTypes:
      aiti=ait(factory,None)
      aiti.rootName=ain
      aiti.basetype=ait		# for use in creating effective types
      aiti.effectiveType=aiti
      if ain=='string':
        aiti.facets['whiteSpace']=wsf2
      else:
        aiti.facets['whiteSpace']=wsf1
      self.typeTable[ain]=aiti

  def installInstanceAttrs(self,factory):
    factory.eltStack=['a']              # hack
    for (attrn,basen) in instanceAttrs:
      fake=attributeElt(factory,None)
      fake.name=attrn
      fake.type=basen
      fake.init(None)
      fake.component.typeDefinitionName=QName(None,basen,XMLSchemaNS)
      self.attributeTable[attrn]=fake.component
    for (attrn,itemn) in instanceLists:
      fake=attributeElt(factory,None)
      fake.name=attrn
      fake.simpleType=simpleTypeElt(factory,None)
      fake.simpleType.list=listElt(factory,None)
      fake.simpleType.list.init(None)
      fake.simpleType.list.component.itemtypeName=QName(None,itemn,XMLSchemaNS)
      fake.simpleType.init(None)
      fake.simpleType.component.variety='list'
      fake.simpleType.component.basetype=urType
      fake.init(None)
      self.attributeTable[attrn]=fake.component
    factory.eltStack=[]              # hack
    ap=Particle(factory,None,AnyAny(factory,None),None)
    ap.term.processContents='lax'
    ap.occurs=(0,None)

  def prepare(self):
    # try to touch everything that might cause errors
    cool=1
    for i in self.typeTable.values():
      if isinstance(i,Type):
        cool=i.prepare() and cool
    for tab in (self.elementTable, self.attributeTable,
                self.groupTable, self.attributeGroupTable):
      for i in tab.values():
        cool=i.prepare() and cool
    return cool

  def error(self,message,elt=None,warning=0):
    # should have code argument to identify SRC/COS
    if warning:
      ee=XML.Element("schemaWarning")
    else:
      ee=XML.Element("schemaError")
    if self.factory.prepared:
      ee.addAttr("phase","instance")
    else:
      ee.addAttr("phase","schema")
    if not warning:
      self.errors=self.errors+1
    if elt:
      where(ee,elt.where)
    ee.children=[XML.Pcdata(message)]
    self.factory.resElt.children.append(ee)

  def newComponent(self,table,kind,comp):
    if not comp:
      return
    if comp.name:
      if table.has_key(comp.name):
        comp.schema.error("attempt to overwrite %s {%s}%s, ignored"%(kind,
                                                                 self.targetNS,
                                                                 comp.name),
                          comp.xrpr.elt,
                          1)
      else:
        table[comp.name]=comp
        comp.qname=QName(None,comp.name,self.targetNS)
    else:
      shouldnt('nc: %s'%comp)

instanceAttrs=[('nil','boolean'),('type','QName'),
               ('noNamespaceSchemaLocation','anyURI')]

instanceLists=[('schemaLocation','anyURI')]

class schemaElt:
  version=None
  id=None
  def __init__(self,factory,elt):
    self.elt=elt
    factory.eltStack[0:0]=[self]
    self.component=Schema(factory,self)
    self.dds=[]

  def init(self,elt):
    sch=self.component
    # todo:oldSchema.annotations=oldSchema.annotations.append(self.annotations)
    if hasattr(self,'annot') and self.annot:
      sch.annotations=map(lambda a:a.component,self.annot)
    if elt:
      ooba=filter(lambda a:a.nsuri,
                  elt.actualAttrs)
      if ooba:
        ooba=map(lambda a:a.elt,ooba)
        if not sch.annotations:
          sch.annotations=[Annotation(sch.factory,None)]
        if sch.annotations[0].attrs:
          self.sch[0].attrs=self.sch[0].attrs.append(ooba)
        else:
          sch.annotations[0].attrs=ooba
    sch.factory.eltStack=sch.factory.eltStack[1:]
    for dd in self.dds:
      if not dd.name:
        continue
      if dd.__class__==complexTypeElt:
        sch.newComponent(sch.typeTable,'type',dd.component)
      elif dd.__class__==elementElt:
        sch.newComponent(sch.elementTable,'element',dd.component)
      elif dd.__class__==attributeElt:
	sch.newComponent(sch.attributeTable,'attribute',dd.component)
      elif isinstance(dd,groupElt):
	sch.newComponent(sch.groupTable,'group',dd.component)
      elif dd.__class__==simpleTypeElt:
	sch.newComponent(sch.typeTable,'type',dd.component)
      elif dd.__class__==attributeGroupElt:
	sch.newComponent(sch.attributeGroupTable,'attributeGroup',dd.component)
      else:
        shouldnt('dd')

class commonElt:
  def __init__(self,factory,elt):
    self.schema=factory.schema
    self.elt=elt

  def error(self,msg,warn=0):
    self.schema.error(msg,self.elt,warn)

fixNSN1=re.compile("^[^a-zA-Z_]")
fixNSN=re.compile("[^a-zA-Z0-9._]")

class Component:
  prepared=0
  name=None
  targetNamespace=None
  idCounter=1
  annotation=None                       # TODO: implement
  def __init__(self,factory,xrpr,ns='ns'):
    self.factory=factory
    if factory:
      self.schema=factory.schema
      if ns=='ns':
        self.targetNamespace=self.schema.targetNS
    self.xrpr=xrpr
    if xrpr:
      if hasattr(xrpr,'name'):
        self.name=xrpr.name
      if hasattr(xrpr,'annot') and xrpr.annot:
        if len(xrpr.annot)!=1:
          error('shouldnt '+str(len(xrpr.annot)))
        self.annotation=xrpr.annot[0].component
      if xrpr.elt:
        ooba=filter(lambda a:a.nsuri,
                    xrpr.elt.actualAttrs)
        if ooba:
          ooba=map(lambda a:a.elt,ooba)
          if not self.annotation:
            self.annotation=Annotation(self.factory,None)
          if self.annotation.attrs:
            self.annotation.attrs=self.annotation.attrs.append(ooba)
          else:
            self.annotation.attrs=ooba
    self.id=self.idCounter
    Component.idCounter=self.id+1

  def error(self,message,warning=0):
    self.schema.error(message,self.xrpr.elt,warning)

class Type(Component):
  annotations=[]                        # TODO: implement this
  def __init__(self,factory,xrpr):
    Component.__init__(self,factory,xrpr,'ns')
    # todo: assemble annotation from ([sc]Content),restriction/(extension|list/union)
    if self.annotation:
      self.annotations=[self.annotation]

  def __getattr__(self,name):
    if name=='basetype':
      st=None
      if self.basetypeName:
        if self.schema.vTypeTable.has_key(self.basetypeName):
          st=self.schema.vTypeTable[self.basetypeName]
          if isinstance(st,ComplexType):
            if isinstance(self,ComplexType):
              if (self.xrpr.content=="textOnly" and not
                  (st.contentType=="textOnly" or
                   (st.contentType=="mixed" and st.emptiable()))):
                self.error("textOnly type %s must have textOnly or emptiable mixed basetype %s:%s"%(self.name,st.name,st.contentType))
                self.basetype=None
                return
              if self.derivationMethod in st.final:
                self.error("Error, %s declares %s as base, which is final"%(self.name,
                                                                       st.name))
                self.basetype=None
                return
            else:
              # we're a SimpleType, this only happens if we're the
              # {content type} of a text-only ComplexType, so need to go inside
              # its basetype
              if st.contentType!="textOnly":
                self.error("textOnly type %s may not have non-textOnly basetype %s, content type %s"%(self.name,st.name,st.contentType))
                self.basetype=None
                return
              st=st.model
          else:
            if (isinstance(self,ComplexType) and
                self.xrpr.content and self.xrpr.content!="textOnly"):
              self.error("type %s with simple basetype %s may not have %s content"%(self.name or '[anonymous]',st.name,self.xrpr.content))
              self.basetype=None
              return
        else:
          self.error("Undefined type %s referenced as basetype of %s"%(self.basetypeName, self.name or '[anonymous]'))
          self.basetype=None
          return
        # removed thorough check for circularity: was stale,
        # not actually forbidden in the spec.????
        if st==self:
          self.error("Basing a type on itself is forbidden")
          self.basetype=None
          return
      else:
        # note the ab-initio types are initialised with themselves
        # as their own effective type and basetype, so we don't come here
        # for them
        if self.xrpr.content=='textOnly':
          # not quite right . . . using self.contentType
          # produces infinite loop
          st=self.factory.sfors.typeTable['string']
        else:
          st=urType
      if st:
        self.basetype=st
      return self.basetype
    else:
      raise AttributeError,name

  def isSubtype(self,other):
    if self==other:
      return 1
    if self.basetype==self or not self.basetype:
      return 0
    if (isinstance(self,SimpleType) and
        self.basetype.variety=='union' and
        self in self.basetype.memberTypes):
      return 1
    return self.basetype.isSubtype(other)

  def redefine(self):
    # we have a component which should be based on itself
    # note this forces some reference resolution normally left until later
    # TODO: check complex vs. complex, simple vs. simple!
    base=self.basetype
    if (not base):
      self.error("attempt to redefine in terms of non-existent type: %s"%self.name)
      return
    if base.name!=self.name:
      # note namespace identity already enforced by including
      self.error("attempt to redefine in terms of type other than self: %s vs. %s"%
                 (self.name,base.name))
    else:
      base.name="original "+base.name
      self.schema.typeTable[self.name]=self
      self.qname=QName(None,self.name,self.schema.targetNS)

class typeElt(commonElt):
  name=None
  basetype=None
  derivedBy=None
  id=None
  annot=None
  def __init__(self,factory,elt):
    commonElt.__init__(self,factory,elt)

## Note that primitive builtins are _not_ SimpleTypes, see AbInitio.
## SimpleType itself is largely a placeholder:  it has a targetNamespace and
##  may have a name.  In principle it always has a basetype, but in practice
##  may only have a basetypeName, and basetype is filled in lazily.

## It also should have a variety, but this is _also_ lazy, as it may depend
##  on the basetype.

## The real action is in the subComp, which should be an instance of Atomic,
##   List or Union, but may be a Restriction which contains one of these as
##   its actual.  Opportunities for improved efficiency obviously exist, by
##   eliminating one or both indirections once the truth is known.

class SimpleType(Type):
  basetypeName=None
  attributeDeclarations={}              # for use when this is a ct's basetype
  contentType='textOnly'                # ditto
  elementTable={}                       # ditto
  reflectedName='simpleTypeDefinition'
  reflectionMap=(('name','string',1,'name'),
                 ('targetNamespace','string',1,'targetNamespace'),
                 ('baseTypeDefinition','component',1,'basetype'),
                 ('facets','special',0,'facetsReflect'),
                 # XXX
                 ('fundamentalFacets','special',0, 'fundamentalFacetsReflect'),
                 # XXX
                 ('final','special',0,'finalReflect'),
                 ('variety','string',0,'variety'),
                 ('primitiveTypeDefinition','special',
                  1,'primitiveTypeReflect'),
                 ('itemTypeDefinition','component',1,'itemType'),
                 ('memberTypeDefinitions','components',1,'memberTypes'),
                 ('annotations','components',0,'annotations') # not per REC,
                                                              # but correct
                 )

  def __init__(self,factory,xrpr,derivedBy,basetypeName,subComponent):
    Type.__init__(self,factory,xrpr)
    self.basetypeName=basetypeName
    self.subComp=subComponent
    if subComponent:
      self.subComp.super=self
    self.derivedBy=derivedBy

  def __str__(self):
    if self.basetype and self.basetype!=self:
      if isinstance(self.basetype,QName):
	bt=" based on %s"%self.basetype
      else:
	bt=" based on %s"%self.basetype.name
    else:
      bt=""
    v='???'
    return str("{Simple %s type {%s}%s%s}"%(v,self.targetNamespace,
                                            self.name,bt))

  def __getattr__(self,name):
    if name not in ('basetype','variety','rootName','primitiveType',
                    'memberTypes','itemType','restrict'):
      raise AttributeError,name
    elif name=='basetype':
      if Type.__getattr__(self,name)==urType and self.variety=='atomic':
        # not allowed for simple types!
        self.error("Must have basetype for atomic simpleType %s"%(self.name or '[anonymous]'))
        self.basetype=None
      return self.basetype
    elif name=='variety':
      # lazy because it involves the real basetype
      if self.derivedBy=='restriction':
        self.variety=self.subComp.variety or 'atomic' # in case of error
      else:
        self.variety=self.derivedBy or 'atomic' # in case of error
      return self.variety
    else:
      return getattr(self.subComp,name)

  def prepare(self):
    if self.prepared:
      return 1
    self.prepared=1
    p1=self.basetype and self.basetype.prepare()
    return p1

  def emptiable(self):
    # should do some real work . . .
    return self==urType

class simpleTypeElt(typeElt):
  content='textOnly'
  restriction=None
  list=None
  union=None
  def init(self,elt):
    basetypeName=None
    if self.restriction:
      derivedBy='restriction'
      if hasattr(self.restriction,'base'):
        basetypeName=QName(self.restriction.base,elt,self.schema.factory)
        if (self.restriction.facets and
            basetypeName.local=='anySimpleType' and
            basetypeName.uri==XMLSchemaNS and
            len(self.restriction.facets)>1 and
            not(isinstance(self.restriction.facets[0],Whitespace))):
          self.error("anySimpleType may not be directly restricted with facets")
          basetypeName=QName('string',elt,self.schema.factory) # hack to keep going
    elif self.list:
      derivedBy='list'
    elif self.union:
      derivedBy='union'
    else:
      # no elt for fakes for builtins
      if elt:
        self.error("simpleType must have one of restriction, list or union as a child")
        self.component=SimpleType(self.schema.factory,self,'unknown',
                                  None,None)
        return
    self.component=SimpleType(self.schema.factory,self,derivedBy,basetypeName,
                              (self.restriction or self.list or
                               self.union).component)

class rulElt(commonElt):
  facets=[]
  def init(self,elt):
    self.component=self.comp(self.schema.factory,self)

class Restriction(Component):
  def __getattr__(self,name):
    if name not in ('variety','rootName','primitiveType','memberTypes',
                    'itemType','actual','validateText'):
      raise AttributeError,name
    if not self.__dict__.has_key('actual'):
      if self.super.basetype:
        self.actual=self.super.basetype.restrict(self)
      else:
        # error already signalled . . .
        self.actual=None
        return None
    if name=='actual':
      return self.actual
    elif self.actual:
      return getattr(self.actual,name)
    else:
      return None

  def restrict(self,restr):
    return self.actual.restrict(restr)
    
class Atomic(Component):
  variety='atomic'
  def __init__(self,factory,restr):
    Component.__init__(self,factory,restr.xrpr)
    self.super=restr.super
    
  def __getattr__(self,name):
    if name=='rootName':
      if self.super.basetype:
        if self.super.basetype!=urSimpleType:
          self.rootName=self.super.basetype.rootName
        else:
          # I must be a root
          self.rootName=self.name or 'anySimpleType'
      else:
        # missing basetype, cheat to forestall worse errors
        self.rootName='string'
      return self.rootName
    elif name=='primitiveType':
      # the basetype of ab initio types is a class, not an instance
      # the only allowed form of type derivation is restriction
      self.primitiveType=None
      # TODO: should we handle 'list' here?
      if self.super.basetype:
        rn=self.rootName
      else:
        rn='string'                     # save catastrophe
      ptd=self.factory.sfors.typeTable[rn]
      if ptd==urSimpleType:
        self.primitiveType=urSimpleType
      else:
        self.primitiveType=ptd.basetype(self.factory,self.super.basetype)
        if self.super.basetype:
          # no basetype means an earlier error
          self.primitiveType.mergeFacets(self.super,self.xrpr.facets)
          # the above is slightly different to the REC:  we merge the facets in
      return self.primitiveType
    else:
      raise AttributeError,name

  def restrict(self,restr):
    return Atomic(self.factory,restr)

class List(Component):
  variety='list'
  itemtypeName=None
  def __init__(self,factory,xrpr):
    Component.__init__(self,factory,xrpr)
    if xrpr.itemType:
      self.itemtypeName=QName(xrpr.itemType,xrpr.elt,factory)
      if xrpr.simpleType:
        self.error("list with 'type' attribute must not have nested type declaration")
    elif xrpr.simpleType:
      self.itemType=xrpr.simpleType.component
    else:
      # no elt means builtin
      if xrpr.elt:
        self.error("list must have 'type' attribute or SimpleType child")
  def __getattr__(self,name):
    if name=='itemType':
      self.itemType=None
      if self.itemtypeName:
        if self.schema.vTypeTable.has_key(self.itemtypeName):
          self.itemType=self.schema.vTypeTable[self.itemtypeName]
        else:
          self.error("Undefined type %s referenced as type definition of %s"%(self.itemtypeName, self.super.name))
      else:
        shouldnt('nlt')
      return self.itemType
    else:
      raise AttributeError,name

  def restrict(self,restr):
    if hasattr(restr.xrpr,'list'):
      self.error("restriction of a list with a list not checked yet",1)
      return restr.xrpr.list.component
    else:
      self.error("restricting a list with facets not implemented yet",1)
      return self

class Union(Component):
  variety='union'
  membertypeNames=[]
  someMembers=None
  def __init__(self,factory,xrpr):
    Component.__init__(self,factory,xrpr)
    if xrpr.memberTypes:
      self.membertypeNames=map(lambda n,e=xrpr.elt,f=factory:QName(n,e,f),
                               string.split(xrpr.memberTypes))
    if xrpr.subTypes:
      self.someMembers=map(lambda sub:sub.component,
                           xrpr.subTypes)
    elif not xrpr.memberTypes:
      # no elt means builtin
      if xrpr.elt:
        self.error("union must have 'memberTypes' attribute or some SimpleType children")
  def __getattr__(self,name):
    if name=='memberTypes':
      self.memberTypes=self.someMembers or []
      for mtn in self.membertypeNames:
        if self.schema.vTypeTable.has_key(mtn):
          self.memberTypes.append(self.schema.vTypeTable[mtn])
        else:
          self.error("Undefined type %s referenced as type definition of %s"%(mtn, self.super.name))
      return self.memberTypes
    else:
      raise AttributeError,name

  def restrict(self,restr):
    if hasattr(restr.xrpr,'union'):
      self.error("restriction of a union with a union not checked yet",1)
      return restr.xrpr.union.component
    else:
      self.error("restricting a union with facets not implemented yet",1)
      return self

class restrictionElt(rulElt):
  # TODO: check base vs content???
  group=None
  all=None
  choice=None
  sequence=None
  attrs=[]
  comp=Restriction
  def init(self,elt):
    if elt and 'complexContent'==(elt.parent.llabel or elt.parent.label):
      # don't init yet, complexContent itself will handle this
      pass
    else:
      tab={}
      if self.facets:
        for facet in self.facets:
          facet.register(tab)
      self.facets=tab
      rulElt.init(self,elt)

class listElt(rulElt):
  # TODO: check base vs content
  comp=List
  simpleType=None
  itemType=None

class unionElt(rulElt):
  # TODO: check base vs content
  comp=Union
  subTypes=[]
  memberTypes=None

class ComplexType(Type):
  reflectedName='complexTypeDefinition'
  reflectionMap=(('name','string',1,'name'),
                 ('targetNamespace','string',1,'targetNamespace'),
                 ('baseTypeDefinition','component',1,'basetype'),
                 ('derivationMethod','string',1,'derivationMethod'),
                 ('final','list',0,'final'),
                 ('abstract','boolean',0,'abstract'),
                 ('attributeUse','special',0,'attributesReflect'),
                 ('attributeWildcard','special',1,'attributeWildcardReflect'),
                 ('contentType','special',0,'contentTypeReflect'),
                 ('prohibitedSubstitutions','list',
                  0,'prohibitedSubstitutions'),
                 ('annotations','components',0,'annotations'))
  def __init__(self,factory,xrpr):
    Type.__init__(self,factory,xrpr)
    self.basetypeName=xrpr.basetype
    self.facets=xrpr.facets
    self.abstract=xrpr.abstract
    self.final=string.split(xrpr.final)
    self.prohibitedSubstitutions=string.split(xrpr.block)

  def __str__(self):
    if self.basetype:
      if isinstance(self.basetype,QName):
	bt=" based on %s"%self.basetype
      else:
	bt=" based on {%s}%s"%(self.basetype.targetNamespace,
                               self.basetype.name)
    else:
      bt=""
    c = " contentType "+self.contentType;
    if (self.contentType in ('elementOnly','mixed')) and self.model:
      model="%s: %s"%(self.model.term.compositor,string.join(map(str,self.model.term.particles),''))
    elif self.contentType=='textOnly' and self.basetype:
      model="{%s}%s"%(self.basetype.targetNamespace,
                      self.basetype.name)
    else:
      model=""
    if self.attributeDeclarations:
      attrs=string.join(map(str,self.attributeDeclarations.values()),'')
    else:
      attrs=""
    return str("{Complex type %s%s%s:%s%s}"%(self.name,bt,c,model,attrs))

  def __getattr__(self,name):
    if name=='basetype':
      return Type.__getattr__(self,name)
    # the next _two_ properties taken together make the REC's {content type}
    elif name=='derivationMethod':
      self.derivationMethod=self.xrpr.derivedBy
      return self.derivationMethod
    elif name=='contentType':
      if self.xrpr.content=='elementOnly' and not self.model:
        self.contentType='empty'
      else:
        self.contentType=self.xrpr.content
      return self.contentType
    elif name=='model':
      if self.xrpr.content=='textOnly':
        if self.xrpr.simpleContent.restriction:
          if hasattr(self.xrpr.simpleContent.restriction,'simpleType'):
            # nested simpleType, use it as base
            # TODO: will ignore higher-level facets???
            self.model=self.xrpr.simpleContent.restriction.simpleType.component
          else:
            # no nested simpleType, shared basetypeName better do it
            fake=simpleTypeElt(self.factory,self.xrpr.elt)
            fake.name=None
            fake.restriction=self.xrpr.simpleContent.restriction
            fake.init(self.xrpr.elt)
            fake.component.basetypeName=self.basetypeName # for chameleons
            self.model=fake.component
        else:
          # todo: why does the extension case need a fake, it appears to be
          # in violation of the REC
          fake=simpleTypeElt(self.factory,self.xrpr.elt)
          fake.name=None
          fake.restriction=restrictionElt(self.factory,self.xrpr.elt)
          fake.restriction.init(None)
          fake.restriction.facets={}
          fake.restriction.base=self.xrpr.simpleContent.extension.base
          fake.restriction.attrs=self.xrpr.simpleContent.extension.attrs
          fake.init(self.xrpr.elt)
          fake.component.basetypeName=self.basetypeName # for chameleons
          self.model=fake.component
      else:
        self.model=self.realModel(self.xrpr.model)
      return self.model
    elif name=='attributeDeclarations':
      # a dictionary of attributeUse instances keyed by qname
      self.attributeDeclarations=self.mergeAttrs(self.basetype,self.derivationMethod)
      return self.attributeDeclarations
    elif name=='elementTable':
      self.elementTable={}
      if self.contentType in ('elementOnly','mixed') and self.model:
        self.model.note(self.elementTable)
      return self.elementTable
    elif name=='fsm':
      if self.contentType not in ("elementOnly","mixed"):
	self.fsm = None
	return self.fsm
      ndfsm = FSM()
      end = FSMNode(ndfsm)
      end.isEndNode = 1
      start = self.model.translate(end, ())
      ndfsm.startNode = start
      self.ndfsm = ndfsm
      self.fsm = ndfsm.determinise()
      relabelFSM(self.fsm)
      nd = checkFSM(self.fsm)
      if nd:
        self.error("non-deterministic content model for type %s: %s" %
                   (self.name, nd))
        self.fsm = self.fsm.determinise()
      return self.fsm
    else:
      raise AttributeError,name

  def prepare(self):
    if self.prepared:
      return 1
    self.prepared=1
    p1=self.basetype and self.basetype.prepare()
    p2=self.model
    p3=self.attributeDeclarations
    if p3:
      for au in p3.values():
        ad=au.attributeDeclaration
        if isinstance(ad,Attribute):
          p3=ad.prepare() and p3
    p4=self.elementTable
    if p4:
      for ed in p4.values():
        p4=ed.prepare() and p4
    p5=self.fsm
    return (p1 and p2 and p3 and p4 and p5)

  def mergeContent(self,basetype,derivedBy):
    if self.xrpr.content and basetype:
      own=self.xrpr.content
      other=basetype.contentType
      if own==other:
        return own
      if own=="empty":
	# sigh, we need to compute emptiable . . .
	if (derivedBy=='extension' or not basetype.emptiable()):
	  self.error("attempt to extend to empty or restrict non-emptiable model of {%s}%s to empty for %s"%(basetype.targetNamespace,basetype.name,self.name))
	  return other # bogus, but how else to keep going?
	else:
	  return own
      elif ((derivedBy=='restriction' and (other in ({"textOnly":("mixed",),
                                                     "elementOnly":("mixed",),
                                                  "mixed":()}[own])))
            or
            (other in ({"elementOnly":("empty",),
                        "mixed":(),
                        "textOnly":()}[own]))):
        return own
      else:
	self.error("incompatible content for %s on type %s (%s) and source %s (%s)"%(derivedBy, self.name, own, basetype.name, other))
	return other # bogus, but how else to keep going?
    else:
      if basetype and isinstance(basetype,ComplexType):
        return basetype.contentType
      else:
        return 'textOnly'
    
  def realModel(self,raw):
    # XXX doesn't deal with all groups yet
    # deals with derivation simple cases only
    if self.basetype:
      other=self.basetype.model
    else:
      # error already
      other=None
    if raw:
      mine=topGroup(self.factory,raw)
    elif self.derivationMethod=='restriction':
      mine=Particle(self.factory,None,
                    Sequence(self.factory,None,self.xrpr))
      mine.occurs=(1,1)
      mine.particles=[]
    else:
      return other
    if self.derivationMethod=='restriction':
      if (not other) or self.basetype==urType or other==urType.model:
        return mine
      else:
        res=mine.merge(other)
        if res:
          return res
        else:
          if self.contentType=='elementOnly':
            self.contentType='empty'
          return None
    elif ((not self.basetype) or self.basetype.contentType=='empty'):
      return mine
    else:
      if (self.basetype.contentType=='textOnly' and
          (self.xrpr.content!='textOnly' or raw!=other)):
        self.error("extension of simple content %s may not have content model"%other)
        return mine

      # needs more checks for allowed extension. . .
      if other and isinstance(other,Particle) and other.term and other.term.compositor=='sequence' and other.occurs==(1,1):
        newp=copy.copy(other)
        newp.term=copy.copy(other.term)
        newp.term.particles=other.term.particles+[mine]
        return newp
      else:
        np=Particle(self.factory,None,
                    Sequence(self.factory,None,mine.xrpr),mine.xrpr)
        np.occurs=(1,1)
        np.term.particles=[other,mine]
        return np

  def mergeAttrs(self,basetype,derivedBy):
    mine=self.expandAttrGroups()
    if basetype:
      others=basetype.attributeDeclarations
    else:
      others={}
    for (adn,ad) in others.items():
      if mine.has_key(adn):
        if derivedBy=='extension':
          self.error("attempt to extend with an attribute already declared {%s}"%adn)
        else:
          # restriction
          me=mine[adn]
          if me.maxOccurs==0:
            if ad.minOccurs==1:
              self.error("attempt to eliminate required attribute %s"%me.qname)
            else:
              del mine[adn]
          else:
            if ad.minOccurs==1:
              if me.minOccurs==0:
                self.error("attempt to make required attribute %s optional"%me.qname)
                me.minOccurs=1
            if ad.valueConstraint:
              if (ad.valueConstraint[0]=='fixed' and
                  ((not me.valueConstraint) or
                   me.valueConstraint[0]!='fixed' or
                   me.valueConstraint[1]!=ad.valueConstraint[1])):
                self.error("attempt to change or abandon fixed value for attribute %s"%me.qname)
            me.attributeDeclaration.checkSubtype(ad.attributeDeclaration)
      else:
        mine[adn]=ad
    return mine

  def expandAttrGroups(self):
    tab={}
    for ad in self.xrpr.attrs:
      ad.component.expand(tab)
    return tab

  def note(self,table):
    self.term.note(table)

  def emptiable(self):
    # TODO: should do some real work . . .
    return (self==urType or
            isinstance(self.model,SimpleType) or      # !!might be wrong!!
            self.model.occurs[0]==0 or
            (len(filter(lambda p:p.occurs[0]==0,self.model.term.particles))==
             len(self.model.term.particles)))

class complexTypeElt(typeElt):
  sub=None
  derivedBy=None
  final=""
  block=""
  abstract="false"
  complexContent=None
  simpleContent=None
  mixed=None
  def __init__(self,factory,elt):
    typeElt.__init__(self,factory,elt)
    factory.eltStack[0:0]=[self]

  def __getattr__(self,name):
    if self.sub:
      if name in ('facets','sequence','choice','all','group','attrs','model'):
        return getattr(self.sub,name)
    else:
      if name in ('facets','attrs','model'):
        return []
    raise AttributeError,name

  def init(self,elt):
    basetypeName=None
    if self.complexContent:
      self.sub=self.complexContent
      if self.complexContent.mixed=='true':
        self.content='mixed'
      elif self.complexContent.mixed=='unspecified' and self.mixed=='true':
        self.content='mixed'
      else:
        self.content='elementOnly'
      if self.complexContent.restriction:
        if self.content=='elementOnly' and not self.complexContent.model:
          self.content='empty'
        self.derivedBy='restriction'
        if hasattr(self.complexContent.restriction,'base'):
          # must be a complex type
          basetypeName=QName(self.complexContent.restriction.base,elt,
                             self.schema.factory)
      elif self.complexContent.extension:
        self.derivedBy='extension'
        if hasattr(self.complexContent.extension,'base'):
          # must be a simple type
          basetypeName=QName(self.complexContent.extension.base,elt,
                             self.schema.factory)
    elif self.simpleContent:
      self.sub=self.simpleContent
      self.content='textOnly'
      if self.simpleContent.restriction:
        self.derivedBy='restriction'
        if hasattr(self.simpleContent.restriction,'base'):
          basetypeName=QName(self.simpleContent.restriction.base,elt,
                             self.schema.factory)
      elif self.simpleContent.extension:
        self.derivedBy='extension'
        if hasattr(self.simpleContent.extension,'base'):
          basetypeName=QName(self.simpleContent.extension.base,elt,
                             self.schema.factory)
      else:
        shouldnt('stcm')
    else:
      # handle shorthand case with no complex/simpleContent
      self.derivedBy='restriction'
      if self.mixed=='true':
        self.content='mixed'
      else:
        self.content='elementOnly'
      if self.__dict__.has_key('sequence'):
        self.model=self.sequence
      elif self.__dict__.has_key('choice'):
        self.model=self.choice
      elif self.__dict__.has_key('group'):
        self.model=self.group
      elif self.__dict__.has_key('all'):
        self.model=self.all
      elif self.__dict__.has_key('attrs'):
        self.model=None                 # TODO: check this actually works!
        if self.content=='elementOnly':
          self.content='empty'
      # attrs case works as is
      if not self.__dict__.has_key('model'):
        # renaming the urType
        # TODO: check this actually works!
        self.model=None
        self.attrs=[]
        if self.content=='elementOnly':
          self.content='empty'
    self.schema.factory.eltStack=self.schema.factory.eltStack[1:]
    if not hasattr(self,'final'):
      self.final=self.schema.finalDefault
    self.component=ComplexType(self.schema.factory,self)
    if basetypeName:
      self.component.basetypeName=basetypeName
    else:
      self.component.basetype=urType

class Ur(ComplexType,SimpleType):

  def __init__(self,factory):
    Component.__init__(self,factory,None)
    self.attrTable={}
    self.elemTable={}

  def isSubtype(self,other):
    return 1

class QName:
  # either QName(qname, item, factory) or QName(prefix, local, uri) or
  #        QName(qname, nsdict, factory) (doesn't happen????)
  # TODO: check the NSURI has been imported and give error if not
  def __init__(self, arg1, arg2, arg3):
    # print "QName(%s,%s,%s),%s,%s" % (arg1, arg2, arg3,isinstance(arg2,layer.Mapper),isinstance(arg2,layer.Mapper) and arg3.processingInclude)
    if isinstance(arg2,layer.Mapper):
      (self.prefix,self.local) = splitQName(arg1)
      if self.prefix or not arg3.processingInclude==2:
        self.uri=arg2.lookupPrefix(self.prefix)
      else:
        # chameleon include fix
        self.uri=arg3.targetNS
      if self.prefix and not self.uri:
        self.uri="error: prefixWasNotDeclared"
    elif type(arg2) == types.DictType:
      (self.prefix,self.local) = splitQName(arg1)
      if arg2.has_key(self.prefix):
        self.uri=arg2[self.prefix]
      elif self.prefix:
        self.uri="error: prefixWasNotDeclared"
      else:
        self.uri=None
    else:
      self.prefix = arg1
      self.local = arg2
      self.uri = arg3
    self.pair = (self.uri, self.local)

  def __str__(self):
    return str(self.string())             # str is in case we're unicode

  def __cmp__(self, other):
    # print "comparing %s and %s" % (self,other)
    if not isinstance(other, QName):
      # ??? XXX
      return -1
    return cmp(self.pair, other.pair)

  def __hash__(self):
    return hash(self.pair)

  def string(self):
    # may be Unicode
    return "%s{%s}:%s" % (self.prefix or "",self.uri,self.local)

def splitQName(qname):
  n=string.find(qname,':')
  if n>-1:
    prefix=qname[0:n]
    local=qname[n+1:]
  else:
    prefix=None
    local=qname
  return (prefix, local)
    
class contentElt(commonElt):
  sub=None
  restriction=None
  extension=None
  def init(self,elt):
    if self.restriction:
      self.sub=self.restriction
    elif self.extension:
      self.sub=self.extension
    else:
      shouldnt('cecm')

  def __getattr__(self,name):
    if (self.sub and
        name in ('facets','sequence','choice','all','group','attrs')):
      return getattr(self.sub,name)
    else:
      raise AttributeError,name
  
class complexContentElt(contentElt):
  mixed='unspecified'
  def init(self,elt):
    contentElt.init(self,elt)
    self.model=self.sequence or self.choice or self.group or self.all

class simpleContentElt(contentElt):
  def init(self,elt):
    contentElt.init(self,elt)

class extensionElt(commonElt):
  group=None
  all=None
  choice=None
  sequence=None
  facets=[]
  attrs=[]

class Group(Component):
  compositor=None
  particles=[]
  reflectedName='modelGroup'
  reflectionMap=(('compositor','string',0,'compositor'),
                 ('particles','components',0,'particles'),
                 ('annotation','component',1,'annotation'))
  def __init__(self,factory,xrpr,surrogate=None):
    Component.__init__(self,factory,xrpr)
    if xrpr and self.compositor:
      self.particles=map(lambda p:p.component,
                         filter(lambda p:p.component,xrpr.model))
    elif surrogate:
      # really for errors only
      self.xrpr=surrogate

  def __str__(self):
    name = self.name or "[anon]"
    model = map(str, self.particles)
    return "{Group %s comp %s:%s}" % (name, self.compositor, string.join(model, ''))

  def merge(self,other):
    # should check cardinality
    # should check for deletions
    op=other.particles
    res=[]
    for mp in self.particles:
      sub=mp.merge(op[0])
      if sub:
        res.append(sub)
      op=op[1:]
    self.particles=res
    return self

  def note(self,table):
    map(lambda p,t=table:p.note(t),
        self.particles)

  def prepare(self):
    if self.prepared:
      return 1
    p1=self.note({})
    self.prepared=1
    return p1

class Particle(Component):
  termName=None
  reflectedName='particle'
  reflectionMap=(('minOccurs','string',0,'minOccurs'),
                 ('maxOccurs','string',0,'maxOccurs'),
                 ('term','component',1,'term'))
  def __init__(self,factory,xrpr,term,surrogate=None):
    Component.__init__(self,factory,xrpr,None)
    if xrpr:
      self.occurs=computeMinMax(xrpr.minOccurs or "1",xrpr.maxOccurs,self)
    elif surrogate:
      # for errors only
      self.xrpr=surrogate
    if term:
      self.term=term

  def __getattr__(self,name):
    if name=='term':
      self.term=None
      if self.termName:
        if self.termType=='element':
          if self.schema.vElementTable.has_key(self.termName):
            self.term=self.schema.vElementTable[self.termName]
          else:
            self.error("Undefined element %s referenced from content model"%self.termName)
        elif self.termType=='group':
          if self.schema.vGroupTable.has_key(self.termName):
            self.term=self.schema.vGroupTable[self.termName]
          else:
            self.error("Undefined group %s referenced from content model"%self.termName)
        else:
          shouldnt('tnt')
      else:
        shouldnt('tn')
      return self.term
    elif name=='minOccurs':
      return str(self.occurs[0])
    elif name=='maxOccurs':
      return str(self.occurs[1] or 'unbounded')
    else:
      raise AttributeError,name

  def merge(self,other):
    if self.occurs==(0,0):
      return None
    if (hasattr(self,'termName') and hasattr(other,'termName') and
        self.termName==other.termName):
      return self
    if self.term.__class__==other.term.__class__:
      if (self.occurs[0]>=other.occurs[0] and
          ((not other.occurs[1]) or
           (self.occurs[1] and
            self.occurs[1]<=other.occurs[1]))):
        self.term=self.term.merge(other.term)
      else:
        self.schema.error('restriction range %s not a sub-range of base %s'%
                          (self.occurs,other.occurs),self.xrpr.elt)
      return self
    if other.term.__class__==Choice and self.term.__class__==Element:
      # special case this because it occurs in SforS
      for om in other.term.particles:
        if (om.term.__class__==Element and
            om.term.name==self.term.name and
            om.term.targetNamespace==self.term.targetNamespace):
          return self.merge(om)
    self.xrpr.error('non-like-for-like restriction not checked yet: %s vs. %s'%(self.term.__class__,other.term.__class__),1)
    return self

  def note(self,table):
    # may be pointless if we ref an undefined element
    if self.term:
      self.term.note(table)

  def translate(self, next, place):
    fsm = next.fsm
    n = next
    if not self.term:
       if self.termType=='element' and self.termName:
         # we can check the content model, recursive type check may fail
         qq=self.termName
       else:
         qq=UndefQName
    if self.occurs[1] and self.occurs[1]>101:
      self.schema.error('performance restriction means max>101 means unbounded',
                        self.xrpr.elt,1)
      self.occurs=(self.occurs[0],None)
    if not self.occurs[1]:
      t = FSMNode(fsm)
      if self.term:
        n = self.term.translate(t, place)
      else:
        n = FSMNode(n.fsm)
        FSMEdge((qq,place),n,t)
      FSMEdge(None, t, n)
      FSMEdge(None, n, next)
    else:
      for i in range(self.occurs[0], self.occurs[1]):
        if self.term:
          n = self.term.translate(n, place)
        else:
          m=n
          n = FSMNode(n.fsm)
          FSMEdge((qq,place),n,m)
        FSMEdge(None, n, next)
    if self.occurs[0]>101:
      self.schema.error('performance restrictions means min>101 means 101',
                        self.xrpr.elt,1)
      self.occurs=(101,self.occurs[1])
    for i in range(0, self.occurs[0]): 
      if self.term:
        n = self.term.translate(n, place)
      else:
        m=n
        n = FSMNode(n.fsm)
        FSMEdge((qq,place),n,m)
    return n

  def exponent(self):
    if self.occurs[0]==0:
      if self.occurs[1]==1:
        return '?'
      else:
        return '*'
    if (self.occurs[0]==1 and
	(self.occurs[1]==1)):
      return ''
    else:
      return '+'

UndefQName=QName(None,'#undef#',None)

class Sequence(Group):
  compositor='sequence'

  def translate(self, next, place):
    n = next
    rmodel = copy.copy(self.particles)
    rmodel.reverse()
    i = 0
    for particle in rmodel:
      n = particle.translate(n, (place,i))
      i = i+1
    return n

class Choice(Group):
  compositor='choice'
  def translate(self, next, place):
    n = FSMNode(next.fsm)
    i = 0
    for particle in self.particles:
      m = particle.translate(next, (place,i))
      i = i+1
      FSMEdge(None, n, m)
    return n

# TODO: why is anybody looking at the details of this, c.f. prisc.{xml,xsd}
urType=Ur(None)
urType.basetype=urType
urType.effectiveType=urType
urType.final=[]
urType.prohibitedSubstitutions=[]
urType.contentType='mixed'
urType.model=Particle(None,None,Sequence(None,None))
urType.model.occurs=(1,1)
urType.model.term.particles=[]
urType.name='anyType'
urType.targetNamespace=XMLSchemaNS
urType.attributeDeclarations={}
urType.derivationMethod='restriction'
urType.abstract='false'
urType.extendable=0			# stale!!

class AbInitio:
  name=None
  attributeDeclarations={}              # for use when this is a ct's basetype
  contentType='textOnly'                # ditto
  elementTable={}                       # ditto
  basetype=None
  final=""
  content=None
  abstract="false"
  variety='atomic'
  targetNamespace=XMLSchemaNS
  allowedFacets=[]
  def __init__(self,factory,basetype):
    self.factory=factory
    self.elements=[]
    self.facets={}
    if basetype and basetype.__class__==SimpleType:
      basetype=basetype.primitiveType
    for fn in self.allowedFacets:
      if basetype and basetype.facets.has_key(fn):
	self.facets[fn]=basetype.facets[fn]
      else:
	self.facets[fn]=None

  def prepare(self):
    return 1

  def restrict(self,restr):
    return Atomic(self.factory,restr)

  def facetValue(self,name):
    if self.facets.has_key(name):
      f=self.facets[name]
      return f and f.value

  def mergeFacets(self,surfaceType,newTable):
    # Called from surfaceType.primitiveType
    # We are always correct, because we started from the basetype
    # facets are all instances of class facet or None
    tname=surfaceType.basetype.name
    allowed=self.allowedFacets
    for facetName in newTable.keys():
      if facetName in allowed:
        newFacet=newTable[facetName]
        old=self.facets[facetName]
        if old and old.fixed and newFacet.value!=old.value:
          surfaceType.error("facet %s is fixed in basetype %s, cannot be changed"%(facetName,tname))
        if not old or getattr(self,checkFacetTable[facetName])(facetName,
                                                               old.value,
                                                               newFacet.value,
                                                               newTable,
                                                               surfaceType):
          self.facets[facetName]=newFacet
      else:
	surfaceType.error("facet %s not allowed on type %s"%(facetName,tname))

  def checkMax(self,facetName,old,newVal,newTable,td):
    return 1

  def checkMin(self,facetName,old,newVal,newTable,td):
    if facetName=='minInclusive':
      b='['
      o='('
      otherName='minExclusive'
    else:
      b='('
      o='['
      otherName='minInclusive'
    if newTable.has_key(otherName):
      td.error("can't use minInclusive and minExclusive in same simple type")
      return
    return self.checkMinVals(facetName,newVal,otherName,old,b,o,td)

  def checkEnum(self,facetName,old,newVal,newTable,td):
    return 1

  def checkPS(self,facetName,old,newVal,newTable,td):
    return 1

  def vacuousCheck(self,facetName,old,newVal,newTable,td):
    return 1

  def isSubtype(self,other):
    if self==other:
      return 1
    if isinstance(self.basetype,AbInitio):
      return self.basetype.isSubtype(other)
    elif other==urType:
      return 1
    else:
      return 0

  def checkMinVals(self,facetName,newVal,otherName,old,b,o,td):
    # should be overriden
    return

urSimpleType=AbInitio(None,None)
urSimpleType.basetype=urType
urSimpleType.rootName=urSimpleType.name='anySimpleType'
urSimpleType.effectiveType=urSimpleType
AbInitio.basetype=urSimpleType

class BooleanST(AbInitio):
  name='boolean'
  allowedFacets=['pattern','whiteSpace']

class StringST(AbInitio):
  name='string'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'length',
		 'maxLength', 'minLength', 'pattern','whiteSpace']

class NumericST(AbInitio):
  def checkMinVals(self,facetName,newVal,otherName,old,b,o,td):
    # implement lots of fiddley constraints on min facets
    new=convertToNum(newVal,facetName,td)
    # some or all of this could be shared with other types . . .
    if new==None:
      return
    ok=1
    if (old!=None and new<old):
      ok=0
    else:
      old=hasattr(self,otherName) and getattr(self,otherName)
      if (old!=None and
	  ((facetName=='minInclusive' and new<=old) or
	   (facetName=='minExclusive' and new<old))):
	ok=0
    if not ok:
      td.error("attempt to reduce range lower bound from %s%d to %s%d"%(o,old,b,new))
      return
    # check against max -- ?? -- doesn't actually say anywhere . . .
    # For now I declare that an empty range, e.g. (3,4) is OK but an
    # incoherent one, e.g. (3,3) or [3,2] is not
    # Note that what consititutes an empty range depends on fractionDigits
    # I'm not sure what follows is correct, done when tired
    # [1.1,1.1] - OK  [1.1,1.1) - no (1.1,1.1] - no (1.1,1.1) no
    max=self.facetValue('maxExclusive')
    if max!=None and new>=max:
      ok=0
      o=')'
    else:
      max=self.facetValue('maxInclusive')
      if (max!=None and ((facetName=='minExclusive' and new>=max) or
			 # minInclusive
			 new>max)):
	ok=0
	o=']'
    if not ok:
      td.error("attempt to raise range lower bound above upper bound: %s%d,%d%s"%(b,new,max,o))
      return
    return 1

class FloatST(NumericST):
  name='float'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class DoubleST(NumericST):
  name='double'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class DecimalST(NumericST):
  name='decimal'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive', 'pattern',
		 'maxInclusive', 'enumeration', 'totalDigits',
                 'fractionDigits', 'whiteSpace']

class TimeDurationST(AbInitio):
  name='duration'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class DateTimeST(AbInitio):
  name='dateTime'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class TimeST(AbInitio):
  name='time'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class DateST(AbInitio):
  name='date'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class gYearMonthST(AbInitio):
  name='gYearMonth'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class gYearST(AbInitio):
  name='gYear'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class gMonthDayST(AbInitio):
  name='gMonthDay'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class gDayST(AbInitio):
  name='gDay'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class gMonthST(AbInitio):
  name='gMonth'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'pattern','whiteSpace']

class HexBinaryST(AbInitio):
  name='hexBinary'
  allowedFacets=['length', 'minLength', 'maxLength',
                 'pattern', 'enumeration','whiteSpace']

class Base64BinaryST(AbInitio):
  name='base64Binary'
  allowedFacets=['length', 'minLength', 'maxLength',
                 'pattern', 'enumeration','whiteSpace']

class URIReferenceST(AbInitio):
  name='anyURI'
  allowedFacets=['length', 'minLength', 'maxLength',
                 'pattern', 'enumeration','whiteSpace']

class NOTATIONST(AbInitio):
  name='NOTATION'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'length',
		 'maxLength', 'minLength', 'pattern','whiteSpace']

class QNameST(AbInitio):
  name='QName'
  allowedFacets=['minExclusive', 'maxExclusive', 'minInclusive',
		 'maxInclusive', 'enumeration', 'length',
		 'maxLength', 'minLength', 'pattern','whiteSpace']

abInitioTypes=(('boolean',BooleanST), ('string',StringST), ('float',FloatST),
	       ('double',DoubleST), ('decimal',DecimalST),
               ('duration',TimeDurationST),
               ('dateTime',DateTimeST), ('time',TimeST), ('date',DateST),
               ('gYearMonth',gYearMonthST), ('gYear',gYearST), 
               ('gMonthDay',gMonthDayST), ('gDay',gDayST), ('gMonth',gMonthST),
	       ('base64Binary',Base64BinaryST), ('hexBinary',HexBinaryST),
               ('anyURI',URIReferenceST),
               ('NOTATION',NOTATIONST),('QName',QNameST))

class Facet(Component,commonElt):
  # note this is schizo -- both elt and component
  annotation=None
  fixed=0
  def __init__(self,factory,elt):
    commonElt.__init__(self,factory,elt)

  def init(self,elt):
    self.xrpr=self
    self.stringValue=self.value
    self.value=self.val()

  def register(self,table):
    if table.has_key(self.name):
      self.error("Not allowed multiple values for %s"%self.name)
    else:
      table[self.name]=self

  def val(self):
    return self.value

def convertToNum(val,facetName,context):
  if type(val) in (types.IntType,types.LongType,types.FloatType):
    return val
  try:
    if ('.' in val) or ('E' in val):
      return string.atof(val)
    else:
      return string.atol(val)
  except:
    context.error("facet %s value not a valid numeric literal: %s"%
                  (facetName,val))
    return

class NumFacet(Facet):
  def val(self):
    return convertToNum(self.value,self.name,self)

class MaxInclusive(NumFacet):
  name='maxInclusive'

class MinInclusive(NumFacet):
  name='minInclusive'

class MinExclusive(NumFacet):
  name='minExclusive'

class MaxExclusive(NumFacet):
  name='maxExclusive'

class FractionDigits(NumFacet):
  name='fractionDigits'

class TotalDigits(NumFacet):
  name='totalDigits'

class Length(NumFacet):
  name='length'

class MaxLength(NumFacet):
  name='maxLength'

class MinLength(NumFacet):
  name='minLength'

class Whitespace(Facet):
  name='whiteSpace'
  
class ListFacet(Facet):
  def register(self,table):
    if table.has_key(self.name):
      table[self.name].value.append(self.value)
    else:
      table[self.name]=self
      self.value=[self.value]

class Pattern(ListFacet):
  name='pattern'

class Enumeration(ListFacet):
  name='enumeration'

builtinTypeNames=[
  ('normalizedString','string',((Whitespace,"replace"),)),
  ('token','normalizedString',((Whitespace,"collapse"),)),
  ('language','string',
   ((Pattern,["([a-zA-Z]{2}|[iI]-[a-zA-Z]+|[xX]-[a-zA-Z]+)(-[a-zA-Z]+)"]),)),
  ('NMTOKEN','string',((Pattern,["\c+"]),)),
  ('Name','string',((Pattern,["\i\c*"]),)),
  ('NCName','Name',((Pattern,["[\i-[:]][\c-[:]]"]),)),
  ('ID','NCName',()),
  ('IDREF','NCName',()),
  ('ENTITY','NCName',()),
  ('integer','decimal',((FractionDigits,0),)),
  ('nonPositiveInteger','integer',((MaxInclusive,0),)),
  ('negativeInteger','nonPositiveInteger', ((MaxInclusive,-1),)),
  ('long','integer',((MinInclusive,-9223372036854775808L),
                     (MaxInclusive,9223372036854775807L))),
  ('int','long',((MinInclusive,-2147483648L),(MaxInclusive,2147483647))),
  ('short','int',((MinInclusive,-32768),(MaxInclusive,32767))),
  ('byte','short',((MinInclusive,-128),(MaxInclusive,127))),
  ('nonNegativeInteger','integer',((MinInclusive,0),)),
  ('unsignedLong','nonNegativeInteger',((MaxInclusive,18446744073709551615L),)),
  ('unsignedInt','unsignedLong',((MaxInclusive,4294967295L),)),
  ('unsignedShort','unsignedInt',((MaxInclusive,65535),)),
  ('unsignedByte','unsignedShort',((MaxInclusive,255),)),
  ('positiveInteger','nonNegativeInteger',((MinInclusive,1),))]

builtinLists=[('NMTOKENS','NMTOKEN'),
              ('ENTITIES','ENTITY'),
              ('NOTATIONS','NOTATION'),
              ('IDREFS','IDREF')]

class Wildcard(Component):
  reflectedName='wildcard'
  reflectionMap=(('namespaceConstraint','special',
                  0,'wildcardNamespaceReflect'),
                 ('processContents','string',0,'processContents'),
                 ('annotation','component',1,'annotation'))
  negated=0
  def __init__(self,factory,xrpr,extra=0):
    Component.__init__(self,factory,xrpr)
    if xrpr:
      self.processContents=xrpr.processContents
    
  def __str__(self):
    return "{Wildcard: %s}"%self.allowed

  def allows(self,namespace):
    if self.negated:
      return namespace not in self.namespaces
    else:
      return namespace in self.namespaces

  def expand(self,tab,use):
    # Called when this wildcard is in a complexType or attributeGroup
    # have to copy when expanded, as could happen several times if
    # we're in a group
    mine=self
    if tab.has_key("#any"):
      mine=self.intersect(tab["#any"])
    newUse=AttributeUse(use.factory,use.xrpr,mine)
    newUse.minOccurs=use.minOccurs
    newUse.maxOccurs=use.maxOccurs
    tab["#any"] = newUse

  def intersect(self,other):
    if self.negated==other.negated:
      if (self.namespaces==other.namespaces and
          self.processContents==other.processContents):
        # no copy in this case?
        return self
      nw=Wildcard(self.factory,self.xrpr)
      nw.negated=self.negated
      nw.namespaces=[]
      if nw.negated:
        # neg/neg
        for n in self.namespaces+other.namespaces:
          if n not in nw.namespaces:
            nw.namespaces.append(n)
      else:
        # pos/pos
        for n in self.namespaces:
          if n in other.namespaces:
            nw.namespaces.append(n)
    else:
      nw=Wildcard(self.factory,self.xrpr)
      nw.negated=0
      if self.negated:
        # neg/pos
        inN=other.namespaces
        outN=self.namespaces
      else:
        # pos/neg
        inN=self.namespaces
        outN=other.namespaces
      if not outN:
        nw.namespaces=inN
      else:
        nw.namespaces=[]
        for n in inN:
          if n not in outN:
            nw.namespaces.append(n)
    if nw.negated:
      nw.allowed='not %s'%nw.namespaces
    else:
      nw.allowed='%s'%nw.namespaces
    if self.processContents==other.processContents:
      nw.processContents=self.processContents
    else:
      if self.processContents=='strict' or other.processContents=='strict':
        nw.processContents='strict'
      elif self.processContents=='lax' or other.processContents=='lax':
        nw.processContents='lax'
      else:
        nw.processContents='skip'
    return nw

  def checkSubtype(self,other):
    # TODO: something
    pass
  
  def isEmpty(self):
    return (not self.negated) and self.namespaces == []
  
  def note(self,table):
    pass

  def translate(self, next, place):
    n = FSMNode(next.fsm)
    FSMEdge((self,place), n, next)
    return n

class AnyAny(Wildcard):
  allowed='##any'  # for trace info
  namespaces=[]
  negated=1

class AnyOther(Wildcard):
  allowed='##other'  # for trace info
  negated=1
  def __init__(self,factory,xrpr,isAW=0):
    Wildcard.__init__(self,factory,xrpr)
    self.namespaces=[self.targetNamespace]
    if isAW and self.namespaces[0]!=None:
      self.namespaces.append(None)

class AnyInList(Wildcard):
  def __init__(self,factory,xrpr,extra=None):
    Wildcard.__init__(self,factory,xrpr)
    self.namespaces=map(self.namespaceCode,string.split(xrpr.namespace))
    self.allowed=self.namespaces

  def namespaceCode(self,arg):
    if arg=='##local':
      return None
    elif arg=='##targetNamespace':
      return self.targetNamespace
    else:
      return arg

class anyElt(commonElt):
  namespace="##any"
  minOccurs="1"
  maxOccurs=None
  processContents="strict"
  def __init__(self,factory,elt):
    commonElt.__init__(self,factory,elt)

  def init(self,elt):
    if self.maxOccurs=="0":
      self.component=None
    else:
      self.component=Particle(self.schema.factory,self,
                              RefineAny(self,self.namespace))

def RefineAny(xrpr,namespace,extra=0):
  if namespace=='##any':
    anyinst=AnyAny
  elif namespace=='##other':
    anyinst=AnyOther
  else:
    anyinst=AnyInList
  return anyinst(xrpr.schema.factory,xrpr,extra)

def computeMinMax(minStr,maxStr,comp):
  try:
    min = string.atoi(minStr)
  except ValueError:
    min=1
    comp.error("%s not a valid minOccurs value"%minStr)
  if maxStr == "unbounded":
    max = None
  elif maxStr:
    try:
      max = string.atoi(maxStr)
    except ValueError:
      max=1
      comp.error("%s not a valid maxOccurs value"%maxStr)
  else:
    max = 1
  return (min,max)

class Element(Component):
  typeDefinitionName=None
  equivClassName=None
  valueConstraint=None                  # TODO: implement this! (and in aps)
  reflectedName='elementDeclaration'
  reflectionMap=(('name','string',0,'name'),
                 ('targetNamespace','string',1,'targetNamespace'),
                 ('typeDefinition','component',1,'typeDefinition'),
                 ('scope','special',1,'scopeReflect'),
                 ('valueConstraint','special',1,'vcReflect'),
                 ('nillable','boolean',0,'nullable'),
                 ('identityConstraintDefinitions','components',
                  0,'identityConstraints'),
                 ('substitutionGroupAffiliation','component',1,'equivalenceClassAffiliation'),
                 ('substitutionGroupExclusions','list',0,'final'),
                 ('disallowedSubstitutions','list',
                  0,'prohibitedSubstitutions'),
                 ('abstract','boolean',0,'abstract'),
                 ('annotation','component',1,'annotation'))
  def __init__(self,factory,xrpr,scope):
    if scope=='global' or xrpr.form=='qualified':
      ns='ns'
    else:
      ns=None
    Component.__init__(self,factory,xrpr,ns)
    if scope=='global':
      self.scope=scope
    else:
      self.scopeRepr=scope              # an xrpr, component not available yet
    self.abstract=xrpr.abstract or 'false'
    self.nullable=xrpr.nullable or 'false'
    if xrpr.substitutionGroup:
      self.equivClassName = QName(xrpr.substitutionGroup,xrpr.elt,
                                  factory)
    self.final=string.split(xrpr.final)
    self.prohibitedSubstitutions=string.split(xrpr.block)
    if xrpr.type:
      self.typeDefinitionName=QName(xrpr.type,xrpr.elt,
                                    factory)
      if xrpr.simpleType or xrpr.complexType:
        self.error("declaration with 'type' attribute must not have nested type declaration")
    elif xrpr.simpleType:
      self.typeDefinition=xrpr.simpleType.component
    elif xrpr.complexType:
      self.typeDefinition=xrpr.complexType.component
    elif not self.equivClassName:
      self.typeDefinition=urType
    if xrpr.fixed:
      # todo: check vc against type
      self.valueConstraint=('fixed',xrpr.fixed)
    elif xrpr.default:
      self.valueConstraint=('default',xrpr.default)
    self.keys=map(lambda e:e.component,xrpr.keys)
    self.keyrefs=map(lambda e:e.component,xrpr.keyrefs)
    self.uniques=map(lambda e:e.component,xrpr.uniques)

  def __str__(self):
    if (self.typeDefinition and self.typeDefinition.name and
        self.typeDefinition.name[0]!='['):
      return "{Element {%s}%s:%s}"%(self.targetNamespace,self.name,
                                self.typeDefinition.name)
    else:
      return "{Element {%s}%s:%s}"%(self.targetNamespace,self.name,
                                str(self.typeDefinition))

  def __getattr__(self,name):
    if name=='equivalenceClassAffiliation':
      self.equivalenceClassAffiliation=None
      if self.equivClassName:
        if self.schema.vElementTable.has_key(self.equivClassName):
          self.equivalenceClassAffiliation=exemplar=self.schema.vElementTable[self.equivClassName]
          if (self.typeDefinition and exemplar.typeDefinition and
              not self.typeDefinition.isSubtype(exemplar.typeDefinition)):
            self.error("type {%s}%s not subtype of type {%s}%s of exemplar %s"%(self.typeDefinition.targetNamespace,self.typeDefinition.name, exemplar.typeDefinition.targetNamespace,exemplar.typeDefinition.name, exemplar.qname))
        else:
          self.error("Undefined element %s referenced as equivalence class affiliation"%self.equivClassName)
      return self.equivalenceClassAffiliation
    elif name=='equivClass':
      # first access propagates everything
      if self.scope!='global':
        shouldnt('not global %s'%self.name)
      for schema in self.factory.schemas.values():
        for ed in schema.elementTable.values():
          if not ed.__dict__.has_key('equivClass'):
            ed.equivClass=[]
          if (ed.abstract!='true' and ed.equivalenceClassAffiliation):
            ed.equivalenceClassAffiliation.addECM(ed)
      return self.equivClass
    elif name=='typeDefinition':
      self.typeDefinition=None
      if self.typeDefinitionName:
        if self.schema.vTypeTable.has_key(self.typeDefinitionName):
          self.typeDefinition=self.schema.vTypeTable[self.typeDefinitionName]
        else:
          self.error("Undefined type %s referenced as type definition of %s"%(self.typeDefinitionName, self.name))
      elif self.equivClassName:
        self.typeDefinition=self.equivalenceClassAffiliation.typeDefinition
      else:
        shouldnt('etd')
      return self.typeDefinition
    elif name=='identityConstraints':
      return self.uniques+self.keys+self.keyrefs
    elif name=='scope':
      self.scope=self.scopeRepr.component
      return self.scope
    else:
      raise AttributeError,name

  def prepare(self):
    if self.prepared:
      return 1
    self.prepared=1
    p5=self.scope
    if p5=='global':
      p1=self.equivalenceClassAffiliation
      p2=self.equivClass
    else:
      p1=p2=1
    p3=self.typeDefinition and self.typeDefinition.prepare()
    p4=self.identityConstraints
    return (p1 and p2 and p3 and p4 and p5)

  def addECM(self,member):
    if not self.__dict__.has_key('equivClass'):
      self.equivClass=[member]
    else:
      self.equivClass.append(member)
    if self.equivalenceClassAffiliation:
      self.equivalenceClassAffiliation.addECM(member)

  def note(self,table):
    qname=QName(None,self.name,self.targetNamespace)
    if table.has_key(qname):
      if self.typeDefinition != table[qname].typeDefinition:
	self.error("illegal redeclaration of %s" % qname)
      elif not (self.scope=='global' and table[qname].scope=='global'):
	self.error("redeclaration of %s ok - same type\n" % qname,1)
      return
    table[qname] = self
    if (self.scope=='global' and
        'substitution' not in self.prohibitedSubstitutions):
      # check for equivalence classes
      # is this necessary -- it's quite expensive
      for decl in self.equivClass:
	table[QName(None,decl.name,decl.schema.targetNS)]=decl

  def merge(self,other):
    # TODO: check default/fixed -- what else?
    if self.name!=other.name or self.targetNamespace!=other.targetNamespace:
      self.error("declaration in a restriction not same name as declaration it corresponds to: {%s}%s vs. {%s}%s"%(self.targetNamespace,self.name,other.targetNamespace,other.name))
    if (self.typeDefinition and other.typeDefinition and
        not self.typeDefinition.isSubtype(other.typeDefinition)):
      self.error("type {%s}%s not subtype of type {%s}%s of {%s}%s in restriction"%(self.typeDefinition.targetNamespace,self.typeDefinition.name, other.typeDefinition.targetNamespace,other.typeDefinition.name, self.targetNamespace, self.name))
    return self

  def translate(self, next, place):
    # what do I do if there are no edges, i.e. abstract element with
    # no descendants?
    fsm = next.fsm
    n = FSMNode(fsm)
    qname=QName(None, self.name, self.targetNamespace)
    if self.abstract!='true':
      FSMEdge((qname,place), n, next)
    if (self.scope=='global' and
        'substitution' not in self.prohibitedSubstitutions):
      for e in self.schema.vElementTable[qname].equivClass:
        FSMEdge((QName(None,e.name,e.targetNamespace),place),n,next)
    return n

class particleElt:
# shared by groupElt and elementElt
  minOccurs=None
  maxOccurs=None
  def init(self):
    pass

class defRefElt(commonElt):
  # shared by groupElt,attributeElt and elementElt
  name=None
  ref=None
  parent=None
  def init(self,eltName,nestingElt,badAttrs=('minOccurs','maxOccurs','ref')):
    if not (self.name or self.ref):
      self.error("%s with no name or ref"%eltName) # die?
    parent=self.schema.factory.eltStack[0]
    if isinstance(parent,complexTypeElt) or isinstance(parent,nestingElt):
      self.parent=parent
      if self.ref:
        # TODO: check ref syntax
        self.checkRefed()
      else:
        # TODO: check name syntax
        self.checkInternal()
    else:
      for an in badAttrs:
        if getattr(self,an)!=None:
          self.error("top-level %s may not have %s"%(eltName,an))
          setattr(self,an,None)
      # top-level def
      # TODO: check name syntax more thoroughly
      if ':' in self.name:
        self.error("'name' must be an NCName") # die?
      self.checkTop()

class elementElt(defRefElt,particleElt):
  type=None
  complexType=None
  simpleType=None
  form=None
  default=None
  fixed=None
  substitutionGroup=None
  nullable=None
  parent=None
  abstract=None
  final=None
  block=None
  def __init__(self,factory,elt):
    defRefElt.__init__(self,factory,elt)
    self.keys=[]
    self.keyrefs=[]
    self.uniques=[]

  def init(self,elt):
    # does some simple checks and calls back on of three following methods
    defRefElt.init(self,'element',groupElt)
    particleElt.init(self)

  def checkRefed(self):
    # called if nested 'ref' form
    for ba in ('type','block','default','nullable','fixed','complexType','simpleType','key','keyref','unique'):
      if hasattr(self,ba) and getattr(self,ba):
        self.error("element with ref can't have %s"%ba)
        setattr(self,ba,None)
    if self.maxOccurs=="0":
      self.component=None
    else:
      self.component=Particle(self.schema.factory,self,None)
      self.component.termName=QName(self.ref,self.elt,
                                    self.schema.factory)
      self.component.termType='element'

  def checkInternal(self):
    # local def
    if not self.form:
      self.form=self.schema.elementFormDefault
    self.final=''
    self.block=''
    nElt=Element(self.schema.factory,self,self.parent)
    if self.maxOccurs=="0":
      self.component=None
    else:
      self.component=Particle(self.schema.factory,self,nElt)

  def checkTop(self):
    # top-level def
    if self.final==None:
      self.final=self.schema.finalDefault
    if self.final=='#all':
      self.final='restriction extension'
    if self.block==None:
      self.block=self.schema.blockDefault
    if self.block=='#all':
      self.block='restriction extension substitution'
    self.component=Element(self.schema.factory,self,'global')

  def merge(self,other):
    # called in content model restricting
    myName=self.name or self.ref
    if other.__class__==Element:
      otherName=other.name or other.ref
      if myName==otherName:
	# should do subsumption check, construct merged type, of course
	return self
      else:
	self.error("can't restrict %s with %s"%(otherName,myName))

attrOccurs={'prohibited':(0,0),
            'optional':(0,1),
            'default':(0,1),
            'fixed':(0,1),
            'required':(1,1)}

class AttributeUse(Component):
  nameType=None
  attributeDeclarationName=None
  valueConstraint=None
  minOccurs=1
  maxOccurs=1
  reflectedName='attributeUse'
  reflectionMap=(('required','boolean',0,'minOccurs'),
                 ('attributeDeclaration','component',1,'attributeDeclaration'),
                 ('valueConstraint','special',1,'vcReflect'))
  def __init__(self,factory,xrpr,attributeDeclaration,use=None,vct=None,
               value=None):
    Component.__init__(self,factory,xrpr,None)
    if use:
      (self.minOccurs,self.maxOccurs)=attrOccurs[use]
    if vct:
      self.valueConstraint=(vct,value)
    if attributeDeclaration:
      self.attributeDeclaration=attributeDeclaration

  def __getattr__(self,name):
    if name=='attributeDeclaration':
      if self.attributeDeclarationName and self.nameType=='attribute':
        if self.schema.vAttributeTable.has_key(self.attributeDeclarationName):
          self.attributeDeclaration=self.schema.vAttributeTable[self.attributeDeclarationName]
        else:
          self.error("Undeclared attribute %s referenced"%(self.attributeDeclarationName))
          self.attributeDeclaration=None
        return self.attributeDeclaration
      else:
        shouldnt('attrUse1')
    elif name=='attributeGroup':
      if self.attributeDeclarationName and self.nameType=='attributeGroup':
        if self.schema.vAttributeGroupTable.has_key(self.attributeDeclarationName):
          self.attributeGroup=self.schema.vAttributeGroupTable[self.attributeDeclarationName]
        else:
          self.error("Undeclared attribute group %s referenced"%(self.attributeDeclarationName))
          self.attributeGroup=None
        return self.attributeGroup
      else:
        shouldnt('attrUse2')
    elif name=='qname':
      # allow type derivation without chasing refs
      if self.attributeDeclarationName:
        self.qname=self.attributeDeclarationName
      else:
        self.qname=QName(None,self.attributeDeclaration.name,
                         self.attributeDeclaration.targetNamespace)
      return self.qname
    else:
      raise AttributeError,name

  def expand(self,table):
    # ref might be broken, so check before forwarding
    if self.nameType=='attributeGroup':
      if self.attributeGroup:
        self.attributeGroup.expand(table)
    elif self.attributeDeclaration:
      # might lose, so check first
      self.attributeDeclaration.expand(table,self)

class Attribute(Component):
  attrName=None
  attrDef=None
  typeDefinitionName=None
  valueConstraint=None
  reflectedName='attributeDeclaration'
  reflectionMap=(('name','string',0,'name'),
                 ('targetNamespace','string',1,'targetNamespace'),
                 ('typeDefinition','component',1,'typeDefinition'),
                 ('scope','special',1,'scopeReflect'),
                 ('valueConstraint','special',1,'vcReflect'),
                 ('annotation','component',1,'annotation'))
  def __init__(self,factory,xrpr,scope):
    if scope=='global' or xrpr.form=='qualified':
      ns='ns'
    else:
      ns=None
    Component.__init__(self,factory,xrpr,ns)
    if scope=='global':
      self.scope=scope
      if xrpr.default!=None:
        self.valueConstraint=('default',xrpr.default)
      elif xrpr.fixed!=None:
        self.valueConstraint=('fixed',xrpr.fixed)
    else:
      self.scopeRepr=scope
    if xrpr.type:
      self.typeDefinitionName=QName(xrpr.type,xrpr.elt,
                                    factory)
      if xrpr.simpleType:
        self.error("declaration with 'type' attribute must not have nested type declaration")
    elif xrpr.simpleType:
      self.typeDefinition=xrpr.simpleType.component
    else:
      self.typeDefinition=urType

  def __str__(self):
    if (self.typeDefinition and self.typeDefinition.name and
        self.typeDefinition.name[0]!='['):
      return "{Attribute {%s}%s:%s}"%(self.targetNamespace,self.name,self.typeDefinition.name)
    else:
      return "{Attribute {%s}%s:%s}"%(self.targetNamespace,self.name,str(self.typeDefinition))

  def __getattr__(self,name):
    if name=='typeDefinition':
      self.typeDefinition=None
      if self.typeDefinitionName:
        if self.schema.vTypeTable.has_key(self.typeDefinitionName):
          self.typeDefinition=self.schema.vTypeTable[self.typeDefinitionName]
          if isinstance(self.typeDefinition,ComplexType):
            self.error("type definition for an attribute ({%s}%s) must not be complex: %s"%(self.targetNamespace,self.name,self.typeDefinitionName))
            self.typeDefinition=None
        else:
          self.error("Undefined type %s referenced as type definition of {%s}%s"%(self.typeDefinitionName, self.targetNamespace, self.name))
      return self.typeDefinition
    elif name=='scope':
      self.scope=self.scopeRepr.component
      return self.scope
    else:
      raise AttributeError,name

  def prepare(self):
    if self.prepared:
      return 1
    self.prepared=1
    p1=self.typeDefinition and self.typeDefinition.prepare()
    p2=self.scope
    return p1 and p2

  def expand(self,tab,use):
    qn=use.qname
    if tab.has_key(qn):
      self.error("attempt to redeclare attribute %s, ignored" % qn)
    else:
      tab[qn]=use

  def checkSubtype(self,other):
    mytype=self.typeDefinition
    if (mytype and
        other.typeDefinition and
        not mytype.isSubtype(other.typeDefinition)):
      self.error("restricting attribute with type {%s}%s not derived from declared base's attribute's type %s{%s}"%(mytype.targetNamespace,mytype.name,other.typeDefinition.targetNamespace,other.typeDefinition.name))

class attributeElt(defRefElt):
  type=None
  simpleType=None
  form=None
  use=None
  default=None
  fixed=None

  def init(self,elt):
    defRefElt.init(self,'attribute',attributeGroupElt,('ref',))

  def checkRefed(self):
    if self.type:
      self.error("attribute with ref %s can't have type %s"%(self.ref,self.type))
      self.type=None
    elif self.simpleType:
      self.error("attribute with ref %s can't have simpleType"%self.ref)
      self.simpleType=None
    if self.default!=None:
      vct='default'
      value=self.default
    elif self.fixed!=None:
      vct='fixed'
      value=self.fixed
    else:
      vct=value=None
    self.component=AttributeUse(self.schema.factory,self,None,
                                self.use or 'optional',vct,value)
    self.component.attributeDeclarationName=QName(self.ref,self.elt,
                                                  self.schema.factory)
    self.component.nameType='attribute'

  def checkInternal(self):
    # local def
    if self.default!=None:
      vct='default'
      value=self.default
    elif self.fixed!=None:
      vct='fixed'
      value=self.fixed
    else:
      vct=value=None
    if not self.form:
      self.form=self.schema.attributeFormDefault
    nAttr=Attribute(self.schema.factory,self,self.parent)
    self.component=AttributeUse(self.schema.factory,self,nAttr,
                                self.use or 'optional',vct,value)

  def checkTop(self):
    # top-level def
    self.component=Attribute(self.schema.factory,self,'global')

class AttributeGroup(Component):
  # TODO: check wildcard intersection during expansion
  base=None
  dummy=None # XXX for wildcard reflection, remove when implemented
  reflectedName='attributeGroupDefinition'
  reflectionMap=(('name','string',0,'name'),
                 ('targetNamespace','string',1,'targetNamespace'),
                 ('attributeUses','components',0, 'attributeDeclarations'),
                 ('attributeWildcard', 'component', 1, 'dummy'), # XXX not done
                 ('annotation','component',1,'annotation'))

  def __str__(self):
    return "{AttrGroup %s}" % self.name

  def __getattr__(self,name):
    if name=='attributeDeclarations':
      tab={}
      for xa in self.xrpr.attrs:
        if xa.component.maxOccurs!=0:
          xa.component.expand(tab)
      if self.base:
        # we were redefined wrt self.base, check restriction
        for ad in self.base.attributeDeclarations:
          if tab.has_key(ad.qname):
            me=tab[ad.qname]
            if ad.minOccurs==1:
              if me.minOccurs==0:
                self.error("attempt to make required attribute %s optional"%me.qname)
                me.minOccurs=1
            if ad.valueConstraint:
              if (ad.valueConstraint[0]=='fixed' and
                  ((not me.valueConstraint) or
                   me.valueConstraint[0]!='fixed' or
                   me.valueConstraint[1]!=ad.valueConstraint[1])):
                self.error("attempt to change or abandon fixed value for attribute %s"%me.qname)
            me.attributeDeclaration.checkSubtype(ad.attributeDeclaration)
          elif ad.minOccurs==1:
            self.error("attempt to eliminate required attribute %s"%ad.qname)
      self.attributeDeclarations=tab.values()
      return self.attributeDeclarations
    else:
      raise AttributeError,name

  def prepare(self):
    if self.prepared:
      return 1
    self.prepared=1
    p1=self.attributeDeclarations
    if p1:
      for au in p1:
        ad=au.attributeDeclaration
        if isinstance(ad,Attribute):
          p1=ad.prepare() and p1
    return p1

  def expand(self,table):
    for au in self.attributeDeclarations:
      au.expand(table)

  def redefine(self):
    # we have a component which should be based on itself
    # note this forces some reference resolution normally left until later
    if not self.schema.attributeGroupTable.has_key(self.name):
      self.error("attempt to redefine in terms of non-existent attribute group: %s"%self.name)
      return
    else:
      redefed=self.schema.attributeGroupTable[self.name]
    qn=QName(None,self.name,self.schema.targetNS)
    selfRefs=self.findSelfRefs(qn)
    if len(selfRefs)>1:
      self.error("more than one self-reference not allowed in attribute group redefinition")
    else:
      redefed.name="original "+self.name
      self.schema.attributeGroupTable[redefed.name]=redefed
      self.schema.attributeGroupTable[self.name]=self
      self.qname=qn
      if len(selfRefs)==0:
        # must be a restriction -- postpone the real work
        self.base=redefed
      elif len(selfRefs)==1:
        # an extension, just use it, duplicates will be detected later
        selfRefs[0].component.attributeDeclarationName=QName(None,redefed.name,
                                                             self.schema.targetNS)

  def findSelfRefs(self,qn):
    return filter(lambda d,qn=qn:(isinstance(d,attributeGroupElt) and
                                  d.component.qname==qn),
                  self.xrpr.attrs)

class attributeGroupElt(defRefElt):
  def __init__(self,factory,elt):
    defRefElt.__init__(self,factory,elt)
    factory.eltStack[0:0]=[self]
    self.attrs=[]

  def init(self,elt):
    self.schema.factory.eltStack=self.schema.factory.eltStack[1:]
    defRefElt.init(self,'attributeGroup',attributeGroupElt,('ref',))

  def checkRefed(self):
    if self.attrs:
      self.error("can't have ref %s and attrs in attributeGroup"%self.ref)
    if self.name:
      self.error("internal attributeGroup with name %s"%self.name)
      self.name=''
    self.component=AttributeUse(self.schema.factory,self,None)
    self.component.attributeDeclarationName=QName(self.ref,self.elt,
                                                  self.schema.factory)
    self.component.nameType='attributeGroup'

  def checkInternal(self):
    self.error("internal attributeGroup must have ref")

  def checkTop(self):
    # only called if we are a top-level attributeGroup
    self.component=AttributeGroup(self.schema.factory,self)

class explicitGroupElt(commonElt):
  minOccurs=None
  maxOccurs=None
  def __init__(self,factory,elt):
    commonElt.__init__(self,factory,elt)
    self.model=[]

  def init(self,elt):
    if self.maxOccurs=="0":
      self.component=None
    else:
      self.component=Particle(self.schema.factory,self,
                              self.compClass(self.schema.factory,self))

class All(Group):
  compositor='all'

  def translate(self, next, place):
    # doesn't enforce cardinality yet
    n = FSMNode(next.fsm)
    i = 0
    for particle in self.particles:
      m = particle.translate(n, (place,i))
      i = i+1
      FSMEdge(None, n, m)
    FSMEdge(None, n, next)
    return n

class allElt(explicitGroupElt):
  compClass=All

class choiceElt(explicitGroupElt):
  compClass=Choice

class sequenceElt(explicitGroupElt):
  compClass=Sequence

class groupElt(defRefElt,particleElt):
  # Note this is _not_ parallel to group -- it is not a common superclass of
  # choiceElt, etc.
  # It actually always disappears -- if nested with a ref, into a particle
  # with a termRef; if top-level, into a named sequence, choice or all
  def __init__(self,factory,elt):
    defRefElt.__init__(self,factory,elt)
    factory.eltStack[0:0]=[self]
    self.model=[]

  def init(self,elt):
    self.schema.factory.eltStack=self.schema.factory.eltStack[1:]
    defRefElt.init(self,'group',groupElt)
    particleElt.init(self)

  def checkRefed(self):
    if self.model:
      self.error("can't have ref %s and model in group"%self.ref)
    if self.name:
      self.error("internal group with name %s"%self.name)
      self.name=''
    if self.maxOccurs=="0":
      self.component=None
    else:
      self.component=Particle(self.schema.factory,self,None)
      self.component.termName=QName(self.ref,self.elt,
                                    self.schema.factory)
      self.component.termType='group'

  def checkInternal(self):
    self.error("internal group must have ref")
    self.component=None

  def checkTop(self):
    # only called if we are a top-level group
    # have to transform into our model
    # our xrpr is lost!
    # note that top-level groups must contain exactly one choice/sequence/all
    if not len(self.model)==1:
      self.error("Top-level model group definitions must contain exactly one choice/sequence/all")
      if len(self.model)==0:
        # arghh
        self.component=None
        return
    mod=self.model[0].component.term
    if mod:
      mod.name=self.name
    self.component=mod

def schemaFile(self,filename,base,reason):
  pass

layer.factory.schemaFile=schemaFile

class includeElt(commonElt):
  schemaLocation=None

  def init(self,elt):
    schemas=self.schema.factory.schemas
    target=self.schema.targetNS
    loc=urljoin(self.schema.factory.fileNames[0],self.schemaLocation)
    ne=XML.Element("includeAttempt")
    ne.addAttr('namespace',target)
    ne.addAttr('URI',loc)
    self.schema.factory.resElt.children.append(ne)
    if (schemas.has_key(target) and loc in schemas[target].locations):
      ne.addAttr('outcome','redundant')
      return
    res=fromFile(loc, self.schema.factory,target,1)
    if res:
      ne.addAttr('outcome','success')
    else:
      ne.addAttr('outcome','failure')
    return res

class redefineElt(includeElt):
  schemaLocation=None

  def init(self,elt):
    schemas=self.schema.factory.schemas
    res=includeElt.init(self,elt)
    if res:
      for dd in self.dds:
        if dd.name:
          dd.component.redefine()

class importElt(commonElt):
  schemaLocation=None
  namespace=None

  def init(self,elt):
    checkinSchema(self.schema.factory,
                  self.namespace,
                  self.schemaLocation,
                  elt,
                  self.schema.factory.fileNames[0])
    # TODO: we should really record the import statements present in a
    # <schema> so that we can check that there was an import in that
    # very <schema>.

def checkinSchema(factory,namespace,location,elt,base):
  ne=XML.Element("importAttempt")
  factory.resElt.children.append(ne)
  if location:
    fullLoc=urljoin(base,location)
  else:
    # what about relative NS URIs?
    fullLoc=namespace
  ne.addAttr('namespace',namespace)
  ne.addAttr('URI',fullLoc)
  if factory.schemas.has_key(namespace):
    other=factory.schemas[namespace]
    if fullLoc in other.locations:
      ne.addAttr('outcome','redundant')
    else:
      ne.addAttr('outcome','skipped')
      ne.addAttr('otherLocs',string.join(other.locations,' '))
    return
  else:
    res=fromFile(fullLoc,factory,namespace)
    if res:
      ne.addAttr('outcome','success')
    else:
      ne.addAttr('outcome','failure')
    return res

class notationElt(commonElt):
  pass

class Kcons(Component):
  reflectedName='identityConstraintDefinition'
  reflectionMap=(('name','string',0,'name'),
                 ('targetNamespace','string',1,'targetNamespace'),
                 ('identityConstraintCategory','string',0,'cname'),
                 ('selector','special',0,'selectorReflect'),
                 ('fields','special',0,'fieldsReflect'),
                 ('referencedKey','component',1,'refer'),
                 ('annotation','component',1,'annotation'))
  refer=None
  def __init__(self,factory,xrpr):
    Component.__init__(self,factory,xrpr)
    self.fields=map(lambda x:xpath.XPath(x.xpath,x.elt.namespaceDict),
                    xrpr.fields)
    self.selector=xpath.XPath(xrpr.selector.xpath,
                              xrpr.selector.elt.namespaceDict)
    
# could these all be double-rooted??
class Unique(Kcons):
  cname='unique'

class uniqueElt(commonElt):
  def init(self,elt):
    self.component=Unique(self.schema.factory,self)

class Keyref(Kcons):
  cname='keyref'
  reflectionMap=(('name','string',0,'name'),
                 ('targetNamespace','string',1,'targetNamespace'),
                 ('identityConstraintCategory','string',0,'cname'),
                 ('selector','special',0,'selectorReflect'),
                 ('fields','special',0,'fieldsReflect'),
                 ('referencedKey','special',0,'referReflect'),
                 ('annotation','component',1,'annotation'))
  def __init__(self,factory,xrpr):
    Kcons.__init__(self,factory,xrpr)
    self.refer=xrpr.refer
    
class keyrefElt(commonElt):
  def init(self,elt):
    self.component=Keyref(self.schema.factory,self)

class Key(Kcons):
  cname='key'

class keyElt(commonElt):
  def init(self,elt):
    self.component=Key(self.schema.factory,self)

class xpathElt(commonElt):
  # TODO: check syntax
  def init(self,elt):
    pass

class fieldElt(xpathElt):
  cname='field'

class selectorElt(xpathElt):
  cname='selector'

class AnyAttribute(Component):
  namespace=None
  reflectedName='wildcard'
  def __init__(self,factory,xrpr,wildcard):
    Component.__init__(self,factory,xrpr,None)
    self.wildcard=wildcard

  def merge(self,mine,other):
    self.error("*** merging anyAttrs %s and %s, not implemented yet\n"%
                   (mine, other),
               1)
    return mine

class anyAttributeElt(commonElt):
  namespace="##any"
  processContents="lax"

  def init(self,elt):
    self.component=AttributeUse(self.schema.factory,self,
                                RefineAny(self,self.namespace,1),
                                'optional')

class annotationElt(commonElt):
  documentation=[]
  appinfo=[]

  def init(self,elt):
    self.component=Annotation(self.schema.factory,self)

class Annotation(Component):
  documentation=[]
  appinfo=[]
  attrs=[]
  reflectedName='annotation'
  reflectionMap=(('applicationInformation','components',0,'appinfo'),
                 ('userInformation','components',0,'documentation'),
                 ('attributes','components',0,'attrs'))

  def __init__(self,factory,xrpr):
    Component.__init__(self,factory,xrpr)
    if xrpr:
      if xrpr.documentation:
        self.documentation=map(lambda a:a.elt.elt,xrpr.documentation)
      if xrpr.appinfo:
        self.appinfo=map(lambda a:a.elt.elt,xrpr.appinfo)

class appinfoElt(commonElt):
  pass

class documentationElt(commonElt):
  pass

eltClasses={}
for en in ["schema","complexType","element","unique","key","keyref",
           "group","all","choice","sequence","any","anyAttribute","simpleType",
           "restriction","list","union","simpleContent","complexContent",
           "field","selector","annotation","appinfo","documentation",
           "extension","attribute","attributeGroup",
           "include","import","redefine","notation"]:
  eltClasses[en]=eval(en+"Elt")
for en in [ "Enumeration","Length","Pattern"]:
  eltClasses[string.lower(en)]=eval(en)
for (en,cn) in [("fractionDigits",FractionDigits),
                ("totalDigits",TotalDigits),
                ("whiteSpace",Whitespace)]:
  eltClasses[en]=cn

for rcn in [ "Inclusive","Exclusive","Length" ]:
  for pre in [ "Max", "Min"]:
    eltClasses["%s%s"%(string.lower(pre),rcn)]=eval("%s%s"%(pre,rcn))

schemaEltDispatch=     {("schema","element"):("group","dds"),
			("group","element"):("group","model"),
			("all","element"):("group","model"),
			("choice","element"):("group","model"),
			("sequence","element"):("group","model"),
                        ("complexType","element"):"error",
                        ("complexType","any"):"error",
			("schema","group"):("group","dds"),
			("redefine","group"):("group","dds"),
                        ("restriction","group"):"self",
			("restriction","all"):"self",
			("restriction","choice"):"self",
			("restriction","sequence"):"self",
                        ("extension","group"):"self",
			("extension","all"):"self",
			("extension","choice"):"self",
			("extension","sequence"):"self",
                        ("complexType","group"):"self",
			("complexType","all"):"self",
			("complexType","choice"):"self",
			("complexType","sequence"):"self",
                        "complexContent":"self",
                        "simpleContent":"self",
			("group","group"):("group","model"),
			("group","all"):("group","model"),
			("group","choice"):("group","model"),
			("group","sequence"):("group","model"),
			("all","group"):("group","model"),
			("all","all"):("group","model"),
			("all","choice"):("group","model"),
			("all","sequence"):("group","model"),
			("choice","group"):("group","model"),
			("choice","all"):("group","model"),
			("choice","choice"):("group","model"),
			("choice","sequence"):("group","model"),
			("sequence","group"):("group","model"),
			("sequence","all"):("group","model"),
			("sequence","choice"):("group","model"),
			("sequence","sequence"):("group","model"),
			"any":("group","model"),
			"anyAttribute":("group","attrs"),
			("attributeGroup","attribute"):("group","attrs"),
			("restriction","attribute"):("group","attrs"),
			("extension","attribute"):("group","attrs"),
			("complexType","attribute"):("group","attrs"),
			("schema","attribute"):("group","dds"),
			"annotation":("group","annot"), # broken for schema
			"documentation":("group","documentation"),
			"appinfo":("group","appinfo"),
			"key":("group","keys"),
			"keyref":("group","keyrefs"),
			"unique":("group","uniques"),
			("attributeGroup","attributeGroup"):("group","attrs"),
			("complexType","attributeGroup"):("group","attrs"),
			("restriction","attributeGroup"):("group","attrs"),
			("extension","attributeGroup"):("group","attrs"),
			("schema","attributeGroup"):("group","dds"),
			("redefine","attributeGroup"):("group","dds"),
			("element","complexType"):"self",
			("element","simpleType"):"self",
			("attribute","simpleType"):"self",
                        "restriction":"self",
                        "extension":"self",
			("restriction","simpleType"):"self",
                        "list":"self",
			("list","simpleType"):"self",
                        "union":"self",
			("union","simpleType"):("group","subTypes"),
			("schema","complexType"):("group","dds"),
			("redefine","complexType"):("group","dds"),
			("schema","simpleType"):("group","dds"),
			("redefine","simpleType"):("group","dds"),
			"maxInclusive":("group","facets"),
			"maxExclusive":("group","facets"),
			"minInclusive":("group","facets"),
			"minExclusive":("group","facets"),
			"enumeration":("group","facets"),
			"fractionDigits":("group","facets"),
			"totalDigits":("group","facets"),
			"length":("group","facets"),
			"maxLength":("group","facets"),
			"minLength":("group","facets"),
			"pattern":("group","facets"),
			"whiteSpace":("group","facets"),
			"field":("group","fields"),
			"selector":"self"
			}

class FSM:
  def __init__(self):
    self.nodes = []
    self.startNode = None

  def assignIDs(self):
    if not self.nodes:
      return
    for n in self.nodes:
      n.id = None
    self.nextID = 1
    s = self.startNode
    s.assignIDs()

  def printme(self,file):
    self.assignIDs()
    self.nodes.sort(FSMNode.compareIDs)
    for n in self.nodes:
      if n.isEndNode:
	file.write("*")
      else:
	file.write(" ")
      file.write("%2d:" % n.id)
#      if n.label:
#  	sys.stdout.write("[")
#  	for nn in n.label:
#  	  sys.stdout.write("%d " % nn.id)
#  	sys.stdout.write("]")
      for e in n.edges:
	file.write(" %s->%d" % (e.label, e.dest.id))
      file.write("\n")

  def asXML(self):
    res=XML.Element("fsm")
    self.assignIDs()
    self.nodes.sort(FSMNode.compareIDs)
    for n in self.nodes:
      ne=XML.Element("node")
      res.children.append(ne)
      ne.addAttr('id',("%d" % n.id))
      if n.isEndNode:
	ne.addAttr('final','true')
      n.edges.sort(FSMEdge.compareLabels)
      for e in n.edges:
        ee=XML.Element("edge")
        ne.children.append(ee)
        if isinstance(e.label,Wildcard):
          lab=str(e.label.allowed)
        else:
          lab=str(e.label)
        ee.addAttr('label',lab)
        ee.addAttr('dest',("%d" % e.dest.id))
    return res

  # cf Aho & Ullman p93

  def determinise(self):
    D = FSM()
    D.startNode = FSMNode(D)
    D.startNode.label = eclosure([self.startNode])
    for nn in D.startNode.label:
      if nn.isEndNode:
	D.startNode.isEndNode = 1
	break
    D.unmarkedNodes = [D.startNode]
    while D.unmarkedNodes:
      x = D.unmarkedNodes[0]
      del D.unmarkedNodes[0]
      destnodes = {}
      for n in x.label:
	for e in n.edges:
	  if destnodes.has_key(e.label):
	    if not e.dest in destnodes[e.label]:
	      destnodes[e.label].append(e.dest)
	  elif e.label:
	    destnodes[e.label] = [e.dest]
      for label in destnodes.keys():
	T = destnodes[label]
	y = eclosure(T)
	found = 0
	for n in D.nodes:
	  if y == n.label:
	    for e in x.edges:
	      if e.label == label and e.dest == y:
		break
	    else:
	      FSMEdge(label, x, n)
	    break
	else:
	  n = FSMNode(D)
	  D.unmarkedNodes.append(n)
	  n.label = y
	  for nn in y:
	    if nn.isEndNode:
	      n.isEndNode = 1
	      break
	  FSMEdge(label, x, n)
    return D

class FSMEdge:
  def __init__(self, label, source, dest):
    self.label = label
    self.source = source
    self.dest = dest
    source.edges.append(self)

  def compareLabels(self,other):
    if self.label < other.label:
      return -1
    elif self.label > other.label:
      return +1
    else:
      return 0

class FSMNode:
  def __init__(self, fsm):
    fsm.nodes.append(self)
    self.fsm = fsm
    self.isEndNode = 0
    self.edges = []
    self.label = None
    self.mark = 0

  def assignIDs(self):
    if self.id:
      return
#    print "assigning id %d to %s" % (self.fsm.nextID,self)
    self.id = self.fsm.nextID
    self.fsm.nextID = self.fsm.nextID + 1
    for e in self.edges:
      e.dest.assignIDs()

  def compareIDs(self,other):
    if self.id < other.id:
      return -1
    elif self.id > other.id:
      return +1
    else:
      return 0

# cf Aho & Ullman p92

def eclosure(T):
  for n in T:
    n.mark=1
  STACK = copy.copy(T)
  ECLOSURE = copy.copy(T)
  while STACK:
    s = STACK[0]
    del STACK[0]
    for e in s.edges:
      if e.label == None:
	t = e.dest
	if not t.mark:
          t.mark=1
	  ECLOSURE.append(t)
	  STACK.insert(0, t)
  # ECLOSURE.sort()			# so we can compare them easily???
  for n in ECLOSURE:
    n.mark=0
  return ECLOSURE

# Change labels of form (name, place) to name

def relabelFSM(fsm):
  for n in fsm.nodes:
    for e in n.edges:
      if type(e.label) == types.TupleType:
        e.label = e.label[0]

# Check a FSM is deterministic

def checkFSM(fsm):
  for n in fsm.nodes:
    for e in range(len(n.edges)):
      l = n.edges[e].label
      for f in range(0,e):
        m = n.edges[f].label
        if isinstance(l, Wildcard):
          if(isinstance(m, Wildcard)):
            if not l.intersect(m).isEmpty():
              return "%s/%s" % (l,m)
          else:
            if l.allows(m.uri):
              return "%s/%s" % (l,m)
        else:
          if(isinstance(m, Wildcard)):
            if m.allows(l.uri):
              return "%s/%s" % (l,m)
          else:
            if l == m:
              return "%s/%s" % (l,m)
  return 0
  
class VMapping:
  def __init__(self, schema, tablename):
    self.schema = schema
    self.tablename = tablename

  def findSchema(self, uri, local):
    if uri == self.schema.targetNS:
      return self.schema
    # look it up
    if self.schema.factory.schemas.has_key(uri):
      return self.schema.factory.schemas[uri]
    else:
      return 0
  
  def has_key(self, key):
    if not key:
      return 0
    if not isinstance(key, QName):
      shouldnt('nk: '+str(key))
      return 0
    s = self.findSchema(key.uri, key.local)
    if not s:
      # not an error to check, we'll record one error when we really go for it
      return 0
    return s.__dict__[self.tablename].has_key(key.local)
                        
  def __getitem__(self, key):
    s = self.findSchema(key.uri, key.local)
    if not s:
      self.schema.error("unknown namespace for %s for %s" % (key.uri,
                                                               key.local))
      return None
    return s.__dict__[self.tablename][key.local]

class SchemaError(Exception):
  pass

def shouldnt(msg):
  error("Shouldn't happen "+msg)

def error(msg):
  raise SchemaError,msg

def where(elt,w):
  if w and w[3]!=0:
    if w[0]!='unnamed entity':
      elt.addAttr('entity',w[0])
    elt.addAttr('line',str(w[1]))
    elt.addAttr('char',str(w[2]))
    elt.addAttr('resource',w[3])

def whereString(w):
  if w and w[3]!=0:
    return ("in %s at line %d char %d of %s" % w)
  else:
    return "location unknown"

def topGroup(factory,rawModel):
  if isinstance(rawModel.component.term,Group):
    # we have exactly one group, so use it
    return rawModel.component
  else:
    shouldnt('tg')

# $Log: XMLSchema.py,v $
# Revision 1.196  2001/06/16 11:13:16  ht
# avoid prepare crashes
#
# Revision 1.195  2001/06/08 08:57:39  ht
# get element fixed/default into component
#
# Revision 1.194  2001/06/06 12:58:29  richard
# correct reflection map for Annotation
#
# Revision 1.193  2001/06/05 10:10:10  ht
# handle schema annotations
#
# Revision 1.192  2001/06/05 09:21:45  ht
# turn oob attributes into annotations
#
# Revision 1.191  2001/06/04 16:04:38  ht
# throw error on tns=""
# fix bug in checkMinMax
# start work on annotations, reflection thereof
# use new namespaceDict mapper property
#
# Revision 1.190  2001/05/12 14:20:48  ht
# fix old (?) bug in home location for compiled version
#
# Revision 1.189  2001/04/27 16:00:53  ht
# fix extend-by-empty pblm
#
# Revision 1.188  2001/04/25 17:01:23  richard
# more reflection
#
# Revision 1.187  2001/04/24 14:13:25  ht
# towards reflection of identity constraint
#
# Revision 1.186  2001/04/24 13:29:13  ht
# (PSV)Infoset reorganisation
#
# Revision 1.185  2001/04/17 11:34:33  ht
# make sure no particle -> empty sequence when needed
#
# Revision 1.184  2001/04/10 15:35:22  ht
# fix some pblms with independent mode
#
# Revision 1.183  2001/04/06 21:00:54  ht
# cap min/max at 101 for performance
#
# Revision 1.182  2001/04/04 20:56:30  ht
# implement -i switch to do forced schema assessment independent of any instance
#
# Revision 1.181  2001/04/04 18:46:09  ht
# make home determination more robust
#
# Revision 1.180  2001/03/31 11:43:34  ht
# number back to decimal
#
# Revision 1.179  2001/03/30 14:50:14  ht
# fix xs: bug,
# remove debugging print
#
# Revision 1.178  2001/03/20 09:28:02  ht
# actually get rid of max=0 components
#
# Revision 1.177  2001/03/17 12:11:14  ht
# merge v2001 back in to main line
#
# Revision 1.176  2001/02/12 11:34:23  ht
# catch extension error
#
# Revision 1.175.2.9  2001/03/15 12:35:19  ht
# simple type rename,
# clear out facet dead wood,
# actually process whiteSpace facet, which had been ignored (oops)
#
# Revision 1.175.2.8  2001/03/15 11:31:01  ht
# another clause in emptiable
# shift to xs: as default in DTD
#
# Revision 1.175.2.7  2001/03/10 23:52:58  ht
# uriReference -> anyURI
# implement block=substitution wrt substitution groups
#
# Revision 1.175.2.6  2001/02/24 23:51:22  ht
# detect chameleon include and treat all unprefixed QNames therein
# specially
#
# Revision 1.175.2.5  2001/02/18 22:12:26  ht
# implement lazy checking of restriction wrt rere attribute groups
# catch deletion of required attr in ct restriction
#
# Revision 1.175.2.4  2001/02/18 21:33:38  ht
# allow elt for choice restriction, to cover new SforS
# do minimal name and type checking on elt for elt restriction
# eliminate forbidden attrs from attribute groups
#
# Revision 1.175.2.3  2001/02/17 23:37:54  ht
# check that restriction doesn't relax/change fixed value constraint on attrs
# restructure attribute group attrDecls to make them properly lazy in preparation
#  for doing restrictive redefinition right, but remove that for the
# time being
#
# Revision 1.175.2.2  2001/02/14 16:59:56  ht
# merge attr use changes back in to main line
# implement attribute group redefinition
#
# Revision 1.175.2.1.2.1  2001/02/07 17:33:03  ht
# make AttrUse a full-fledged one
#
# Revision 1.175.2.1  2001/02/07 14:30:01  ht
# change NS to 2001, implement null->nil
#
# Revision 1.175  2001/02/06 14:19:37  ht
# parse XPaths at component build time
# fix crash on subst subtype check error message
#
# Revision 1.174  2001/02/06 11:21:17  ht
# merged forceDTD+infoset back to mainline
#
# Revision 1.173.2.15.2.5  2001/01/15 14:19:24  ht
# fix bug in wildcard as edge label in reflection
#
# Revision 1.173.2.15.2.4  2001/01/04 20:36:06  ht
# support PSVI of attrGroupDefn,
# protect better against missing types
#
# Revision 1.173.2.15.2.3  2000/12/23 13:07:24  ht
# fix spelling of whiteSpace,
# make equivClass computation less inefficient,
# catch loops in schema doc reading themselves
#
# Revision 1.173.2.15.2.2  2000/12/22 18:34:50  ht
# add hooks for whitespace
#
# Revision 1.173.2.15.2.1  2000/12/21 18:31:45  ht
# real facets
#
# Revision 1.173.2.15  2000/12/16 12:11:41  ht
# fix equiv name, add stubs for identity constraint reflection
#
# Revision 1.173.2.14  2000/12/14 15:59:13  ht
# put facets back in reflection, in the right place,
# move assignUid out
#
# Revision 1.173.2.13  2000/12/14 14:21:15  ht
# fix conflicting message on local elt redef
#
# Revision 1.173.2.12  2000/12/13 23:29:12  ht
# bring urtype in line with spec,
# reflection for particle, model group
#
# Revision 1.173.2.11  2000/12/13 18:22:50  ht
# fix bug in instanceList implementation,
# treat primitiveType reflection as special (always pointer),
# make non-global scope lazy,
# derive CDATA from string and token from CDATA
#
# Revision 1.173.2.10  2000/12/12 17:36:34  ht
# fix a few minor properties,
# add reflectedName and reflectionMap to many components
#
# Revision 1.173.2.9  2000/12/08 18:06:53  ht
# assign printable proper IDs to components on request,
# provide reflectedNames for some components
#
# Revision 1.173.2.8  2000/12/07 10:17:24  ht
# fix type name in builtin -instance types
#
# Revision 1.173.2.7  2000/12/06 22:43:49  ht
# start adding built-in attr decls for xsi:type etc.
#
# Revision 1.173.2.6  2000/10/31 14:58:13  ht
# store whole declaration in typeTable
# enforce new restrictions on element ref=
# handle defaulting of nullable properly
#
# Revision 1.173.2.5  2000/10/30 14:55:11  ht
# Another order of magnitude speedup of eclosure, by not sorting the
# results
#
# Revision 1.173.2.4  2000/10/30 14:39:49  ht
# speed up eclosure (by factor of 10 :-)
#
# Revision 1.173.2.3  2000/10/30 12:36:26  ht
# removed errors now caught by DTD checking
#
# Revision 1.173.2.2  2000/10/27 14:40:23  ht
# handle errors during DTD enforcement on schemas
#
# Revision 1.173.2.1  2000/10/27 14:16:47  ht
# Use a local XMLSchema.dtd to check schema documents if they dont
# supply an external DTD themselves.  Some error cases not well-handled, yet.
#
# Revision 1.174  2000/10/27 14:13:38  ht
# Use a local XMLSchema.dtd to check schema documents if they dont
# supply an external DTD themselves.  Some error cases not well-handled, yet.
#
# Revision 1.174  2000/10/27 11:28:40  ht
# Use a local XMLSchema.dtd to check schema documents if they don't
# supply an external DTD themselves.  Some error cases not well-handled
# yet.
#
# Revision 1.173  2000/10/19 09:36:04  ht
# fix bug in local scoping of defaults to <included> schemas
#
# Revision 1.172  2000/10/19 09:08:46  ht
# allow use on global elt attrs
#
# Revision 1.171  2000/10/18 15:53:32  ht
# add actual urSimpleType, use it appropriately
# fix recording of 'fixed', 'default' for attrs
# use subordinate simpleType in double-restriction case under
# simpleContent
#
# Revision 1.170  2000/10/17 13:22:08  ht
# typo in last fix :-(
#
# Revision 1.169  2000/10/17 13:13:08  ht
# allow for empty model wiht 'mixed'
#
# Revision 1.168  2000/10/17 12:44:22  ht
# keep going if restriction missing
#
# Revision 1.167  2000/10/16 12:18:30  ht
# fix spelling of NotASchema
# allow restriction to a member of a union for xsi:type
# fix empty vs. mixed bug
# add CDATA and token as synonyms of string, for now
#
# Revision 1.166  2000/09/29 14:37:59  ht
# actually put urType in type table under name anyType
#
# Revision 1.165  2000/09/28 15:53:13  ht
# always count errors
#
# Revision 1.164  2000/09/28 10:00:34  ht
# realModel tries to recover from no basetype
#
# Revision 1.163  2000/09/27 17:17:08  richard
# Add handy splitQName function and use in in class QName
#
# Revision 1.162  2000/09/26 14:03:42  richard
# Move checkString methods to applyschema.py, because they may need to look
# at *instance* in-scope namespaces
#
# Revision 1.161  2000/09/25 11:57:51  ht
# can't restrict list with list
#
# Revision 1.160  2000/09/23 11:17:31  ht
# merge in CR branch
#
# Revision 1.159  2000/09/11 17:49:25  ht
# implement chameleon include
#
# Revision 1.158.2.13  2000/09/23 11:06:46  ht
# new NS URI
#
# Revision 1.158.2.12  2000/09/21 09:15:37  ht
# don't collapse as much wrt sequences when extending
#
# Revision 1.158.2.11  2000/09/20 15:18:03  ht
# implement redefine
# back out restricition of union by union
# change concrete syntax of field and selector
#
# Revision 1.158.2.10  2000/09/05 11:31:39  ht
# more urtype name changes
#
# Revision 1.158.2.9  2000/09/04 21:37:35  ht
# accommodate (partly) to new name for simple ur type
#
# Revision 1.158.2.8  2000/09/03 15:59:16  ht
# minimal support for restrictions of lists and unions
#
# Revision 1.158.2.7  2000/09/03 13:47:48  ht
# implement changes to content model of complexContent
#
# Revision 1.158.2.6  2000/08/31 21:26:49  ht
# a few more simpletype change leftovers
#
# Revision 1.158.2.5  2000/08/31 15:29:10  ht
# residual small bugs from simple type changes
#
# Revision 1.158.2.4  2000/08/31 11:47:47  ht
# fix a residual bug in SimpleType, implement List and Union
#
# Revision 1.158.2.3  2000/08/31 09:44:42  ht
# Change SimpleType component to agree with new design
#
# Revision 1.158.2.2  2000/08/30 12:33:42  ht
# convert to new XML repr of simpleType
#
# Revision 1.158.2.1  2000/08/29 21:01:56  ht
# New XML Schema NS for CR
# implement equivClass -> substitutionGroup move
#
# Revision 1.158  2000/08/22 15:47:35  ht
# fix bug ignoring nested attributeGroup references
#
# Revision 1.157  2000/08/22 13:12:46  ht
# treat simple content as emptiable for now
# provide null default for checkMinVals
#
# Revision 1.156  2000/07/25 14:50:39  ht
# don't die if no group in group, if no type for attr
#
# Revision 1.155  2000/07/12 09:33:32  ht
# handle no basetype in mergeContent
# stub for min checking of strings
#
# Revision 1.154  2000/07/10 12:12:00  ht
# recover from no-ref internal group
# handle missing base when checking isSubtype
# check 'content' attribute value
#
# Revision 1.153  2000/07/07 12:57:38  ht
# provide a printable name for Wildcards
#
# Revision 1.152  2000/07/05 14:48:05  ht
# check restriction occurs only if groups match;
# handle one trivial case of emptiable;
# allow textonly from emptiable mixed derivation
#
# Revision 1.151  2000/07/05 09:06:35  ht
# replace complex with simplistic check for circularity, give builtins a targetNamespace
#
# Revision 1.150  2000/07/04 11:04:19  ht
# deal with knock-on effect of group defn fix
#
# Revision 1.149  2000/07/04 10:39:43  ht
# catch LayerError
# check model group defns for exactly one child
# sort FSM edges on output
#
# Revision 1.148  2000/07/03 15:52:19  ht
# at least give better error messages for facets when derivedBy='list'
#xs
# Revision 1.147  2000/07/03 15:02:17  ht
# try once again to simplify/fix include/import/namespace processing
#
# Revision 1.146  2000/07/03 09:40:13  ht
# give float, double, decimal a common superclass to host CheckMinMax
# give error if facet missing value attr
#
# Revision 1.145  2000/06/27 09:40:27  ht
# fix bug (circularity) introduced in previous fix for textOnly stuff
#
# Revision 1.144  2000/06/27 09:09:01  ht
# catch use of complexType as base for simpleType, type of attr
# fix (?) indentation bugs in checking for textOnly in right places
#
# Revision 1.143  2000/06/26 08:22:48  ht
# cover empty <import>
#
# Revision 1.142  2000/06/24 11:17:07  ht
# fix bug in unqualified xsi:type
#
# Revision 1.141  2000/06/24 09:11:26  ht
# Make fixed not == required
#
# Revision 1.140  2000/06/22 12:03:24  ht
# give error if type attr and nested type defn
#
# Revision 1.139  2000/06/20 14:50:16  richard
# wildcard intersection fixes
# any/any determinacy checking
#
# Revision 1.138  2000/06/20 12:47:57  ht
# typo in new allows code for Wildcard
#
# Revision 1.137  2000/06/20 12:41:25  ht
# typo in minInclusive fix
#
# Revision 1.136  2000/06/20 12:05:38  ht
# Fixed bugs in attr type error message, fullName/primitiveType
#   loophole, minInclusive
# Reworked implementation of wildcards and wildcard intersection
#
# Revision 1.135  2000/06/20 08:07:42  ht
# merge xmlout branches back in to main line
#

# Revision 1.134  2000/05/25 07:56:12  ht
# integrate basetype calculation patch from other branch
#
# Revision 1.133  2000/05/14 12:16:28  ht
# change handling of prefix-lookup failure
#
# Revision 1.132  2000/05/14 12:12:16  ht
# Add method to QName to check basic validity
#
# Revision 1.131  2000/05/13 11:44:13  ht
# pop schema stack after fromFile even if it fails
#
# Revision 1.130.2.12  2000/06/16 15:48:56  richard
# Content model determinism checking (not complete: any/any not done)
#
# Revision 1.130.2.11  2000/06/15 16:02:07  ht
# protect against more missing definitions
#
# Revision 1.130.2.10  2000/06/15 09:50:58  ht
# cope with missing attrDecl
#
# Revision 1.130.2.9  2000/05/31 11:30:09  ht
# use convertToNum throughout, raise error if not a numeral
#
# Revision 1.130.2.8  2000/05/29 08:42:51  ht
# register values, not instances
# change enumeration to cope with above change
#
# Revision 1.130.2.7  2000/05/29 08:21:04  ht
# make fsm asXML label readable when wildcard
#
# Revision 1.130.2.6  2000/05/24 20:43:28  ht
# fix handling of complex type with simple model derived from complex
# type (with simple model)
#
# Revision 1.130.2.5  2000/05/24 12:01:28  ht
# handle enumerations a bit
# change import handling
#
# Revision 1.130.2.4  2000/05/16 16:29:03  ht
# manage unnamed top-level components without crashing
# fix bug in handling of undefined *ed element ref in content model
# allow undefined refed elements to match in fsm
#
# Revision 1.130.2.3  2000/05/14 12:30:21  ht
# merge QName checking from main branch,
# fix QName error messages
#
# Revision 1.130.2.2  2000/05/13 12:15:23  ht
# integrate import/null-schema patch from other branch
#
# Revision 1.130.2.1  2000/05/11 14:09:42  ht
# convert error to log an element onto factory.resElt
# convert where to add location attributes to an element
# add asXML method to fsm for applyschema error logging
# name the elt we want from layer.fromFile
#
# Revision 1.133  2000/05/14 12:16:28  ht
# change handling of prefix-lookup failure
#
# Revision 1.132  2000/05/14 12:12:16  ht
# Add method to QName to check basic validity
#
# Revision 1.131  2000/05/13 11:44:13  ht
# pop schema stack after fromFile even if it fails
#
# Revision 1.130  2000/05/11 11:09:49  ht
# protect against missing basetype,
# handle Unicode names in QName
#
# Revision 1.130  2000/05/11 11:09:49  ht
# protect against missing basetype,
# handle Unicode names in QName
#
# Revision 1.130  2000/05/11 11:09:49  ht
# protect against missing basetype,
# handle Unicode names in QName
#
# Revision 1.129  2000/05/09 14:52:52  ht
# Check for strings in a way that works with or without 16-bit support
#
# Revision 1.128  2000/05/09 12:34:23  ht
# shift to using python's built-in url parsing
#
# Revision 1.127  2000/05/05 17:41:39  ht
# handle value and use on attributes
#
# Revision 1.126  2000/04/28 22:19:42  ht
# fix name error in group ref comparison in cm merger
#
# Revision 1.125  2000/04/28 17:37:07  ht
# various missing property initialisations fixed
#
# Revision 1.124  2000/04/28 11:08:08  ht
# oops, remove debugging printout
#
# Revision 1.123  2000/04/28 11:06:55  ht
# fix bug (in spec. too:-( wrt anyAttribute ##other)
#
# Revision 1.122  2000/04/27 16:01:28  ht
# catch and allow more cases of restrictive derivations from the ur-type
#
# Revision 1.121  2000/04/27 09:30:20  ht
# check that inputs are actually schemas,
# remove schema arg to doImport, checkInSchema
#
# Revision 1.120  2000/04/26 17:21:53  ht
# replace stale use of anyElt in urType
#
# Revision 1.119  2000/04/26 16:59:25  ht
# handle undefed ref in note and translate
#
# Revision 1.118  2000/04/26 13:00:40  ht
# add copyright
#
# Revision 1.117  2000/04/24 20:46:40  ht
# cleanup residual bugs with massive rename,
# rename Any to Wildcard,
# replace AnyAttribute with Wildcard,
# get validation of Wildcard working in both element and attribute contexts
#
# Revision 1.116  2000/04/24 15:00:09  ht
# wholesale name changes -- init. caps for all classes,
# schema.py -> XMLSchema.py
#
# Revision 1.115  2000/04/24 13:51:01  ht
# get rid of AnyWrap, separate any and anyElt
# structure sub-classes of any for validation use
#
# Revision 1.114  2000/04/24 12:26:00  ht
# move fsm translation to 'translate' methods on appropriate classes
#
# Revision 1.113  2000/04/24 11:09:50  ht
# add version string
# fix typo in previous equivClass fix :-(
#
# Revision 1.112  2000/04/22 17:31:09  ht
# fixed race condition in type inheritance check
#
# Revision 1.111  2000/04/22 17:10:19  ht
# remove lots of dead wood
#
# Revision 1.110  2000/04/21 14:23:36  ht
# fix missing particle in urtype
#
# Revision 1.109  2000/04/21 09:31:11  ht
# value check on min/max occurs
#
# Revision 1.108  2000/04/21 09:15:00  ht
# removed dump, dumpForDTD and all related methods to separate file
#
# Revision 1.107  2000/04/20 22:12:11  ht
# move resolveURL around a bit to get it right
#
# Revision 1.106  2000/04/20 15:45:08  ht
# better handling of use of ns uri for loc
#
# Revision 1.105  2000/04/20 14:39:44  ht
# merge in private and comp branches
#
# Revision 1.104.2.13  2000/04/20 14:38:19  ht
# merge in comp branch
#

# Revision 1.104.2.12  2000/04/08 11:54:12  ht
# odd attrs patch
#
# Revision 1.104.2.11  2000/04/07 11:38:45  ht
# add modest attempt at resolving relative URLs
#
# Revision 1.104.2.10  2000/04/06 09:52:45  ht
# residual bug in import
#
# Revision 1.104.2.9  2000/04/03 14:51:50  ht
# lots of futzy fixes to effectiveStuff for Last Call schemas
#
# Revision 1.104.2.9.2.15  2000/04/20 14:23:18  ht
# remove special casing of schema creation for no elt
#
# Revision 1.104.2.9.2.14  2000/04/20 12:02:05  ht
# Do imports inline
# Use namespace URI for import if no schemaLoc
# Hack textonly complextypes w/o base
# Fix bug in hard-case for extension construction
# A few defaults to allow DTD-free operation
#
# Revision 1.104.2.9.2.13  2000/04/20 08:45:41  ht
# completed basic conversion to components,
# works for triv, tiny, tiny-nns and self
#
# Revision 1.104.2.9.2.12  2000/04/18 09:33:43  ht
# begin work on content model merger
# fold in url normalisation
# attribute groups improved
#
# Revision 1.104.2.9.2.11  2000/04/16 16:47:02  ht
# working on unifying particles and defrefs
#
# Revision 1.104.2.9.2.10  2000/04/14 21:11:09  ht
# much improved, minimal coverage almost everywhere,
# groups properly handled,
# facets and keys
#
# Revision 1.104.2.9.2.9  2000/04/13 23:03:31  ht
# get builtin facets right, finally
# fix bug in numerical checkString, cover for non-numbers
# propagate particle/term/group structure through translation into FSMs
# get noteElement, noteParticle working again
#
# Revision 1.104.2.9.2.8  2000/04/12 17:29:37  ht
# begin work on model merger,
#
# Revision 1.104.2.9.2.7  2000/04/11 18:13:18  ht
# interpolate attributeUse between complexType and attributeDeclaration,
# parallel to particle
#
# Revision 1.104.2.9.2.6  2000/04/10 17:20:26  ht
# bring built-in datatypes in to line with WD
# add list types
#
# Revision 1.104.2.9.2.5  2000/04/10 15:51:04  ht
# fix facet initialisation of derived simple types
# stub for string validation
# minimal complex type support
#
# Revision 1.104.2.9.2.4  2000/04/09 16:13:27  ht
# working on complex type, attribute;
# back out component.qname
#
# Revision 1.104.2.9.2.3  2000/04/06 09:43:47  ht
# starting on complex type
#
# Revision 1.104.2.9.2.2  2000/04/05 12:11:34  ht
# Getting the basics sorted out:  bare minimum of simple type def and
# element
#
# Revision 1.104.2.9.2.1  2000/04/03 14:55:15  ht
# beginning upheaval
#
# Revision 1.104.2.8  2000/04/01 18:04:15  ht
# lots of hacks to get effective type more involved
#
# Revision 1.104.2.7  2000/03/25 12:10:44  ht
# change to full import, no duplicates;
# redo error handling, use line nums if available
#
# Revision 1.104.2.6  2000/03/21 16:03:47  ht
# allow 208 override,
# some cleanup of complexType attr checking and defaulting
#
# Revision 1.104.2.5  2000/03/20 17:17:35  ht
# top-level attrs,
# implement 208
#
# Revision 1.104.2.4  2000/03/14 18:37:48  ht
# convert to camelCase for builtin datatype names
#
# Revision 1.104.2.3  2000/03/14 18:11:13  ht
# remove abstract elements from equivClass expansion for 'dump'
#
# Revision 1.104.2.2  2000/03/14 11:26:45  ht
# trivial move to new def'n of restriction
#
# Revision 1.104.2.1  2000/03/11 11:14:19  ht
# convert * to unbounded for maxOccurs
#
# Revision 1.104  2000/03/08 15:39:35  ht
# exact->block, tentatively
#
# Revision 1.103  2000/03/08 15:28:46  ht
# merge private branches back into public after 20000225 release
#
# Revision 1.102.2.6  2000/02/24 23:40:33  ht
# fix any bug
#
# Revision 1.102.2.5  2000/02/23 09:12:35  ht
# source->base, restrictions goes away, uri -> uri-reference
#
# Revision 1.102.2.4  2000/02/16 17:52:27  ht
# convert dump to use simple/complexType
# expand equivClass in dumped models (should have a switch)
#
# Revision 1.102.2.3  2000/02/08 21:59:24  ht
# fix minor datatypes initialisation bug
#
# Revision 1.102.1.3  2000/02/08 17:57:56  ht
# fix minor datatypes initialisation bug
#
# Revision 1.102.1.2  2000/02/08 17:32:48  ht
# handle #any --> anyAttribute in dumping
#
# Revision 1.102.1.1  2000/02/08 14:00:19  ht
# fork branck for non-public changes
# lots of changes to accommodate datatype/type -> simple/complexType,
# group -> all, choice, sequence
# builtins in separate namespace
# got rid of subtypes and leaves, not used
#
# Revision 1.102  2000/01/27 13:28:27  ht
# info->documentation; fix bug wrt dump of empty type with attrs
#
# Revision 1.101  2000/01/18 15:56:24  ht
# implement inheritance for attribute restrictions
#
# Revision 1.100  2000/01/18 14:38:58  ht
# fix long-standing serious bug in implementation of restriction for
# complex types
#
# Revision 1.99  2000/01/17 18:02:28  ht
# simple indentation
# key etc. handled
# namespace prefixes are broken for all but schema for schemas
#
# Revision 1.98  2000/01/17 17:22:54  ht
# dump now works, I think, except for keys etc. and indentation
#
# Revision 1.97  2000/01/17 14:01:20  ht
# fix bug ignoring inline types for attributes
# part-way into normalisation 'dump' method on schema
#
# Revision 1.96  2000/01/10 17:36:21  richard
# minor changes for xsi:schemaLocation
#
# Revision 1.95  2000/01/08 23:33:50  ht
# towards support for xsi:schemaLocation
#
# Revision 1.94  2000/01/07 17:09:20  richard
# QNames using python-level NS dictionary
#
# Revision 1.93  2000/01/05 13:00:13  ht
# remove debugging prints
# turn references to element class exemplars into disjunctions
#
# Revision 1.92  2000/01/04 20:47:54  ht
# fix bug in setting up occurs;
# working on dumpToDTD: equivClasses
#
# Revision 1.91  2000/01/04 17:35:38  ht
# Got DumpToDTD working again, minimal support for QNames
#
# Revision 1.90  2000/01/03 14:10:51  ht
# define error to raise a SchemaError exception
#
# Revision 1.89  2000/01/02 19:29:05  ht
# minor initialisations for key processing at schema levels
#
# Revision 1.88  1999/12/27 20:02:29  ht
# better message on import failure
#
# Revision 1.87  1999/12/26 15:43:41  ht
# missing defaults
#
# Revision 1.86  1999/12/22 10:40:56  ht
# init keys etc.
#
# Revision 1.85  1999/12/21 15:02:19  ht
# fix anyattr merge
# groups for key types
#
# Revision 1.84  1999/12/21 13:45:36  richard
# start anyAttribute support
#
# Revision 1.83  1999/12/20 17:51:35  ht
# Make urType more real, use it for ultimate default
# Fix another attr ns bug, uncover incoherence in use of attrGroupRef to
# get ns-qualified attr names in instances!
#
# Revision 1.82  1999/12/20 16:13:07  ht
# correct check of equivClass type derivation from exemplar
#
# Revision 1.81  1999/12/20 15:44:27  ht
# allow for qualified attr names
#
# Revision 1.80  1999/12/20 15:21:50  ht
# fix (?) some attribute type bugs
#
# Revision 1.79  1999/12/20 14:12:45  ht
# stub for anyAttr
#
# Revision 1.78  1999/12/17 17:59:37  ht
# began getting dumpTo/ForDTD back in shape
#
# Revision 1.77  1999/12/17 16:24:02  ht
# more stubs
#
# Revision 1.76  1999/12/17 15:59:44  ht
# stubs for keys etc.
#
# Revision 1.75  1999/12/14 23:10:35  ht
# fixed ns bug in noteElement
#
# Revision 1.74  1999/12/14 18:07:51  richard
# save and restore factory.schema in fromFile
#
# Revision 1.73  1999/12/14 15:06:48  richard
# implement import statements
#
# Revision 1.72  1999/12/14 14:34:45  ht
# pull equivalence class building up front in preparation phase
#
# Revision 1.71  1999/12/14 12:55:51  ht
# Save ALL table & type initialisation stuff until user calls 'prepare',
# to avoid deadly embraces.  Handle abinitio/builtin stuff lazily.
#
# Revision 1.70  1999/12/14 10:42:19  ht
# handle targetNamespace earlier and better
# build stub schema for schemas properly if necessary
#
# Revision 1.69  1999/12/13 20:09:24  richard
# lots of changes for qnames
#
# Revision 1.68  1999/12/13 15:07:59  ht
# attempting to start QName processing
#
# Revision 1.67  1999/12/13 12:29:59  ht
# merged in new-design branch
#
# Revision 1.66.1.5  1999/12/13 10:30:25  ht
# recursive handling of equiv. classes, equiv. classes into fsm
#
# Revision 1.66.1.4  1999/12/12 23:20:38  ht
# slowly working through type derivation
#
# Revision 1.66.1.3  1999/12/11 18:30:14  ht
# more work on new-design
#
# Revision 1.66.1.2  1999/12/10 20:13:11  ht
# working on new-design
#
# Revision 1.66.1.1  1999/12/10 14:47:47  ht
# begun to work on new-design upgrade
#
# Revision 1.66  1999/12/03 11:29:56  ht
# fix info
#
# Revision 1.65  1999/12/01 12:27:13  richard
# fix bug in translating min/max occurs to fsm
# a little more <any> support
#
# Revision 1.64  1999/12/01 11:14:05  ht
# fix typo, add/correct some facets
#
# Revision 1.63  1999/12/01 10:57:03  ht
# handle more classes, rename source to basetype
#
# Revision 1.62  1999/12/01 09:25:43  ht
# add some facets
#
# Revision 1.61  1999/11/30 15:30:02  richard
# handle <any> (not complete yet)
#
# Revision 1.60  1999/11/26 17:22:51  richard
# use realorder not order
#
# Revision 1.59  1999/11/26 13:30:03  richard
# fix case where start node of determinsed fsm is also end node
#
# Revision 1.58  1999/11/26 12:32:47  ht
# resurrect dumpTo/ForDTD, preliminary
#
# Revision 1.57  1999/11/26 10:01:29  aqw
# change handling of default attribute values -- make them class vars
# which get shadowed by assignments to instances
#
# Revision 1.56  1999/11/25 21:31:52  ht
# fix basetype window bug
#
# Revision 1.55  1999/11/25 17:14:19  aqw
# minor bugfixes, make rootName a self-computing property
#
# Revision 1.54  1999/11/25 15:57:44  richard
# minor bugs
#
# Revision 1.53  1999/11/25 15:28:45  aqw
# fix some bugs in interaction between complex types and new approach to
# simple types
# give textonly complex types a coreType to carry their datatype part
#
# Revision 1.52  1999/11/25 13:13:46  ht
# merge in branch which switched to separate classes for ab initio types
#
# Revision 1.48.1.2  1999/11/25 10:21:24  aqw
# convert to classes for primitive types, use them as effectiveType for
# all simpleTypes
#
# Revision 1.48.1.1  1999/11/22 16:03:10  aqw
# classes for ab initio types
#

