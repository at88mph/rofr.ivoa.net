# Copyright (C) 2000 LTG -- See accompanying COPYRIGHT and COPYING files
# actually apply a schema to an instance
# $Id: applyschema.py,v 1.98 2001/06/16 11:56:53 ht Exp $

from PyLTXML import *
import XML
import XMLInfoset
import LTXMLInfoset
import PSVInfoset
import os
import XMLSchema
import layer
import sys
import re
import types
import string
from urlparse import urljoin
import tempfile
import traceback
import asInfoset
import time

whitespace = re.compile("^[ \t\r\n]*$")
xsi = XMLSchema.XMLSchemaInstanceNS
vsraw="$Revision: 1.98 $ of $Date: 2001/06/16 11:56:53 $"
vss=string.split(vsraw)
vs="XSV %s/%s of %s %s"%(string.split(XMLSchema.versionString)[0],
                         vss[1],vss[5],vss[6])
dontWarn=1

def readXML(url):
  if url:
    doc = LTXMLInfoset.documentFromURI(url)
  else:
    file = FOpen(sys.stdin,
                 NSL_read+NSL_read_all_bits+NSL_read_namespaces+
                 NSL_read_no_consume_prolog)
    doc = LTXMLInfoset.documentFromFile(file)
    Close(file)
  return doc

def validate(element, typedef, schema, eltDecl):
  if not hasattr(schema.factory,'errors'):
    schema.factory.errors=0
  validateElement(element, typedef, schema, eltDecl)
  return schema.factory.errors

def validateElement(element, type, schema, eltDecl=None):
  global vel, vtype
  vel = element
  vtype = type
  if not eltDecl:
    eqn=XMLSchema.QName(None,element.localName,element.namespaceName or None)
    if s.vElementTable.has_key(eqn):
      eltDecl=s.vElementTable[eqn]
      if eltDecl:
        type=eltDecl.typeDefinition
  validateXSIAttrs(element,schema)
  nullable = eltDecl and eltDecl.nullable # TODO: is this right if no eltDecl?
  nulled = 0
  if element.attributes.has_key((xsi, "nil")):
    if not nullable:
      verror(element,
             "xsi:nil specified on non-nillable element %s" % element.originalName,
             schema,"cvc-elt.1.1")
      element.assess(schema.factory,eltDecl)
      return
    nulla=element.attributes[(xsi,"nil")]
    nulled = (nulla.validity=='valid' and
              nulla.schemaNormalizedValue == "true")
  if element.attributes.has_key((xsi, "type")):
    typea=element.attributes[(xsi, "type")]
    if typea.validity=='valid':
      t = typea.schemaNormalizedValue;
      (tp,tl) = XMLSchema.splitQName(t)
      if element.inScopeNamespaces.has_key(tp):
        qt = XMLSchema.QName(tp, tl, element.inScopeNamespaces[tp].namespaceName)
      elif tp:
        verror (element,
                "no inscope namespace declaration for prefix of xsi:type %s"%t,
                schema,"src-qname")
      else:
        qt= XMLSchema.QName(tp,tl,None)
      if schema.vTypeTable.has_key(qt):
        xsitype=schema.vTypeTable[qt]
      else:
        verror(element,"xsi:type %s undefined" % qt,schema,"cvc-elt.2.2")
        element.assess(schema.factory,eltDecl)
        return
      if type and not xsitype.isSubtype(type):
        verror(element,
           "xsi:type %s is not a subtype of the declared type %s"%(qt,
                                                                   type.name),
               schema,"cvc-elt.2.3")
        element.assess(schema.factory,eltDecl)
        return
      if type:
        vwarn(element,
              "using xsi:type %s instead of original %s" % (qt, type.name))
      else:
        vwarn(element,"using xsi:type %s" % qt)
      type = xsitype
  element.assessedType = type
  lax = not type
  # might have none in case of recursive call inside <any/>, or at top level
  if nulled:
    validateElementNull(element, type, schema)
  if type:
    # TODO: check element is not abstract
    if ((not type==XMLSchema.urType) and
        (isinstance(type, XMLSchema.AbInitio) or
         isinstance(type, XMLSchema.SimpleType))):
      if not nulled:
        validateElementSimple(element, type, schema, eltDecl)
      if eltDecl:
        validateKeys(eltDecl,element)
      element.assess(schema.factory,eltDecl)
      return
    # a complexType
    if type.abstract=='true':
      verror(element,"attempt to use abstract type %s to validate"%type.name,
             schema,'cvc-complex-type.1')
      element.assess(schema.factory,eltDecl)
      return
    ad=type.attributeDeclarations
    ps=type.prohibitedSubstitutions
    et=type.elementTable
  else:
    ps=[]
    ad={}
    et={}
  assignAttributeTypes(element, ad, ps, schema, lax)
  validateAttributeTypes(element, element.attrTable, ad, schema)
  #  print "assigning types for %s" % element.originalName
  if not nulled:
    assignChildTypes(element.chunkedChildren, et, ps, schema, lax)
    # we must look at the content model before checking the types, so that
    # we know which children matched <any>
    if type:
      validateContentModel(element, type, schema, eltDecl)
    validateChildTypes(element.chunkedChildren, schema, lax)
  if eltDecl:
    validateKeys(eltDecl,element)
  element.assess(schema.factory,eltDecl)
  
def validateElementNull(element, type, schema):
  if len(element.chunkedChildren) != 0:
    verror(element,"element %s is nilled but is not empty" % element.originalName,
           schema,"cvc-elt.1.2.1")
  else:
    element.null=1
  # TODO: should check for fixed value constraint

def validateElementSimple(element, type, schema, declaration):
  # check that:
  #   it has no attributes (except xsi: ones)
  #   it has one pcdata child, and if so
  #     the text of the pcdata matches the type
  if element.attributes:
    for a in element.attributes.values():
      if a.namespaceName != xsi:
        verror(element,
               "element {%s}%s with simple type not allowed attributes"%
               (element.namespaceName, element.localName),
               schema,"cvc-elt.4.1.1")
        return
  return validateTextModel(element, type, schema, declaration)

def validateXSIAttrs(element,schema):
  for a in element.attributes.values():
    if a.namespaceName == xsi:
      if a.localName not in ('type','nil','schemaLocation','noNamespaceSchemaLocation'):
        verror(element,"unknown xsi attribute %s" % a.localName,schema,
               "cvc-complex-type.1.3")
        a.type=None
      else:
        a.type=schema.factory.sforsi.attributeTable[a.localName]
        res=a.type.typeDefinition.validateText(a.normalizedValue,a,
                                               element,schema)
        a.assessedType = a.type.typeDefinition
        if res:
          verror(element,
                 "attribute type check failed for %s: %s%s"%(a.localName,
                                                             a.normalizedValue,
                                                             res),
                 schema,'cvc-attribute.1.2',0,None,a)
        else:
          a.schemaNormalizedValue=a.normalizedValue
      a.assess(schema.factory,a.type)

def assignAttributeTypes(element, attrdefs, extendable, schema, lax):
  # look up each attribute in attrdefs and assign its type
  # error if attr declaration is not found and type is not extendable
#  print "assigning attrs for %s {%s}%s" % (element.originalName, element.namespaceName, element.localName)
#  print "declared attrs are:"
#  for zz in attrdefs.keys():
#    if isinstance(zz, XMLSchema.QName):
#      print "{%s}%s " % (zz.uri, zz.local)
#    else:
#      print zz
  element.attrTable={}
  for a in element.attributes.values():
#    print "assigning attr %s {%s}%s,%s,%s" % (a.originalName, a.namespaceName, a.localName,lax,attrdefs.has_key("#any"))
    an=XMLSchema.QName(None,a.localName,a.namespaceName or None)
    element.attrTable[an]=a
    if a.namespaceName == xsi:
      continue
    elif attrdefs.has_key(an):
      a.type = attrdefs[an]
    elif lax:
      if a.namespaceName and schema.vAttributeTable.has_key(an):
        a.type=schema.vAttributeTable[an]
      else:
        a.type=None
    elif (attrdefs.has_key("#any") and
          attrdefs["#any"].attributeDeclaration.allows(a.namespaceName or None)):
      a.type = attrdefs["#any"].attributeDeclaration
    else:
      verror(element,"undeclared attribute %s" % an,schema,
               "cvc-complex-type.1.3")
      a.type = None
  return

def validateAttributeTypes(element,attrs, attrdefs, schema):
  # check that each attribute matches its type
  # check that all required attributes are present
  # TODO: add defaulted/fixed attributes (shouldn't need to check their types)
  for (adq,ad) in attrdefs.items():
    if not attrs.has_key(adq):
      if ad.minOccurs==1:
        verror(element,"required attribute %s not present"%adq,schema,
               'cvc-complex-type.1.4')
      vc=ad.valueConstraint
      if not vc and isinstance(ad.attributeDeclaration,XMLSchema.Attribute):
        vc=ad.attributeDeclaration.valueConstraint
      if vc:
        na=XMLInfoset.Attribute(element,adq.uri,adq.local,None,
                                        vc[1],
                                        0)
        element.addAttribute(na)
  for (an,a) in attrs.items():
    if an.uri==xsi:
      # handled already
      continue
    elif a.type:
      if isinstance(a.type,XMLSchema.AttributeUse):
        ad=a.type.attributeDeclaration
        td=ad.typeDefinition
        vc=a.type.valueConstraint or ad.valueConstraint
      else:
        ad=a.type
        if not isinstance(ad,XMLSchema.Wildcard):
          td=ad.typeDefinition
          vc=ad.valueConstraint
      if isinstance(ad,XMLSchema.Wildcard):
        res=ad.validate(a,schema,'attribute',element)
      else:
        if td:
          res=td.validateText(a.normalizedValue,a,element, schema)
          a.assessedType = td
          if vc and vc[0]=='fixed':
            if a.normalizedValue!=vc[1]:
              verror(element,"fixed value did not match for attribute %s: %s!=%s"%(an,a.normalizedValue,vc[1]),schema,"cvc-attribute.1.3")
        else:
          res=None
      if res:
        verror(element,"attribute type check failed for %s: %s%s"%(an,
                                                                   a.normalizedValue,
                                                                   res),
               schema,'cvc-attribute.1.2',0,None,a)
        a.schemaNormalizedValue=None
    else:
      ad=None
    a.assess(schema.factory,ad)

def assignChildTypes(children, elementTable, extendable, schema, lax):
  # look up each child tag and record the type
  # (it may not be an error if it is not declared; we don't know that
  #  until we see what it matches in the content model)
  # TODO: extendable
  for child in children:
    if isinstance(child,XMLInfoset.Element):
      qname = XMLSchema.QName(None,child.localName,child.namespaceName or None)
      if elementTable.has_key(qname):
        decl=elementTable[qname]
        child.type = decl.typeDefinition
        child.eltDecl = decl
      elif lax and child.namespaceName and schema.vElementTable.has_key(qname):
        decl=schema.vElementTable[qname]
        child.type=decl.typeDefinition
        child.eltDecl=decl
      else:
	child.type = None
        child.eltDecl=None
  return 1

def validateContentModel(element, type, schema, declaration):
  # trace a path through the content model
  # if a child matches an <any tag=... type=...> we need to indicate
  # that that child should be validated with its xsd:type if it has one
  # if a child matches some other kind of <any> we need to indicate
  # that it's not an error if we can't find its type

#  print "validating model for %s content type %s" % (element.originalName, type.contentType)
  if type.contentType == "empty":
    validateEmptyModel(element, type, schema)
  elif type.contentType == "textOnly":
    validateTextModel(element, type.model, schema, declaration)
  else:
    # todo: default/fixed for "mixed" content
    validateElementModel(element, type.fsm,
                         type.contentType == "mixed", schema)

def validateEmptyModel(element, type, schema):
  if len(element.chunkedChildren) != 0:
    verror(element,"element %s must be empty but is not" % element.originalName,schema,
           "cvc-complex-type.1.2")

def validateTextModel(element, type, schema,declaration=None):
  # check that:
  #   it has one pcdata child, and if so
  #     the text of the pcdata matches the type
  name = element.localName
  text=None
  for child in element.chunkedChildren:
    if isinstance(child,XMLInfoset.Characters):
      if not text:
        text=child.characters
      else:
        text=text+child.characters
    elif isinstance(child,XMLInfoset.Element):
      verror(element,
             "element {%s}%s with simple type not allowed element children"%
             (element.namespaceName,name),schema,"cvc-complex-type.1.2.2")
      # TODO: mark this (and any others) as not validated
      return
  else:
    if declaration:
      vc=declaration.valueConstraint
    if not text:
      if vc:
        text=vc[1]
      else:
        text=""
    res=type.validateText(text, element, element, schema)
    # todo: normalize the default text once
    if res:
      verror(element,"element content failed type check: %s%s"%(text,res),
             schema,"cvc-complex-type.1.2.2")
      element.schemaNormalizedValue=None
    elif (vc and vc[0]=='fixed' and element.schemaNormalizedValue!=vc[1]):
      verror(element,"fixed value did not match: %s!=%s"%(element.schemaNormalizedValue,vc[1]),schema,"cvc-element.5.2.2.2")
      element.schemaNormalizedValue=None
      
def validateElementModel(element, fsm, mixed, schema):
  #  print "validating element model for %s" % element.originalName
  n = fsm.startNode
  for c in element.chunkedChildren:
    if isinstance(c,XMLInfoset.Characters):
      if (not mixed) and (not whitespace.match(c.characters)):
	verror(element,
               "text not allowed: |%s|" % c.characters,
               schema,"cvc-complex-type.1.2.3")
	return
    elif isinstance(c,XMLInfoset.Element):
      qname = XMLSchema.QName(None, c.localName, c.namespaceName or None)
      next = None
      anynext = None
      for e in n.edges:
        if e.label == qname:
	  next = e.dest
          c.strict = 1
	  break
        if isinstance(e.label, XMLSchema.Wildcard):
          if e.label.allows(c.namespaceName or None):
            anynext = e.dest
            anylab = e.label
      if not next:
        if anynext:
          n = anynext
          c.strict = (anylab.processContents == 'strict')
# this is no longer an error, but something more complicated is XXX
#          if c.type:
#            where(child.where)
#            print "element matched <any> but had a type assigned"
#            v = 0
#          else:
#            c.type = "<any>"
          c.type = anylab
        else:
          allowed=[]
          for e in n.edges:
            if isinstance(e.label, XMLSchema.QName):
              allowed.append(str(e.label))
            elif isinstance(e.label, XMLSchema.Wildcard):
              allowed.append("{%s}:*"%str(e.label.allowed))
          fx=fsm.asXML()
          verror(c,
                 "element %s not allowed here (%s) in element %s, expecting %s:\n"%
                 (qname, n.id,
                  XMLSchema.QName(None,element.localName,element.namespaceName or None),
                  allowed),
                 schema,"cvc-complex-type.1.2.4",0,fx)
      else:
        n = next
  if not n.isEndNode:
    allowed=[]
    for e in n.edges:
      if isinstance(e.label, XMLSchema.QName):
        allowed.append(str(e.label))
      elif isinstance(e.label, XMLSchema.Wildcard):
        allowed.append(e.label.allowed)
    fx=fsm.asXML()
    verror(element,
           "content of %s is not allowed to end here (%s), expecting %s:\n"%
           (element.originalName,n.id,allowed),
           schema,"cvc-complex-type.1.2.4",1,fx)
  return

def validateChildTypes(children, schema, lax):
  # validate each child element against its type, if we know it
  # report an error if we don't know it and it's not in <any>
  v = 1
  for child in children:
    if isinstance(child,XMLInfoset.Element):
      if child.type:
        if child.eltDecl:
          validateElement(child,child.type,schema,child.eltDecl)
        else:
          # child.type is actually a wildcard
          child.type.validate(child,schema,'element',child)
      elif lax:
        # TODO: check that this branch ever happens at all
        # TODO: record impact of missing type in PSVI
        validateElement(child,None,schema) # will be lax because no type
      else:
	verror(child,
               "undeclared element %s"%
               XMLSchema.QName(None,child.localName,child.namespaceName or None),
               schema,"src-resolve")

def validateKeys(decl,elt):
  elt.keyTabs={}
  validateKeys1(elt,decl.keys,1)
  validateKeys1(elt,decl.uniques,0)
  validateKeyRefs(elt,decl.keyrefs)

def validateKeys1(elt,kds,reqd):
  # TODO: propagate upwards
  for key in kds:
    tab={}
    candidates=key.selector.find(elt)
    if candidates:
      for s in candidates:
        keyKey=buildKey(s,key.fields,key.schema)
        if keyKey:
          if len(keyKey)>1:
            keyKey=tuple(keyKey)
          else:
            keyKey=keyKey[0]
        else:
          if reqd:
            verror(s,
                   "missing one or more fields %s from key %s"%(key.fields,
                                                                key.name),
                   key.schema,"cvc-identity-constraint.2.2.2")
          continue
	if tab.has_key(keyKey):
          if reqd:
            code="cvc-identity-constraint.2.2.3"
          else:
            code="cvc-identity-constraint.2.1.2"
	  verror(s,"duplicate key %s, first appearance was %s"%
                 (str(keyKey),
                  XMLSchema.whereString(tab[keyKey].where)),
                 key.schema,code)
	else:
	  tab[keyKey]=s
    elt.keyTabs[key.name]=tab

def buildKey(s,fps,schema):
  keyKey=[]
  for fp in fps:
    kv=fp.find(s)
    if kv:
      if len(kv)>1:
        verror(s,"Field XPath %s produced multiple hits"%fp.str,
               schema,
               "cvc-identity-constraint.3")
      if isinstance(kv[0],XMLInfoset.Element):
        if (len(kv[0].chunkedChildren)>0 and
            isinstance(kv[0].chunkedChildren[0],XMLInfoset.Characters)):
          keyKey.append(kv[0].chunkedChildren[0].characters)
        else:
          # XPath says in this case value is the empty string
          pass
      elif XML.somestring(type(kv[0])):
        keyKey.append(kv[0])
      else:
        # TODO error or shouldnt?
        vwarn(s,"oops, key value %s:%s"%(type(kv[0]),kv[0]))
    else:
      return
  return keyKey

def validateKeyRefs(elt,krds):
  res=1
  for ref in krds:
    if elt.keyTabs.has_key(ref.refer):
      keyTab=elt.keyTabs[ref.refer]
      if keyTab=='bogus':
	break
    else:
      elt.keyTabs[ref.refer]='bogus'
      verror(elt,
             "No key or unique constraint named %s declared, refed by keyref %s"%(ref.refer,ref.name),
             ref.schema,"cvc-identity-constraint.2.3.2")
      break
    candidates=ref.selector.find(elt)
    if candidates:
      for s in candidates:
        keyKey=buildKey(s,ref.fields,ref.schema)
        if not keyKey:
          continue
	if len(keyKey)>1:
	  keyKey=tuple(keyKey)
	else:
	  keyKey=keyKey[0]
	if not keyTab.has_key(keyKey):
	  verror(s,"no key in %s for %s"%(ref.refer,str(keyKey)),ref.schema,
                 "cvc-identity-constraint.2.3.2")

def findSchemaLocs(element,schema):
  pairs = []
  for a in element.attributes.values():
    if a.namespaceName == xsi:
      if a.localName == "schemaLocation":
        scls=string.split(a.normalizedValue)
        while scls:
          if len(scls)>1:
            pairs.append((scls[0], scls[1]))
          else:
            verror(element,"xsi:schemaLocation must be a list with an even number of members: %s"%string.split(a.normalizedValue),schema,"???")
          scls=scls[2:]
      elif a.localName == "noNamespaceSchemaLocation":
        pairs.append((None,a.normalizedValue))
  for c in element.chunkedChildren:
    if isinstance(c, XMLInfoset.Element):
      scl=findSchemaLocs(c,schema)
      if scl:
        pairs = pairs + scl
  return pairs
  
def runitAndShow(en,rns=[],k=0,style=None,enInfo=None,outfile=None,dw=1,
                 timing=0,reflect=0,independent=0,reflect2=0):
  global dontWarn
  dontWarn=dw
  if timing:
    timing=time.time()
  (res,encoding,errs)=runit(en,rns,k,timing,independent,reflect2)
  if timing:
    sys.stderr.write("Finished:         %6.2f\n"%(time.time()-timing))
  if not encoding:
    encoding='UTF-8'
  if outfile:
    try:
      outf=open(outfile,"w")
    except:
      sys.stderr.write("couldn't open %s for output, falling back to stderr"%
                       outfile)
      outf=sys.stderr
  else:
    outf=sys.stderr
  errout=OpenStream(outf,
                    CharacterEncodingNames[encoding],
                    NSL_write+NSL_write_plain)
  if encoding!='UTF-8':
    es=" encoding='%s'"%encoding
  else:
    es=""
  PrintTextLiteral(errout,"<?xml version='1.0'%s?>\n"%es)
  if style:
    PrintTextLiteral(errout,
                     "<?xml-stylesheet type='text/xsl' href='%s'?>\n"%style)
  if enInfo:
    for (k,v) in enInfo.items():
      res.addAttr(k,v)
  if errs:
    res.addAttr("crash","true")
  res.printme(errout)
  PrintTextLiteral(errout,"\n")
  Close(errout)
  if reflect and not errs:
    dumpInfoset(sys.stdout)
  if reflect2 and not errs:
    dumpInfoset(refprefix + "-after")
  if errs:
    return string.join(map(lambda es:string.join(es,''),errs),'')
  else:
    return

class SchemaValidationError(Exception):
  def __init__(self,arg):
    Exception.__init__(self,arg)

def runit(en,rns=[],k=0,timing=0,independent=0,reflect2=0):
  global s,e,t,f,res,ed,btlist
  if independent:
    rns[0:0]=[en]
  btlist=[]

  ss = s = None

  f=XMLSchema.newFactory()
  f.errors=0
  base=f.fileNames[0]
  if en:
    ren=urljoin(base,en)
  else:
    ren=None

  res=XML.Element("xsv")
  f.resElt=res
  res.addAttr("xmlns","http://www.w3.org/2000/05/xsv")
  res.addAttr("version",vs)
  if independent:
    res.addAttr("target","[standalone schema assessment]")
  else:
    res.addAttr("target",ren or "[stdin]")
    
  if rns:
    res.addAttr("schemaDocs",string.join(rns,' '))

  rdn=tempfile.mktemp("xsverrs")
  redirect=open(rdn,"w+")
  savedstderr=os.dup(2)                        # save stderr
  os.dup2(redirect.fileno(),2)
  if independent:
    e=None
    encoding="UTF-8"
  else:
    try:
      doc=readXML(ren)
      e=doc.documentElement
      f.docElt=e
      encoding=doc.characterEncodingScheme
      if timing:
        os.write(savedstderr,"target read:      %6.2f\n"%(time.time()-timing))
      if reflect2:
        dumpInfoset(refprefix + "-before")
    except:
      pfe=XML.Element("bug")
      pfe.children=[XML.Pcdata("validator crash during target reading")]
      res.children.append(pfe)
      e=None
      encoding=None
    if not e:
      res.addAttr('instanceAssessed',"false")
      sys.stderr.flush()
      registerRawErrors(redirect,res)
      # put stderr back
      os.dup2(savedstderr,2)
      btlist.append(traceback.format_exception(sys.exc_type,
                                               sys.exc_value,
                                               sys.exc_traceback))
      return (res,None,btlist)

  # TODO: check each schema doc against schema for schemas, if possible,
  # unless caller explicitly opts out (?)
  if rns:
    try:
      sf=schemaFile(rns[0],base,res)
      if sf:
        s = XMLSchema.fromFile(sf,f)
        if timing:
          os.write(savedstderr,"schema read:      %6.2f\n"%(time.time()-timing))
    except:
      pfe=XML.Element("bug")
      pfe.children=[XML.Pcdata("validator crash during schema reading")]
      res.children.append(pfe)
      btlist.append(traceback.format_exception(sys.exc_type,
                                               sys.exc_value,
                                               sys.exc_traceback))
    for rn in rns[1:]:
      try:
        sf=schemaFile(rn,base,res)
        if sf:
          ffr=XMLSchema.fromFile(sf,f)
          if timing:
            os.write(savedstderr,"schema read:      %6.2f\n"%(time.time()-timing))
          ss=ss or ffr
      except:
        pfe=XML.Element("bug")
        pfe.children=[XML.Pcdata("validator crash during schema reading")]
        res.children.append(pfe)
        btlist.append(traceback.format_exception(sys.exc_type,
                                                 sys.exc_value,
                                                 sys.exc_traceback))

  if not s:
    if ss:
      s=ss
    else:
      s = XMLSchema.Schema(f,None)
      s.targetNS='##dummy'

  if not independent:
    schemaLocs = findSchemaLocs(e,s)
    res.addAttr('schemaLocs',string.join(map(lambda p:"%s -> %s"%(p[0] or 'None',p[1]),
                                      schemaLocs),
                                  '; '))
    for (ns, sl) in schemaLocs:
      try:
        sf=schemaFile(sl,ren or "[stdin]",res)
        if sf:
          XMLSchema.checkinSchema(f, ns, sf,e,None)
          if timing:
            os.write(savedstderr,"schema read:      %6.2f\n"%(time.time()-timing))
      except:
        pfe=XML.Element("bug")
        pfe.children=[XML.Pcdata("validator crash during schema reading")]
        res.children.append(pfe)
        btlist.append(traceback.format_exception(sys.exc_type,
                                                 sys.exc_value,
                                                 sys.exc_traceback))

    res.addAttr('docElt',"{%s}%s"%(e.namespaceName,e.localName))
    if (e.namespaceName and
        (e.namespaceName not in ('http://www.w3.org/XML/1998/namespace',xsi)) and
        not f.schemas.has_key(e.namespaceName)):
      try:
        sf=schemaFile(e.namespaceName,ren or "[stdin]",res)
        if sf:
          XMLSchema.checkinSchema(f,e.namespaceName,sf,e,None)
          if timing:
            os.write(savedstderr,
                     "schema read:      %6.2f\n"%(time.time()-timing))
          res.addAttr('nsURIDeref','success')
        else:
          res.addAttr('nsURIDeref','failure')
      except:
        pfe=XML.Element("bug")
        pfe.children=[XML.Pcdata("validator crash during schema reading")]
        res.children.append(pfe)
        btlist.append(traceback.format_exception(sys.exc_type,
                                                 sys.exc_value,
                                                 sys.exc_traceback))
        res.addAttr('nsURIDeref','failure')
    
  sys.stderr.flush()
  registerRawErrors(redirect,res)
  # put stderr back
  os.dup2(savedstderr,2)

  try:
    ecount=XMLSchema.prepare(f)
    if timing:
      sys.stderr.write("factory prepared: %6.2f\n"%(time.time()-timing))
    if independent:
      for ss in f.schemas.values():
        ss.prepare()
      if timing:
        sys.stderr.write("schemas prepared: %6.2f\n"%(time.time()-timing))
  except:
    ecount=-1
    btlist.append(traceback.format_exception(sys.exc_type,
                                             sys.exc_value,
                                             sys.exc_traceback))
    pfe=XML.Element("bug")
    pfe.children=[XML.Pcdata("validator crash during factory preparation")]
    res.children.append(pfe)

  if independent:
    kgm="false"
    kg=0
  else:
    kgm="true"
    kg=1
  if ecount:
    if ecount<0:
      kg=0
    else:
      if not k:
        kg=0
    if not kg:
     kgm="false"
  res.addAttr('instanceAssessed',kgm)
  if not kg:
    if independent or ecount<0:
      if ecount<0:
        ecount=0
      for sch in f.schemas.values():
        ecount=ecount+sch.errors
    res.addAttr('schemaErrors',str(ecount))
    return (res,encoding,btlist)

  cl=string.find(':',e.originalName)
  if cl>-1:
    prefix=e.originalName[0:cl]
  else:
    prefix=None
  eltname = XMLSchema.QName(prefix,e.localName,e.namespaceName or None)

  if not s:
    # any one will do
    s = f.sfors
  t=None
  
  ed=None
  if s and s.vElementTable.has_key(eltname):
    ed=s.vElementTable[eltname]
    t=ed.typeDefinition
  if t:
    if t.name:
      if hasattr(t,'qname'):
        tn=t.qname.string()
      else:
        tn=t.name
    else:
      tn='[Anonymous]'
    res.addAttr('rootType',tn)
    res.addAttr('validation','strict')
  else:
    res.addAttr('validation','lax')

  if e and s:
    try:
      validate(e, t, s, ed)
      sforsi=PSVInfoset.NamespaceSchemaInformation(f.schemas[XMLSchema.XMLSchemaNS])
      sforsi.schemaComponents.sort(PSVInfoset.compareSFSComps) # ensure atomics
                                                         # are first
      e.schemaInformation=[sforsi,
                           PSVInfoset.NamespaceSchemaInformation(f.schemas[xsi])]
      for s in f.schemas.values():
        if s.targetNS not in (XMLSchema.XMLSchemaNS,xsi):
          e.schemaInformation.append(PSVInfoset.NamespaceSchemaInformation(s))
    except:
      btlist.append(traceback.format_exception(sys.exc_type,
                                               sys.exc_value,
                                               sys.exc_traceback))
      pfe=XML.Element("bug")
      pfe.children=[XML.Pcdata("validator crash during validation")]
      res.children.append(pfe)
    res.addAttr('instanceErrors',str(s.factory.errors))
    ec=0
    for sch in f.schemas.values():
      ec=ec+sch.errors
    res.addAttr('schemaErrors',str(ec))
    return (res,encoding,btlist)

def schemaFile(fn,base,res):
  # sniff the file to see what it's like
  full=urljoin(base,fn)
  em=None
  while full:
    ffull=full
    try:
      f=Open(full,NSL_read|NSL_read_namespaces)
    except error:
      full=None
      em="couldn't open"
      break
    b=GetNextBit(f)
    while (b and b.type not in ('start','empty')):
      if b.type=='bad':
        b=None
        break
      b=GetNextBit(f)
    if not b:
      sys.stderr.write("%s has no elements???\n"%full)
      raise error # PyLTXML.error
    nsdict=b.item.nsdict
    if b.item.llabel=="html" and b.item.nsuri=="http://www.w3.org/1999/xhtml":
      # may be RDDL file
      rddl=0
      for (key,val) in nsdict.items():
        if val=="http://www.rddl.org/":
          rddl=1
          break
      if rddl:
        ne=XML.Element("importAttempt")
        ne.addAttr('URI',full)
        ne.addAttr('outcome','RDDL')
        res.children.append(ne)
        b=GetNextBit(f)
        while b:
          if ((b.type=='start' or b.type=='empty') and
              b.item.llabel=="resource" and
              b.item.nsuri=="http://www.rddl.org/"):
            # this is somewhat tedious
            for (p,n) in b.item.nsdict.items():
              if n=="http://www.w3.org/1999/xlink":
                break
            else:
              p=""                    # hack to fail
            if GetAttrVal(b.item,"%s:role"%p)==XMLSchema.XMLSchemaNS:
              newf=GetAttrVal(b.item,"%s:href"%p)
              if newf:
                full=urljoin(full,newf)
              else:
                em="RDDL resource for W3C XML Schema lacked an xlink:href"
                full=None
              break
          elif b.type=='bad':
            Close(f)
            raise error
          b=GetNextBit(f)
        else:
          full=None
          em="Recognised as RDDL, but no W3C XML Schema resource found"
    elif b.item.llabel=="schema":
      if b.item.nsuri==XMLSchema.XMLSchemaNS:
        Close(f)
        return full
      em="Root was <schema>, but not in W3C XML Schema namespace: %s (was %s)"%(XMLSchema.XMLSchemaNS,b.item.nsuri)
      full=None
    else:
      em="Not recognised as W3C XML Schema or RDDL: {%s}:%s"(b.item.nsuri,
                                                             b.item.llabel)
      full=None
  try:
    Close(f)
  except:
    pass
  ne=XML.Element("notASchema")
  ne.addAttr('filename',ffull)
  if em:
    ne.children=[XML.Pcdata(em)]
  res.children.append(ne)

def registerRawErrors(redirect,res):
  if redirect.tell(): 
    redirect.seek(0)
    ro=XML.Element("XMLMessages")
    o="\n%s"%redirect.read()
    ro.children=[XML.Pcdata(o)]
    res.children.append(ro)
  redirect.close()

def verror(elt,message,schema,code=None,two=0,daughter=None,iitem=None):
  # code argument identifies CVC
  ve=XML.Element("invalid")
  ve.children=[XML.Pcdata(message)]
  if code:
    ve.addAttr("code",code)
  if two:
    XMLSchema.where(ve,elt.where2)
  else:
    XMLSchema.where(ve,elt.where)
  if daughter:
    ve.children.append(daughter)
  res.children.append(ve)
  schema.factory.errors=schema.factory.errors+1
  if not iitem:
    iitem=elt
  if iitem.errorCode:
    iitem.errorCode.append(" "+code)
  else:
    iitem.errorCode=[code]

def vwarn(elt,message):
  if dontWarn:
    return
  ve=XML.Element("warning")
  ve.children=[XML.Pcdata(message)]
  if elt:
    XMLSchema.where(ve,elt.where)
  res.children.append(ve)

# validation methods for schema components

def av(self,child,schema,kind,elt):
  q = XMLSchema.QName(None,child.localName,child.namespaceName or None)
  vwarn(elt,"allowing %s because it matched wildcard(%s)" %
        (q,self.allowed))
  if self.processContents!='skip':
#   print "looking for decl for %s" % child.originalName
    if schema.factory.schemas.has_key(child.namespaceName):
      # only try if we might win -- needs work
      try:
        if kind=='element':
          e = schema.vElementTable[q]
        else:
          e = schema.vAttributeTable[q]
      except KeyError:
        e=None
#     print "decl for %s is %s" % (child.originalName, e)
      if e and e.typeDefinition:
        vwarn(None,"validating it against %s" %
              (e.typeDefinition.name or 'anonymous type'))
        if kind=='element':
          validateElement(child, e.typeDefinition, schema)
        else:
          child.assessedType = e.typeDefinition
          res=e.typeDefinition.validateText(child.normalizedValue,child,
                                            elt, schema)
          if res:
            verror(elt,
                   "attribute type check failed for %s: %s%s"%(q,
                                                               child.normalizedValue,
                                                               res),
                   schema,'cvc-attribute.1.2',0,None,child)
            child.schemaNormalizedValue=None
      elif (self.processContents=='strict' and
            not (kind=='element' and child.attributes.has_key((xsi, "type")))):
        # TODO check this against actual def'n of missing component
        verror(elt,
               "can't find a type for wildcard-matching %s %s" %(kind, q),
               schema,
               "src-resolve")
      elif kind=='element':
        vwarn(None,"validating it laxly")
        validateElement(child,None,schema)
  
XMLSchema.Wildcard.validate=av

def tv(self,child,schema,kind,elt):
  validateElement(child, self, schema)

XMLSchema.Type.validate=XMLSchema.AbInitio.validate=tv

def validateText(self, text, item, context, schema):
  if self==XMLSchema.urType:
    return
  else:
    if self.variety=='atomic':
      # ref may have failed
      if self.primitiveType:
        return self.primitiveType.checkString(self.primitiveType.normalize(text,
                                                                           item),
                                              context)
      else:
        item.schemaNormalizedValue=None
        return
    elif self.variety=='list':
      it=self.itemType
      # TODO: what about post-list facets?
      if not it:
        return
      for substr in string.split(normalize(None,text,item,'collapse')):
        res=it.validateText(substr,None,context,schema)
        if res:
          return res+' in list'
      return
    elif self.variety=='union':
      mts=self.memberTypes
      subres=[]
      # TODO: what about post-union facets?
      for mt in mts:
        if mt:
          res=mt.validateText(text,item,context,schema)
          if res:
            subres.append(res)
          else:
            # bingo
            return
      # no subtypes won, we lose
      item.schemaNormalizedValue=None
      return " no members of union succeeded: %s"%subres
    else:
      XMLSchema.shouldnt('vv '+str(self.variety))

XMLSchema.SimpleType.validateText=validateText

def validateText(self, text, item, context, schema):
    return self.checkString(self.normalize(text,item),context)

XMLSchema.AbInitio.validateText=validateText

# checkString methods

def checkString(self,str,context):
  if self.facetValue('enumeration')!=None:
    evs=self.facetValue('enumeration')
    for val in evs:
      if val==str:
        return
    return " not in enumeration %s"%evs

XMLSchema.AbInitio.checkString = checkString

wsoChar=re.compile("[\t\r\n]")
wsChars=re.compile("[ \t\r\n]+")
iSpace=re.compile("^ ")
fSpace=re.compile(" $")
def normalize(self,str,item,ws=None):
  if not item:
    # list component, shouldn't be processed
    return str
  if not ws:
    ws=self.facetValue('whiteSpace')
  if ws=='replace':
    str=wsoChar.sub(' ',str)
  elif ws=='collapse':
    str=wsChars.sub(' ',str)
    str=iSpace.sub('',str)
    str=fSpace.sub('',str)
  # else  ((not ws) or ws=='preserve'): pass
  item.schemaNormalizedValue=str
  return str

XMLSchema.AbInitio.normalize=normalize

def checkString(self,str,context):
  try:
    if ('.' in str) or ('E' in str):
      val=string.atof(str)
    else:
      val=string.atoi(str)
  except ValueError:
    return " does not represent a number"
  minI=self.facetValue('minInclusive')
  if minI!=None and val<minI:
    return "<%d"%minI
  minE=self.facetValue('minExclusive')
  if minE!=None and val<=minE:
    return "<=%d"%minE
  maxI=self.facetValue('maxInclusive')
  if maxI!=None and val>maxI:
    return ">%d"%maxI
  maxE=self.facetValue('maxExclusive')
  if maxE!=None and val>=maxE:
    return ">=%d"%maxE
  return XMLSchema.AbInitio.checkString(self,str,context)

XMLSchema.DecimalST.checkString = checkString

def checkString(self,str,context):
  # not complete by any means
  parts=string.split(str,':')
  if len(parts)>2:
    return " has more than one colon"
  if len(parts)==2 and not context.inScopeNamespaces.has_key(parts[0]):
    return " has undeclared prefix: %s"%parts[0]
  return XMLSchema.AbInitio.checkString(self,str,context)

XMLSchema.QNameST.checkString = checkString

# assess methods

def assess(self,factory,decl):
  allfull = 1
  allnone = 1
  nochildren = 1
  for c in self.chunkedChildren:
    if isinstance(c, XMLInfoset.Element):
      nochildren = 0
      validationAttempted = c.__dict__.has_key("validationAttempted") and c.validationAttempted
      if validationAttempted != 'full':
        allfull = 0
      if validationAttempted and c.validationAttempted != 'none':
        allnone = 0
  attrs=self.attributes.values()
  for c in attrs:
    if isinstance(c, XMLInfoset.Attribute):
      nochildren = 0
      validationAttempted = c.__dict__.has_key("validationAttempted") and c.validationAttempted
      if validationAttempted != 'full':
        allfull = 0
      if validationAttempted and c.validationAttempted != 'none':
        allnone = 0

  if nochildren:
    if self.assessedType:
      self.validationAttempted = 'full'
    else:
      self.validationAttempted = 'none'
  else:
    if allfull and self.assessedType:
      self.validationAttempted = 'full'
    elif allnone and not self.assessedType:
      self.validationAttempted = 'none'
    else:
      self.validationAttempted = 'partial'

  if self.errorCode:
    self.validity = 'invalid'
  else:
    has_losing_child = 0
    has_untyped_strict_child = 0
    has_non_winning_typed_child = 0
    for c in self.chunkedChildren:
      if not isinstance(c, XMLInfoset.Element):
        continue
      strict = c.__dict__.has_key("strict") and c.strict
      validatedType = c.__dict__.has_key("assessedType") and c.assessedType
      validity = c.__dict__.has_key("validity") and c.validity
      if validity == 'invalid':
        has_losing_child = 1
      if strict and not validatedType:
        has_untyped_strict_child = 1
      if validatedType and validity != 'valid':
        has_non_winning_typed_child = 1
    for c in attrs:
      if not isinstance(c, XMLInfoset.Attribute):
        continue
      strict = c.__dict__.has_key("strict") and c.strict
      validatedType = c.__dict__.has_key("assessedType") and c.assessedType
      validity = c.__dict__.has_key("validity") and c.validity
      if validity == 'invalid':
        has_losing_child = 1
      if strict and not validatedType:
        has_untyped_strict_child = 1
      if validatedType and validity != 'valid':
        has_non_winning_typed_child = 1
    if has_losing_child or has_untyped_strict_child:
      self.validity = 'invalid'
    elif has_non_winning_typed_child:
      self.validity = 'notKnown'
    else:
      self.validity = 'valid'

  if self.assessedType and self.validity=='valid':
    self.typeDefinition=self.assessedType
    self.elementDeclaration=decl
  self.validationContext=factory.docElt
  
XMLInfoset.Element.assess=assess
XMLInfoset.Element.assessedType=None

def assess(self,factory,decl):
  if self.errorCode:
    self.validity = 'invalid'
  else:
    self.validity = 'valid'
  if self.assessedType:
    self.validationAttempted = 'full'
    if self.validity=='valid':
      self.typeDefinition=self.assessedType
      self.attributeDeclaration=decl
  else:
    self.validationAttempted = 'none'
    self.validity = 'notKnown'
  self.validationContext=factory.docElt
  
XMLInfoset.Attribute.assess=assess
XMLInfoset.Attribute.assessedType=None

def dumpInfoset(fileOrName):
  close=0
  if type(fileOrName)==types.FileType:
    ff=fileOrName
  else:
    close=1
    ff = open(fileOrName, "w")
  r = f.docElt.parent.reflect()
  r.documentElement.inScopeNamespaces["psv"]=XMLInfoset.Namespace("psv",
                                           PSVInfoset.infosetSchemaNamespace)
  r.indent("",1)
  r.printme(ff)
  if close:
    ff.close()
  
# run at import if top

if __name__=='__main__':
  argl=sys.argv[1:]
  k=0
  dw=1
  timing=0
  style=None
  outfile=None
  reflect=0
  reflect2=0
  independent=0
  proFile=None
  while argl:
    if argl[0]=='-k':
      k=1
    elif argl[0]=='-s':
      style=argl[1]
      argl=argl[1:]
    elif argl[0]=='-o':
      outfile=argl[1]
      argl=argl[1:]
    elif argl[0]=='-p':
      proFile=argl[1]
      argl=argl[1:]
    elif argl[0]=='-w':
      dw=0
    elif argl[0]=='-t':
      timing=1
    elif argl[0]=='-r':
      reflect=1
    elif argl[0]=='-R':
      reflect2=1
      refprefix=argl[1]
      argl=argl[1:]
    elif argl[0]=='-i':
      independent=1
    elif argl[0][0]=='-':
      sys.stderr.write("Usage: [-ktwri] [-s stylesheet] [-o outputFile] [-p profileOut] file [schema1 schema2 . . .]\n")
      sys.exit(-1)
    else:
      break
    argl=argl[1:]

  if argl:
    if proFile:
      import profile
      res=profile.run("""runitAndShow(argl[0],argl[1:],k,
                         style,None,outfile,dw,timing,reflect,independent,reflect2)""",
                      proFile)
    else:
      res=runitAndShow(argl[0],argl[1:],
                       k,style,None,outfile,dw,timing,reflect,independent,reflect2)
  else:
    res=runitAndShow(None,[],k,
                     style,None,outfile,dw,timing,reflect,independent,reflect2)

  if res:
    raise SchemaValidationError,res

# $Log: applyschema.py,v $
# Revision 1.98  2001/06/16 11:56:53  ht
# protect a read
#
# Revision 1.97  2001/06/09 19:14:08  ht
# implement (most of) fixed/default for elements
# support RDDL for command line, xsi:schemaLoc and namespace URIs
#
# Revision 1.96  2001/06/04 16:05:29  ht
# no namespace == no prefix == None
#
# Revision 1.95  2001/05/07 08:38:12  ht
# and again reflect2
#
# Revision 1.94  2001/05/07 08:34:56  ht
# fix reflect2 binding bug
#
# Revision 1.93  2001/04/24 13:29:48  ht
# (PSV)Infoset reorganisation
#
# Revision 1.92  2001/04/12 10:54:04  ht
# raise multiple key error
#
# Revision 1.91  2001/04/10 14:41:49  ht
# fix bug handling unknown xsi attrs
#
# Revision 1.90  2001/04/07 11:19:51  ht
# log target reading crash better
#
# Revision 1.89  2001/04/04 20:56:30  ht
# implement -i switch to do forced schema assessment independent of any instance
#
# Revision 1.88  2001/03/17 12:11:13  ht
# merge v2001 back in to main line
#
# Revision 1.87  2001/02/16 16:38:43  richard
# fix key/keyref/unique field code
#
# Revision 1.86  2001/02/12 11:34:46  ht
# catch unbound prefix in xsi:type
#
# Revision 1.85.2.5  2001/03/15 11:37:59  ht
# check for and rule out use of abstract types
#
# Revision 1.85.2.4  2001/02/24 23:47:56  ht
# handle unbound prefix in xsi:type
# fix typo in assess
#
# Revision 1.85.2.3  2001/02/17 23:33:11  ht
# assignAttrTypes sets a.type to the use if there is one
# so either valueConstraint can be used
#
# Revision 1.85.2.2  2001/02/14 17:01:11  ht
# merge attr use back in to v2001 line
#
# Revision 1.85.2.1.2.1  2001/02/07 17:34:28  ht
# use AttrUse to supply defaults
#
# Revision 1.85.2.1  2001/02/07 14:30:01  ht
# change NS to 2001, implement null->nil
#
# Revision 1.85  2001/02/07 09:23:24  ht
# report low-level failure correctly
#
# Revision 1.84  2001/02/06 14:20:41  ht
# accommodate to earlier XPath construction for fields/selector
#
# Revision 1.83  2001/02/06 11:30:51  ht
# merged infoset-based back to mainline
#
# Revision 1.74.2.26  2001/01/15 14:18:55  ht
# improve wildcard error msg
#
# Revision 1.74.2.25  2001/01/03 19:13:19  ht
# accommodate to change of exception with LTXMLInfoset
#
# Revision 1.74.2.24  2001/01/03 11:57:52  ht
# fix xsi:type bugs
#
# Revision 1.74.2.23  2000/12/23 13:08:21  ht
# fix spelling of whiteSpace,
# add -p file switch for profiling
#
# Revision 1.74.2.22  2000/12/22 18:33:54  ht
# add whitespace processing,
# fix some bugs?
#
# Revision 1.74.2.21  2000/12/21 18:29:38  ht
# accommodate to .facets and real facets
#
# Revision 1.74.2.20  2000/12/16 12:10:29  ht
# add logging of relevant declaration to elt/attr assess
#
# Revision 1.74.2.19  2000/12/14 14:21:59  ht
# fix bug in e-o error msg
#
# Revision 1.74.2.18  2000/12/13 23:30:09  ht
# add -r switch to produce reflected output
#
# Revision 1.74.2.17  2000/12/12 17:33:06  ht
# get builtin-schemas out first, sort AbInitios to the front,
# more details in content-model error messages,
# set null property
#
# Revision 1.74.2.16  2000/12/08 18:08:42  ht
# install schemaInformation on validation root,
# assign type defn as such to typeDefinition property
#
# Revision 1.74.2.15  2000/12/08 15:16:07  ht
# put the docElt in the factory,
# use it for reflection at the end,
# and to implement validationContext
#
# Revision 1.74.2.14  2000/12/07 13:18:42  ht
# work around null vs "" for missing namespace name
#
# Revision 1.74.2.13  2000/12/07 10:20:48  ht
# handle xsi: attrs cleanly
#
# Revision 1.74.2.12  2000/12/06 22:43:11  ht
# make assess a method on Element,
# add one on Attribute,
# refer to the latter from the former
#
# Revision 1.74.2.11  2000/12/06 09:21:05  ht
# add psv infoset namespace URI to reflected docapplyschema.py
#
# Revision 1.74.2.10  2000/12/04 22:31:03  ht
# stubs for schemaNormalizedValue in place
#
# Revision 1.74.2.9  2000/12/04 22:09:00  ht
# remove convert,
# accommodate change to importing XML,
# put attribute verror on right item
#
# Revision 1.74.2.8  2000/12/04 13:30:42  ht
# merge in main line fixes thru 1.82
#
# Revision 1.74.2.7  2000/10/13 12:48:42  richard
# more infoset contributions
#
# Revision 1.74.2.6  2000/10/02 13:33:28  richard
# update values for validity property
#
# Revision 1.74.2.5  2000/09/29 17:18:09  richard
# More towards PSV infoset
#
# Revision 1.74.2.4  2000/09/29 16:45:27  richard
# correct errorCode setting
#
# Revision 1.74.2.3  2000/09/29 16:04:24  richard
# More towards PSV infoset
#
# Revision 1.74.2.2  2000/09/29 14:16:15  ht
# towards PSVI contributions
#
# Revision 1.74.2.1  2000/09/27 17:21:20  richard
# Changes for infoset-based
#
# Revision 1.77  2000/09/28 15:54:50  ht
# schema error count includes all errors, not just those found at prep
# time
#
# Revision 1.76  2000/09/28 15:09:14  ht
# try catching and returning any crashes
#
# Revision 1.75  2000/09/28 08:41:57  ht
# add usage message
# add -o outfile cmd line arg
#
# Revision 1.82  2000/10/31 16:30:47  ht
# validate subordinate elements with eltdecl if available
# return schema error count if not attempting instance validation
#
# Revision 1.81  2000/10/27 15:33:30  ht
# Output timing info if -t on command line
#
# Revision 1.80  2000/10/18 15:54:58  ht
# make effort to check 'fixed' attribute values
#
# Revision 1.79  2000/10/17 13:35:41  ht
# put switch on warnings, default is don't
#
# Revision 1.78  2000/10/17 12:45:15  ht
# try to catch and log all crashes
# replace stale reference to atribute.characters
#
# Revision 1.77  2000/09/28 15:54:50  ht
# schema error count includes all errors, not just those found at prep
# time
#
# Revision 1.76  2000/09/28 15:09:14  ht
# try catching and returning any crashes
#
# Revision 1.75  2000/09/28 08:41:57  ht
# add usage message
# add -o outfile cmd line arg
#
# Revision 1.74  2000/09/27 13:48:47  richard
# Use infoset-like names for slots (now provided in XML.py) to reduce
# differences with infoset-based version.
#
# Revision 1.73  2000/09/27 12:22:22  richard
# correct element.name to element.local in an error message
#
# Revision 1.72  2000/09/26 14:29:36  richard
# Oops, didn't change AbInitio to XMLSchema.AbInitio when moving methods
#
# Revision 1.71  2000/09/26 14:05:28  richard
# Move checkString methods from XMLSchema.py, because they may need to look
# at *instance* in-scope namespaces
#
# Revision 1.70  2000/09/26 13:38:49  ht
# protect against undefined list itemType/union memberType
#
# Revision 1.69  2000/09/23 11:17:31  ht
# merge in CR branch
#

# Revision 1.68  2000/09/23 11:14:26  ht
# towards merge in CR branch
#
# Revision 1.66.2.3  2000/09/21 09:14:33  ht
# property name change
#
# Revision 1.66.2.2  2000/09/11 12:23:27  ht
# Move to branch: more debug in vv crash
#
# Revision 1.68  2000/09/03 15:57:23  ht
# more debug in vv crash

# Revision 1.67  2000/09/11 12:59:09  ht
# allow stdin,
# fix stupid bug missing third schema on command line

# Revision 1.67  2000/08/31 11:48:41  ht
# Direct support for validating lists and unions

# Revision 1.66  2000/08/22 13:11:30  ht
# handle type w/o qname as document validation type
# remove special treatment for AbInitio simple types on elements,
# thereby fixing list validation bug

# Revision 1.66.2.3  2000/09/21 09:14:33  ht
# property name change
#
# Revision 1.66.2.2  2000/09/11 12:23:27  ht
# Move to branch: more debug in vv crash
#
# Revision 1.68  2000/09/03 15:57:23  ht
# more debug in vv crash
#
# Revision 1.67  2000/08/31 11:48:41  ht
# Direct support for validating lists and unions
#

# Revision 1.66  2000/08/22 13:11:30  ht
# handle type w/o qname as document validation type
# remove special treatment for AbInitio simple types on elements,
# thereby fixing list validation bug
#
# Revision 1.65  2000/07/12 09:31:58  ht
# try harder to always have a schema
#
# Revision 1.64  2000/07/10 14:39:02  ht
# prepare for fileinfo to runit
#
# Revision 1.63  2000/07/05 09:05:37  ht
# change name to PyLTXML
#
# Revision 1.62  2000/07/03 09:37:38  ht
# bail out if textonly has elt daughter(s)
# add missing import
#
# Revision 1.61  2000/06/27 09:25:51  ht
# attempt to handle interaction between xsi:type and <any>
#
# Revision 1.60  2000/06/24 11:17:07  ht
# fix bug in unqualified xsi:type
#
# Revision 1.59  2000/06/22 10:31:33  ht
# Bug in unique processing -- broke on missing field
#
# Revision 1.58  2000/06/20 08:07:42  ht
# merge xmlout branches back in to main line
#

# Revision 1.57  2000/05/18 08:01:25  ht
# fix bug in handling of xsi:type
#
# Revision 1.56  2000/05/14 12:19:34  ht
# add context to checkSting calls
#
# Revision 1.55  2000/05/11 11:55:57  ht
# just better handling of lax validation from other branch
#
# Revision 1.54.2.16  2000/06/15 16:03:20  ht
# cover several missing definition cases
#
# Revision 1.54.2.15  2000/06/03 16:29:30  ht
# oops, removing debugging comment
#
# Revision 1.54.2.14  2000/06/03 13:45:55  ht
# catch arity bug in xsi:schemaLoc
#
# Revision 1.54.2.13  2000/05/30 09:35:43  ht
# fix encoding bug when things break down altogether
#
# Revision 1.54.2.12  2000/05/29 08:46:53  ht
# strong enforcement of nullable
# add error codes to all errors
# remove remaining __class__ tests
# change error reporting wrt disallowed content
#
# Revision 1.54.2.11  2000/05/24 20:46:47  ht
# make validateText a method, split across SimpleType and AbInitio
#
# Revision 1.54.2.10  2000/05/24 12:03:28  ht
# modest effort to validate list types
# fix bug in noNamespaceSchemaLocation handling at validation time
#
# Revision 1.54.2.9  2000/05/22 16:11:52  ht
# use OpenStream, take more control of encoding
#
# Revision 1.54.2.8  2000/05/18 17:37:40  ht
# parameterise stylesheet,
# remove formatting from xsv:xsv attributes,
# add namespace decl
#
# Revision 1.54.2.7  2000/05/18 07:59:48  ht
# fix xsi:type validation bug
#
# Revision 1.54.2.6  2000/05/16 16:31:11  ht
# fix bug handling un-typed element declarations == urType validation
#
# Revision 1.54.2.5  2000/05/14 12:29:59  ht
# merge QName checking from main branch
#
# Revision 1.54.2.4  2000/05/12 15:15:01  ht
# process keys even if type is simple,
# add a few codes to get started
#
# Revision 1.54.2.3  2000/05/11 13:59:11  ht
# convert verror/vwarn to produce elements
# eliminate a few special error outputs in favour of special
# sub-elements
#
# Revision 1.54.2.2  2000/05/11 11:14:00  ht
# more error protection
# handle lax recursively and at the start
#
# Revision 1.54.2.1  2000/05/10 11:36:47  ht
# begin converting to XML output
#
# Revision 1.56  2000/05/14 12:19:34  ht
# add context to checkSting calls
#
# Revision 1.55  2000/05/11 11:55:57  ht
# just better handling of lax validation from other branch
#
# Revision 1.54  2000/05/09 14:52:52  ht
# Check for strings in a way that works with or without 16-bit support
#
# Revision 1.53  2000/05/09 12:27:58  ht
# replace our hack with python's url parsing stuff
# make f global for debugging
#
# Revision 1.52  2000/05/05 15:15:45  richard
# wrong (?) elt arg to verror in validateKeyRefs
#
# Revision 1.51  2000/05/04 07:56:35  ht
# Fix typo in opportunistic attribute validation
#
# Revision 1.50  2000/05/01 15:07:00  richard
# bug fix schema -> key.schema
#
# Revision 1.49  2000/05/01 10:05:43  ht
# catch various missing file errors more gracefully
#
# Revision 1.48  2000/04/28 15:40:01  richard
# Implement xsi:null (still don't check nullable)
#
# Revision 1.47  2000/04/28 15:11:23  richard
# allow xsi: attributes on simple type
# moved eltDecl code up validateElement ready for implementing xsi:null
#
# Revision 1.46  2000/04/27 09:41:18  ht
# remove raw types from error messages
#
# Revision 1.45  2000/04/27 09:30:21  ht
# check that inputs are actually schemas,
# remove schema arg to doImport, checkInSchema
#
# Revision 1.44  2000/04/26 13:00:40  ht
# add copyright
#
# Revision 1.43  2000/04/24 20:46:40  ht
# cleanup residual bugs with massive rename,
# rename Any to Wildcard,
# replace AnyAttribute with Wildcard,
# get validation of Wildcard working in both element and attribute contexts
#
# Revision 1.42  2000/04/24 15:08:34  ht
# minor glitches, tiny.xml works again
#
# Revision 1.41  2000/04/24 15:00:09  ht
# wholesale name changes -- init. caps for all classes,
# schema.py -> XMLSchema.py
#
# Revision 1.40  2000/04/24 11:09:17  ht
# make version string universally available
#
# Revision 1.39  2000/04/24 10:06:59  ht
# add version info to message
#
# Revision 1.38  2000/04/24 10:02:39  ht
# change invocation message
#
# Revision 1.37  2000/04/24 09:41:43  ht
# clean up invocation some more, add k arg't to runit
#
# Revision 1.36  2000/04/21 09:32:21  ht
# another dose of resolveURL
# use tiny only if run from command line
#
# Revision 1.35  2000/04/20 22:12:43  ht
# use resolveURL on input, schemaLocs
#
# Revision 1.34  2000/04/20 15:45:08  ht
# better handling of use of ns uri for loc
#
# Revision 1.33  2000/04/20 14:26:59  ht
# merge in private and comp branches
#
# Revision 1.32.2.5  2000/04/20 14:25:54  ht
# merge in comp branch
#
# Revision 1.32.2.4.2.9  2000/04/20 14:22:39  ht
# manage document validation schema creation and search better
#
# Revision 1.32.2.4.2.8  2000/04/20 12:03:21  ht
# Remove a few lingering effectiveTypes
# Allow better for absent types etc.
#
# Revision 1.32.2.4.2.7  2000/04/14 21:18:27  ht
# minor attr names/path changes to track schema
#
# Revision 1.32.2.4.2.6  2000/04/13 23:04:39  ht
# allow for urType as simple type (?)
# track Any->AnyWrap change
#
# Revision 1.32.2.4.2.5  2000/04/12 17:29:37  ht
# begin work on model merger,
#
# Revision 1.32.2.4.2.4  2000/04/11 18:13:17  ht
# interpolate attributeUse between complexType and attributeDeclaration,
# parallel to particle
#
# Revision 1.32.2.4.2.3  2000/04/10 15:48:46  ht
# put modest attribute validation in place
#
# Revision 1.32.2.4.2.2  2000/04/09 16:13:26  ht
# working on complex type, attribute;
# back out component.qname
#
# Revision 1.32.2.4.2.1  2000/04/05 12:12:36  ht
# accommodate changes in schema.py
#
# Revision 1.32.2.4  2000/04/01 18:01:25  ht
# various minor compatibility fixes
#
# Revision 1.32.2.3  2000/03/25 12:12:27  ht
# restructure error handling/reporting;
# allow for switching 208 on and off
#
# Revision 1.32.2.2  2000/03/21 15:57:23  ht
# fix bug in skip,
# allow 208 override
#
# Revision 1.32.2.1  2000/03/20 17:22:52  ht
# better coverage of <any>, including beginning of processcontents
#
# Revision 1.33  2000/03/20 17:20:53  ht
# better coverage of <any>, including beginning of processcontents
#
# Revision 1.32  2000/03/08 15:28:46  ht
# merge private branches back into public after 20000225 release
#
# Revision 1.31.2.3  2000/02/24 23:40:32  ht
# fix any bug
#
# Revision 1.31.2.2  2000/02/21 09:18:13  ht
# bug in <any> handling
#
# Revision 1.31.2.1  2000/02/08 21:43:39  ht
# fork private branch to track internal drafts
# change calling sequence of checkinSchema
#
# Revision 1.31.1.1  2000/02/08 13:54:25  ht
# fork branch for non-public changes
# calling sequence to checkinSchema changed
#
# Revision 1.31  2000/01/13 16:55:42  richard
# Finally do something with xsi:type
#
# Revision 1.30  2000/01/10 17:36:34  richard
# changes for xsi:schemaLocation
#
# Revision 1.29  2000/01/08 23:33:50  ht
# towards support for xsi:schemaLocation
#
# Revision 1.28  2000/01/08 12:07:38  ht
# Change command-line arg sequence in preparation for use of schemaLocation!!!!!
# Add debug printout for schemaLocation for now
#
# Revision 1.27  2000/01/07 17:08:26  richard
# start on xsi:type
#
# Revision 1.26  2000/01/06 14:59:38  ht
# fix command line bug, display args on entry
#
# Revision 1.25  2000/01/06 14:38:56  ht
# detect cross-scope keyref and signal error
#
# Revision 1.24  2000/01/03 17:02:37  ht
# Include result of sub-ordinate key checking in overall result
# Accommodate new calling sequence for xpath.find
# add Log and Id
#
#
