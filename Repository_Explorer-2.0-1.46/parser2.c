//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 2.0-1.46      |
// | Hussein Suleman                                                      |
// | January 2005                                                         |
//  ----------------------------------------------------------------------
// |  University of Cape Town                                             |
// |  Department of Computer Science                                      |
// |  Advanced Information Management Laboratory                          |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : parser
//  Purpose    : Parse valid XML
// ======================================================================

// #include <fstream.h>
// #include <iostream.h>
#include <fstream>
#include <iostream>

using namespace std;

#include <string.h>

#include "config.h"
#include "escape.h"
#include "language.h"
#include "parser2.h"

void ProcessGRField2 ( TagList *, int );
void ProcessGRHeader2 ( TagList * );

// ----------------------------------------------------------------------
//  Function   : Error{IllegalTag,..}
// ----------------------------------------------------------------------
//  Purpose    : Output error messages
//  Parameters : (variable)
//  Result     : (none)
// ----------------------------------------------------------------------

void ErrorIllegalTag2 ( char *tag, char *parent, char *correct )
{
   cout << Translate ("Error") << ": " 
        << Translate ("Illegal tag")
        << " : ";
   if (parent)
      cout << "&lt;" << parent << "&gt;/";
   cout << "&lt;" << tag << "&gt;";
   if (correct)
      cout << " <> {" << correct << "}";
   cout << "<p>";
}

void ErrorMissingField2 ( char *tag, char *parent )
{
   cout << Translate ("Error") << ": "
        << Translate ("Missing field")
        << " : &lt;" << parent << "&gt;/&lt;" << tag << "&gt;"        
        << "<p>";
}

void ErrorTextWhereTag2 ( char *text )
{
   cout << Translate ("Error") << ": " 
        << Translate ("Text found where tag expected")
        << " : ";
   Escape (text);
   cout << "<p>";
}

void ErrorTagNotFound2 ( char *tag )
{
   cout << Translate ("Error") << ": " 
        << Translate ("Tag expected but not found")
        << " : &lt;" << tag << "&gt;<p>";
}

void ErrorOnlyOneInstance2 ( char *tag )
{
   cout << Translate ("Error") << ": "
        << Translate ("Only one instance allowed")
        << " : &lt;" << tag << "&gt;<p>";
}

void ErrorTextNotFound2 ( char *tag )
{
   cout << Translate ("Error") << ": "
        << Translate ("Text expected for field but not found")
        << " : &lt;" << tag << "&gt;<p>";
}

int GeneralError2 ( TagList *tl )
{
   int found = 0;
   
   // check for error fields
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "error") == 0)
         {
            if (found == 0)
            {
               cout << "<table width=\"100%\" cellpadding=\"5\" cellspacing=\"5\">";
               found = 1;
            }
         
            char *description = NULL;
            if ((p->head) && (p->head->type == TEXT))
               description = p->head->tag;
            char *code = p->attr->Search ("code");
            cout << "<tr><th bgcolor=\"#" << headercolorpreset << "\">"
                 << Translate ("Error") << "/" << Translate ("Exception")
                 << "</th><td bgcolor=\"#" << blockcolorpreset << "\">";
            if (code)
               cout << Translate ("Code") << "=" << code << ", ";
            if (description)
               cout << Translate ("Description") << "=\"" << description << "\"";
            cout << "</td></tr>";
         }
      }
      p = p->next;
   }
   
   if (found == 1)
   {
      cout << "</table><hr>";
      return 1;
   }
   else
   {
      return 0;
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessIRecord
// ----------------------------------------------------------------------
//  Purpose    : Process identity tags
//  Parameters : 1. Tag pointer
//               2. Depth in nested structure
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessIRecord2 ( TagList *tl, int depth=0 )
{
   // indent
   for ( int i=0; i<depth*3; i++ )
      cout << " ";
      
   // output tag
   Escape (tl->tag);
   cout << ": ";
   tl->attr->List ();
   
   // empty tag
   if (tl->head == NULL)
   {
      cout << "\n";
   }
   
   // tag containing text
   else if (tl->head->type == TEXT)
   {
      Escape (tl->head->tag);
      cout << "\n";
   }
   
   // tag with child elements
   else
   {
      cout << "\n";
      TagList *p = tl->head;
      while (p != NULL)
      {
         ProcessIRecord2 (p, depth+1);
         p = p->next;
      }
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessI
// ----------------------------------------------------------------------
//  Purpose    : Process Identify tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessI2 ( TagList *tl )
{
   cout << "<h2>" << Translate ("Archive Self-Description") 
        << "</h2><p><pre>";
   
   char *repositoryName = NULL;
   char *baseURL = NULL;
   char *protocolVersion = NULL;
   char *adminEmail = NULL;
   char *granularity = NULL;
   char *earliestDatestamp = NULL;
   char *deletedRecord = NULL;
   
   // check for mandatory fields
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "repositoryName") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               repositoryName = p->head->tag;
         }
         else if (strcmp (p->tag, "baseURL") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               baseURL = p->head->tag;
         }
         else if (strcmp (p->tag, "protocolVersion") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               protocolVersion = p->head->tag;
         }
         else if (strcmp (p->tag, "adminEmail") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               adminEmail = p->head->tag;
         }
         else if (strcmp (p->tag, "granularity") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               granularity = p->head->tag;
         }
         else if (strcmp (p->tag, "earliestDatestamp") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               earliestDatestamp = p->head->tag;
         }
         else if (strcmp (p->tag, "deletedRecord") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               deletedRecord = p->head->tag;
         }
      }
      p = p->next;
   }
   
   // output metadata
   cout << "<table border=1 cellspacing=2 cellpadding=5>";
   if (repositoryName)
      cout << "<tr><th align=left>" << Translate ("Repository Name") 
           << "</th><td>" << repositoryName << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField2 ("repositoryName", "Identify");
      cout << "</td></tr>";
   }
   
   if (baseURL)
      cout << "<tr><th align=left>" << Translate ("Base URL") 
           << "</th><td>" << baseURL << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField2 ("baseURL", "Identify");
      cout << "</td></tr>";
   }
   
   if (protocolVersion)
      cout << "<tr><th align=left>" << Translate ("Protocol Version")
           << "</th><td>" << protocolVersion << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField2 ("protocolVersion", "Identify");
      cout << "</td></tr>";
   }
   
   // list admin fields
   p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "adminEmail") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               cout << "<tr><th align=left>" << Translate ("Admin Email")
                    << "</th><td>" << p->head->tag << "</td></tr>";
         }
      }
      p = p->next;
   }

   if (adminEmail == NULL)
//      cout << "<tr><th align=left>" << Translate ("Admin Email")
//           << "</th><td>" << adminEmail << "</td></tr>";
//   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField2 ("adminEmail", "Identify");
      cout << "</td></tr>";
   }

   if (earliestDatestamp)
      cout << "<tr><th align=left>" << Translate ("Earliest Datestamp")
           << "</th><td>" << earliestDatestamp << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField2 ("earliestDatestamp", "Identify");
      cout << "</td></tr>";
   }

   if (deletedRecord)
      cout << "<tr><th align=left>" << Translate ("Deleted Record Handling")
           << "</th><td>" << deletedRecord << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField2 ("deletedRecord", "Identify");
      cout << "</td></tr>";
   }

   if (granularity)
      cout << "<tr><th align=left>" << Translate ("Granularity")
           << "</th><td>" << granularity << "</td></tr>";

   // list compression fields
   p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "compression") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               cout << "<tr><th align=left>" << Translate ("Compression")
                    << "</th><td>" << p->head->tag << "</td></tr>";
         }
      }
      p = p->next;
   }

   cout << "<tr><th align=left>" << Translate ("Other Information")
        << "</th><td><pre>";

   // process optional descriptions
   p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "description") == 0)
            ProcessIRecord2 (p);
         else
            if ((strcmp (p->tag, "responseDate") != 0) &&
                (strcmp (p->tag, "requestURL") != 0) &&
                (strcmp (p->tag, "repositoryName") != 0) &&
                (strcmp (p->tag, "baseURL") != 0) &&
                (strcmp (p->tag, "protocolVersion") != 0) &&
                (strcmp (p->tag, "adminEmail") != 0) &&
                (strcmp (p->tag, "granularity") != 0) &&
                (strcmp (p->tag, "compression") != 0) &&
                (strcmp (p->tag, "earliestDatestamp") != 0) &&
                (strcmp (p->tag, "deletedRecord") != 0))
               ErrorIllegalTag2 (p->tag, "Identify", "repositoryName,baseURL,protocolVersion,adminEmail,earliestDatestamp,deletedRecord,granularity,compression");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }

   cout << "</pre></td></tr>";
   cout << "</table>";

   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessIdentify
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response for ListIdentify
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessIdentify2 ( TagList *tl )
{
   if (tl->head == NULL)
      ErrorTagNotFound2 ("Identify");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag2 (tl->head->tag);
   else if (strcmp (tl->head->tag, "Identify") != 0)
      ErrorIllegalTag2 (tl->head->tag, NULL, "Identify");
   else
      ProcessI2 (tl->head);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLSResumptionToken
// ----------------------------------------------------------------------
//  Purpose    : Process resumptionToken tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLSResumptionToken2 ( TagList *tl )
{
   TagList *p = tl->head;
   while (p != NULL)
   {
      if ((p->type == TEXT) && (strlen (p->tag) > 0))
      {
         cout << "\n <a href=\"\" onClick=\"FillInRT (\'ListSets\', \'" << p->tag << "\');"
              << "return false\">" 
              << Translate ("Resume from") << " [" << p->tag << "] </a> ";
         cout << "\n\n";
      }
      p = p->next;
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessLSSet
// ----------------------------------------------------------------------
//  Purpose    : Process set tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLSSet2 ( TagList *tl )
{
   char *setSpec = NULL;
   char *setName = NULL;

   // find fields in metadata
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "setSpec") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (setSpec)
                  ErrorOnlyOneInstance2 ("setSpec");
               else
                  setSpec = p->head->tag;
            }
            else
               ErrorTextNotFound2 ("setSpec");
         }
         else if (strcmp (p->tag, "setName") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (setName)
                  ErrorOnlyOneInstance2 ("setName");
               else
                  setName = p->head->tag;
            }
            else
               ErrorTextNotFound2 ("setName");
         }
         else if (strcmp (p->tag, "setDescription") != 0)
            ErrorIllegalTag2 (p->tag, "set", "setSpec,setName,setDescription");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }

   // output set link and description
   if (setSpec == NULL)
      ErrorMissingField2 ("setSpec", "set");
   else if (setName == NULL)
      ErrorMissingField2 ("setSpec", "set");
   else
   {   
      cout << "<a href=\"\" onClick=\"FillInLI (\'" << setSpec << "\');"
           << "return false\">";
      Escape (setName);
      cout << "</a>"; 
      cout << "<p>";
   }

   // iterate over set descriptions
   p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "setDescription") == 0)
         {
            cout << "<p><b>set description:</b>\n";
            TagList *q = p->head;
            while (q != NULL)
            {
               ProcessGRField2 (q, 1);
               q = q->next;
            }
            cout << "</p>\n";
         }
      }
      p = p->next;
   }

}

// ----------------------------------------------------------------------
//  Function   : ProcessLS
// ----------------------------------------------------------------------
//  Purpose    : Process ListSets tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLS2 ( TagList *tl )
{
   cout << "<h2>" << Translate ("List of Sets") << "</h2><i>"
        << Translate ("Click on the link to list the contents")
        << "</i><p><pre>";
   
   // iterate over all sets
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "set") == 0)
            ProcessLSSet2 (p);
         else if (strcmp (p->tag, "resumptionToken") == 0)
            ProcessLSResumptionToken2 (p);
         else
            ErrorIllegalTag2 (p->tag, "ListSets", "set");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessListSets
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response for List-Sets
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListSets2 ( TagList *tl )
{
   if (tl->head == NULL)
      ErrorTagNotFound2 ("ListSets");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag2 (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListSets") != 0)
      ErrorIllegalTag2 (tl->head->tag, NULL, "ListSets");
   else
      ProcessLS2 (tl->head);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLMFMetadataFormat
// ----------------------------------------------------------------------
//  Purpose    : Process metadataformat tag
//  Parameters : 1. Tag pointer
//               2. Pointer to full id
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLMFMetadataFormat2 ( TagList *tl, char *identifier )
{
   char *metadataPrefix = NULL;
   char *schema = NULL;
   char *metadataNamespace = NULL;

   // find fields in tag list
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "metadataPrefix") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (metadataPrefix)
                  ErrorOnlyOneInstance2 ("metadataprefix");
               else
                  metadataPrefix = p->head->tag;
            }
            else
               ErrorTextNotFound2 ("metadataPrefix");
         }
         else if (strcmp (p->tag, "schema") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (schema)
                  ErrorOnlyOneInstance2 ("schema");
               else
                  schema = p->head->tag;
            }
            else
               ErrorTextNotFound2 ("schema");
         }
         else if (strcmp (p->tag, "metadataNamespace") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (metadataNamespace)
                  ErrorOnlyOneInstance2 ("metadataNamespace");
               else
                  metadataNamespace = p->head->tag;
            }
            else
               ErrorTextNotFound2 ("metadataNamespace");
         }
         else
            ErrorIllegalTag2 (p->tag, "metadataFormat", "metadataPrefix,schema,metadataNamespace");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }

   // output information about metadata format and links
   if (metadataPrefix == NULL)
      ErrorMissingField2 ("metadataPrefix", "metadataFormat");
   else if (schema == NULL)
      ErrorMissingField2 ("schema", "metadataFormat");
   else
   {   
      cout << Translate ("Prefix") << "=[" << metadataPrefix << "]<br>";
      if (metadataNamespace)
         cout << Translate ("NameSpace") << "=[" << metadataNamespace << "]<br>";
      cout << Translate ("Schema") << "=[" << "<a href=\"" << schema << "\">" << schema << "</a>]<p>";
  
      if ((identifier) && (strlen (identifier) > 0))
      {
         cout << " <a href=\"\" onClick=\"FillInGR (\'" << metadataPrefix << "\',\'" << identifier << "\');"
              << "return false\">" << "[" 
              << Translate ("display record") << "]" << "</a> ";
      }

      cout << "<p>";
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessLMF
// ----------------------------------------------------------------------
//  Purpose    : Process ListMetadataFormats tag
//  Parameters : 1. Tag pointer
//               2. Pointer to full id
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLMF2 ( TagList *tl, char *identifier )
{
   cout << "<h2>" << Translate ("List of Metadata Formats") 
        << "</h2><i>" << Translate ("Click on the link to view schema")
        << "</i><p>";

   // iterate over metadata formats
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "metadataFormat") == 0)
            ProcessLMFMetadataFormat2 (p, identifier);
         else
            ErrorIllegalTag2 (p->tag, "ListMetadataFormats", "metadataFormat");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessListMetadataFormats
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response for ListMetadataFormats
//  Parameters : 1. Tag pointer
//               2. Pointer to full id
//  Parameters : 1. Tag pointer
//               2. identifier
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListMetadataFormats2 ( TagList *tl, char *identifier )
{
   if (tl->head == NULL)
      ErrorTagNotFound2 ("ListMetadataFormats");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag2 (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListMetadataFormats") != 0)
      ErrorIllegalTag2 (tl->head->tag, NULL, "ListMetadataFormats");
   else
      ProcessLMF2 (tl->head, identifier);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLIIdentifier
// ----------------------------------------------------------------------
//  Purpose    : Process identifier within identifiers tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLIIdentifier2 ( TagList *tl )
{
   TagList *p = tl->head;
   TagList *identifier = NULL;
   TagList *datestamp = NULL;

   // iterate over fields
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "identifier") == 0)
         {
            if (identifier)
               ErrorOnlyOneInstance2 ("identifier");
            else
               identifier = p;
         }
         else if (strcmp (p->tag, "datestamp") == 0)
         {
            if (datestamp)
               ErrorOnlyOneInstance2 ("datestamp");
            else
               datestamp = p;
         }
         else if (strcmp (p->tag, "setSpec") != 0)
            ErrorIllegalTag2 (p->tag, "header", "identifier,datestamp,setSpec");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }
   
   // check for status
   char *status = tl->attr->Search ("status");

   // output header information
   cout << "\n\n<b>header:</b>\n";
   if (status)
   {
      cout << " status : " << status << "\n";
   }
   if (identifier)
   {
      if ((identifier->head) && (identifier->head->type == TEXT))
         cout << "  " << "identifier" << " : " 
              << identifier->head->tag << "\n";
      else
         ErrorTextNotFound2 ("identifier");
   }
   else
      ErrorMissingField2 ("identifier", "header");
   if (datestamp)
   {
      if ((datestamp->head) && (datestamp->head->type == TEXT))
         cout << "  " << "datestamp" 
              << " : " << datestamp->head->tag << "\n";
      else
         ErrorTextNotFound2 ("datestamp");
   }
   else
      ErrorMissingField2 ("datestamp", "header");
      
   // iterate over setSpec fields
   p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "setSpec") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               cout << "  " << "setSpec" << " : "
                    << p->head->tag << "\n";
         }
      }
      p = p->next;
   }
   cout << "\n";

   if ((identifier) && (identifier->head) && (identifier->head->type == TEXT))
   {
      cout << "<a href=\"\" onClick=\"FillInGR (\'oai_dc\',\'" << identifier->head->tag << "\');"
           << "return false\">" << "[" 
           << Translate ("display record in Dublin Core") << "]" << "</a> ";
      cout << "<a href=\"\" onClick=\"FillInLMF (\'" << identifier->head->tag << "\');"
           << "return false\">" << "["
           << Translate ("display metadata formats") << "]" << "</a> ";
      cout << "<p>";
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessLIResumptionToken
// ----------------------------------------------------------------------
//  Purpose    : Process resumptionToken tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLIResumptionToken2 ( TagList *tl )
{
   TagList *p = tl->head;
   while (p != NULL)
   {
      if ((p->type == TEXT) && (strlen (p->tag) > 0))
      {
         cout << "\n <a href=\"\" onClick=\"FillInRT (\'ListIdentifiers\',\'" << p->tag << "\');"
              << "return false\">" 
              << Translate ("Resume from") << " [" << p->tag << "] </a> ";
         cout << "\n\n";
      }
      p = p->next;
   }

   char *completeListSize = tl->attr->Search ("completeListSize");
   char *cursor = tl->attr->Search ("cursor");
//   char *resumeAfter = tl->attr->Search ("resumeAfter");
   char *expirationDate = tl->attr->Search ("expirationDate");
   
   if (completeListSize)
      cout << Translate ("Complete List Size") << " : " << completeListSize << "\n";
   if (cursor)
      cout << Translate ("Cursor") << " : " << cursor << "\n";
//   if (resumeAfter)
//      cout << Translate ("Resume After") << " : " << resumeAfter << "\n";
   if (expirationDate)
      cout << Translate ("Expiration Date") << " : " << expirationDate << "\n";
}

// ----------------------------------------------------------------------
//  Function   : ProcessLI
// ----------------------------------------------------------------------
//  Purpose    : Process ListIdentifiers tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLI2 ( TagList *tl )
{
   cout << "<h2>" << Translate ("List of Record Identifiers")
        << "</h2><i>" << Translate ("Select a link to view more information")
        << "</i><p><pre>";
        
   // iterate over identifiers
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "header") == 0)
            ProcessLIIdentifier2 (p);
         else if (strcmp (p->tag, "resumptionToken") == 0)
            ProcessLIResumptionToken2 (p);
         else
            ErrorIllegalTag2 (p->tag, "ListIdentifiers", "identifier,resumptionToken");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessListIdentifiers
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response to ListIdentifiers
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListIdentifiers2 ( TagList *tl )
{
   if (tl->head == NULL)
      ErrorTagNotFound2 ("ListIdentifiers");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag2 (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListIdentifiers") != 0)
      ErrorIllegalTag2 (tl->head->tag, NULL, "ListIdentifiers");
   else
      ProcessLI2 (tl->head);
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRField
// ----------------------------------------------------------------------
//  Purpose    : Process fields of record
//  Parameters : 1. Tag pointer
//               2. Depth in nested structure
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRField2 ( TagList *tl, int depth )
{
   // indent
   for ( int i=0; i<depth*3; i++ )
      cout << " ";
      
   // output tag and attributes   
   Escape (tl->tag);
   cout << ": ";
   tl->attr->List (); 

   // empty tag
   if (tl->head == NULL)
      cout << "\n";
   
   // tag containing text
   else if (tl->head->type == TEXT)
   {
      // check for links within metadata
      int http = 0;
      if (strlen (tl->head->tag) > 7)
      {
         char protocol[8];
         for ( int j=0; j<7; j++ )
            protocol[j] = tl->head->tag[j];
         protocol[7] = 0;
         if (strcmp (protocol, "http://") == 0)
            http = 1;
      }
      if (http)
         cout << "<a href=\"" << tl->head->tag << "\">" << tl->head->tag << "</a>\n";
      else
      {
         Escape (tl->head->tag);
         cout << "\n";
      }
   }
   
   // tag with elements
   else
   {
      cout << "\n";
      TagList *p = tl->head;
      while (p != NULL)
      {
         ProcessGRField2 (p, depth+1);
         p = p->next;
      }
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRMetadata
// ----------------------------------------------------------------------
//  Purpose    : Process record metadata tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRMetadata2 ( TagList *tl )
{
   cout << "<b>metadata:</b>\n";
   ProcessGRField2 (tl, 1);
   cout << "\n";
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRAbout
// ----------------------------------------------------------------------
//  Purpose    : Process record about tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRAbout2 ( TagList *tl )
{
   cout << "<b>about:</b>\n";
   ProcessGRField2 (tl, 1);
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRHeader
// ----------------------------------------------------------------------
//  Purpose    : Process record header tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRHeader2 ( TagList *tl )
{
   TagList *p = tl;
   TagList *identifier = NULL;
   TagList *datestamp = NULL;

   // iterate over fields
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "identifier") == 0)
         {
            if (identifier)
               ErrorOnlyOneInstance2 ("identifier");
            else
               identifier = p;
         }
         else if (strcmp (p->tag, "datestamp") == 0)
         {
            if (datestamp)
               ErrorOnlyOneInstance2 ("datestamp");
            else
               datestamp = p;
         }
         else if (strcmp (p->tag, "setSpec") != 0)
            ErrorIllegalTag2 (p->tag, "header", "identifier,datestamp,setSpec");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }
   
   // check for status
   char *status = tl->attr->Search ("status");

   // output header information
   cout << "\n\n<b>header:</b>\n";
   if (status)
   {
      cout << " status : " << status << "\n";
   }
   if (identifier)
   {
      if ((identifier->head) && (identifier->head->type == TEXT))
         cout << "  " << "identifier" << " : " 
              << identifier->head->tag << "\n";
      else
         ErrorTextNotFound2 ("identifier");
   }
   else
      ErrorMissingField2 ("identifier", "header");
   if (datestamp)
   {
      if ((datestamp->head) && (datestamp->head->type == TEXT))
         cout << "  " << "datestamp" 
              << " : " << datestamp->head->tag << "\n";
      else
         ErrorTextNotFound2 ("datestamp");
   }
   else
      ErrorMissingField2 ("datestamp", "header");
      
   // iterate over setSpec fields
   p = tl;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "setSpec") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               cout << "  " << "setSpec" << " : "
                    << p->head->tag << "\n";
         }
      }
      p = p->next;
   }

   cout << "\n";
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRRecord
// ----------------------------------------------------------------------
//  Purpose    : Process record tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRRecord2 ( TagList *tl )
{
   TagList *p = tl->head;
   TagList *header = NULL;
   TagList *metadata = NULL;

   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "header") == 0)
         {
            if (header)
               ErrorOnlyOneInstance2 ("header");
            else
               header = p->head;
         }
         else if (strcmp (p->tag, "metadata") == 0)
         {
            if (metadata)
               ErrorOnlyOneInstance2 ("metadata");
            else
               metadata = p->head;
         }
         else if (strcmp (p->tag, "about") != 0)
            ErrorIllegalTag2 (p->tag, "record", "header,metadata,about");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }

   if (header)
      ProcessGRHeader2 (header);
   else
      ErrorMissingField2 ("header", "record");
   if (metadata)
      ProcessGRMetadata2 (metadata);

   // iterate over about containers
   p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "about") == 0)
         {
            ProcessGRAbout2 (p->head);
         }
      }
      p = p->next;
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessGR
// ----------------------------------------------------------------------
//  Purpose    : Process GetRecord tag
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGR2 ( TagList *tl )
{
   cout << "<h2>" << Translate ("List of Fields") << "</h2><pre>";

   // iterate over "records"
   TagList *p = tl->head;
   TagList *record = NULL;
   while (p != NULL)
   {  
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "record") == 0)
         {
            if (record)
               ErrorOnlyOneInstance2 ("record");
            else
               record = p;
         }
         else
            ErrorIllegalTag2 (p->tag, "GetRecord", "record");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }
   if (record)
      ProcessGRRecord2 (record);
   else
      ErrorMissingField2 ("record", "GetRecord");
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessGetRecord
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response to GetRecord
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGetRecord2 ( TagList *tl )
{
   if (tl->head == NULL)
      ErrorTagNotFound2 ("GetRecord");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag2 (tl->head->tag);
   else if (strcmp (tl->head->tag, "GetRecord") != 0)
      ErrorIllegalTag2 (tl->head->tag, NULL, "GetRecord");
   else
      ProcessGR2 (tl->head);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLRResumptionToken
// ----------------------------------------------------------------------
//  Purpose    : Process resumptionToken tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLRResumptionToken2 ( TagList *tl )
{
   TagList *p = tl->head;
   while (p != NULL)
   {
      if ((p->type == TEXT) && (strlen (p->tag) > 0))
      {
         cout << "\n <a href=\"\" onClick=\"FillInRT (\'ListRecords\',\'" << p->tag << "\');"
              << "return false\">" 
              << Translate ("Resume from") << " [" << p->tag << "] </a> ";
         cout << "\n\n";
      }
      p = p->next;
   }

   char *completeListSize = tl->attr->Search ("completeListSize");
   char *cursor = tl->attr->Search ("cursor");
//   char *resumeAfter = tl->attr->Search ("resumeAfter");
   char *expirationDate = tl->attr->Search ("expirationDate");
   
   if (completeListSize)
      cout << Translate ("Complete List Size") << " : " << completeListSize << "\n";
   if (cursor)
      cout << Translate ("Cursor") << " : " << cursor << "\n";
//   if (resumeAfter)
//      cout << Translate ("Resume After") << " : " << resumeAfter << "\n";
   if (expirationDate)
      cout << Translate ("Expiration Date") << " : " << expirationDate << "\n";
}

// ----------------------------------------------------------------------
//  Function   : ProcessLR
// ----------------------------------------------------------------------
//  Purpose    : Process ListRecords tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLR2 ( TagList *tl )
{
   cout << "<h2>" << Translate ("List of Records") 
        << "</h2><i>" << Translate ("Select a link to view more information")
        << "</i><p><pre>";

   TagList *p = tl->head;
   while (p != NULL)
   {  
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "resumptionToken") == 0)
            ProcessLRResumptionToken2 (p);
         else if (strcmp (p->tag, "record") == 0)
            ProcessGRRecord2 (p);
         else
            ErrorIllegalTag2 (p->tag, "ListRecords", "record,resumptionToken");
      }
      else
         ErrorTextWhereTag2 (p->tag);
      p = p->next;
   }
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessListRecords
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response to ListRecords
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListRecords2 ( TagList *tl )
{
   if (tl->head == NULL)
      ErrorTagNotFound2 ("ListRecords");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag2 (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListRecords") != 0)
      ErrorIllegalTag2 (tl->head->tag, NULL, "ListRecords");
   else
      ProcessLR2 (tl->head);
}

// ----------------------------------------------------------------------
//  Function   : ProcessOAIPMH
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response to all verbs
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//               4. verb
//               5. identifier for LMFs
//               6. buffer to use for request
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessOAIPMH ( TagList *tl, char **requestURL, char **responseDate, char *verb, char* identifier, char *buffer )
{
   if (tl->head == NULL)
      ErrorTagNotFound2 ("OAI-PMH");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag2 (tl->head->tag);
   else if (strcmp (tl->head->tag, "OAI-PMH") != 0)
      ErrorIllegalTag2 (tl->head->tag, NULL, "OAI-PMH");
   else
   {
      // get request url and response date
      TagList *p = tl->head->head;
      while (p != NULL)
      {  
         if (p->type == TAG)
         {
            if (strcmp (p->tag, "request") == 0)
            {
               if ((p->head) && (p->head->type == TEXT))
               {
                  *requestURL = buffer;
                  strcpy (*requestURL, p->head->tag);
                  char *verb = p->attr->Search ("verb");
                  char *from = p->attr->Search ("from");
                  char *until = p->attr->Search ("until");
                  char *metadataPrefix = p->attr->Search ("metadataPrefix");
                  char *identifier = p->attr->Search ("identifier");
                  char *set = p->attr->Search ("set");
                  char *resumptionToken = p->attr->Search ("resumptionToken");
                  if (verb) { strcat (*requestURL, ", verb="); strcat (*requestURL, verb); }
                  if (from) { strcat (*requestURL, ", from="); strcat (*requestURL, from); }
                  if (until) { strcat (*requestURL, ", until="); strcat (*requestURL, until); }
                  if (metadataPrefix) { strcat (*requestURL, ", metadataPrefix="); strcat (*requestURL, metadataPrefix); }
                  if (identifier) { strcat (*requestURL, ", identifier="); strcat (*requestURL, identifier); }
                  if (set) { strcat (*requestURL, ", set="); strcat (*requestURL, set); }
                  if (resumptionToken) { strcat (*requestURL, ", resumptionToken="); strcat (*requestURL, resumptionToken); }
               }
            }
            else if (strcmp (p->tag, "responseDate") == 0)
            {
               if ((p->head) && (p->head->type == TEXT))
                  *responseDate = p->head->tag;
            }
         }
         p = p->next;
      }
      
      // get errors
      int found = GeneralError2 (tl->head);
      
      // find response tag container
      p = tl->head->head;
      while (p != NULL)
      {
         if (p->type == TAG)
         {
            if (strcmp (p->tag, verb) == 0)
            {
               if (strcmp (verb, "Identify") == 0)
                  ProcessI2 (p);
               else if (strcmp (verb, "ListSets") == 0)
                  ProcessLS2 (p);
               else if (strcmp (verb, "ListMetadataFormats") == 0)
                  ProcessLMF2 (p, identifier);
               else if (strcmp (verb, "ListIdentifiers") == 0)
                  ProcessLI2 (p);
               else if (strcmp (verb, "GetRecord") == 0)
                  ProcessGR2 (p);
               else if (strcmp (verb, "ListRecords") == 0)
                  ProcessLR2 (p);
               found += 1;
            }
         }
         p = p->next;
      }
      
      // check for correct number of tags
      if (found == 0)
         ErrorTagNotFound2 (verb);
      else if (found > 1)
         ErrorOnlyOneInstance2 (verb);
   }      
}

