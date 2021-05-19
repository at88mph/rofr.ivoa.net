import string
from XMLInfoset import *
from PyLTXML import *

def documentFromURI(uri):
  file = Open(uri, NSL_read+NSL_read_all_bits+NSL_read_namespaces+NSL_read_no_consume_prolog)
  doc = documentFromFile(file)
  Close(file)
  return doc

def documentFromFile(file):
  d = Document(None)
  # d.baseURI = file.baseURI  XXX
  ents = file.doctype.entities
  for ename in ents.keys():
    pass                                # for now
#  docent = EntityDeclaration("DocumentEntity", None, None, None, None, None, None, file.doctype.xencoding)
#  d.addEntityDeclaration(docent)
  w = file.where
  b = GetNextBit(file)
  while b:
    if  b.type == "bad":
      raise Exception, "parse error"
    elif b.type == "pi":
      pass                            # XXX
    elif b.type == "comment":
      pass                            # XXX
    elif b.type == "start" or b.type == "empty":
      d.addChild(elementFromBit(d, b, file, w))
      return d
    w = file.where
    b = GetNextBit(file)
  # shouldn't be possible to fall out without errror
  raise Exception, "oops, ran off end of XML file"

def elementFromBit(parent, bit, file, w):
  nsname = bit.nsuri
  localname = bit.llabel
  colon = string.find(bit.label, ':')
  if colon >= 0:
    prefix = bit.label[0:colon]
  else:
    prefix = None
  baseuri = w[3]                        # XXX change when xml:base implemented
  inscopens = {}
  for n in bit.item.nsdict.keys():
    inscopens[n] = Namespace(n, bit.item.nsdict[n]) # XXX
  declns = None                         # XXX
  if file.doctype.elementTypes.has_key(bit.label):
    spec = file.doctype.elementTypes[bit.label]
  else:
    spec = None
  e = Element(parent, nsname, localname, prefix, baseuri, inscopens)
  e.originalName = bit.label
  e.where = w                           # position of start tag
  
  atts = ItemActualAttributesNS(bit.item)
  for (aname,avalue,ansname,alocalname) in atts:
    colon = string.find(aname, ':')
    if colon >= 0:
      prefix = aname[0:colon]
    else:
      prefix = None
    a = Attribute(e, ansname, alocalname, prefix, avalue)
    e.addAttribute(a)
    a.originalName = aname
  if spec:
    defaulted_attrs = []
    for aspec in spec.attrDefns.values(): # find types and default values
      for a in e.attributes.values():
        if a.originalName == aspec.name:
          a.attributeType = aspec.type
          if aspec.defType == "NONE" or aspec.defType == "#FIXED":
            a.default = aspec.defValue
          break
      else:
        # attribute not present, see if there is a default
        # (but not for namespace attrs, they have already been handled)
        if aspec.name == "xmlns" or aspec.name[0:6] == "xmlns:":
          pass
        elif aspec.defType == "NONE" or aspec.defType == "#FIXED":
          a = makeDefaultAttribute(e, aspec, inscopens)
          if a:
            defaulted_attrs.append(a)
    for a in defaulted_attrs:
      e.addAttribute(a)

  if bit.type == "empty":
    e.where2 = e.where
    return e

  w = file.where
  b = GetNextBit(file)
  while b.type != "end":
    if  b.type == "bad":
      raise Exception, "parse error"
    elif b.type == "pi":
      pass                            # XXX
    elif b.type == "comment":
      pass                            # XXX
    elif b.type == "start" or b.type == "empty":
      e.addChunkedChild(elementFromBit(e, b, file, w))
    elif b.type == "text":
      t = Characters(e, b.body, (spec or 0) and spec.type == "ELEMENT")
      t.where = w
      e.addChunkedChild(t)
    w =  file.where
    b = GetNextBit(file)

  e.where2 = w                          # position of end tag
  
  return e

def makeDefaultAttribute(element, spec, inscopens):
  parts = string.split(spec.name, ":")
  nparts = len(parts)
  if nparts > 2 or parts[0] == "" or (nparts == 2 and parts[1] == ""):
    return None                         # ignore namespace-bad attrs
  if nparts == 2:
    prefix = parts[0]
    local = parts[1]
    if not inscopens.has_key(prefix):
      return None                       # ditto (parser will have reported it)
    ns = inscopens[prefix].namespaceName
  else:
    local = parts[0]
    ns = None
    prefix = None
  a = Attribute(element, ns, local, prefix, spec.defValue, 0, spec.type)
  a.originalName = spec.name
  return a
