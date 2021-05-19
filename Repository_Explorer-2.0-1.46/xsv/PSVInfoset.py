# XXX todo: schemaSpecified, notation, idIdrefTable, identityConstraintTable
#     fundamental facets, final for simple types, identityConstraintDefinitions

import XMLInfoset
import sys
import XMLSchema
import string

Element = XMLInfoset.Element
Characters = XMLInfoset.Characters
Attribute = XMLInfoset.Attribute
xsiNamespace = XMLInfoset.xsiNamespace

infosetSchemaNamespace = "http://www.w3.org/2001/05/PSVInfosetExtension"

def reflectPointer(self, name, target):
  e = Element(self, infosetSchemaNamespace, name)
  self.addChunkedChild(e)
  c = Element(e, XMLInfoset.infosetSchemaNamespace, "pointer")
  e.addChunkedChild(c)
  refAttr = Attribute(c, None, "ref", None, target)
  c.addAttribute(refAttr)

XMLInfoset.InformationItem.reflectPointer=reflectPointer

def reflectAtom(self, name, eName=None):
  if eName:
    e = Element(self, infosetSchemaNamespace, eName)
    self.addChunkedChild(e)
  else:
    e = self
  c = Element(e, infosetSchemaNamespace, "atom")
  e.addChunkedChild(c)
  valAttr = Attribute(c, None, "name", None, name)
  c.addAttribute(valAttr)

XMLInfoset.InformationItem.reflectAtom=reflectAtom

class NamespaceSchemaInformation(XMLInfoset.InformationItem):

  def __init__(self, schema):
    self.schemaNamespace = schema.targetNS
    self.schemaComponents=[]
    for tab in (schema.typeTable,schema.elementTable,schema.attributeTable,
                schema.groupTable,schema.attributeGroupTable):
      self.schemaComponents=self.schemaComponents+tab.values()
    self.schemaAnnotations=schema.annotations
    self.schemaDocuments = map(lambda l:schemaDocument(l),
                               schema.locations)
    # we could save the document elements of each schema . . .

  def reflect(self, parent=None):
    
    nsi = Element(parent, infosetSchemaNamespace, "namespaceSchemaInformation")

    self.reflectString(nsi, "schemaNamespace", self.schemaNamespace, 1,
                       infosetSchemaNamespace)

    comps = Element(nsi, infosetSchemaNamespace, "schemaComponents")
    nsi.addChunkedChild(comps)

    # two passes -- assign names, to avoid internal defn's of named stuff
    for c in self.schemaComponents:
      if isinstance(c,XMLSchema.Component):
        c.assignUid()
    for c in self.schemaComponents:
      comps.addChunkedChild(c.reflect(comps,1))

    docs = Element(nsi, infosetSchemaNamespace, "schemaDocuments")
    nsi.addChunkedChild(docs)

    for d in self.schemaDocuments:
      docs.addChunkedChild(d.reflect(docs))

    annots = Element(nsi, infosetSchemaNamespace, "schemaAnnotations")
    nsi.addChunkedChild(annots)

    for a in self.schemaAnnotations:
      annots.addChunkedChild(a.reflect(annots))

    return nsi

class schemaDocument(XMLInfoset.InformationItem):

  def __init__(self, location, document=None):
    self.documentLocation = location
    self.document = document

  def reflect(self, parent=None):
    sd = Element(parent, infosetSchemaNamespace, "schemaDocument")
    self.reflectString(sd, "documentLocation", self.documentLocation, 1,
                       infosetSchemaNamespace)
    self.reflectNull(sd, "document",
                       infosetSchemaNamespace)
    return sd

def componentReflect(self,parent,forceFull=0):
  if self.uid and not forceFull:
    # a pointer
    c = Element(parent, XMLInfoset.infosetSchemaNamespace, "pointer")
    refAttr = Attribute(c, None, "ref", None, self.uid)
    c.addAttribute(refAttr)
    return c
  else:
    e = Element(parent, infosetSchemaNamespace, self.reflectedName)
    if self.needsId and not forceFull:
      self.assignUid()
    if self.uid:
      idAttr = Attribute(e, None, "id", None, self.uid)
      e.addAttribute(idAttr)
    for rme in self.reflectionMap:
      # reflectionMap entries: (compPropertyName,valueType,nullable,
      #                         pythonPropertyName)
#      print ('rme',rme)
      value=getattr(self,rme[3])
#      print ('vv',self,value)
      if rme[1]=='string':
        e.reflectString(e,rme[0],value,
                        rme[2],infosetSchemaNamespace)
      elif rme[1]=='list':
        rel=Element(e,infosetSchemaNamespace,rme[0])
        e.addChunkedChild(rel)
        if len(value)>0:
          rel.addChunkedChild(Characters(e,string.join(value,' ')))
      elif rme[1]=='boolean':
        if str(value) not in ('true','false'):
          if value:
            value='true'
          else:
            value='false'
        e.reflectString(e,rme[0],value,
                        rme[2],infosetSchemaNamespace)
      elif rme[1]=='component':
        if value:
          rel=Element(e,infosetSchemaNamespace,rme[0])
          e.addChunkedChild(rel)
          rel.addChunkedChild(value.reflect(rel))
        elif rme[2]:
          if rme[2]==1:
            e.reflectNull(e,rme[0],infosetSchemaNamespace)
        else:
          shouldnt()
      elif rme[1]=='namedComponent':
        if value:
          if value.uid:
            c = Element(e, XMLInfoset.infosetSchemaNamespace, "pointer")
            e.addChunkedChild(c)
            refAttr = Attribute(c, None, "ref", None, value.uid)
            c.addAttribute(refAttr)
          else:
            # if no uid then must be anon, no point naming
            e.addChunkedChild(value.reflect(e))
        elif rme[2]:
          e.reflectNull(e,rme[0],infosetSchemaNamespace)
        else:
          shouldnt2()
      elif rme[1]=='special':
        value(e)
      elif rme[1]=='components':
        if value==None and rme[2]:
          e.reflectNull(e,rme[0],infosetSchemaNamespace)
          continue
        rel=Element(e,infosetSchemaNamespace,rme[0])
        e.addChunkedChild(rel)
        for vv in value or []:
          rel.addChunkedChild(vv.reflect(rel))
    return e 

XMLSchema.Component.reflect=componentReflect
XMLSchema.Component.needsId=0
XMLSchema.ComplexType.needsId=1 # only nested Elts, Attrs, CTs and STs need Ids
XMLSchema.SimpleType.needsId=1
XMLSchema.Element.needsId=1
XMLSchema.Attribute.needsId=1

allPrefixes={'xsd':XMLSchema.XMLSchemaNS,
             'xsi':xsiNamespace}
allNSs={xsiNamespace:'xsi',
        XMLSchema.XMLSchemaNS:'xsd'}

def assignUid(self):
  cnn=None
  nn=self.name
  if self.targetNamespace:
    if allNSs.has_key(self.targetNamespace):
      cnn="%s:"%allNSs[self.targetNamespace]
    elif (self.xrpr and self.xrpr.elt and self.xrpr.elt.namespaceDict):
      for (n,v) in self.xrpr.elt.namespaceDict.items():
        # note that this namespaceDict is a Mapper hack from layer.py
        if v==self.targetNamespace:
          if n!=None and (not allPrefixes.has_key(n)):
            allNSs[self.targetNamespace]=n
            allPrefixes[n]=self.targetNamespace
            cnn="%s:"%n
          break
    if not cnn:
      n="x%d"%self.id
      allNSs[self.targetNamespace]=n
      allPrefixes[n]=self.targetNamespace
      cnn="%s:"%n
  else:
    cnn=""
    if nn:
      nn="%s.%s"%(nn,self.id)
  self.uid="%s%s.%s"%(cnn,self.kind,nn or "_anon_%d"%self.id)

XMLSchema.Component.uid=None
XMLSchema.Component.assignUid=assignUid
XMLSchema.Type.kind='type'
XMLSchema.Element.kind='elt'
XMLSchema.Attribute.kind='attr'
XMLSchema.Group.kind='mg'
XMLSchema.AttributeGroup.kind='ag'
# XMLSchema.Notation.kind='ntn'

def abInitioReflect(self,parent,force=0):
  if self.uid:
    # a pointer
    c = Element(parent, XMLInfoset.infosetSchemaNamespace, "pointer")
    refAttr = Attribute(c, None, "ref", None, self.uid)
    c.addAttribute(refAttr)
    return c
  else:
    self.uid=self.name
    e = Element(parent, infosetSchemaNamespace, 'simpleTypeDefinition')
    idAttr = Attribute(e, None, "id", None, self.uid)
    e.addAttribute(idAttr)
    nullAttr = Attribute(e, xsiNamespace, "nil", None, "true")
    e.addAttribute(nullAttr)
    return e

XMLSchema.AbInitio.reflect=abInitioReflect
XMLSchema.AbInitio.uid=None
XMLSchema.AbInitio.itemType=None
XMLSchema.AbInitio.memberTypes=None
XMLSchema.Atomic.itemType=None
XMLSchema.Atomic.memberTypes=None
XMLSchema.List.primitiveType=None
XMLSchema.List.memberTypes=None
XMLSchema.Union.primitiveType=None
XMLSchema.Union.itemType=None

def compareSFSComps(c1,c2):
  # AbInitios first
  if isinstance(c1,XMLSchema.AbInitio):
    if isinstance(c2,XMLSchema.AbInitio):
      return 0
    else:
      return -1
  elif isinstance(c2,XMLSchema.AbInitio):
    return 1
  else:
    return 0

def scopeReflect(self,parent):
  if self.scope:
    se=Element(parent,infosetSchemaNamespace,"scope")
    parent.addChunkedChild(se)
    if self.scope=='global':
      se.addChunkedChild(Characters(se, 'global'))
    else:
      se.addChunkedChild(self.scope.reflect(self))
  else:
    parent.reflectNull(parent,'scope',infosetSchemaNamespace)

XMLSchema.Element.scopeReflect=scopeReflect
XMLSchema.Attribute.scopeReflect=scopeReflect

def vcReflect(self,parent):
  if self.valueConstraint:
    vc=Element(parent,infosetSchemaNamespace,'valueConstraint')
    parent.addChunkedChild(vc)
    parent=vc
    vc=Element(parent,infosetSchemaNamespace,'valueConstraint')
    parent.addChunkedChild(vc)
    vc.reflectString(vc,'variety',self.valueConstraint[0],
                     1,infosetSchemaNamespace)
    vc.reflectString(vc,'value',self.valueConstraint[1],
                     0,infosetSchemaNamespace)
  else:
    parent.reflectNull(parent,'valueConstraint',infosetSchemaNamespace)

XMLSchema.Element.vcReflect=vcReflect
XMLSchema.Attribute.vcReflect=vcReflect
XMLSchema.AttributeUse.vcReflect=vcReflect

def adReflect(self,parent):
  tab={}
  for ad in self.attributeDeclarations:
    ad.expand(tab)
  rel=Element(parent,infosetSchemaNamespace,'attributeDeclarations')
  parent.addChunkedChild(rel)
  for vv in tab.values():
    rel.addChunkedChild(vv.reflect(rel))

XMLSchema.AttributeGroup.adReflect=adReflect

def wnsReflect(self,parent):
  ns=Element(parent,infosetSchemaNamespace,'namespaceConstraint')
  parent.addChunkedChild(ns)
  parent=ns
  ns=Element(parent,infosetSchemaNamespace,'namespaceConstraint')
  parent.addChunkedChild(ns)
  if self.allowed=='##any':
    ns.reflectString(ns, 'variety', 'any', 0, infosetSchemaNamespace)
    ns.reflectNull(ns, 'namespaces', infosetSchemaNamespace)
  else:
    if self.negated:
      ns.reflectString(ns, 'variety', 'negative', 0, infosetSchemaNamespace)
    else:
      ns.reflectString(ns, 'variety', 'positive', 0, infosetSchemaNamespace)
    nss = Element(ns, infosetSchemaNamespace, 'namespaces')
    ns.addChunkedChild(nss)
    first=1
    for nn in self.namespaces:
      if first:
        first=0
      else:
        nss.addChunkedChild(Characters(nss, ' '))
      if nn:
        nss.addChunkedChild(Characters(nss, nn))
      else:
        nss.addChunkedChild(Characters(nss, '##none'))

XMLSchema.Wildcard.wildcardNamespaceReflect=wnsReflect

def ctReflect(self,parent):
  if self.contentType:
    ct=Element(parent,infosetSchemaNamespace,'contentType')
    parent.addChunkedChild(ct)
    parent=ct
    ct=Element(parent,infosetSchemaNamespace,'contentType')
    parent.addChunkedChild(ct)
    if self.contentType=='empty':
      ct.reflectString(ct, 'variety','empty',0,infosetSchemaNamespace)
      ct.reflectNull(ct,'simpleTypeDefinition',infosetSchemaNamespace)
      ct.reflectNull(ct,'particle',infosetSchemaNamespace)
    elif self.contentType in ('elementOnly','mixed'):
      ct.reflectString(ct, 'variety',self.contentType,0,infosetSchemaNamespace)
      ct.reflectNull(ct,'simpleTypeDefinition',infosetSchemaNamespace)
      particle=Element(ct, infosetSchemaNamespace, 'particle')
      ct.addChunkedChild(particle)
      particle.addChunkedChild(self.model.reflect(particle))
    else:
      ct.reflectString(ct, 'variety','simple',0,infosetSchemaNamespace)
      st=Element(ct, infosetSchemaNamespace, 'simpleTypeDefinition')
      ct.addChunkedChild(st)
      st.addChunkedChild(self.model.reflect(st))
      ct.reflectNull(ct,'particle',infosetSchemaNamespace)
  else:
    parent.reflectNull(parent,'contentType',infosetSchemaNamespace)

XMLSchema.ComplexType.contentTypeReflect=ctReflect

def attrsReflect(self,parent):
  rel=Element(parent,infosetSchemaNamespace,'attributeUses')
  parent.addChunkedChild(rel)
  for au in self.attributeDeclarations.values():
    if isinstance(au.attributeDeclaration,XMLSchema.Attribute):
      rel.addChunkedChild(au.reflect(rel))

XMLSchema.ComplexType.attributesReflect=attrsReflect

def awReflect(self,parent):
  wc=None
  for ad in self.attributeDeclarations.values():
    if isinstance(ad.attributeDeclaration,XMLSchema.Wildcard):
      wc=ad.attributeDeclaration
      break
  if wc:
    wcp=Element(parent,infosetSchemaNamespace,'attributeWildcard')
    parent.addChunkedChild(wcp)
    wcp.addChunkedChild(wc.reflect(wcp))
  else:
    parent.reflectNull(parent,'attributeWildcard',infosetSchemaNamespace)

XMLSchema.ComplexType.attributeWildcardReflect=awReflect

def selReflect(self,parent):
  selp=Element(parent,infosetSchemaNamespace,'selector')
  parent.addChunkedChild(selp)
  parent=selp
  selp=Element(parent,infosetSchemaNamespace,'xpath')
  parent.addChunkedChild(selp)
  selp.reflectString(selp, 'path',self.xrpr.selector.xpath,0,infosetSchemaNamespace)

XMLSchema.Kcons.selectorReflect=selReflect

def referReflect(self,parent):
  parent.reflectPointer('referencedKey', self.refer)

XMLSchema.Kcons.referReflect=referReflect


def fsReflect(self,parent):
  fsp=Element(parent,infosetSchemaNamespace,'fields')
  parent.addChunkedChild(fsp)
  for f in self.xrpr.fields:
    xp=Element(parent,infosetSchemaNamespace,'xpath')
    fsp.addChunkedChild(xp)
    xp.reflectString(xp, 'path',f.xpath,0,infosetSchemaNamespace)

XMLSchema.Kcons.fieldsReflect=fsReflect

def ptReflect(self,parent):
  if self.primitiveType:
    parent.reflectPointer('primitiveTypeDefinition',self.primitiveType.name)
  else:
    parent.reflectNull(parent,'primitiveTypeDefinition',infosetSchemaNamespace)

XMLSchema.SimpleType.primitiveTypeReflect=ptReflect

def facetsReflect(self,parent):
  ff=Element(parent,infosetSchemaNamespace,"facets")
  parent.addChunkedChild(ff)
  if not self.primitiveType or self.primitiveType==self:
    # XSV bug, facets not present on lists or unions
    return
  for fn in self.primitiveType.allowedFacets:
    facet=self.primitiveType.facets[fn]
    fval=facet and facet.value
    if fval:
      f=Element(ff,infosetSchemaNamespace,fn)
      ff.addChunkedChild(f)
      f.reflectString(f,"value",str(fval),
                         0,infosetSchemaNamespace)
      f.reflectBoolean(f,"fixed",facet.fixed,
                       0,infosetSchemaNamespace)
      if facet.annotation:
          rel=Element(f,infosetSchemaNamespace,'annotation')
          f.addChunkedChild(rel)
          rel.addChunkedChild(facet.annotation.reflect(rel))
      else:
        f.reflectNull(f,'annotation',infosetSchemaNamespace)

XMLSchema.SimpleType.facetsReflect=facetsReflect

def fundamentalFacetsReflect(self,parent):
  ff=Element(parent,infosetSchemaNamespace,"fundamentalFacets")
  parent.addChunkedChild(ff)
  # XXX

XMLSchema.SimpleType.fundamentalFacetsReflect=fundamentalFacetsReflect

def finalReflect(self,parent):
  ff=Element(parent,infosetSchemaNamespace,"final")
  parent.addChunkedChild(ff)
  # XXX

XMLSchema.SimpleType.finalReflect=finalReflect

def elementReflect(self, parent=None):
#  sys.stderr.write("using new reflect on %s, %s\n" % (self,parent));
#  sys.stderr.write("%s" % self.__dict__);
  if self.schemaInformation:
    # we are a validation start, so we need an ID _before_ recursion
    self.id=gensym().id                          # for others to point to
    # we need to build all the top-level defns also
    info = Element(parent, infosetSchemaNamespace, "schemaInformation")
    for i in self.schemaInformation:
      info.addChunkedChild(i.reflect(info))

  element = self.oldReflect(parent,not self.schemaNormalizedValue)

  if self.schemaInformation:
    element.addAttribute(Attribute(element, None, "id", None, self.id))
    element.addChunkedChild(info)
    info.parent=element
  else:
    self.reflectNull(element, "schemaInformation", infosetSchemaNamespace)

  self.reflectString(element, "validationAttempted",
                     self.validationAttempted, 1,
                       infosetSchemaNamespace)

  if self.validationContext:
    element.reflectPointer("validationContext",self.validationContext.id)
  else:
    self.reflectNull(element,"validationContext",infosetSchemaNamespace)

  self.reflectString(element, "validity", self.validity, 1,
                       infosetSchemaNamespace)

  errorCode = Element(element, infosetSchemaNamespace, "schemaErrorCode")
  element.addChunkedChild(errorCode)
  if self.errorCode:
    for err in self.errorCode:
      errorCode.addChunkedChild(Characters(errorCode, err))
  else:
    nullAttr = Attribute(errorCode, xsiNamespace, "nil", None, "true")
    errorCode.addAttribute(nullAttr)

  self.reflectString(element, "schemaNormalizedValue", self.schemaNormalizedValue, 1,
                       infosetSchemaNamespace)

  self.reflectNull(element, "schemaSpecified", infosetSchemaNamespace) # XXX
  
  if self.typeDefinition:         # XXX
    typeDefinition = Element(element, infosetSchemaNamespace, "typeDefinition")
    element.addChunkedChild(typeDefinition)
    typeDefinition.addChunkedChild(self.typeDefinition.reflect(typeDefinition))
  else:
    self.reflectNull(element, "typeDefinition",
                       infosetSchemaNamespace)

  self.reflectString(element, "memberTypeDefinition", self.memberTypeDefinition, 1,
                       infosetSchemaNamespace)

#    self.reflectString(element, "typeDefinitionType", self.typeDefinitionType, 1)

#    self.reflectString(element, "typeDefinitionNamespace",
#                       self.typeDefinitionNamespace, 1,
#                       infosetSchemaNamespace)

#    self.reflectBoolean(element, "typeDefinitionAnonymous",
#                        self.typeDefinitionAnonymous, 1,
#                       infosetSchemaNamespace)

#    self.reflectString(element, "typeDefinitionName", self.typeDefinitionName, 1,
#                       infosetSchemaNamespace)

#    self.reflectString(element, "memberTypeDefinitionNamespace",
#                       self.memberTypeDefinitionNamespace, 1,
#                       infosetSchemaNamespace)

#    self.reflectBoolean(element, "memberTypeDefinitionAnonymous",
#                        self.memberTypeDefinitionAnonymous, 1,
#                       infosetSchemaNamespace)

#    self.reflectString(element, "memberTypeDefinitionName",
#                       self.memberTypeDefinitionName, 1,
#                       infosetSchemaNamespace)

  if self.elementDeclaration:
    ee = Element(element, infosetSchemaNamespace, "declaration")
    element.addChunkedChild(ee)
    ee.addChunkedChild(self.elementDeclaration.reflect(ee))
  else:
    self.reflectNull(element, "declaration",
                       infosetSchemaNamespace)

  self.reflectBoolean(element, "nil", self.null, 1,
                       infosetSchemaNamespace)


  self.reflectNull(element, "notation", infosetSchemaNamespace) # XXX
  self.reflectNull(element, "idIdrefTable", infosetSchemaNamespace) # XXX
  self.reflectNull(element, "identityConstraintTable", infosetSchemaNamespace) # XXX
  
  return element

Element.psvReflect = elementReflect

class gensym:
  
  nextid = 1

  def __init__(self):
    self.id = "g%s" % gensym.nextid
    gensym.nextid = gensym.nextid + 1

def attributeReflect(self, parent=None):
#  sys.stderr.write("using new reflect on %s, %s\n" % (self,parent));
#  sys.stderr.write("%s" % self.__dict__);
  attribute = self.oldReflect(parent)

  self.reflectString(attribute, "validationAttempted",
                     self.validationAttempted, 1,
                       infosetSchemaNamespace)

  if self.validationContext:
    attribute.reflectPointer("validationContext",self.validationContext.id)
  else:
    self.reflectNull(attribute,"validationContext",infosetSchemaNamespace)

  self.reflectString(attribute, "validity", self.validity, 1,
                       infosetSchemaNamespace)

  errorCode = Element(attribute, infosetSchemaNamespace, "schemaErrorCode")
  attribute.addChunkedChild(errorCode)
  if self.errorCode:
    for err in self.errorCode:
      errorCode.addChunkedChild(Characters(errorCode, err))
  else:
    nullAttr = Attribute(errorCode, xsiNamespace, "nil", None, "true")
    errorCode.addAttribute(nullAttr)

  self.reflectString(attribute, "schemaNormalizedValue", self.schemaNormalizedValue, 1,
                       infosetSchemaNamespace)

  self.reflectNull(attribute, "schemaSpecified", infosetSchemaNamespace) # XXX
  
  if self.typeDefinition:         # XXX
    typeDefinition = Element(attribute,
                             infosetSchemaNamespace, "typeDefinition")
    attribute.addChunkedChild(typeDefinition)
    typeDefinition.addChunkedChild(self.typeDefinition.reflect(typeDefinition))
  else:
    self.reflectNull(attribute, "typeDefinition",
                       infosetSchemaNamespace)

  self.reflectString(attribute, "memberTypeDefinition", self.memberTypeDefinition, 1,
                       infosetSchemaNamespace)

#    self.reflectString(attribute, "typeDefinitionType", self.typeDefinitionType, 1,
#                       infosetSchemaNamespace)

#    self.reflectString(attribute, "typeDefinitionNamespace",
#                       self.typeDefinitionNamespace, 1,
#                       infosetSchemaNamespace)

#    self.reflectBoolean(attribute, "typeDefinitionAnonymous",
#                        self.typeDefinitionAnonymous, 1,
#                       infosetSchemaNamespace)

#    self.reflectString(attribute, "typeDefinitionName", self.typeDefinitionName, 1,
#                       infosetSchemaNamespace)

#    self.reflectString(attribute, "memberTypeDefinitionNamespace",
#                       self.memberTypeDefinitionNamespace, 1,
#                       infosetSchemaNamespace)

#    self.reflectBoolean(attribute, "memberTypeDefinitionAnonymous",
#                        self.memberTypeDefinitionAnonymous, 1,
#                       infosetSchemaNamespace)

#    self.reflectString(attribute, "memberTypeDefinitionName",
#                       self.memberTypeDefinitionName, 1,
#                       infosetSchemaNamespace)

  if self.attributeDeclaration:
    aa = Element(attribute, infosetSchemaNamespace, "declaration")
    attribute.addChunkedChild(aa)
    aa.addChunkedChild(self.attributeDeclaration.reflect(aa))
  else:
    self.reflectNull(attribute, "declaration",
                       infosetSchemaNamespace)

  return attribute

Attribute.psvReflect = attributeReflect

Element.oldReflect = Element.reflect
Element.reflect = Element.psvReflect

Element.validationAttempted = None
Element.validationContext = None
Element.validity = None
Element.errorCode = None
Element.schemaNormalizedValue = None
Element.typeDefinition = None
Element.memberTypeDefinition = None
Element.typeDefinitionType = None
Element.typeDefinitionNamespace = None
Element.typeDefinitionAnonymous = None
Element.typeDefinitionName = None
Element.memberTypeDefinitionNamespace = None
Element.memberTypeDefinitionAnonymous = None
Element.memberTypeDefinitionName = None
Element.elementDeclaration = None
Element.null = 0
Element.schemaInformation = None

Attribute.oldReflect = Attribute.reflect
Attribute.reflect = Attribute.psvReflect

Attribute.validationAttempted = None
Attribute.validationContext = None
Attribute.validity = None
Attribute.errorCode = None
Attribute.schemaNormalizedValue = None
Attribute.typeDefinition = None
Attribute.memberTypeDefinition = None
Attribute.typeDefinitionType = None
Attribute.typeDefinitionNamespace = None
Attribute.typeDefinitionAnonymous = None
Attribute.typeDefinitionName = None
Attribute.memberTypeDefinitionNamespace = None
Attribute.memberTypeDefinitionAnonymous = None
Attribute.memberTypeDefinitionName = None
Attribute.attributeDeclaration = None
