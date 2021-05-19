#!/usr/bin/perl

#
# scan through all C sources files and extract a list of all translatable
# strings to create a default language translation files
#

sub GenerateEnglish
{
   opendir (DIR, ".");
   @files = readdir (DIR);
   closedir (DIR);

   %tags = ();
   
   open (EFILE, ">efile.lst");

   foreach $afile (grep {/\.c$/} @files)
   {
      open (FILE, "<$afile");
      @contents = <FILE>;
      close (FILE);
   
      foreach $aline (@contents)
      {
         chomp $aline;
         if ($aline =~ /Translate \("[^"]/)
         {
            $aline =~ s/^(.*)Translate \("([^"]*)(.*)$/$2/g;
            if ($tags{$aline} ne 'yes')
            {
               print EFILE "$aline\n";
               $tags{$aline} = 'yes';
            }
         }
      }   
   }

   close (EFILE);
}

sub Translate 
{
   my ($phrase, $langname) = @_;
   
   $phrase =~ s/ /\+/g;
   
   system ("lynx -source \"http://ets3.freetranslation.com:5081/textform".
           "?Language=English/$langname&sequence=core&mode=html&".
           "srctext=$phrase\" | head -11 | tail -1 > trans.tmp" );

   open (TFILE, "trans.tmp");
   my $tphrase = <TFILE>;
   chomp $tphrase;
   close (TFILE);

   unlink "trans.tmp";
   
   $tphrase;
}

sub CreateTranslation
{
   my ($lang, $langname) = @_;
   
   print "\nCreating translation file for $langname ...\n";
   
   %tags = ();

   if (-e "$lang.lan")
   {
      open (FILE, "<$lang.lan");
      @contents = <FILE>;
      close (FILE);
      foreach $aline (@contents)
      {
         chomp $aline;
         $tags{$aline} = 'yes';
      }
      $gotfile = 1;
   }
   else
   {
      $gotfile = 0;
   }
   
   open (FILE, "<efile.lst");
   @edata = <FILE>;
   close (FILE);

   open (FILE, ">>$lang.lan");
   
   if ($gotfile == 0)
   {
      print FILE "$langname\n#\n".
                 "# Language translation file for Repository Explorer\n".
                 "# Language: default (english-->$langname)\n#\n";
   }

   foreach $aline (@edata)
   {
      chomp $aline;
      if ($tags{$aline} ne 'yes')
      {
         my $tline = $aline;
         if ($lang ne 'enus')
         {
            $tline = Translate ($aline, $langname);
         }

         print FILE "#\n$aline\n$tline\n";

         if (length ($tline) > 30) 
         { $tline = substr ($tline, 0, 30); }
         if (length ($aline) > 30) 
         { $aline = substr ($aline, 0, 30); }
         print "TR: $aline --> $tline\n";
      }
   }

   close (FILE);
}

sub main
{
   print "Dictionary Creator for TESTOAI\n";
   
   print "\nGenerating phrase list ...\n";
   GenerateEnglish;

   CreateTranslation ('enus', 'English');
#   CreateTranslation ('gr', 'German');
#   CreateTranslation ('es', 'Spanish');
#   CreateTranslation ('fr', 'French');
#   CreateTranslation ('pt', 'Portuguese');
   
   unlink 'efile.lst';
}

main;
