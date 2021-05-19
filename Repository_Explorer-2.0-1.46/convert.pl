#!/usr/bin/perl

sub convert
{
   my ($s) = @_;

   print $s."\n";
   
   my $t;
   
   for ( $a=0; $a<length ($s); $a++ )
   {
      $digit = substr ($s, $a, 1);
      
      printf ("%b\n", ord ($digit));
      
      if ($digit > 128)
      {
         $t .= 'X';   
      }
      else
      {
         $t .= $digit;
      }
   }
   
   $s;
}

print convert ('æå­')."\n";


