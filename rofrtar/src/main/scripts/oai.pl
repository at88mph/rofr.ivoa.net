#!/usr/bin/perl -w -I.
use strict;

#  ----------------------------------------------------------------------
# | Open Archives Initiative Harvesting Framework version 2.0            |
# | Hussein Suleman                                                      |
# | June 2002                                                            |
#  ----------------------------------------------------------------------
# |  Virginia Polytechnic Institute and State University                 |
# |  Department of Computer Science                                      |
# |  Digital Library Research Laboratory                                 |
#  ----------------------------------------------------------------------

# Installation :
#   copy all files into a directory from which you can execute scripts
#
# Testing :
#   use the repository explorer (http://purl.org/net/oai_explorer)
#   to test your interface


use FindBin;
use lib "$FindBin::Bin/../lib";

# use XMLFile::XMLFileD3;
use VORegInABox::FileStoreOAI;

sub main
{
   chdir "$FindBin::Bin/..";
   my $OAI = new VORegInABox::FileStoreOAI ("conf/config.xml");
   $OAI->Run;
   $OAI->dispose;
}

main;
