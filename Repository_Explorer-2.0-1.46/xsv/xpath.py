# Copyright (C) 2000 LTG -- See accompanying COPYRIGHT and COPYING files
# TODO: raise some errors on bogus patterns!
import string
import types
import XMLInfoset

class XPath:
  def __init__(self,str,nsdict):
    self.str=str
    self.nsdict=nsdict
    self.pats=self.parse(str)

  def parse(self,str):
    disjuncts=map(lambda s:string.split(s,'/'),string.split(str,'|'))
    # weird result for //
    return map(lambda d,ss=self:map(lambda p,s=ss:s.patBit(p),
                                    d),
               disjuncts)

  def patBit(self,part):
    if part=='':
      # // in string
      return None
    elif part=='.':
      return idWrap
    elif part[0]=='@':
      if ':' in part:
        cp=string.find(part,':')
        ns=self.nsdict[part[1:cp]]
        part=part[cp+1:]
      else:
        part=part[1:]
        ns=None
      return lambda e,y=None,s=self,a=part,ns=ns:s.attrs(e,a,ns,y)
    else:
      if ':' in part:
        cp=string.find(part,':')
        ns=self.nsdict[part[0:cp]]
        part=part[cp+1:]
      else:
        ns=None
      b=string.find(part,'[')
      if b>-1:
        f=string.find(part,']')
        return lambda e,y=None,s=self,n=part[0:b],ns=ns,m=self.patBit(part[b+1:f]):s.children(e,n,ns,y,m)
      else:
        return lambda e,y=None,s=self,n=part,ns=ns:s.children(e,n,ns,y)

  def find(self,element):
    res=[]
    for pat in self.pats:
      sub=self.process(element,pat)
      if sub:
        res=res+sub
    if res:
      return res
    else:
      return None

  def find1(self,nodelist,pat):
    res=[]
    for e in nodelist:
      sub=self.process(e,pat)
      if sub:
	res=res+sub
    if res:
      return res
    else:
      return None

  def process(self,element,pat):
    pe=pat[0]
    if pe:
      res=pe(element)
    elif len(pat)>1:
      # None means descendant, side effect of split is two Nones in first place
      if pat[1]:
        pat=pat[1:]
      elif len(pat)>2:
        pat=pat[2:]
      else:
        # bogus pattern ending in //
        return None
      if pat[0]:
        res=pat[0](element,1)
      else:
        # bogus pattern -- ///?
        return None
    else:
      # bogus pattern ending in /
      return None
    if not res:
      return None
    if len(pat)>1:
      return self.find1(res,pat[1:])
    else:
      return res

  def attrs(self,element,aname,ns,anywhere):
    # assume this is the end of the line
    for a in element.attributes.values():
      if (a.localName == aname and a.namespaceName==ns) or aname == "*":
        res=[a.normalizedValue]
        break
    else:
      res=None
    if anywhere:
      for c in element.children:
        if isinstance(c,XMLInfoset.Element):
          sr=self.attrs(c,aname,ns,1)
          if sr:
            if res:
              res=res+sr
            else:
              res=sr
    return res

  def children(self,element,cname,ns,anywhere,subPat=None):
    # trickier, we need to stay in control
    res=[]
    for c in element.chunkedChildren:
      if isinstance(c,XMLInfoset.Element):
        if (c.localName==cname and c.namespaceName==ns) or cname == "*":
          if (not subPat) or subPat(c):
            res.append(c)
        if anywhere:
          sr=self.children(c,cname,ns,1,subPat)
          if sr:
            if res:
              res=res+sr
            else:
              res=sr
    if res:
      return res
    else:
      return None

def idWrap(e):
  return [e]
