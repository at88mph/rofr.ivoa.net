#  ---------------------------------------------------------------------
#   IVOA Publishing Registry-in-a-Box.  A simple implmentation of the 
#     IVOA Profile on the OAI-PMH standard.  This has been adapted 
#     Ramon Williamson and Ray Plante at the National Center for 
#     Supercomputing Applicaitons from the following library:
#  ---------------------------------------------------------------------
#   OAI-PMH2 XMLFile data provider
#    v2.0
#    July 2002
#  ------------------+--------------------+-----------------------------
#   Hussein Suleman  |   hussein@vt.edu   |    www.husseinsspace.com    
#  ------------------+--------------------+-+---------------------------
#   Department of Computer Science          |        www.cs.vt.edu       
#     Digital Library Research Laboratory   |       www.dlib.vt.edu      
#  -----------------------------------------+-------------+-------------
#   Virginia Polytechnic Institute and State University   |  www.vt.edu  
#  -------------------------------------------------------+-------------


package VORegInABox::FileStoreOAI;


use Pure::EZXML;
use Pure::X2D;

use OAI::OAI2DP;
use vars ('@ISA');
@ISA = ("OAI::OAI2DP");

use Data::Dumper;


# constructor
sub new
{
   my ($classname, $configfile) = @_;
   my $self = $classname->SUPER::new ($configfile);

   # get configuration from file
   my $con = new Pure::X2D ($configfile);
   $self->{'repositoryName'} = $con->param ('repositoryName', 'XML-File Archive');
   $self->{'adminEmail'} = $con->param ('adminEmail', "someone\@somewhere");
   $self->{'archiveId'} = $con->param ('archiveId', 'XMLFileArchive');
   $self->{'recordlimit'} = $con->param ('recordlimit', 500);
   $self->{'datadir'} = $con->param ('datadir', 'data');
   $self->{'filematch'} = $con->{'filematch'};
   $self->{'metadata'} = $con->{'metadata'};

   # VORegInABox specific config
   $self->{'confdir'} = $con->param('confdir', '.');
   if (! -d "$self->{confdir}" || ! -r "$self->{confdir}") {
       print STDERR "VORegInABox::FileStoreOAI: Warning: $self->{'confdir'} ",
                    "does not exist with read permission; reverting to '.'\n";
       $self->{'confdir'} = '.';
   }
   $self->{'deletedRecord'} = $con->param ('deletedRecord', 'no');

   $self->{'resumptionseparator'} = '!';
   
   # remove default metadata information
   $self->{'metadatanamespace'} = {};
   $self->{'metadataschema'} = {};
   $self->{'metadatatransform'} = {};

   # add in seconds support
   $self->{'granularity'} = 'YYYY-MM-DDThh:mm:ssZ';
   
   # add in metadata formats from list in configuration
   foreach my $metadata (@{$con->{'metadata'}})
   {
      my $metadataPrefix = $metadata->{'prefix'}->[0];
      $self->{'metadatanamespace'}->{$metadataPrefix} = $metadata->{'namespace'}->[0];
      $self->{'metadataschema'}->{$metadataPrefix} = $metadata->{'schema'}->[0];
      if (defined $metadata->{'transform'}->[0])
      {
         $self->{'metadatatransform'}->{$metadataPrefix} = $metadata->{'transform'}->[0];
      }
      else
      {
         $self->{'metadatatransform'}->{$metadataPrefix} = '';
      }
   }

   # load in set mappings
   $self->{'setnames'} = {};
   $self->read_setdesc("$self->{confdir}/setnames.xml") 
       if (-e "$self->{confdir}/setnames.xml");

   # load in complete database
   $self->read_database ('');

   bless $self, $classname;
   return $self;
}


# destructor
sub dispose
{
   my ($self) = @_;
   $self->SUPER::dispose ();
}

# read a set description file
sub read_setdesc {
    my($self, $file, $spec) = @_;
    $spec = '' if (! (defined $spec));

    if (! -r "$file") {
        warn "$file not found or readable";
        return;
    }

    my $parser = new Pure::EZXML;
    my $setnamedoc = $parser->parsefile("$file")->getDocumentElement;

    my @sets = ();
    if ($setnamedoc->getNodeName ne "set") {
        @sets = $setnamedoc->getElementsByTagName ('set', 0);
    }
    else {
        @sets = ($setnamedoc);
    }
    my @elems = ('spec', 'name', 'description' );

    foreach my $set (@sets) {
        my %data = ();
        foreach my $nm (@elems) {
            my $elem = $set->getElementsByTagName ($nm, 0);
            if ($elem->getLength > 0) {
                $data{$nm} = $elem->item(0)->getChildNodes->toString;
            }
        }
        my @includedins = $set->getElementsByTagName ('includedIn', 0);
        if (@includedins > 0) {
            $data{'includedIn'} = [];
            foreach my $includedin (@includedins) {
                push(@{$data{'includedIn'}}, 
                     $includedin->getChildNodes->toString);
            }
        }

        if (defined $data{'spec'}) {
            $data{'spec'} =~ s/^\s+//;
            $data{'spec'} =~ s/\s+$//;
        }

        next if (defined $data{'spec'} && $spec ne '' && 
                 $data{'spec'} ne $spec);
        next if (! (defined $data{'spec'}) && $spec eq '');

        my $specnm = ($spec eq '') ? $data{'spec'} : $spec;

        $data{'name'} = $specnm if (! (defined $data{'name'}));

        if (! (defined $self->{'setnames'}->{$specnm})) {
            $self->{'setnames'}->{$specnm} = {'name' => $data{'name'}};
            $self->{'setnames'}->{$specnm}->{'description'} = 
                $data{'description'} if (defined $data{'description'});
            $self->{'setnames'}->{$specnm}->{'includedIn'} = 
                $data{'includedIn'} if (defined $data{'includedIn'});
        }
    }

}

# create database of files, directories and other information
sub read_database
{
   my ($self, $directory) = @_;
   
   # clear database if top-level
   if ($directory eq '')
   {
      $self->{'database'} = { set2id => {}, id2set => {}, id2rec => {}, setname => {} };
   }
   
   # get contents of the current directory
   opendir (DIR, "$self->{'datadir'}$directory");
   my @files = readdir (DIR);
   closedir (DIR);
   
   # go through each entry in the directory
   foreach my $afile (@files)
   {
      # skip the directory markers
      if (($afile eq '.') || ($afile eq '..'))
      {
         next;
      }
   
      # if its a directory ...
      if (-d "$self->{'datadir'}$directory/$afile")
      {
         # create empty set container
         my $mainset = $directory;
         if ($mainset ne '')
         {
            $mainset = substr ($mainset, 1);
            $mainset =~ s/\//:/go;
            $mainset .= ':';
         }
         $self->{'database'}->{'set2id'}->{$mainset.$afile} = [] 
          if (! (defined($self->{'database'}->{'set2id'}->{$mainset.$afile})));
         
         # add in set name if it exists
         $self->{'database'}->{'setname'}->{$mainset.$afile} = 
             { 'name' => $mainset.$afile };
         if (-e "$self->{'datadir'}$directory/$afile/_set_")
         {
            $self->read_setdesc("$self->{'datadir'}$directory/$afile/_set_",
                                $mainset.$afile);
         }
         if (exists $self->{'setnames'}->{$mainset.$afile})
         {
             my $setdesc = $self->{'setnames'}->{$mainset.$afile};
             my $setdata = $self->{'database'}->{'setname'}->{$mainset.$afile};
             $setdata->{'name'} = $setdesc->{'name'} 
                 if (defined $setdesc->{'name'});
             $setdata->{'includedIn'} = $setdesc->{'includedIn'} 
                 if (defined $setdesc->{'includedIn'});
             if (defined $setdesc->{'description'}) {
                 $setdesc->{'description'} =~ s/\n\s+$/\n/s;
                 $setdata->{'description'} = $setdesc->{'description'} 
             }
         }
      
         $self->read_database ("$directory/$afile");
      }

      # if its a file ...
      elsif (-f "$self->{'datadir'}$directory/$afile")
      {
         # screen out for files that do not match
         my $good = 0;
         foreach my $filematch (@{$self->{'filematch'}})
         {
            if ($afile =~ /$filematch/)
            {
               $good = 1;
            }
         }
         if (($good == 0) || ($afile eq '_name_') || ($afile eq '_set_'))
         {
            next;
         }

         my($identifier, $datestamp, $status) = 
           $self->extract_oai_metadata("$self->{'datadir'}$directory/$afile");
         
         # create list of sets
         my $mainset = $directory;
         if ($mainset ne '')
         {
            $mainset = substr ($mainset, 1);
            $mainset =~ s/\//:/go;
         }
         my @splitsets = ();
         my $splitsettemp = '';

         foreach my $setpart (split (':', $mainset))
         {
            if ($splitsettemp ne '')
            {
               $splitsettemp .= ':';
            }
            $splitsettemp .= $setpart;
            push (@splitsets, $splitsettemp);
         }
         
         # add to identifier_to_set hash
         if (! exists $self->{'database'}->{'id2set'}->{$identifier})
         {
            print STDERR "Pushed $identifier";
            $self->{'database'}->{'id2set'}->{$identifier} = [];
         }
         else {
            print STDERR "Identifier $identifier already exists.";
         }
         if ($mainset ne '')
         {
            push (@{$self->{'database'}->{'id2set'}->{$identifier}}, $mainset);

            if (defined 
                $self->{'database'}->{'setname'}->{$mainset}->{'includedIn'})
            {
                my @sts = 
             @{$self->{'database'}->{'setname'}->{$mainset}->{'includedIn'}};
                foreach my $stnm (@sts) {
                    push (@{$self->{'database'}->{'id2set'}->{$identifier}}, 
                          $stnm);
                }
            }
         }

         # add to set_to_identifier hash
         foreach my $aset (@splitsets, '')
         {
            # check if it isnt there already
            my $found = 0;
            foreach my $id (@{$self->{'database'}->{'set2id'}->{$aset}})
            {
               if ($id eq $identifier) { $found = 1; last; }
            }
            if ($found == 0)
            {
               push (@{$self->{'database'}->{'set2id'}->{$aset}}, $identifier);
            }
         }

         if (defined 
             $self->{'database'}->{'setname'}->{$mainset}->{'includedIn'}) 
         {
             my @sts = 
             @{$self->{'database'}->{'setname'}->{$mainset}->{'includedIn'}};
             foreach my $stnm (@sts) {
                 push (@{$self->{'database'}->{'set2id'}->{$stnm}}, 
                       $identifier);
             }
         }
         
         # add to identifier_to_record hash
         $self->{'database'}->{'id2rec'}->{$identifier} = 
             [ $datestamp, $status, "$directory/$afile" ];
      }
   }
}

# extract metadata from an xml file for database
sub extract_oai_metadata {
    my ($self, $file) = @_;
    my ($date, $id, $status) = ('', '', '');

    my $parser = new Pure::EZXML;
    my $doc = $parser->parsefile ("$file")->getDocumentElement;

    $date = $doc->getAttribute("updated");
    $date =~ s/^\s*//;  $date =~ s/\s*$//;
    $date .= "Z" if ($date !~ /Z$/);

    $status = $doc->getAttribute("status");
    $status =~ s/^\s*//;  $status =~ s/\s*$//;
    $status = "active" if ($status eq '');

    $id = $doc->getElementsByTagName("identifier", 0)->item(0)->getChildNodes->toString;
    $id =~ s/^\s*//;  $id =~ s/\s*$//;

    return ($id, $date, $status);
}

# format header for ListIdentifiers
sub Archive_FormatHeader
{
   my ($self, $hashref, $metadataFormat) = @_;
   
   my($datestamp,$status,$p) = @{$self->{'database'}->{'id2rec'}->{$hashref}};
   
   $self->FormatHeader ($hashref,
                        $datestamp,
                        $status,
                        $self->{'database'}->{'id2set'}->{$hashref}
                       );
}


# retrieve records from the source archive as required
sub Archive_FormatRecord
{
   my ($self, $hashref, $metadataFormat) = @_;
   
   if ($self->MetadataFormatisValid ($metadataFormat) == 0)
   {
      $self->AddError ('cannotDisseminateFormat', 'The value of metadataPrefix is not supported by the repository');
      return '';
   }

   # get data file and tranform accordingly
   my ($datestamp, $status, $pathname) = 
       @{$self->{'database'}->{'id2rec'}->{$hashref}};
   my $metadataTransform = $self->{'metadatatransform'}->{$metadataFormat};
   open (FILE, "cat $self->{'datadir'}$pathname | $metadataTransform");
   my @data = <FILE>;
   close (FILE);
   my $fstr = join ('', @data);

   # get rid of XML declaration
   $fstr =~ s/^<\?[^\?]+\?>//o;

   $self->FormatRecord ($hashref,
                        $datestamp,
                        $status,
                        $self->{'database'}->{'id2set'}->{$hashref},
                        $fstr,
                        '',
                       );
}


# add additional information into the identification
sub Archive_Identify
{
   my ($self) = @_;
   
   my $identity = {};
   
   # add in description for toolkit
   if (! exists $identity->{'description'})
   {
      $identity->{'description'} = [];
   }

   $identity->{'deletedRecord'} = $self->{'deletedRecord'};
   
   # add in external description containers
   opendir (DIR, "$self->{confdir}");
   my @files = readdir (DIR);
   closedir (DIR);

   foreach my $identityfile (grep { /^identity[^\.]*\.xml$/ } @files)
   {
      if (! open (FILE, "$self->{confdir}/$identityfile")) {
          print STDERR "VORegInABox::FileStoreOAI: Unable to read ",
                       "$identityfile: $!\n";
          next;
      }
      my @data = <FILE>;
      close (FILE);
      
      my $joineddata = join ('', @data);

      # get rid of XML declaration
      $joineddata =~ s/^<\?[^\?]+\?>//o;
      
      push (@{$identity->{'description'}}, $joineddata );
   }
   
   $identity;
}

# Obtain the ri:Resource to be put into the Identify Description.
sub Archive_IdentifyDescription
{
   my ($self) = @_;

   # get data file and tranform accordingly
   my $identifyMetadataPrefix = "ivo_vor";
   my $identifyID = "ivo://ivoa.net/rofr";
   my $recref = $self->Archive_GetRecord ($identifyID, $identifyMetadataPrefix);
   my ($datestamp, $status, $pathname) =
       @{$self->{'database'}->{'id2rec'}->{$recref}};
   my $metadataTransform = $self->{'metadatatransform'}->{$identifyMetadataPrefix};
   open (FILE, "cat $self->{'datadir'}$pathname | $metadataTransform");
   my @data = <FILE>;
   close (FILE);
   my $fstr = join ('', @data);

   # get rid of XML declaration
   $fstr =~ s/^<\?[^\?]+\?>//o;
   return $fstr;
}


# get full list of mdps or list for specific identifier
sub Archive_ListMetadataFormats
{
   my ($self, $identifier) = @_;
   
   if ((defined $identifier) && ($identifier ne '') && (! exists $self->{'database'}->{'id2rec'}->{$identifier}))
   {
      $self->AddError ('idDoesNotExist', 'The value of the identifier argument is unknown or illegal in this repository');
   }
   return [];
}


# get full list of sets from the archive
sub Archive_ListSets
{
   my ($self) = @_;

   delete $self->{'database'}->{'set2id'}->{''};

   my $sd = $self->{'database'}->{'setname'};
   my $start = <<EOF;

      <oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                 xmlns:dc="http://purl.org/dc/elements/1.1/"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
          xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ 
                              http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
         <dc:description>
EOF
chomp $start;
   my $end = <<EOF;
         </dc:description>
      </oai_dc:dc>
EOF

   [
      map {
         [ $_, $sd->{$_}->{'name'},  
           ($sd->{$_}->{'description'}) ? 
                    $start.$sd->{$_}->{'description'}.$end : undef 
           ]
      } keys %{$self->{'database'}->{'setname'}}
   ];
}
                              
# minor override of inherited ListSets (hey, neatness counts!)
sub ListSets
{
   my ($self) = @_;

   my $setlist = $self->Archive_ListSets;
   
   if ($#$setlist == -1)
   {
      $self->AddError ('noSetHierarchy', 'The repository does not support sets');
   }

   my $buffer = $self->xmlheader;
   if ($#{$self->{'error'}} == -1)
   {   
      foreach my $item (@$setlist)
      {
         $buffer .= "<oai:set>\n".
                    "  <oai:setSpec>".$self->{'utility'}->lclean ($$item[0])."</oai:setSpec>\n".
                    "  <oai:setName>".$self->{'utility'}->lclean ($$item[1])."</oai:setName>\n";
         if (defined $$item[2])
         {
            $buffer .= "  <oai:setDescription>".$$item[2]."  </oai:setDescription>\n";
         }
         $buffer .= "</oai:set>\n";
      }
   }
   $buffer.$self->xmlfooter;
}


# get a single record from the archive
sub Archive_GetRecord
{
   my ($self, $identifier, $metadataFormat) = @_;

   my ($userIdentifier, $key, $registryPart, $localPart, $arrayIdentifier);

   # Per the IVOA Identifiers v 2.0 specification section 2.6
   # "Normalization and Comparison" identifiers are compared 
   # case-insensitively in their registry part but case-sensitively
   # in their local part, i.e. everything after ? in the pattern
   # ivo://<authority><path>?<query>#<fragment>

   ($registryPart, $localPart) = split(/[?]/, $identifier);
   $registryPart =~ tr/A-Z/a-z/;
   if (length($localPart) == 0) {
     $userIdentifier = $registryPart;
   } else {
     $userIdentifier = $registryPart . '?' . $localPart;
   }

   foreach $key (keys(%{$self->{'database'}->{'id2rec'}})) {
     ($registryPart, $localPart) = split(/[?]/, $key);
     $registryPart =~ tr/A-Z/a-z/;
     if (length($localPart) == 0) {
       $arrayIdentifier = $registryPart;
     } else {
       $arrayIdentifier = $registryPart . '?' . $localPart;
     }

     if ($arrayIdentifier eq $userIdentifier) {
       $identifier = $key;
     }
     
   }
   
   if (! exists $self->{'database'}->{'id2rec'}->{$identifier})
   {
      $self->AddError ('idDoesNotExist', 'The value of the identifier argument is unknown or illegal in this repository');
      return undef;
   }

   return $identifier;
}


# list all records in the archive
sub Archive_ListRecords
{
   my ($self, $set, $from, $until, $metadataPrefix, $resumptionToken) = @_;

   # handle resumptionTokens
   my ($offset);
   if ($resumptionToken eq '')
   {
      $offset = 0;
   }
   else
   {
      my @rdata = split ($self->{'resumptionseparator'}, $resumptionToken);
      ($set, $from, $until, $metadataPrefix, $offset) = @rdata;
      if ((! defined $set) || (! defined $from) || (! defined $until) ||
          (! defined $metadataPrefix) || (! defined $offset))
      {
         $self->AddError ('badResumptionToken', 'The resumptionToken is not in the correct format');
         return '';
      }
   }

   my $count = 0;
   my @allrows = ();
   my $gotmore = 0;
   
   # check for existence of set
   if (! defined $self->{'database'}->{'set2id'}->{$set})
   {
      $self->AddError ('badArgument', "The specified set ($set) does not exist");
      return '';
   }
   
   # got through all the identifiers in the set and extract those that match the other parameters
   foreach my $identifier (@{$self->{'database'}->{'set2id'}->{$set}})
   {
      my $datestamp = $self->{'database'}->{'id2rec'}->{$identifier}->[0];
      if ((($from eq '') || ($self->ToSeconds ($datestamp) >= $self->ToSeconds ($from, 1))) &&
          (($until eq '') || ($self->ToSeconds ($datestamp) <= $self->ToSeconds ($until))))
      {
         $count++;
         if ($count > $offset)
         {
            if ($count <= $offset+$self->{'recordlimit'}) 
            {
               push (@allrows, $identifier);
            }
            else
            {
               $gotmore = 1;
            }
         }
      }
   }

   # create a new resumptionToken if necessary
   $resumptionToken = '';
   if ($gotmore == 1)
   {
      $resumptionToken = join ($self->{'resumptionseparator'}, ($set,$from,$until,$metadataPrefix,$offset+$self->{'recordlimit'}));
   }
   if ($count == 0)
   {
      $self->AddError ('noRecordsMatch', 'The combination of the values of arguments results in an empty set');
   }

   ( \@allrows, $resumptionToken, $metadataPrefix, { 'completeListSize' => $count, 'cursor' => $offset } );
}


# list headers for all records in the archive
sub Archive_ListIdentifiers
{
   my ($self, $set, $from, $until, $metadataPrefix, $resumptionToken) = @_;

   # check for metadataPrefix if it is provided
   if ((defined $metadataPrefix) && ($metadataPrefix ne '') && ($self->MetadataFormatisValid ($metadataPrefix) == 0))
   {
      $self->AddError ('cannotDisseminateFormat', 'The value of metadataPrefix is not supported by the repository');
      return '';
   }
   
   $self->Archive_ListRecords ($set, $from, $until, $metadataPrefix, $resumptionToken);
}


1;

