# Copyright (C) 2000 LTG -- See accompanying COPYRIGHT and COPYING files
# Read, validate and build application-orientated data structures from
# an XML instance
# $Id: layer.py,v 1.37 2001/06/04 16:09:22 ht Exp $

# Data structures are controlled by a pair of input tables for now,
# should be derived from schema annotations

Element=None
Attribute=None
GetAttrVal=None
import XML                       # use map source='XML'
import PyLTXML                     # use map source='PyLTXML'
import XMLInfoset                       # use map source='XMLInfoset'
import LTXMLInfoset                       # use map source='XMLInfoset'
import sys
import types
import string

def mapIt(elt,source,parent=None):
  if source=='PyLTXML':
    if XML.somestring(type(elt)):
      return elt
    else:
      return PyLTXMLMapper(elt,parent)
  elif source=='XMLInfoset':
    if isinstance(elt,XMLInfoset.Characters):
      return elt.characters
    else:
      return XMLIMapper(elt,parent)
  else:
    if elt.__class__==XML.Pcdata:
      return elt.value
    else:
      return XMLMapper(elt,parent)

class Mapper:
  pass

class PyLTXMLMapper(Mapper):
  def __init__(self,elt):
    self.elt=elt
    if type(elt)==PyLTXML.ItemType:
      self.type='ItemType'
    elif type(elt)==types.TupleType:
      self.type='AttributeType'
    else:
      self.type=None

  def __getattr__(self,name):
    if self.type=='ItemType':
      if name=='actualAttrs':
        self.actualAttrs=map(lambda a:mapIt(a,'PyLTXML'),
                             PyLTXML.ItemActualAttributes(self.elt))
        return self.actualAttrs
      elif name=='data':
        self.data=map(lambda e,p=self:mapIt(e,'PyLTXML',p),self.elt.data)
        return self.data
      elif name=='where':
        return (0,0,0,0)
      elif name=='namespaceDict':
        return self.elt.nsdict
      else:
        return getattr(self.elt,name)
    elif self.type=='AttributeType':
      if name=='name':
        return self.elt[0]
      elif name=='label':
        return self.elt[0]
      elif name=='value':
        return self.elt[1]
      else:
        raise AttributeError,name

  def attrVal(self,name):
    val=PyLTXML.GetAttrVal(self.elt,name)
    if val==None:
      raise AttributeError,name
    else:
      return val

  def hasAttrVal(self,name):
    return PyLTXML.GetAttrVal(self.elt,name)!=None

  def lookupPrefix(self,prefix):
    return PyLTXML.LookupPrefix(self.elt,prefix)

class XMLMapper(Mapper):
  def __init__(self,elt,parent=None):
    self.elt=elt
    self.parent=parent
    type=elt.__class__
    if type==XML.Element:
      self.type='ItemType'
    elif type==XML.Attribute:
      self.type='AttributeType'
    else:
      self.type=None

  def __getattr__(self,name):
    if self.type=='ItemType':
      if name=='label':
        return self.elt.name
      elif name=='llabel':
        return self.elt.local
      elif name=='nsuri':
        return self.elt.uri
      elif name=='data':
        self.data=map(lambda e,p=self:mapIt(e,'XML',p),self.elt.children)
        return self.data
      elif name=='actualAttrs':
        self.actualAttrs=map(lambda a:mapIt(a,'XML'),self.elt.attrs.values())
        return self.actualAttrs
      elif name=='where':
        return self.elt.where
      elif name=='namespaceDict':
        return self.elt.nsdict
      else:
        raise AttributeError,name
    elif self.type=='AttributeType':
      if name=='label':
        return self.elt.name
      else:
        return getattr(self.elt,name)

  def lookupPrefix(self,prefix):
    if self.elt.nsdict.has_key(prefix):
      return self.elt.nsdict[prefix]
    else:
      return None

  def attrVal(self,name):
    return self.elt.attrs[name].value

  def hasAttrVal(self,name):
    return self.elt.attrs.has_key(name)

class XMLIMapper(Mapper):
  def __init__(self,elt,parent=None):
    self.elt=elt
    self.parent=parent
    type=elt.__class__
    if type==XMLInfoset.Element:
      self.type='ItemType'
    elif type==XMLInfoset.Attribute:
      self.type='AttributeType'
    else:
      self.type=None

  def __getattr__(self,name):
    if self.type=='ItemType':
      if name=='label':
        if self.elt.prefix:
          self.label='%s:%s'%(self.elt.prefix,self.elt.localName)
        else:
          self.label=self.elt.localName
        return self.label
      elif name=='llabel':
        return self.elt.localName
      elif name=='nsuri':
        return self.elt.namespaceName
      elif name=='data':
        self.data=map(lambda e,p=self:mapIt(e,'XMLInfoset',p),
                      self.elt.chunkedChildren)
        return self.data
      elif name=='actualAttrs':
        self.actualAttrs=map(lambda a:mapIt(a,'XMLInfoset'),
                             self.elt.attributes.values())
        return self.actualAttrs
      elif name=='where':
        return self.elt.where
      elif name=='namespaceDict':
        self.namespaceDict={}
        if self.elt.inScopeNamespaces:
          for (n,v) in self.elt.inScopeNamespaces.items():
            self.namespaceDict[n]=v.namespaceName
        return self.namespaceDict
      else:
        raise AttributeError,name
    elif self.type=='AttributeType':
      if name=='label':
        if self.elt.prefix:
          self.label='%s:%s'%(self.elt.prefix,self.elt.localName)
        else:
          self.label=self.elt.localName
        return self.label
      elif name=='llabel':
        return self.elt.localName
      elif name=='nsuri':
        return self.elt.namespaceName
      elif name=='value':
        return self.elt.normalizedValue
      else:
        raise AttributeError,name
    else:
      raise AttributeError,name

  def lookupPrefix(self,prefix):
    if self.namespaceDict.has_key(prefix):
      return self.namespaceDict[prefix]
    else:
      return None

  def attrVal(self,name):
    return self.elt.attributes[(None,name)].normalizedValue

  def hasAttrVal(self,name):
    return self.elt.attributes.has_key((None,name))

class factory:
  def fromFile(self,eltDispatchTable,attrDispatchTable,classLookup,
	       defaultEltDispatch=None,defaultAttrDispatch=None,
	       defaultNamespace=None,filename=None,docElt=None,
               DTDfile=None,otherDefaultDispatch=None):
    self.edt=eltDispatchTable
    self.adt=attrDispatchTable
    self.defEd=defaultEltDispatch
    self.defAd=defaultAttrDispatch
    self.defNS=defaultNamespace
    self.classLookup=classLookup
    self.oDD=otherDefaultDispatch
    if not filename:
      return
    if filename[1]==':':
      filename='file:///'+filename
    if DTDfile:
      if type(DTDfile)==types.StringType:
        try:
          dFile=PyLTXML.Open(DTDfile,
                             PyLTXML.NSL_read|PyLTXML.NSL_read_namespaces)
        except:
          return
      else:
        # better be the real thing
        dFile=DTDfile
      docType=dFile.doctype
    else:
      docType=None
    try:
      flags=(PyLTXML.NSL_read|PyLTXML.NSL_read_validate|
             PyLTXML.NSL_read_namespaces|
             PyLTXML.NSL_read_relaxed_any|
             PyLTXML.NSL_read_allow_undeclared_nsattributes|
             PyLTXML.NSL_read_defaulted_attributes)
    except:
      flags=(PyLTXML.NSL_read|PyLTXML.NSL_read_validate|
             PyLTXML.NSL_read_namespaces|
             PyLTXML.NSL_read_defaulted_attributes)
    try:
      file=PyLTXML.Open(filename,docType,flags)
    except:
      return
    # XML.py version mapIt(XML.Element(file,1),'XML')
    try:
      root=mapIt(LTXMLInfoset.documentFromFile(file).documentElement,
                 'XMLInfoset')  # builds the whole document tree
    except:
      root=None
    try:
      if root and file.seenValidityError:
        root=None
    except:
      pass
    if not root:
      res=None
    else:
      if (docElt and
          ((root.llabel or root.label)==docElt[1] and
           root.nsuri==docElt[0])):
  # replace from here
  #    bit=PyLTXML.GetNextBit(file)
  #    while bit.type!='start':
  #      bit=PyLTXML.GetNextBit(file)
  #    root=mapIt(PyLTXML.ItemParse(file,bit.item),'PyLTXML')
  # to here
        res=self.processElement(root)
      else:
        res=None
    try:
      PyLTXML.Close(file)
      if DTDfile:
        PyLTXML.Close(dFile)
    except:
      pass
    return res

  def processElement(self,elt):
    lab=elt.llabel or elt.label
    fullName=None
    act=None
    key=None
    if self.edt.has_key(lab) and ((not self.defNS) or
				  self.defNS==elt.nsuri):
      act=self.edt[lab]
    elif elt.nsuri:
      fullName="%s %s"%(elt.nsuri,lab)
      if self.edt.has_key(fullName):
	act=self.edt[fullName]
    if not act:
      if elt.parent:
        plab=elt.parent.llabel or elt.parent.label
      else:
        plab=None
      if plab:
	key=(plab,lab)
	if self.edt.has_key(key) and ((not self.defNS) or
				  self.defNS==elt.nsuri):
	  act=self.edt[key]
	elif elt.nsuri and elt.parent.nsuri:
	  fullPName="%s %s"%(elt.parent.nsuri,plab)
	  key=(fullPName,fullName)
	  if self.edt.has_key(key):
	    act=self.edt[key]
    if act=='error':
      if key:
        cc=' here'
      else:
        cc=''
      error("Element '%s' not allowed%s"%(lab,cc),elt)
      return
    if not act:
      if (not self.defNS) or self.defNS==elt.nsuri:
        defAct=self.defEd
      else:
        defAct=self.oDD
      if defAct:
	if defAct=='error':
	  error("Unknown element: %s"%lab,elt)
	elif defAct=='warning':
	  sys.stderr.write("Unknown element: %s"%lab)
	  return
	else:
	  act=defAct
      else:
	return
    # get content
    # oops, what about defaulted attrs
    ares=map(lambda a,e=elt,s=self:s.processAttr(a,e),elt.actualAttrs)
    eres=[]
    avp=(act in ('variable','list'))
    if act in ('instance','self'):
      res=self.classLookup(lab)(self,elt)
    elif type(act)==types.TupleType:
      if act[0]=='type':
	res=act[1](self,elt)
      elif act[0] in ('group','alias'):
	# ex-post-facto list or rename
	res=self.classLookup(lab)(self,elt)
      elif act[0]=='table':
	# ex-post-facto table
	avp=1
      elif act[0]=='variable':
        # one-off labelled instance
	res=act[1](self,elt)
      else:
	error("bad compound action: %s"%act[0],elt)
    for d in elt.data:
      if not XML.somestring(type(d)):
	eres.append(self.processElement(d))
    if avp:
      # assume ares is [] and eres of length 1
      if eres:
	res=eres[0]
      # maybe there's text
      elif len(elt.data)==1 and XML.somestring(type(elt.data[0])):
	res=elt.data[0]
      else:
	res=""
      if act=='variable':
	return (lab,res)
      elif act=='list':
	# assume ares to be []
	return ((lab,),res)
      else:
	# subordinate table
	return ((act[1],),lab,res)
    elif act=='table':
      res={}
      for p in eres:
	if type(p)==types.TupleType:
	  self.addVar(res,p,elt)
      for p in ares:
	if type(p)==types.TupleType:
	  self.addVar(res,p,elt)
      return (lab,res)
    else:
      if act not in ('instance','self') and type(act)!=types.TupleType:
	error("bad action: %s"%act,elt)
      for p in ares:
	if type(p)==types.TupleType:
	  self.addVar(res.__dict__,p,elt)
      for p in eres:
	if type(p)==types.TupleType:
	  self.addVar(res.__dict__,p,elt)
      if hasattr(res,'init'):
	res.init(elt)
      if act=='instance':
	return res
      elif act=='self':
        return (lab,res)
      elif act[0]=='group':
	return ((act[1],),res)
      elif act[0]=='alias':
	return (act[1],res)
      else:
	return (lab,res)
	
  def addVar(self,dict,p,inElt):
    if type(p[0])==types.TupleType:
      # uni-tuple indicating group or table
      key=p[0][0]
      if dict.has_key(key):
	ent=dict[key]
	if type(ent)==types.ListType:
	  ent.append(p[1])
	else:
	  if ent.has_key(p[1]):
	    error("multiple %s elements not allowed here"%p[1],inElt)
	  ent[p[1]]=p[2]
      else:
	dict[key]=[p[1]]
    else:
      if dict.has_key(p[0]):
	error("multiple %s elements not allowed here"%p[0],inElt)
      dict[p[0]] = p[1]

  def processAttr(self,attr,elt):
    name=attr.label
    if self.adt.has_key(name):
      act=self.adt[name]
    elif self.defAd:
      if self.defAd=='error':
	error("Unknown attribute: %s"%name,elt)
      elif self.defAd=='warning':
	sys.stderr.write("Unknown attribute: %s"%name)
	return
      else:
	act=self.defAd
    else:
      return
    if act=='instance':
      error("shouldn't happen?",elt)
    elif act=='variable':
      return (name,attr.value)
    elif act=='list':
      return (name,string.split(attr.value))

class LayerError(Exception):
  def __init__(self,arg):
    Exception.__init__(self,arg)

def error(msg,elt):
  w=elt.where
  if w and w[3]!=0:
     loc=(" in %s at line %d char %d of %s" % w)
  else:
    loc=" location unknown"
  raise LayerError,msg+loc

# $Log: layer.py,v $
# Revision 1.37  2001/06/04 16:09:22  ht
# Add and switch to a basis of XMLInfoset items, with new XMLIMapper
# Add and use in all three a namespaceDict property
# Retrofit a 'label' property for attrs and use it in one place
#
# Revision 1.36  2000/12/21 18:28:54  ht
# add extra handling of other-namespace elts
#
# Revision 1.35  2000/10/27 16:36:59  ht
# work w/o new PyLTXML
#
# Revision 1.34  2000/10/27 14:07:43  ht
# remove use of Tk to find input if not supplied
# add doctype file arg't to fromFile: if present, check validity
#
# Revision 1.33  2000/10/17 13:14:28  ht
# fix error logging in addVar
#
# Revision 1.32  2000/09/03 13:42:39  ht
# allow "error" as act, add line/char to error message if pos.
#
# Revision 1.31  2000/07/05 09:05:37  ht
# change name to PyLTXML
#
# Revision 1.30  2000/07/04 09:53:44  ht
# add context to error about multiple definitions
#
# Revision 1.29  2000/05/13 11:42:35  ht
# catch more source opening errors
#
# Revision 1.28  2000/05/11 14:14:33  ht
# add docElt argument, enforce it
#
# Revision 1.27  2000/05/10 15:28:26  ht
# better error behaviour
#
# Revision 1.26  2000/05/09 14:52:52  ht
# Check for strings in a way that works with or without 16-bit support
#
# Revision 1.25  2000/04/26 13:00:40  ht
# add copyright
#
# Revision 1.24  2000/04/20 22:14:46  ht
# dont rely on tk
#
# Revision 1.23  2000/04/20 08:43:41  ht
# add hasAttrVal,
#  make attrVal and lookupPrefix more consistent in face of missing info
#
# Revision 1.22  2000/04/16 16:44:17  ht
# make self work as self-labelled instance
#
# Revision 1.21  2000/03/25 12:08:55  ht
# allow for either PyXML or XML as parser
#
# Revision 1.20  2000/02/08 11:45:40  ht
# add support for aliasing
#
# Revision 1.19  2000/01/18 14:38:03  ht
# add 'variable' compound action
#
# Revision 1.18  2000/01/03 14:12:37  ht
# Add Id and Log
# define error to raise a LayerError exception
#
