//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 1.0-1.1       |
// | Hussein Suleman                                                      |
// | January 2001                                                         |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : parser
//  Purpose    : Parse valid XML
// ======================================================================

// #include <fstream.h>
// #include <iostream.h>
#include <fstream>
#include <iostream>

// added pnh
#include <string.h>
using namespace std;

#include "escape.h"
#include "language.h"
#include "parser.h"

// ----------------------------------------------------------------------
//  Function   : Error{IllegalTag,..}
// ----------------------------------------------------------------------
//  Purpose    : Output error messages
//  Parameters : (variable)
//  Result     : (none)
// ----------------------------------------------------------------------

void ErrorIllegalTag ( char *tag, char *parent, char *correct )
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

void ErrorMissingField ( char *tag, char *parent )
{
   cout << Translate ("Error") << ": "
        << Translate ("Missing field")
        << " : &lt;" << parent << "&gt;/&lt;" << tag << "&gt;"        
        << "<p>";
}

void ErrorTextWhereTag ( char *text )
{
   cout << Translate ("Error") << ": " 
        << Translate ("Text found where tag expected")
        << " : ";
   Escape (text);
   cout << "<p>";
}

void ErrorTagNotFound ( char *tag )
{
   cout << Translate ("Error") << ": " 
        << Translate ("Tag expected but not found")
        << " : &lt;" << tag << "&gt;<p>";
}

void ErrorOnlyOneInstance ( char *tag )
{
   cout << Translate ("Error") << ": "
        << Translate ("Only one instance allowed")
        << " : &lt;" << tag << "&gt;<p>";
}

void ErrorTextNotFound ( char *tag )
{
   cout << Translate ("Error") << ": "
        << Translate ("Text expected for field but not found")
        << " : &lt;" << tag << "&gt;<p>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessIRecord
// ----------------------------------------------------------------------
//  Purpose    : Process identity tags
//  Parameters : 1. Tag pointer
//               2. Depth in nested structure
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessIRecord ( TagList *tl, int depth=0 )
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
         ProcessIRecord (p, depth+1);
         p = p->next;
      }
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessI
// ----------------------------------------------------------------------
//  Purpose    : Process Identify tag
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessI ( TagList *tl, char **requestURL, char **responseDate )
{
   cout << "<h2>" << Translate ("Archive Self-Description") 
        << "</h2><p><pre>";
   
   char *repositoryName = NULL;
   char *baseURL = NULL;
   char *protocolVersion = NULL;
   char *adminEmail = NULL;
   
   // check for mandatory fields
   TagList *p = tl->head;
   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "responseDate") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *responseDate = p->head->tag;
         }
         else if (strcmp (p->tag, "requestURL") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *requestURL = p->head->tag;
         }
         else if (strcmp (p->tag, "repositoryName") == 0)
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
      ErrorMissingField ("repositoryName", "Identify");
      cout << "</td></tr>";
   }
   if (baseURL)
      cout << "<tr><th align=left>" << Translate ("Base URL") 
           << "</th><td>" << baseURL << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField ("baseURL", "Identify");
      cout << "</td></tr>";
   }
   if (protocolVersion)
      cout << "<tr><th align=left>" << Translate ("Protocol Version")
           << "</th><td>" << protocolVersion << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField ("protocolVersion", "Identify");
      cout << "</td></tr>";
   }
   if (adminEmail)
      cout << "<tr><th align=left>" << Translate ("Admin Email")
           << "</th><td>" << adminEmail << "</td></tr>";
   else
   {
      cout << "<tr><td colspan=2>";
      ErrorMissingField ("adminEmail", "Identify");
      cout << "</td></tr>";
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
            ProcessIRecord (p);
         else
            if ((strcmp (p->tag, "responseDate") != 0) &&
                (strcmp (p->tag, "requestURL") != 0) &&
                (strcmp (p->tag, "repositoryName") != 0) &&
                (strcmp (p->tag, "baseURL") != 0) &&
                (strcmp (p->tag, "protocolVersion") != 0) &&
                (strcmp (p->tag, "adminEmail") != 0))
               ErrorIllegalTag (p->tag, "Identify", "repositoryName,baseURL,protocolVersion,adminEmail");
      }
      else
         ErrorTextWhereTag (p->tag);
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
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessIdentify ( TagList *tl, char **requestURL, char **responseDate )
{
   if (tl->head == NULL)
      ErrorTagNotFound ("Identify");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag (tl->head->tag);
   else if (strcmp (tl->head->tag, "Identify") != 0)
      ErrorIllegalTag (tl->head->tag, NULL, "Identify");
   else
      ProcessI (tl->head, requestURL, responseDate);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLSResumptionToken
// ----------------------------------------------------------------------
//  Purpose    : Process resumptionToken tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLSResumptionToken ( TagList *tl )
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
//  Parameters : 1. Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLSSet ( TagList *tl )
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
                  ErrorOnlyOneInstance ("setSpec");
               else
                  setSpec = p->head->tag;
            }
            else
               ErrorTextNotFound ("setSpec");
         }
         else if (strcmp (p->tag, "setName") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (setName)
                  ErrorOnlyOneInstance ("setName");
               else
                  setName = p->head->tag;
            }
            else
               ErrorTextNotFound ("setName");
         }
         else
            ErrorIllegalTag (p->tag, "set", "setSpec,setName");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }

   // output set link and description
   if (setSpec == NULL)
      ErrorMissingField ("setSpec", "set");
   else if (setName == NULL)
      ErrorMissingField ("setSpec", "set");
   else
   {   
      cout << "<a href=\"\" onClick=\"FillInLI (\'" << setSpec << "\');"
           << "return false\">";
      Escape (setName);
      cout << "</a>"; 
      cout << "<p>";
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessLS
// ----------------------------------------------------------------------
//  Purpose    : Process ListSets tag
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLS ( TagList *tl, char **requestURL, char **responseDate )
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
            ProcessLSSet (p);
         else if (strcmp (p->tag, "resumptionToken") == 0)
            ProcessLSResumptionToken (p);
         else if (strcmp (p->tag, "requestURL") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *requestURL = p->head->tag;
         }
         else if (strcmp (p->tag, "responseDate") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *responseDate = p->head->tag;
         }
         else
            ErrorIllegalTag (p->tag, "ListSets", "set");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessListSets
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response for List-Sets
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListSets ( TagList *tl, char **requestURL, char **responseDate )
{
   if (tl->head == NULL)
      ErrorTagNotFound ("ListSets");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListSets") != 0)
      ErrorIllegalTag (tl->head->tag, NULL, "ListSets");
   else
      ProcessLS (tl->head, requestURL, responseDate);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLMFMetadataFormat
// ----------------------------------------------------------------------
//  Purpose    : Process metadataformat tag
//  Parameters : 1. Tag pointer
//               2. Pointer to full id
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLMFMetadataFormat ( TagList *tl, char *identifier )
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
                  ErrorOnlyOneInstance ("metadataprefix");
               else
                  metadataPrefix = p->head->tag;
            }
            else
               ErrorTextNotFound ("metadataPrefix");
         }
         else if (strcmp (p->tag, "schema") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (schema)
                  ErrorOnlyOneInstance ("schema");
               else
                  schema = p->head->tag;
            }
            else
               ErrorTextNotFound ("schema");
         }
         else if (strcmp (p->tag, "metadataNamespace") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
            {
               if (metadataNamespace)
                  ErrorOnlyOneInstance ("metadataNamespace");
               else
                  metadataNamespace = p->head->tag;
            }
            else
               ErrorTextNotFound ("metadataNamespace");
         }
         else
            ErrorIllegalTag (p->tag, "metadataFormat", "metadataPrefix,schema,metadataNamespace");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }

   // output information about metadata format and links
   if (metadataPrefix == NULL)
      ErrorMissingField ("metadataPrefix", "metadataFormat");
   else if (schema == NULL)
      ErrorMissingField ("schema", "metadataFormat");
   else
   {   
      cout << Translate ("Prefix") << "=[" << metadataPrefix << "]<br>";
      if (metadataNamespace)
         cout << Translate ("NameSpace") << "=[" << metadataNamespace << "]<br>";
      cout << Translate ("Schema") << "=[" << "<a href=\"" << schema << "\">" << schema << "</a>]<p>";
   
      int metamatch = 0;
      fstream sfcmeta ("sfcmeta", ios::in);
      char ameta[1024];
      while (!(sfcmeta.eof ()))
      {
         sfcmeta.getline (ameta, sizeof (ameta));
         if (strcmp (ameta, metadataPrefix) == 0)
            metamatch = 1;
      }
   
      if (metamatch == 0)
      {
         cout << "[" << Translate ("Not a standard OAI metadata name")
              << "]";
      }
      
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
//               3. requestURL pointer
//               4. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLMF ( TagList *tl, char *identifier, char **requestURL, char **responseDate )
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
            ProcessLMFMetadataFormat (p, identifier);
         else if (strcmp (p->tag, "requestURL") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *requestURL = p->head->tag;
         }
         else if (strcmp (p->tag, "responseDate") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *responseDate = p->head->tag;
         }
         else
            ErrorIllegalTag (p->tag, "ListMetadataFormats", "metadataFormat");
      }
      else
         ErrorTextWhereTag (p->tag);
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
//               2. requestURL pointer
//               3. responseDate pointer

//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListMetadataFormats ( TagList *tl, char *identifier, char **requestURL, char **responseDate )
{
   if (tl->head == NULL)
      ErrorTagNotFound ("ListMetadataFormats");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListMetadataFormats") != 0)
      ErrorIllegalTag (tl->head->tag, NULL, "ListMetadataFormats");
   else
      ProcessLMF (tl->head, identifier, requestURL, responseDate);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLIIdentifier
// ----------------------------------------------------------------------
//  Purpose    : Process identifier within identifiers tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLIIdentifier ( TagList *tl )
{
   TagList *p = tl->head;
   while (p != NULL)
   {
      if ((p->type == TEXT) && (strlen (p->tag) > 0))
      {
         cout << p->tag << " ";
         cout << "<a href=\"\" onClick=\"FillInGR (\'oai_dc\',\'" << p->tag << "\');"
              << "return false\">" << "[" 
              << Translate ("display record in Dublin Core") << "]" << "</a> ";
         cout << "<a href=\"\" onClick=\"FillInLMF (\'" << p->tag << "\');"
              << "return false\">" << "["
              << Translate ("display metadata formats") << "]" << "</a> ";
         cout << "<p>";
      }
      p = p->next;
   }
}

// ----------------------------------------------------------------------
//  Function   : ProcessLIResumptionToken
// ----------------------------------------------------------------------
//  Purpose    : Process resumptionToken tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLIResumptionToken ( TagList *tl )
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
}

// ----------------------------------------------------------------------
//  Function   : ProcessLI
// ----------------------------------------------------------------------
//  Purpose    : Process ListIdentifiers tag
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLI ( TagList *tl, char **requestURL, char **responseDate )
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
         if (strcmp (p->tag, "requestURL") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *requestURL = p->head->tag;
         }
         else if (strcmp (p->tag, "responseDate") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *responseDate = p->head->tag;
         }
         else if (strcmp (p->tag, "identifier") == 0)
            ProcessLIIdentifier (p);
         else if (strcmp (p->tag, "resumptionToken") == 0)
            ProcessLIResumptionToken (p);
         else
            ErrorIllegalTag (p->tag, "ListIdentifiers", "identifier,resumptionToken");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessListIdentifiers
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response to ListIdentifiers
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListIdentifiers ( TagList *tl, char **requestURL, char **responseDate )
{
   if (tl->head == NULL)
      ErrorTagNotFound ("ListIdentifiers");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListIdentifiers") != 0)
      ErrorIllegalTag (tl->head->tag, NULL, "ListIdentifiers");
   else
      ProcessLI (tl->head, requestURL, responseDate);
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRField
// ----------------------------------------------------------------------
//  Purpose    : Process fields of record
//  Parameters : 1. Tag pointer
//               2. Depth in nested structure
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRField ( TagList *tl, int depth )
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
         ProcessGRField (p, depth+1);
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

void ProcessGRMetadata ( TagList *tl )
{
   cout << "<b>metadata:</b>\n";
   ProcessGRField (tl, 1);
   cout << "\n";
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRAbout
// ----------------------------------------------------------------------
//  Purpose    : Process record about tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRAbout ( TagList *tl )
{
   cout << "<b>about:</b>\n";
   ProcessGRField (tl, 1);
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRHeader
// ----------------------------------------------------------------------
//  Purpose    : Process record header tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRHeader ( TagList *tl )
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
               ErrorOnlyOneInstance ("identifier");
            else
               identifier = p;
         }
         else if (strcmp (p->tag, "datestamp") == 0)
         {
            if (datestamp)
               ErrorOnlyOneInstance ("datestamp");
            else
               datestamp = p;
         }
         else
            ErrorIllegalTag (p->tag, "header", "identifier,datestamp");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }

   // output header information
   cout << "\n\n<b>header:</b>\n";
   if (identifier)
   {
      if ((identifier->head) && (identifier->head->type == TEXT))
         cout << "  " << "identifier" << " : " 
              << identifier->head->tag << "\n";
      else
         ErrorTextNotFound ("identifier");
   }
   else
      ErrorMissingField ("identifier", "header");
   if (datestamp)
   {
      if ((datestamp->head) && (datestamp->head->type == TEXT))
         cout << "  " << "datestamp" 
              << " : " << datestamp->head->tag << "\n";
      else
         ErrorTextNotFound ("datestamp");
   }
   else
      ErrorMissingField ("datestamp", "header");
   cout << "\n";
}

// ----------------------------------------------------------------------
//  Function   : ProcessGRRecord
// ----------------------------------------------------------------------
//  Purpose    : Process record tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGRRecord ( TagList *tl )
{
   TagList *p = tl->head;
   TagList *header = NULL;
   TagList *metadata = NULL;
   TagList *about = NULL;

   while (p != NULL)
   {
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "header") == 0)
         {
            if (header)
               ErrorOnlyOneInstance ("header");
            else
               header = p->head;
         }
         else if (strcmp (p->tag, "metadata") == 0)
         {
            if (metadata)
               ErrorOnlyOneInstance ("metadata");
            else
               metadata = p->head;
         }
         else if (strcmp (p->tag, "about") == 0)
         {
            if (about)
               ErrorOnlyOneInstance ("about");
            else
               about = p->head;
         }
         else
            ErrorIllegalTag (p->tag, "record", "header,metadata,about");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }

   if (header)
      ProcessGRHeader (header);
   else
      ErrorMissingField ("header", "record");
   if (metadata)
      ProcessGRMetadata (metadata);
   if (about)
      ProcessGRAbout (about);
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

void ProcessGR ( TagList *tl, char **requestURL, char **responseDate )
{
   cout << "<h2>" << Translate ("List of Fields") << "</h2><pre>";
   
   // iterate over "records"
   TagList *p = tl->head;
   TagList *record = NULL;
   while (p != NULL)
   {  
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "requestURL") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *requestURL = p->head->tag;
         }
         else if (strcmp (p->tag, "responseDate") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *responseDate = p->head->tag;
         }
         else if (strcmp (p->tag, "record") == 0)
         {
            if (record)
               ErrorOnlyOneInstance ("record");
            else
               record = p;
         }
         else
            ErrorIllegalTag (p->tag, "GetRecord", "record");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }
   if (record)
      ProcessGRRecord (record);
   else
      ErrorMissingField ("record", "GetRecord");
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessGetRecord
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response to GetRecord
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessGetRecord ( TagList *tl, char **requestURL, char **responseDate )
{
   if (tl->head == NULL)
      ErrorTagNotFound ("GetRecord");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag (tl->head->tag);
   else if (strcmp (tl->head->tag, "GetRecord") != 0)
      ErrorIllegalTag (tl->head->tag, NULL, "GetRecord");
   else
      ProcessGR (tl->head, requestURL, responseDate);
}

// ----------------------------------------------------------------------
//  Function   : ProcessLRResumptionToken
// ----------------------------------------------------------------------
//  Purpose    : Process resumptionToken tag
//  Parameters : Tag pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLRResumptionToken ( TagList *tl )
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
}

// ----------------------------------------------------------------------
//  Function   : ProcessLR
// ----------------------------------------------------------------------
//  Purpose    : Process ListRecords tag
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessLR ( TagList *tl, char **requestURL, char **responseDate )
{
   cout << "<h2>" << Translate ("List of Records") 
        << "</h2><i>" << Translate ("Select a link to view more information")
        << "</i><p><pre>";

   TagList *p = tl->head;
   while (p != NULL)
   {  
      if (p->type == TAG)
      {
         if (strcmp (p->tag, "requestURL") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *requestURL = p->head->tag;
         }
         else if (strcmp (p->tag, "responseDate") == 0)
         {
            if ((p->head) && (p->head->type == TEXT))
               *responseDate = p->head->tag;
         }
         else if (strcmp (p->tag, "resumptionToken") == 0)
            ProcessLRResumptionToken (p);
         else if (strcmp (p->tag, "record") == 0)
            ProcessGRRecord (p);
         else
            ErrorIllegalTag (p->tag, "ListRecords", "record,resumptionToken");
      }
      else
         ErrorTextWhereTag (p->tag);
      p = p->next;
   }
   cout << "</pre>";
}

// ----------------------------------------------------------------------
//  Function   : ProcessListRecords
// ----------------------------------------------------------------------
//  Purpose    : Process OAI response to ListRecords
//  Parameters : 1. Tag pointer
//               2. requestURL pointer
//               3. responseDate pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessListRecords ( TagList *tl, char **requestURL, char **responseDate )
{
   if (tl->head == NULL)
      ErrorTagNotFound ("ListRecord");
   else if (tl->head->type == TEXT)
      ErrorTextWhereTag (tl->head->tag);
   else if (strcmp (tl->head->tag, "ListRecords") != 0)
      ErrorIllegalTag (tl->head->tag, NULL, "ListRecords");
   else
      ProcessLR (tl->head, requestURL, responseDate);
}
