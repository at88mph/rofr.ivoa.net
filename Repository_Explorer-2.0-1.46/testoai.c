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
//  Module     : testoai
//  Purpose    : Main program
// ======================================================================

#include <dirent.h>
//
// #include <fstream.h>
// #include <iostream.h>
#include <fstream>
#include <iostream>

// added pnh
using namespace std;

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/types.h>

#include "blocks.h"
#include "cgi.h"
#include "config.h"
#include "language.h"
#include "parser.h"
#include "parser2.h"
#include "sortlist.h"
#include "taglist.h"
#include "validate.h"
#include "xml.h"

// ----------------------------------------------------------------------
//  Function   : GetTimeString
// ----------------------------------------------------------------------
//  Purpose    : Get current time/date in string format
//  Parameters : (none)
//  Result     : Time/date string
// ----------------------------------------------------------------------

char *GetTimeString ()
{
   static char nowstr[80];
   time_t tt;
   time (&tt);
   strcpy (nowstr, ctime (&tt));
   nowstr[strlen (nowstr)-1] = 0;
   return nowstr;
}

// ----------------------------------------------------------------------
//  Function   : Log
// ----------------------------------------------------------------------
//  Purpose    : Log activity with time/date stamp
//  Parameters : String to log
//  Result     : (none)
// ----------------------------------------------------------------------

void Log ( char *s )
{
   fstream f ("userlog", ios::app|ios::out);
   f << GetTimeString () << " : ";
   f << getenv ("REMOTE_ADDR") << " : ";
   f << s << "\n";
   f.close ();
}

// ----------------------------------------------------------------------
//  Function   : OutputArchiveSelector
// ----------------------------------------------------------------------
//  Purpose    : Display archive input box and preselecteds
//  Parameters : 1. language name
//               2. bgcolor
//               3. headercolor
//               4. blockcolor
//               5. archive baseURL
//  Result     : (none)
// ----------------------------------------------------------------------

void OutputArchiveSelector ( char *language, char *bgcolor, 
                             char *headercolor, char *blockcolor,
                             char *archive )
{
   cout << "<center>"
        << "<p>" << Translate ("This site presents an interface to interactively test archives for compliance with the OAI Protocol for Metadata Harvesting")
        << " [ <a href=\"http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm\">"
        << Translate ("Click here for details") << "</a> ]"
        << "</p><p>" << Translate ("JavaScript is required")
        << "</p><p>" << Translate ("Note: To avoid HTTP errors, please wait for each page to finish loading before clicking on any link.")
        << "</p></center><hr/>";

   cout << "<p>" << "<table><tr><td>"
        << Translate ("Enter the OAI baseURL")
        << "</td><td> : </td><td><input type=\"text\" size=\"100\" name=\"archive\"></td></tr>"
        << "<tr><td colspan=\"3\"><center><i>OR</i></center></td></tr>"
        << "<tr><td>Select from the list </td><td> : </td><td>";
      
   char aname[1024];
   char aurl[1024];
   char adesc[1024];
   char aline[2048];
   fstream *predef;
   char separator = separatorchar;

   // create stored list of archives in javascript
   predef = new fstream ("predef2", ios::in);
   SortList sl;
   cout << "<script>\n"
        << "var namelist = new Array();\n"
        << "var urllist = new Array();\n"
        << "var desclist = new Array();\n";
   while (!(predef->eof ()))
   {
      predef->getline (aline, sizeof (aline));

      //split up string based on separator character
      char *start = aline;
      char *end = aline;
      while ((*end != 0) && (*end != separator))
         end++;
      if (*end != 0)
      {
         *end = 0;
         strcpy (aname, start);
         start = end+1;
         end = start;
         while ((*end != 0) && (*end != separator)) 
            end++;
         if (*end != 0)
         {
            *end = 0;
            strcpy (aurl, start);
            strcpy (adesc, end+1);
      
//      predef->getline (aname, sizeof (aname));
//      predef->getline (aurl, sizeof (aurl));
//      predef->getline (adesc, sizeof (adesc));
            if ((strcmp (aname, "") != 0) && (strcmp (aurl, "") != 0))
               sl.Add (aname, aurl, adesc);
         }
      }
   }
   
   SortNode *sn = sl.head;
   while (sn != NULL)   
   {
      cout << "namelist[namelist.length] = \"" << sn->name << "\";\n"
           << "urllist[urllist.length] = \"" << sn->url << "\";\n"
           << "desclist[desclist.length] = \"" << sn->site << "\";\n";
      sn = sn->next;
   }
   
   // javascript functions to update form
   cout << "\nfunction UpdateArchive ()\n"
        << "{\n"
        << "   document.mainform.archive.value = urllist[document.mainform.archivelist.selectedIndex];\n"
        << "   document.mainform.description.value = desclist[document.mainform.archivelist.selectedIndex];\n"
        << "}\n"
        << "\nfunction LoadDescription ()\n"
        << "{\n"
        << "   if (document.mainform.archivelist.selectedIndex >= 0)\n"
        << "      window.location = desclist[document.mainform.archivelist.selectedIndex];\n"
        << "   else\n"
        << "      alert (\"" << Translate ("Sorry, you can only view the website for a selected predefined archive") << "\");"
        << "}\n"
        << "\nfunction LoadTestArchive ()\n"
        << "{\n"
        << "   window.location = \'addarchive?language=" << language
        << "&bgcolor=" << bgcolor
        << "&headercolor=" << headercolor
        << "&blockcolor=" << blockcolor
        << "&archiveurl=\'" << "+document.mainform.archive.value;\n" 
        << "}\n\n";
        
   cout << "</script>";
      
   delete predef;

   // output selection list of predefined archives
   cout << "<select name=archivelist size=1 onChange=\""
        << "UpdateArchive ()\">";
   sn = sl.head;
   while (sn != NULL)
   {
      cout << "<option value=\"" << sn->url << "\">" << sn->name << "</option>";
      sn = sn->next;
   }
   cout << "</select></td></tr></table>";
   
   if (language == NULL)
      language = "";
   cout << "<p><div align=\"right\"> [ <a href=\"javascript:LoadDescription ()\">"
        << Translate ("View Archive Website") << "</a> ]"
        << "[ <a href=\"javascript:LoadTestArchive ()\">"
        << Translate ("Test the specified/selected baseURL")
        << "</a> ]</div></p>";
}

// ----------------------------------------------------------------------
//  Function   : OutputHeadersAndForm
// ----------------------------------------------------------------------
//  Purpose    : Output headers for page
//  Parameters : 1. protocol version
//               2. archive URL
//               3. language name
//               4. bgcolor
//               5. headercolor
//               6. blockcolor
//               7. URL to display
//               8. descriptive URL for archive
//               9. verb
//  Result     : (none)
// ----------------------------------------------------------------------

void OutputHeadersAndForm ( char *protocolVersion, char *archive, char *language, 
                            char *bgcolor, char *headercolor, char *blockcolor, 
                            char *url = NULL, char *description = NULL, 
                            char *verb = NULL )
{
   OutputHeaders (protocolVersion, bgcolor, headercolor);
   
   // output url and description line
   if ((url) && (strcmp (url, "") != 0))
   {
      cout << "<h4><center>";
      cout << url;
      cout << "</center></h4>";
   
      if ((description) && (strcmp (description, "") != 0))
      {
         cout << "<h5><center>";
         cout << Translate ("Archive details");
         cout << " : <a href=\"";
         cout << description << "\">" << description << "</a>";
         cout << "</center></h5>";
      }
      
      cout << "<hr/>";
   }

   // create hidden form and archive input section if necessary
   cout << "<form name=\"mainform\" method=\"post\" action=\"" << testoai << "\">";
   
   if ((archive) && (strcmp (archive, "") != 0))
   { 
      cout << "<input type=\"hidden\" name=\"archive\" value=\"";
      cout << archive;
      cout << "\">";
      
      cout << "<script>\n";
      
      // function to erase selective parameters
      cout << "function ZapParameters (mask)\n"
           << "{\n"
           << "   if ((mask & 1) == 1) document.mainform.from.value = \'\';\n"
           << "   if ((mask & 2) == 2) document.mainform.until.value = \'\';\n"
           << "   if ((mask & 4) == 4) document.mainform.set.value = \'\';\n"
           << "   if ((mask & 8) == 8) document.mainform.metadataPrefix.value = \'\';\n"
           << "   if ((mask & 16) == 16) document.mainform.identifier.value = \'\';\n"
           << "   if ((mask & 32) == 32) document.mainform.resumptionToken.value = \'\';\n"
           << "}\n";

      // function to fill in resumptionToken forms
      cout << "function FillInRT (averb,atoken)\n"
           << "{\n"
           << "   ZapParameters (31);\n"
           << "   document.mainform.verb.value = averb;\n"
           << "   document.mainform.resumptionToken.value = atoken;\n"
           << "   document.mainform.submit ();\n"
           << "}\n";

      // function to fill in ListIdentifiers forms
      cout << "function FillInLI (aset)\n"
           << "{\n"
           << "   ZapParameters (56);\n"
           << "   document.mainform.verb.value = \'ListIdentifiers\';\n"
           << "   document.mainform.set.value = aset;\n"
           << "   document.mainform.metadataPrefix.value = \'oai_dc\';\n"
           << "   document.mainform.submit ();\n"
           << "}\n";

      // function to fill in GetRecord forms
      cout << "function FillInGR (ametadataPrefix,anidentifier)\n"
           << "{\n"
           << "   ZapParameters (39);\n"
           << "   document.mainform.verb.value = \'GetRecord\';\n"
           << "   document.mainform.metadataPrefix.value = ametadataPrefix;\n"
           << "   document.mainform.identifier.value = anidentifier;\n"
           << "   document.mainform.submit ();\n"
           << "}\n";

      // function to fill in ListMetadataFormats forms
      cout << "function FillInLMF (anidentifier)\n"
           << "{\n"
           << "   ZapParameters (47);\n"
           << "   document.mainform.verb.value = \'ListMetadataFormats\';\n"
           << "   document.mainform.identifier.value = anidentifier;\n"
           << "   document.mainform.submit ();\n"
           << "}\n";
           
      cout << "</script>\n";
   }
   else
      OutputArchiveSelector (language, bgcolor, headercolor, blockcolor, archive);
      
   cout << "<input type=\"hidden\" name=\"description\" value=\"";
   if (description)
      cout << description;
   cout << "\">";
   
   cout << "<input type=\"hidden\" name=\"verb\" value=\"";
   if (verb)
      cout << verb;
   cout << "\">";
   
   cout << "<input type=\"hidden\" name=\"bgcolor\" value=\"" << bgcolor << "\">";
   cout << "<input type=\"hidden\" name=\"headercolor\" value=\"" << headercolor << "\">";
   cout << "<input type=\"hidden\" name=\"blockcolor\" value=\"" << blockcolor << "\">";
   
   cout.flush ();
}

// ----------------------------------------------------------------------
//  Function   : OutputMenu
// ----------------------------------------------------------------------
//  Purpose    : Output verb/options menu
//  Parameters : 1. language name
//               2. headercolor
//               3. blockcolor
//               4. display type (raw/parsed/both)
//               5. validation type (none/local/online)
//               from/until/metadataPrefix/identifier/set/resumptionToken
//               6. protocol version
//  Result     : (none)
// ----------------------------------------------------------------------

void OutputMenu ( char *language, char *headercolor, char *blockcolor,
                  char *display, char *validation, char *from, char *until, 
                  char *metadataPrefix, char *identifier, char *set,
                  char *resumptionToken, char *protocolVersion )
{
   cout << "<center><table width=\"100%\" border=\"0\" cellspacing=\"5\" cellpadding=\"5\">"
        << "<tr bgcolor=\"#" << headercolor <<  "\"><th>" << Translate ("Verbs")
        << "</th><th colspan=\"2\">" << Translate ("Parameters") << "</th></tr>";

   // output protocol version
   if (protocolVersion)
      cout << "<input type=\"hidden\" name=\"protocolVersion\" value=\""
           << protocolVersion << "\">";
 
   // output verb links  
   cout << "<tr bgcolor=\"#" << blockcolor << "\"><td>"
        << "<a href=\"\" onClick=\"document.mainform.verb.value=\'Identify\'; document.mainform.submit (); return false\">"
        << "Identify" << "</a><br/>"
        << "<a href=\"\" onClick=\"document.mainform.verb.value=\'ListMetadataFormats\'; document.mainform.submit (); return false\">"
        << "List Metadata Formats" << "</a><br/>"
        << "<a href=\"\" onClick=\"document.mainform.verb.value=\'ListSets\'; document.mainform.submit (); return false\">"
        << "List Sets" << "</a><br/>"
        << "<a href=\"\" onClick=\"document.mainform.verb.value=\'ListIdentifiers\'; document.mainform.submit (); return false\">"
        << "List Identifiers" << "</a><br/>"
        << "<a href=\"\" onClick=\"document.mainform.verb.value=\'ListRecords\';"
        << "document.mainform.submit (); return false\">"
        << "List Records" << "</a><br/>"
        << "<a href=\"\" onClick=\"document.mainform.verb.value=\'GetRecord\';"
        << "document.mainform.submit (); return false\">"
        << "Get Record" << "</a>"
        << "</td>";
   
   // output parameter fields
   cout << "<td colspan=\"2\"><table>";

   cout << "<tr><td align=\"right\">" 
        << "from (eg., YYYY-MM-DD)" << "</td><td>:</td><td>"
        << "<input type=\"text\" name=\"from\" size=\"20\" maxlength=\"80\" value=\"";
   if (from)
      cout << from;
   cout << "\"></td></tr>";
        
   cout << "<tr><td align=\"right\">" 
        << "until (eg., YYYY-MM-DD)" << "</td><td>:</td><td>"
        << "<input type=\"text\" name=\"until\" size=\"20\" maxlength=\"80\" value=\"";
   if (until)
      cout << until;     
   cout << "\"></td></tr>";
        
   cout << "<tr><td align=right>" 
        << "metadataPrefix" << "</td><td>:</td><td>"
        << "<input type=\"text\" name=\"metadataPrefix\" size=\"15\" maxlength=\"1024\" value=\"";
   if (metadataPrefix)
      cout << metadataPrefix;
   cout << "\"></td></tr>";

   cout << "<tr><td align=\"right\">"
        << "identifier" << "</td><td>:</td><td>"
        << "<input type=\"text\" name=\"identifier\" size=\"20\" maxlength=\"1024\" value=\"";
   if (identifier)
      cout << identifier;
   cout << "\"></td></tr>";
                
   cout << "<tr><td align=\"right\">" 
        << "set" << "</td><td>:</td><td>"
        << "<input type=\"text\" name=\"set\" size=\"20\" maxlength=\"1024\" value=\"";
   if (set)
      cout << set;
   cout << "\"></td></tr>";
         
   cout << "<tr><td align=\"right\">" 
        << "resumptionToken" << "</td><td>:</td><td>"
        << "<input type=\"text\" name=\"resumptionToken\" size=\"20\" maxlength=\"1024\" value=\"";
   if (resumptionToken)
      cout << resumptionToken;
   cout << "\"></td></tr>";

   cout << "</table></td></tr>";

   // output options fields   
   cout << "<tr bgcolor=\"#" << headercolor << "\"><th>" 
        << Translate ("Language") << "</th><th>" << Translate ("Display") 
        << "</th><th>" << Translate ("Schema Validation") << "</th></tr>";
        
   cout << "<tr bgcolor=\"#" << blockcolor << "\">";

   cout << "<td><select name=\"language\" onChange=\"document.mainform.submit (); return false\">";
   DIR *aDir = opendir (".");
   struct dirent *sd;
   if (aDir)
   {
      while (sd = readdir (aDir))
      {
         int sdl = strlen (sd->d_name)-4;
         if ((sd->d_name[sdl] == '.') &&
             (sd->d_name[sdl+1] == 'l') &&
             (sd->d_name[sdl+2] == 'a') &&
             (sd->d_name[sdl+3] == 'n'))
         {                            
            cout << "<option value=\"" << sd->d_name << "\"";
            if ((language != NULL) && (strcmp (sd->d_name, language) == 0))
               cout << " selected=\"selected\"";
            cout << ">";
            fstream sdf (sd->d_name, ios::in);
            char languagename[1024];
            sdf >> languagename;
            cout << languagename << "</option>";
         }
      }
      closedir (aDir);
   }
   cout << "</select></td>";
   
   cout << "<td>";
   cout << "<input type=\"radio\" name=\"display\" value=\"parsed\"";
   if ((display != NULL) && (strcmp (display, "parsed") == 0))
      cout << " checked=\"checked\"";
   cout << ">" << Translate ("Parsed");
   cout << "<br/><input type=\"radio\" name=\"display\" value=\"raw\"";
   if ((display != NULL) && (strcmp (display, "raw") == 0))
      cout << "checked=\"checked\"";
   cout << ">" << Translate ("Raw XML");
   cout << "<br/><input type=\"radio\" name=\"display\" value=\"both\"";
   if ((display != NULL) && (strcmp (display, "both") == 0))
      cout << "checked=\"checked\"";
   cout << ">" << Translate ("Both");
   cout << "</td>";

   cout << "<td>";
   cout << "<input type=\"radio\" name=\"validation\" value=\"none\"";
   if ((validation != NULL) && (strcmp (validation, "none") == 0))
      cout << " checked=\"checked\"";
   cout << ">" << Translate ("None");
   cout << "<br/><input type=\"radio\" name=\"validation\" value=\"localxerces\"";
   if ((validation != NULL) && (strcmp (validation, "localxerces") == 0))
      cout << "checked=\"checked\"";
   cout << ">" << Translate ("Local mirror of schemata") << " (Xerces)";
   cout << "<br/><input type=\"radio\" name=\"validation\" value=\"onlinexerces\"";
   if ((validation != NULL) && (strcmp (validation, "onlinexerces") == 0))
      cout << "checked=\"checked\"";
   cout << ">" << Translate ("Online schemata") << " (Xerces)";
   
//   cout << "<br/><input type=\"radio\" name=\"validation\" value=\"localxsv\"";
//   if ((validation != NULL) && (strcmp (validation, "localxsv") == 0))
//      cout << "checked=\"checked\"";
//   cout << ">" << Translate ("Local mirror of schemata") << " (XSV)";
   
//   cout << "<br/><input type=\"radio\" name=\"validation\" value=\"onlinexsv\"";
//   if ((validation != NULL) && (strcmp (validation, "onlinexsv") == 0))
//      cout << "checked=\"checked\"";
//   cout << ">" << Translate ("Online schemata") << " (XSV)";
   
   cout << "</td>";
   
   cout << "</tr>";

   cout << "</table></center>";
   
   cout << "</form>";
}

// ----------------------------------------------------------------------
//  Function   : DumpFile
// ----------------------------------------------------------------------
//  Purpose    : Outputs a raw XML file
//  Parameters : filename
//  Result     : None
// ----------------------------------------------------------------------

void DumpFile ( char *filename )
{
   FILE *rawf = fopen (filename, "r");
   char achar;
   cout << "<pre>";
   while (!feof (rawf))
   {
      achar = getc (rawf);
      if (!feof (rawf)) 
      {
         if (achar == '<')
            cout << "&lt;";
         else if (achar == '>')
            cout << "&gt;";
         else if (achar == '&')
            cout << "&amp;";
         else
            cout << achar;
      }
   }
   cout << "</pre>";
   fclose (rawf);
}

// ----------------------------------------------------------------------
//  Function   : AddParameter
// ----------------------------------------------------------------------
//  Purpose    : Adds a non-empty parameter to a growing string list
//  Parameters : 1. parameter list string
//               2. string to add on
//               3. name of parameter being added
//  Result     : None
// ----------------------------------------------------------------------

void AddParameter ( char *parameters, char *value, char *name )
{
   if ((value) && (strlen (value) > 0))
   {
      strcat (parameters, "&");
      strcat (parameters, name);
      strcat (parameters, "=");
      for ( int i=0; i<strlen (value); i++ )
      {
         if (value[i] == '/') 
            strcat (parameters, "%2F");
         else if (value[i] == '?')
            strcat (parameters, "%3F"); 
         else if (value[i] == '#')
            strcat (parameters, "%23"); 
         else if (value[i] == '=')
            strcat (parameters, "%3D"); 
         else if (value[i] == '&')
            strcat (parameters, "%26"); 
         else if (value[i] == ':')
            strcat (parameters, "%3A"); 
         else if (value[i] == ';')
            strcat (parameters, "%3B"); 
         else if (value[i] == ' ')
            strcat (parameters, "%20"); 
         else if (value[i] == '%')
            strcat (parameters, "%25"); 
         else if (value[i] == '+')
            strcat (parameters, "%2B");
         else if (value[i] == '"')
            strcat (parameters, "%22");
         else
         {
            char t[2];
            t[0] = value[i];
            t[1] = 0;
            strcat (parameters, t);
         }
      }
   }
}


char *getProtocolVersion ( char *verb, char *datafilename, char *archive )
{
   char *Identifyfilename;
//   char Identifybuffer[L_tmpnam];
//   char errorfilename[L_tmpnam];
   char Identifybuffer[128];
   char errorfilename[128];
   char oaiurl[2048], lynxcommand[2048];
   TagList tl;
   static char protocolVersion[32];

   if (strcmp (verb, "Identify") == 0)
   {
      Identifyfilename = datafilename;
   }
   else
   {
      strcpy (Identifybuffer, "/tmp/re.XXXXXX");
      strcpy (errorfilename, "/tmp/re.XXXXXX");
      close (mkstemp (Identifybuffer));
      close (mkstemp (errorfilename));
//      tmpnam (Identifybuffer);
//      tmpnam (errorfilename);
      Identifyfilename = Identifybuffer;

      sprintf (oaiurl, "%s?verb=Identify", archive);
      sprintf (lynxcommand, "%s -error_file=%s -source \"%s\" > %s", lynxpath, errorfilename, oaiurl, Identifyfilename);
      Log (oaiurl);
      system (lynxcommand);
      
      // check for HTTP errors
      char linebuffer[2048];
      int statuscode;
      fstream errorfile (errorfilename, ios::in);
      errorfile.getline (linebuffer, 2048);
      errorfile >> linebuffer >> statuscode;
      errorfile.getline (linebuffer, 2048);
      
      unlink (errorfilename);

      if (statuscode >= 400)
      {
         unlink (Identifyfilename);
         strcpy (protocolVersion, reprotocolversion);
         return protocolVersion;
      }
   }
   
   if (runparser (&tl, Identifyfilename))
   {
      TagList *p = tl.head->Search ("protocolVersion");
      if ((p) && (p->head) && (p->head->type == TEXT))
         strcpy (protocolVersion, p->head->tag);
      p = tl.head->Search ("Identify");
      if (p)
      {
         TagList *q = p->Search ("protocolVersion");
         if ((q) && (q->head) && (q->head->type == TEXT))
            strcpy (protocolVersion, q->head->tag);
      }
   }
   else
   {
      strcpy (protocolVersion, reprotocolversion);
   }

   if (strcmp (verb, "Identify") != 0)
   {
      unlink (Identifyfilename);
   }
   
   return protocolVersion;
}


// --------------------------------------------------------------------------
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------

int main ()
{
   // CGI parameters for protocol and options
   CGIProcessor cgi;
   char *archive = cgi.ParmList.Search ("archive");   
   char *verb = cgi.ParmList.Search ("verb");
   char *description = cgi.ParmList.Search ("description");
   char *identifier = cgi.ParmList.Search ("identifier");
   char *metadataPrefix = cgi.ParmList.Search ("metadataPrefix");
   char *set = cgi.ParmList.Search ("set");
   char *from = cgi.ParmList.Search ("from");
   char *until = cgi.ParmList.Search ("until");
   char *resumptionToken = cgi.ParmList.Search ("resumptionToken");
   char *display = cgi.ParmList.Search ("display");
   char *validation = cgi.ParmList.Search ("validation");
   char *language = cgi.ParmList.Search ("language");
   char *bgcolor = cgi.ParmList.Search ("bgcolor");
   char *headercolor = cgi.ParmList.Search ("headercolor");
   char *blockcolor = cgi.ParmList.Search ("blockcolor");
   char *protocolVersion = cgi.ParmList.Search ("protocolVersion");

   // set options defaults
   if ((!display) || (strlen (display) == 0))
      display = "parsed";
   if ((!validation) || (strlen (validation) == 0))
      validation = "localxerces";
   if ((!language) || (strlen (language) == 0))
      language = "enus.lan";
   if ((!bgcolor) || (strlen (bgcolor) == 0))
      bgcolor = bgcolorpreset;
   if ((!headercolor) || (strlen (headercolor) == 0))
      headercolor = headercolorpreset;
   if ((!blockcolor) || (strlen (blockcolor) == 0))
      blockcolor = blockcolorpreset;
   if (!protocolVersion)
      protocolVersion = "";
 
   // set language from parameter
   SetLanguage (language);

   // temporary filenames for output from system calls
//   char datafilename[L_tmpnam];
//   char errorfilename[L_tmpnam];
//   char xsvfilename[L_tmpnam];
   char datafilename[128];
   char errorfilename[128];
   char xsvfilename[128];
//   tmpnam (datafilename);
//   tmpnam (errorfilename);
//   tmpnam (xsvfilename);
   strcpy (datafilename, "/tmp/re.XXXXXX");
   strcpy (errorfilename, "/tmp/re.XXXXXX");
   strcpy (xsvfilename, "/tmp/re.XXXXXX");
   close (mkstemp (datafilename));
   close (mkstemp (errorfilename));
   close (mkstemp (xsvfilename));

   // buffers for commands and XML tree
   char lynxcommand[2048], oaiurl[2048];
   TagList tl;
   
   // output headers, depending on whether or not an archive has been selected
   if ((archive) && (strlen (archive) > 0) && (verb) && (strlen (verb) > 0))
   {
      char parameters[1024];
      parameters[0] = 0;
      AddParameter (parameters, from, "from");
      AddParameter (parameters, until, "until");
      AddParameter (parameters, metadataPrefix, "metadataPrefix");
      AddParameter (parameters, set, "set");
      AddParameter (parameters, identifier, "identifier");
      AddParameter (parameters, resumptionToken, "resumptionToken");
      sprintf (oaiurl, "%s?verb=%s%s", archive, verb, parameters);
      sprintf (lynxcommand, "%s -error_file=%s -source \"%s\" > %s", lynxpath, errorfilename, oaiurl, datafilename);
      Log (oaiurl);
      OutputHeadersAndForm (protocolVersion, archive, language, bgcolor, headercolor, 
                            blockcolor, oaiurl, description, verb);
   }
   else
      OutputHeadersAndForm (protocolVersion, archive, language, bgcolor, headercolor, 
                            blockcolor);

   if ((archive) && (strlen (archive) > 0) && (verb) && (strlen (verb) > 0))
   {
      // send HTTP request to OAI server
      system (lynxcommand);
      
      // check for HTTP errors
      char linebuffer[2048];
      int statuscode;
      fstream errorfile (errorfilename, ios::in);
      errorfile.getline (linebuffer, 2048);
      errorfile >> linebuffer >> statuscode;
      errorfile.getline (linebuffer, 2048);

      if (statuscode >= 400)
      {
         cout << Translate ("HTTP Status Code") << " : " << statuscode 
              << "<p>" << Translate ("HTTP Error Message") 
              << " : " << linebuffer << "\n";
         DumpFile (datafilename);
      }
      
      // if no HTTP errors ...
      else
      {
         if (((!protocolVersion) || (strlen (protocolVersion) == 0)) &&
             ((strcmp (validation, "localxerces") == 0) ||
              (strcmp (validation, "onlinexerces") == 0)
//              || (strcmp (validation, "localxsv") == 0)
//              || (strcmp (validation, "onlinexsv") == 0)
             ) )
            protocolVersion = getProtocolVersion (verb, datafilename, archive);
            
         // force xsv if protocol version is 1.0
         if (strcmp (protocolVersion, "1.0") == 0)
         {
            if (strcmp (validation, "localxerces") == 0)
               validation = "localxsv";
            else if (strcmp (validation, "onlinexerces") == 0)
               validation = "onlinexsv";
         }
      
         // perform schema validation
         char *xsverror;         
         if (strcmp (validation, "localxerces") == 0)
            xsverror = ValidateXerces (protocolVersion, datafilename, xsvfilename, verb, metadataPrefix, 1);
         else if (strcmp (validation, "onlinexerces") == 0)
            xsverror = ValidateXerces (protocolVersion, datafilename, xsvfilename, verb, metadataPrefix, 0);
         else if (strcmp (validation, "localxsv") == 0)
            xsverror = ValidateXSV (protocolVersion, datafilename, xsvfilename, verb, metadataPrefix, 1);
         else if (strcmp (validation, "onlinexsv") == 0)
            xsverror = ValidateXSV (protocolVersion, datafilename, xsvfilename, verb, metadataPrefix, 0);
         else
            xsverror = NULL;

         // make frame for dual display
         if (strcmp (display, "both") == 0)
         {
            cout << "<table width=\"100%\" cellspacing=\"2\" cellpadding=\"10\" border=\"2\">"
                 << "<tr bgcolor=\"#00cc00\"><th align=\"left\">"
                 << Translate ("Parsed Output") << "</th><th align=\"left\">" 
                 << Translate ("Raw XML Output") << "</th></tr>"
                 << "<tr><td valign=\"top\">";
         }

         // report schema errors
         if (xsverror)
         {
            cout << "<h3>"
                 << Translate ("XML Schema Validation Error !")
                 << "</h3><p>";
            cout << xsverror << "</p>";
            DumpFile (xsvfilename);
         }
         
         // if no schema errors ...
         else
         {
            // parse XML file
            if ((strcmp (display, "parsed") == 0) || (strcmp (display, "both") == 0))
            {
               if (runparser (&tl, datafilename))
               {
                  char *requestURL, *responseDate;
                  requestURL = NULL;
                  responseDate = NULL;
                  char buffer[2048];
                  
                  // run correct parser
                  if ((strcmp (protocolVersion, "1.0") == 0) ||
                      (strcmp (protocolVersion, "1.1") == 0))
                  {
                     if (strcmp (verb, "Identify") == 0)
                        ProcessIdentify  (&tl, &requestURL, &responseDate);
                     else if (strcmp (verb, "ListMetadataFormats") == 0)
                        ProcessListMetadataFormats  (&tl, identifier, &requestURL, &responseDate);
                     else if (strcmp (verb, "ListSets") == 0)
                        ProcessListSets  (&tl, &requestURL, &responseDate);            
                     else if (strcmp (verb, "ListIdentifiers") == 0)
                        ProcessListIdentifiers  (&tl, &requestURL, &responseDate);            
                     else if (strcmp (verb, "ListRecords") == 0)
                        ProcessListRecords  (&tl, &requestURL, &responseDate);            
                     else if (strcmp (verb, "GetRecord") == 0)
                        ProcessGetRecord  (&tl, &requestURL, &responseDate);
                  }
                  else
                  {
                     ProcessOAIPMH (&tl, &requestURL, &responseDate, verb, identifier, buffer);
                  }
                     
                  // check for existence of requestURL and responseDate
                  char RUError[1024];
                  strcpy (RUError, "requestURL ");
                  strcat (RUError, Translate ("missing !"));
                  char RDError[1024];
                  strcpy (RDError, "responseDate ");
                  strcat (RDError, Translate ("missing !"));
                  if (requestURL == NULL)
                     requestURL = RUError;
                  if (responseDate == NULL)
                     responseDate = RDError;
                  cout << "<hr/><b>" << Translate ("Request") 
                       << " : </b>" << requestURL;
                  cout << "<br/><b>" << Translate ("Response Date") 
                       << " : </b>" << responseDate;
               }
            }
            
            // or just display it plain
            else if (strcmp (display, "raw") == 0)
            {
               DumpFile (datafilename);
            }
         }
         
         // close frame if displaying in dual mode
         if (strcmp (display, "both") == 0)
         {
            cout << "</td><td valign=\"top\">";
            DumpFile (datafilename);
            cout << "</td></tr></table>";
         }
         
         unlink (xsvfilename);
      }
      
      unlink (datafilename);
      unlink (errorfilename);
   }
   
   cout << "<hr/>";
   
   // output bottom of screen forms/links
   OutputMenu (language, headercolor, blockcolor, display, validation, 
               from, until, metadataPrefix, identifier, set, resumptionToken,
               protocolVersion);

   OutputFooters (language, bgcolor, headercolor, blockcolor);
   
   cout.flush ();
   return 0;
}
	
