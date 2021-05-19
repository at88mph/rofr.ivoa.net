import string

infosetSchemaNamespace = "http://www.w3.org/2001/05/XMLInfoset"
xsiNamespace = "http://www.w3.org/2001/XMLSchema-instance"

class InformationItem:

  def printme(self, file, namespaces={}):
    file.write("<!-- XXX %s XXX -->" % self)

  def reflect(self, parent=None):
    return Element(parent, infosetSchemaNamespace, "XXX")

  def reflectString(self, parent, name, value, nullable, ns=None):
#    sys.stderr.write("reflecting string %s, nullable=%s\n" % (value, nullable))
    if nullable and value == None:
      return self.reflectNull(parent, name, ns)
    else:
      e = Element(parent, ns or infosetSchemaNamespace, name)
      parent.addChunkedChild(e)
      if len(value) > 0:
        e.addChunkedChild(Characters(e, value))
      return e

  def reflectNull(self, parent, name, ns=None):
    e = Element(parent, ns or infosetSchemaNamespace, name)
    parent.addChunkedChild(e)
    nullAttr = Attribute(e, xsiNamespace, "nil", None, "true")
    e.addAttribute(nullAttr)
    
  def reflectBoolean(self, parent, name, value, nullable, ns=None):
#    sys.stderr.write("reflecting boolean %s, nullable=%s\n" % (value, nullable))
    if value != None:
      if value:
        value = "true"
      else:
        value = "false"
    return self.reflectString(parent, name, value, nullable, ns)
                    
class Document(InformationItem):
  
  def __init__(self, baseURI, encoding=None, standalone=None, version=None):
    self.children = []
    self.documentElement = None
    self.notations = []
    self.unparsedEntities = []        
    self.baseURI = baseURI
    self.characterEncodingScheme = encoding
    self.standalone = standalone
    self.version = version
    self.allDeclarationsProcessed = 1
    
  def addChild(self, child):
    if isinstance(child, Element):
      if self.documentElement:
        raise Exception, "attempt to add second Element child to Document"
      else:
        self.documentElement = child
    self.children.append(child)

  def addNotation(self, notation):
    self.notation.append(notations)

  def addUnparsedEntity(self, entity):
    self.unparsedEntities.append(entity)

  def printme(self, file):
    if self.version:
      file.write("<?xml version='%s'?>\n" % self.version)
    self.documentElement.printme(file,
                                 {"http://www.w3.org/XML/1998/namespace":Namespace("xml",
                                        "http://www.w3.org/XML/1998/namespace")})
    file.write("\n")

  def reflect(self, parent=None):

    doc = Document(None, None, "yes")

    document = Element(doc, infosetSchemaNamespace, "document", None, None,
                       {None:Namespace(None, infosetSchemaNamespace),
                        "xsi":Namespace("xsi", xsiNamespace),
                        "xml":Namespace("xml",
                                        "http://www.w3.org/XML/1998/namespace")})
    doc.addChild(document)
    
    children = Element(document, infosetSchemaNamespace, "children")
    document.addChunkedChild(children)

    for c in self.children:
      cc = c.reflect(children)
      if isinstance(cc, InformationItem):
        children.addChunkedChild(cc)
      else:
        for ccc in cc:
          children.addChunkedChild(ccc)

#    docel =  Element(document, infosetSchemaNamespace, "documentElement")
#    document.addChunkedChild(docel)
#    XXX
    self.reflectString(document, "documentElement", None, 1) # fix me

    notations = Element(document, infosetSchemaNamespace, "notations")
    document.addChunkedChild(notations)
    for n in self.notations:
      nn = n.reflect(notations)
      notations.addChunkedChild(nn)

    unparsed = Element(document, infosetSchemaNamespace, "unparsedEntities")
    document.addChunkedChild(unparsed)
    for e in self.unparsedEntities:
      ee = e.reflect(unparsed)
      unparsed.addChunkedChild(ee)

    self.reflectString(document, "baseURI", self.baseURI, 1)

    self.reflectString(document, "characterEncodingScheme", self.characterEncodingScheme, 1)

    self.reflectString(document, "standalone", self.standalone, 1)

    self.reflectString(document, "version", self.version, 1)

    self.reflectBoolean(document, "allDeclarationsProcessed", self.allDeclarationsProcessed, 0)
    
    return doc

  def indent(self, indent="",indentMixed=None):
    self.documentElement.indent(indent,indentMixed)
    
class Element(InformationItem):

  def __init__(self, parent, namespaceName, localName, prefix=None, baseURI=0, inScopeNamespaces=0):
    self.parent = parent
    self.namespaceName = namespaceName
    self.localName = localName
    self.prefix = prefix
    self.chunkedChildren = []
    self.attributes = {}
    if baseURI == 0:
      if isinstance(parent, InformationItem):
        self.baseURI = parent.baseURI
      else:
        self.baseURI = None
    else:
      self.baseURI = baseURI
    self.baseURI = baseURI
    self.namespaceAttributes = {}
    if inScopeNamespaces == 0:
      if isinstance(parent, Element):
        self.inScopeNamespaces = parent.inScopeNamespaces
      else:
        self.inScopeNamespaces = {"xml":
                                  Namespace("xml",
                                       "http://www.w3.org/XML/1998/namespace")}
    else:
      self.inScopeNamespaces = inScopeNamespaces
      
  def addChunkedChild(self, child):
    self.chunkedChildren.append(child)

  def addAttribute(self, attr):
    self.attributes[(attr.namespaceName, attr.localName)] = attr

  def addNamespaceAttribute(self, attr):
    self.namespaceAttributes[(attr.namespaceName, attr.localName)] = attr

  def printme(self, file, namespaces={}):
    nsname = self.namespaceName
    
    ans = self.attributes.keys()
    ans.sort()

    # do we need any namespace attributes ...
    ns = {}
    count = len(namespaces)
    # ... for in-scope namespaces?
    for inns in self.inScopeNamespaces.values():
      if namespaces.has_key(inns.namespaceName):
        if namespaces[inns.namespaceName].prefix == inns.prefix:
          pass                          # already got it
        else:
          ns[inns.namespaceName] = inns
          count = count+1
      else:
        ns[inns.namespaceName] = inns
        count = count+1
    # ... for the element name?
    if nsname and not namespaces.has_key(nsname) and not ns.has_key(nsname):
      ns[nsname] = Namespace("ns%d" % count, nsname)
      count = count+1
    # ... for the attributes?
    for a in self.attributes.values():
      ansname = a.namespaceName
      if ansname and not namespaces.has_key(ansname) and not ns.has_key(ansname):
        ns[ansname] = Namespace("ns%d" % count, ansname)
        count = count+1
        
    if ns:
      namespaces = namespaces.copy()
      for n in ns.values():
        namespaces[n.namespaceName] = n

    if nsname:
      if namespaces[nsname].prefix == None:
        file.write("<%s" % self.localName)
      else:
        file.write("<%s:%s" % (namespaces[nsname].prefix, self.localName))
    else:
      file.write("<%s" % self.localName)

    for n in ns.values():
      n.printme(file)
      
    for an in ans:
      self.attributes[an].printme(file, namespaces)

    if not self.chunkedChildren:
      file.write("/>")
      return
    file.write(">")

    for child in self.chunkedChildren:
      child.printme(file, namespaces)

    if nsname:
      if namespaces[nsname].prefix == None:
        file.write("</%s>" % self.localName)
      else:
        file.write("</%s:%s>" % (namespaces[nsname].prefix, self.localName))
    else:
      file.write("</%s>" % self.localName)

  def reflect(self, parent=None, dumpChars=1):

    element = Element(parent, infosetSchemaNamespace, "element")

    self.reflectString(element, "namespaceName", self.namespaceName, 1)

    self.reflectString(element, "localName", self.localName, 0)

    self.reflectString(element, "prefix", self.prefix, 1)

    children = Element(element, infosetSchemaNamespace, "children")
    element.addChunkedChild(children)

    for c in self.chunkedChildren:
      if (not dumpChars) and isinstance(c,Characters):
        continue
      cc = c.reflect(children)
      if isinstance(cc, InformationItem):
        children.addChunkedChild(cc)
      else:
        for ccc in cc:
          children.addChunkedChild(ccc)

    attributes = Element(element, infosetSchemaNamespace, "attributes")
    element.addChunkedChild(attributes)

    for a in self.attributes.values():
      aa = a.reflect(attributes)
      attributes.addChunkedChild(aa)

    namespaceAttributes = Element(element, infosetSchemaNamespace,
                             "namespaceAttributes")
    element.addChunkedChild(namespaceAttributes)

    if self.namespaceAttributes:
      for a in self.namespaceAttributes.values():
        aa = a.reflect(namespaceAttributes)
        namespaceAttributes.addChunkedChild(aa)

    inScopeNamespaces = Element(element, infosetSchemaNamespace,
                             "inScopeNamespaces")
    element.addChunkedChild(inScopeNamespaces)

    if self.inScopeNamespaces:
      for a in self.inScopeNamespaces.values():
        aa = a.reflect(inScopeNamespaces)
        inScopeNamespaces.addChunkedChild(aa)

    self.reflectString(element, "baseURI", self.baseURI, 1)

    return element

  # A hack to indent nested elements by inserting whitespace
  def indent(self, indent="",indentMixed=0):
    if not self.chunkedChildren:
      return
    elementOnly=1
    textOnly=1
    for c in self.chunkedChildren:
      if isinstance(c, Element):
        textOnly=0
        if not elementOnly:
          break
      elif isinstance(c, Character) or isinstance(c, Characters):
        elementOnly=0
        if not textOnly:
          break
    if textOnly or ((not elementOnly) and (not indentMixed)):
      return
    old = self.chunkedChildren
    self.chunkedChildren = []
    for c in old:
      self.addChunkedChild(Characters(self, "\n"+indent+"  ", 1))
      if isinstance(c, Element):
        c.indent(indent+"  ",indentMixed)
      self.addChunkedChild(c)
    self.addChunkedChild(Characters(self, "\n"+indent, 1))      
    
class Character(InformationItem):

  def __init__(self, parent, characterCode, elementContentWhitespace=0):
    self.parent = parent
    self.characterCode = characterCode
    self.elementContentWhitespace = elementContentWhitespace

# sequence of characters represented as string
class Characters(InformationItem):

  def __init__(self, parent, characters, elementContentWhitespace=0):
    self.parent = parent
    self.characters = characters
    self.elementContentWhitespace = elementContentWhitespace

  def printme(self, file, namespaces={}):
    text = escape(self.characters, 1)
    file.write(text)

  def reflect(self, parent=None):
#    print "reflecting chars %s" % self.characters
    clist = []
    for char in self.characters:
      
      c = Element(parent, infosetSchemaNamespace, "character")
      clist.append(c)

      self.reflectString(c, "characterCode", "%s" % ord(char), 0)

      self.reflectBoolean(c, "elementContentWhitespace",
                         self.elementContentWhitespace, 0)

    return clist
    
class Attribute(InformationItem):

  def __init__(self, ownerElement, namespaceName, localName, prefix, normalizedValue, specified=1, attributeType=None):
    self.ownerElement = ownerElement
    self.namespaceName = namespaceName
    self.localName = localName
    self.prefix = prefix
    self.normalizedValue = normalizedValue
    self.specified = specified
    self.attributeType = attributeType
    
  def printme(self, file, namespaces={}):
    nsname = self.namespaceName
    text = escape(self.normalizedValue, 1)
    if nsname:
      file.write(" %s:%s" % (namespaces[nsname].prefix, self.localName))
    else:
      file.write(" %s" % self.localName)
    file.write("='%s'" % text)

  def reflect(self, parent=None):
    attribute = Element(parent, infosetSchemaNamespace, "attribute")

    self.reflectString(attribute, "namespaceName", self.namespaceName, 1)

    self.reflectString(attribute, "localName", self.localName, 0)

    self.reflectString(attribute, "prefix", self.prefix, 1)

    self.reflectString(attribute, "normalizedValue", self.normalizedValue, 0)

    self.reflectBoolean(attribute, "specified", self.specified, 0)

    self.reflectString(attribute, "attributeType", self.attributeType, 1)

    self.reflectString(attribute, "references", None, 1) # not implemented
    
    return attribute

class Namespace(InformationItem):

  def __init__(self, prefix, namespaceName):
    self.prefix = prefix
    self.namespaceName = namespaceName;

  def printme(self, file, namespaces={}):
    text = escape(self.namespaceName, 1)
    if self.prefix == None:
      file.write(" xmlns='%s'" % text)
    else:
      file.write(" xmlns:%s='%s'" % (self.prefix, text))

  def reflect(self, parent=None):
    namespace = Element(parent, infosetSchemaNamespace, "namespace")

    self.reflectString(namespace, "prefix", self.prefix, 1)

    self.reflectString(namespace, "namespaceName",
                       self.namespaceName, 0)

    return namespace

#  class EntityDeclaration(InformationItem):

#    def __init__(self, entityType, name, systemIdentifier, publicIdentifier, baseURI, notation, content, charset):
#      self.entityType = entityType
#      self.name = name
#      self.systemIdentifier = systemIdentifier
#      self.publicIdentifier = publicIdentifier
#      self.baseURI = baseURI
#      self.notation = notation
#      self.content = content
#      self.charset = charset

#    def reflect(self, parent=None):
#      pass                                # not yet
  
def escape(text, isattr=0):
  if "&" in text:
    text=string.replace(text,"&","&amp;")
  if "<" in text:
    text=string.replace(text,"<","&lt;")
  if isattr and "'" in text:
    text=string.replace(text,"'","&apos;")
  return text


import sys

if 0:
  d = Document("http://base")
  e = Element(d, "http://some/namespace", "foo", None, d.baseURI)
  d.addChild(e)
  a = Attribute(e, "http://some/namespace", "attr", None, "value")
  e.addAttribute(a)
  e2 = Element(d, "http://some/namespace", "bar", None, d.baseURI)
  e.addChunkedChild(e2)
  c = Characters(e, "\nhello world\n")
  e.addChunkedChild(c)
  d.printme(sys.stderr)
