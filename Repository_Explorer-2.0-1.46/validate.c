//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 2.0b1-1.43    |
// | Hussein Suleman                                                      |
// | May 2002                                                             |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : validate
//  Purpose    : Perform XSD validation on XML file
// ======================================================================

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/resource.h>
#include <sys/time.h>
#include <unistd.h>

#include "config.h"
#include "replace.h"
#include "taglist.h"
#include "validate.h"
#include "xml.h"


// ----------------------------------------------------------------------
//  Function   : ValidateXerces
// ----------------------------------------------------------------------
//  Purpose    : Validates XML according to its declared schema using 
//               Xerces
//  Parameters : 1. protocol version
//               2. XML file
//               3. XSV file
//               4. the verb to check for
//               5. metadata prefix
//               6. whether the schemas are local or not
//  Result     : Pointer to error message or NULL
// ----------------------------------------------------------------------

char *ValidateXerces ( char *protocolVersion, char *datafilename, 
                       char *xsvfilename, char *verb, char *metadataPrefix, int local  )
{
   // check for illegal (non-UTF8) encodings
   FILE *rawf = fopen (datafilename, "r");
   char buffer[1024];
   fgets (buffer, sizeof (buffer), rawf);
   fclose (rawf);
   for ( int a=0; a<strlen (buffer); a++ )
      buffer[a] = toupper (buffer[a]);
   if (strstr (buffer, "ENCODING"))
   {
      if (strstr (buffer, "UTF-8") == NULL)
      {
         FILE *rawf = fopen (xsvfilename, "w");
         fclose (rawf);
         return "Illegal character encoding in XML";
      }
   }

   // create local filename and perform local transformations
//   char localfilename[L_tmpnam];
//   tmpnam (localfilename);
   char localfilename[128];
   strcpy (localfilename, "/tmp/re.XXXXXX");
   close (mkstemp (localfilename));
   LocalTransform (protocolVersion, datafilename, localfilename, verb, metadataPrefix, local);

   char xercescommand[2048];
   
   sprintf (xercescommand, "%s %s > %s 2>&1", xercespath, localfilename, xsvfilename);
   
   // restrict memory usage of process
   static struct rlimit rlim;
   rlim.rlim_cur = 100000000;
   rlim.rlim_max = 100000000;
   setrlimit (RLIMIT_RSS, &rlim);
//   setrlimit (RLIMIT_AS, &rlim);

   system (xercescommand);
   
   // remove local file
   unlink (localfilename);
   
   // check if there were any errors
   unsigned char charone;
   FILE *xercesfile = fopen (xsvfilename, "r");
   fread (&charone, 1, 1, xercesfile);
   fclose (xercesfile);
   
   if (charone == '[')
   {
      return " ";
   }
   
   return NULL;
}

// ----------------------------------------------------------------------
//  Function   : ValidateXSV
// ----------------------------------------------------------------------
//  Purpose    : Validates XML according to its declared schema using XSV
//  Parameters : 1. protocol version
//               2. XML file
//               3. XSV file
//               4. the verb to check for
//               5. metadata prefix
//               6. whether the schemas are local or not
//  Result     : Pointer to error message or NULL
// ----------------------------------------------------------------------

char *ValidateXSV ( char *protocolVersion, char *datafilename, 
                    char *xsvfilename, char *verb, char *metadataPrefix, int local  )
{
   // check for illegal (non-UTF8) encodings
   FILE *rawf = fopen (datafilename, "r");
   char buffer[1024];
   fgets (buffer, sizeof (buffer), rawf);
   fclose (rawf);
   for ( int a=0; a<strlen (buffer); a++ )
      buffer[a] = toupper (buffer[a]);
   if (strstr (buffer, "ENCODING"))
   {
      if (strstr (buffer, "UTF-8") == NULL)
      {
         FILE *rawf = fopen (xsvfilename, "w");
         fclose (rawf);
         return "Illegal character encoding in XML";
      }
   }

   // create local filename and perform local transformations
//   char localfilename[L_tmpnam];
//   tmpnam (localfilename);
   char localfilename[128];
   strcpy (localfilename, "/tmp/re.XXXXXX");
   close (mkstemp (localfilename));
   LocalTransform (protocolVersion, datafilename, localfilename, verb, metadataPrefix, local);

   char xsvcommand[2048];
   
   if (strcmp (protocolVersion, "1.0") == 0)
      sprintf (xsvcommand, "%s %s > %s 2>&1", oldxsvpath, localfilename, xsvfilename);
   else
      sprintf (xsvcommand, "%s %s > %s 2>&1", xsvpath, localfilename, xsvfilename);

   // restrict memory usage of process
   static struct rlimit rlim;
   rlim.rlim_cur = 100000000;
   rlim.rlim_max = 100000000;
   setrlimit (RLIMIT_RSS, &rlim);
//   setrlimit (RLIMIT_AS, &rlim);

   system (xsvcommand);
   
   // remove local file
   unlink (localfilename);

   char docElt[2048];
   strcpy (docElt, " ");

   char schema[2048];
   strcpy (schema, " ");

   if ( (strcmp (verb, "Identify") == 0) ||
        (strcmp (verb, "ListMetadataFormats") == 0) ||
        (strcmp (verb, "ListSets") == 0) ||
        (strcmp (verb, "ListIdentifiers") == 0) ||
        (strcmp (verb, "ListRecords") == 0) ||
        (strcmp (verb, "GetRecord") == 0) )
   {
      if (strcmp (protocolVersion, "1.0") == 0)
         sprintf (docElt, "{%s%s}%s", namespaceprefix_1_0, verb, verb);
      else if (strcmp (protocolVersion, "1.1") == 0)
         sprintf (docElt, "{%s%s}%s", namespaceprefix_1_1, verb, verb);
      else
         sprintf (docElt, "{%s}OAI-PMH", namespaceprefix_2_0);

      if (local == 1)
      {
         if (strcmp (protocolVersion, "1.0") == 0)
            sprintf (schema, "%s%s -> %s%s.xsd", namespaceprefix_1_0, verb, 
                                                 localschemaprefix_1_0, verb);
         else if (strcmp (protocolVersion, "1.1") == 0)
            sprintf (schema, "%s%s -> %s%s.xsd", namespaceprefix_1_1, verb, 
                                                 localschemaprefix_1_1, verb);
         else
            sprintf (schema, "%s -> %sOAI-PMH.xsd", namespaceprefix_2_0, 
                                               localschemaprefix_2_0);
      }
      else
      {
         if (strcmp (protocolVersion, "1.0") == 0)
            sprintf (schema, "%s%s -> %s%s.xsd", namespaceprefix_1_0, verb, 
                                                 remoteschemaprefix_1_0, verb);
         else if (strcmp (protocolVersion, "1.1") == 0)
            sprintf (schema, "%s%s -> %s%s.xsd", namespaceprefix_1_1, verb, 
                                                 remoteschemaprefix_1_1, verb);
         else
            sprintf (schema, "%s -> %sOAI-PMH.xsd", namespaceprefix_2_0,
                                               remoteschemaprefix_2_0);
      }
   }
        
   TagList tl;
   
   runparser (&tl, xsvfilename);
   
   char *docEltp = tl.head->attr->Search ("docElt");
   char *instanceAssessedp = tl.head->attr->Search ("instanceAssessed");
   char *instanceErrorsp = tl.head->attr->Search ("instanceErrors");
   char *schemaLocsp = tl.head->attr->Search ("schemaLocs");
   char *schemaErrorsp = tl.head->attr->Search ("schemaErrors");
   
   if (strcmp (tl.head->tag, "xsv") != 0)
      return "Cannot run schema validation software successfully";
   
   if ((docEltp) && (strcmp (docEltp, docElt) != 0))
      return "Document root element or schema is not correct";
   
   if ((schemaErrorsp) && (strcmp (schemaErrorsp, "0") != 0))
      return "Errors in schema";

   if ((instanceAssessedp) && (strcmp (instanceAssessedp, "true") != 0))
      return "Instance assessed should be true but is not";
   
   if ((instanceErrorsp) && (strcmp (instanceErrorsp, "0") != 0))
      return "Errors in XML instance";
   
   if ((schemaLocsp) && (strstr (schemaLocsp, schema) == NULL ))
      return "Wrong namespace and/or schema";
      
   TagList *p = tl.head->head;
   while (p != NULL)
   {
      if (strcmp (p->tag, "notASchema") == 0)
         return "Schema cannot be loaded successfully";
      p = p->next;
   }

   p = tl.head->head;
   while (p != NULL)
   {
      if (strcmp (p->tag, "importAttempt") == 0)
      {
         if (strcmp (p->attr->Search ("outcome"), "failure") == 0)
            return "Schema cannot be loaded successfully";
      }
      p = p->next;
   }

   return NULL;
}



// ----------------------------------------------------------------------
//  Function   : Localize
// ----------------------------------------------------------------------
//  Purpose    : Creates local transform mapping
//  Parameters : 1. SRFSM
//               2. source
//               3. localdir
//               4. filename
//  Result     : (none)
// ----------------------------------------------------------------------

void Localize ( SRFSM *f, char *source, char *localdir, char *filename )
{
   char t[2048]; 
   
   strcpy (t, localdir);
   strcat (t, filename);
   f->Add (source, t);
}


// ----------------------------------------------------------------------
//  Function   : LocalTransform
// ----------------------------------------------------------------------
//  Purpose    : Transforms schemas to local copies wherever possible
//  Parameters : 1. protocol version 
//               2. XML filename
//               3. local XML filename
//               4. verb
//               5. metadata prefix
//  Result     : (none)
// ----------------------------------------------------------------------

void LocalTransform ( char *protocolVersion, char *datafilename, 
                      char *localfilename, char *verb, char *metadataPrefix, int local )
{
   SRFSM * f = new SRFSM ();
   
  Localize (f, "http://oai.dlib.vt.edu/OAI/metadata/toolkit.xsd", physicalpath, "toolkit.xsd");

   if (local)
   {
      if (strcmp (protocolVersion, "1.0") == 0)
      {
         Localize (f, "http://www.openarchives.org/OAI/1.0/OAI_Identify.xsd", local10schema, "OAI_Identify.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.0/OAI_ListSets.xsd", local10schema, "OAI_ListSets.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.0/OAI_ListMetadataFormats.xsd", local10schema, "OAI_ListMetadataFormats.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.0/OAI_ListIdentifiers.xsd", local10schema, "OAI_ListIdentifiers.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.0/OAI_ListRecords.xsd", local10schema, "OAI_ListRecords.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.0/OAI_GetRecord.xsd", local10schema, "OAI_GetRecord.xsd");
         Localize (f, "http://www.openarchives.org/OAI/dc.xsd", local10schema, "dc.xsd");
         Localize (f, "http://www.openarchives.org/OAI/oai_marc.xsd", local10schema, "oai_marc.xsd");
         Localize (f, "http://www.openarchives.org/OAI/rfc1807.xsd", local10schema, "rfc1807.xsd");
         Localize (f, "http://www.openarchives.org/OAI/eprints.xsd", local10schema, "eprints.xsd");
         Localize (f, "http://www.openarchives.org/OAI/oai-identifier.xsd", local10schema, "oai-identifier.xsd");
      }
      else if (strcmp (protocolVersion, "1.1") == 0)
      {
         Localize (f, "http://www.openarchives.org/OAI/1.1/OAI_Identify.xsd", local11schema, "OAI_Identify.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/OAI_ListSets.xsd", local11schema, "OAI_ListSets.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/OAI_ListMetadataFormats.xsd", local11schema, "OAI_ListMetadataFormats.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/OAI_ListIdentifiers.xsd", local11schema, "OAI_ListIdentifiers.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/OAI_ListRecords.xsd", local11schema, "OAI_ListRecords.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/OAI_GetRecord.xsd", local11schema, "OAI_GetRecord.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/dc.xsd", local11schema, "dc.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/oai_marc.xsd", local11schema, "oai_marc.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/rfc1807.xsd", local11schema, "rfc1807.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/eprints.xsd", local11schema, "eprints.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/oai-identifier.xsd", local11schema, "oai-identifier.xsd");
      }
      else
      {
         if ((strcmp (metadataPrefix, "oai_dc") == 0) &&
             ((strcmp (verb, "GetRecord") == 0) ||
              (strcmp (verb, "ListRecords") == 0)))
            Localize (f, "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", local20schema, "OAI-PMHdc.xsd");
         else
            Localize (f, "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", local20schema, "OAI-PMH.xsd");
         Localize (f, "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", local20schema, "oai_dc.xsd");
         Localize (f, "http://www.openarchives.org/OAI/2.0/provenance.xsd", local20schema, "provenance.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/oai_marc.xsd", local11schema, "oai_marc.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/rfc1807.xsd", local11schema, "rfc1807.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/eprints.xsd", local11schema, "eprints.xsd");
         Localize (f, "http://www.openarchives.org/OAI/1.1/oai-identifier.xsd", local11schema, "oai-identifier.xsd");
      }
   }

   f->SearchAndReplace (datafilename, localfilename);
 
   delete f;
}
