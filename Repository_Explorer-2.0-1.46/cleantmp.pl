#!/usr/bin/perl

#
# delete all temp files /tmp/re*
#

sub deleteTemp
{
   opendir (TMP, "/tmp");
   my @files = grep { /^re\..*$/ } readdir (TMP);
   closedir (TMP);
   
   foreach my $afile (@files)
   {
      chomp $afile;
      unlink "/tmp/$afile";
      print ".";
   }
   
   print "\nDone.\n\n";
}

sub main
{
   print "Deleting RE files from /tmp\n\n";
   deleteTemp;
}

main;
