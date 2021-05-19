#!/usr/bin/perl

# script to check the all archives comply with the tests, and 
# remove those that do not.
# hussein suleman
# 23 march 2006

print "Repository Explorer Archive Re-checker\n";
print "--------------------------------------\n";

use FindBin;
chdir "$FindBin::Bin";

# find out what the separator character is
print "reading configuration information ...\n";
open (my $configfile, "config.h");
my @configlines = <$configfile>;
close ($configfile);

my $separator = ':';
foreach my $aconfigline (@configlines)
{
   if ($aconfigline =~ /separatorchar +([0-9]+)/)
   { $separator = chr($1); }
}

# get a list of the archives
print "reading list of archives ...\n";
open (my $predeffile, "predef2");
my @archives = <$predeffile>;
close ($predeffile);

# make a backup of the file
my @timebits = localtime();
my @monthname = ('jan','feb','mar','apr','may','jun',
                 'jul','aug','sep','oct','nov','dec');
my $backupname = 'predef2.'.$timebits[3].$monthname[$timebits[4]].($timebits[5]+1900);
print "backing up file into $backupname ...\n";
open (my $predeffile, ">$backupname");
foreach my $archiveline (@archives)
{ print $predeffile $archiveline; }
close ($predeffile);

# check each archive
my @checkedarchives = ();
foreach my $archive (@archives)
{
   my ($name, $baseURL, $URL) = split ($separator, $archive);
   print "checking $name ... ";
   my $retcode = system ("./comply $baseURL enus.lan >> recheck.log");
   if ($retcode == 0)
   {
      print "OK\n";
      push (@checkedarchives, $archive);
   }
   else
   {
      print "FAILED\n";
   }
}

# write out new predef file
print "writing new predef file ...\n";
open (my $predeffile, ">predef2");
foreach my $archiveline (@checkedarchives)
{ print $predeffile $archiveline; }
close ($predeffile);

print "finis.\n";
