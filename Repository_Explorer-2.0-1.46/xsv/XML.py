# Copyright (C) 2000 LTG -- See accompanying COPYRIGHT and COPYING files
from PyLTXML import *
from string import *
import sys
import types

class Pcdata:

  def __init__(self, value, where=None):
    self.value = value
    self.where = where

  def __getattr__(self,name):
    # infoset-like slot aliases
    if name == 'characters':
      return self.value
    else:
      raise AttributeError, name
    
  def printme(self, file):
    Print(file, self.value)

class Attribute:
  cv=None
  def __init__(self, name, value, spec=None, uri=None, local=None):
    self.name = name
    self.value = value
    self.spec = None
    self.uri=uri
    self.local=local

  def __getattr__(self,name):
    # infoset-like slot aliases
    if name == 'originalName':
      return self.name
    elif name == 'localName':
      return self.local
    elif name == 'namespaceName':
      return self.uri
    elif name == 'normalizedValue':
      return self.value
    else:
      raise AttributeError, name
    
  def printme(self, file):
    if self.value!=None:
      if self.cv:
        val=self.cv
      else:
        val=self.value
        if "&" in val:
          val=replace(val,"&","&amp;")
        if "'" in val:
          val=replace(val,"'","&apos;")
        if "<" in val:
          val=replace(val,"<","&lt;")
        self.cv=val
      PrintTextLiteral(file, " %s='%s'" % (self.name, val))

class Element:

  def __init__(self, arg1, arg2=None, arg3=None):
    self.attrs = {}
    self.nsattrs = {}
    self.children = []

    if somestring(type(arg1)):
      self.fromString(arg1)
    elif type(arg1) == BitType: 
      self.fromBit(arg1, arg2, arg3)
    elif type(arg1) == ItemType:
      self.fromItem(arg1, arg2)
    elif type(arg1) == FileType:
      self.fromFile(arg1, arg2)
  
  def __getattr__(self,name):
    # infoset-like slot aliases
    if name == 'originalName':
      return self.name
    elif name == 'localName':
      return self.local
    elif name == 'namespaceName':
      return self.uri
    elif name == 'attributes':
      return self.nsattrs
    elif name == 'chunkedChildren':
      return self.children
    else:
      raise AttributeError, name
    
  def fromString(self, name):
    self.name = name
    self.spec = None
    self.local = None
    self.uri = None
    return

  def fromBit(self, bit, file, where):
    doctype = file.doctype
    if doctype.elementTypes.has_key(bit.label):
      self.spec = doctype.elementTypes[bit.label]
    else:
      self.spec = None
    self.name = bit.label
    self.local = bit.llabel
    self.uri = bit.nsuri
    self.nsdict = bit.item.nsdict
    self.getAttrs(bit.item)
    self.where = where
    if bit.type == "empty":
      if where:
	self.where2 = file.where
      else:
	self.where2 = None
      return
    if where:
      w = file.where
    else:
      w = None
    b = GetNextBit(file)
    while b.type != "end":
      if  b.type == error:
	raise Exception, "parse error"
      if b.type == "start" or b.type == "empty":
	self.children.append(Element(b, file, w))
      elif b.type == "text":
	self.children.append(Pcdata(b.body, w))
      if where:
	w = file.where
      b = GetNextBit(file)
    self.where2 = w
    return

  def fromItem(self, item, doctype):
    if doctype.elementTypes.has_key(item.label):
      self.spec = doctype.elementTypes[item.label]
    else:
      self.spec = None
    self.name = item.label
    self.local = item.llabel
    self.uri = item.nsuri
    self.nsdict = item.nsdict
    self.getAttrs(item)

    for child in item.data:
      if type(child) == ItemType:
	self.children.append(Element(child, doctype))
      else:
	self.children.append(Pcdata(child))

  def fromFile(self, file, notewhere):
    if notewhere:
      w = file.where
    else:
      w = None
    b = GetNextBit(file)
    while b:
      if b.type != "start" and b.type != "empty":
        if  b.type == error:
          raise Exception, "parse error"
      else:
        if notewhere:
          w = file.where
        self.fromBit(b, file, w)
        return
      b = GetNextBit(file)
    # if we fall through the file was broken
    raise Exception, "I/O error/empty file"

  def getAttrs(self, item):
    atts = ItemActualAttributesNS(item)
    for (name,value,uri,local) in atts:
      if self.spec and self.spec.attrDefns.has_key(name):
        a = Attribute(name, value, self.spec.attrDefns[name], uri, local)
      else:
        a = self.attrs[name] = Attribute(name, value, None, uri, local)
      self.attrs[name] = a
      self.nsattrs[(uri,local)] = a
      
  def printme(self, file, notElementOnly=0):
    PrintTextLiteral(file, "<%s" % self.name)
    ans=self.attrs.keys()
    ans.sort()
    for an in ans:
      self.attrs[an].printme(file)
    if not self.children:
      PrintTextLiteral(file,"/>")
      return
    PrintTextLiteral(file, ">")
    eo=1
    if notElementOnly:
      eo=0
    else:
      for child in self.children:
        if isinstance(child,Pcdata):
          eo=0
          break
    if eo:
      PrintTextLiteral(file,"\n")
    for child in self.children:
      child.printme(file)
      if eo:
        PrintTextLiteral(file,"\n")
    PrintTextLiteral(file, "</%s>" % self.name)

  def addAttr(self,name,value):
    self.attrs[name]=Attribute(name,value)

if types.__dict__.has_key('UnicodeType'):
  def somestring(type):
   return type==types.StringType or type==types.UnicodeType
else:
  def somestring(type):
    return type==types.StringType

