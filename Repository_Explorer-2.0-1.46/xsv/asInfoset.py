# Copyright (C) 2000 LTG -- See accompanying COPYRIGHT and COPYING files
# make infoset items out of components
# $Id: asInfoset.py,v 1.1 2000/09/29 14:20:34 ht Exp $

def asInfoitem(self):
  return 'type'+self.name
