//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 2.0-1.46b     |
// | Hussein Suleman                                                      |
// | November 2005                                                        |
//  ----------------------------------------------------------------------
// |  University of Cape Town                                             |
// |  Department of Computer Science                                      |
// |  Advanced Information Management Laboratory                          |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : comply
//  Purpose    : automatic tests to check an archive for OAI compliance
// ======================================================================

//#include <iostream.h>
//#include <fstream.h>
#include <iostream>
#include <fstream>

// added pnh
using namespace std;

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include <curl/curl.h>

#include "config.h"
#include "language.h"
#include "taglist.h"
#include "validate.h"
#include "xml.h"

// --------------------------------------------------------------------------

// global variables for storing parameters across tests
char repositoryName[2048] = "";
char protocolVersion[2048] = "";
char baseURL[2048] = "";
char adminEmail[2048] = "";
char metadataFormat[2048] = "";
char earliestDatestamp[2048] = "1970-01-01T00:00:00Z";
char set[2048] = "";
char identifier[2048] = "";
char setSpec[10000] = "";
char set_resumptionToken[2048] = "";
char identifier_resumptionToken[2048] = "";
char record_resumptionToken[2048] = "";
char granularity[2048] = "";
char tempstring[2048] = "";

// error message pointer
char *errormsg;

// to update global variables or not
#define NO_UPDATE 0
#define UPDATE_VARIABLES 1

// to allow errors or not or maybe not to care at all
#define NO_ERROR 0
#define YES_ERROR 1
#define YES_OR_NO_ERROR 2

// number of errors tracked from Xerces for highlighting
#define ERRORS_TRACKED 100


// ----------------------------------------------------------------------
//  Function   : DumpFile
// ----------------------------------------------------------------------
//  Purpose    : Outputs a raw XML file
//  Parameters : 1. filename
//               2. number of errors in file
//               3. array of error line and column numbers
//  Result     : None
// ----------------------------------------------------------------------

void DumpFile ( char *filename, int noOfErrors = 0, long *errorList = NULL )
{
//   cout << "DEBUG: FILE" << filename << "\n";
   FILE *rawf = fopen (filename, "r");
   char achar;
   long currentLine = 1;
   long currentColumn = 1;
   
   cout << "------ Start of XML Response ------\n";
   if (rawf)
   {
      while (!feof (rawf))
      {
         achar = getc (rawf);
         if (!feof (rawf)) 
         {
            for ( int a=0; a<noOfErrors; a++ )
            {
               if ((errorList[a*2] == currentLine) && (errorList[a*2+1] == currentColumn))
               {
                  cout << "<font color=\"#0000ff\">[ ERROR " << a+1 << " ]</font>";
               }
            }
         
            if (achar == '<')
               cout << "&lt;";
            else if (achar == '>')
               cout << "&gt;";
            else if (achar == '&')
               cout << "&amp;";
            else
               cout << achar;
               
            if (achar == 13)
            {
               currentLine++;
               currentColumn = 0;
            }
            else
            {
               currentColumn++;
            }
         }
      }
      fclose (rawf);
   }
   cout << "\n------- End of XML Response -------\n";
}


// ----------------------------------------------------------------------
//  Function   : DumpSchemaCheckFile
// ----------------------------------------------------------------------
//  Purpose    : Outputs the output from the Schema validation program
//  Parameters : 1. filename
//               2. pointer to number of errors
//               3. array of error line and column numbers
//  Result     : None
// ----------------------------------------------------------------------

void DumpSchemaCheckFile ( char *filename, int *noOfErrors, long *errorList )
{
   FILE *rawf = fopen (filename, "r");
   char achar;
   char line[2048];
   char t[2];
   t[1] = 0;
   line[0] = 0;
   (*noOfErrors) = 0;
   
   cout << "------ Response from Xerces Schema Validation ------\n";
   if (rawf)
   {
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

            if ((achar == 13) || (achar == 10))
            {
               char* firstcolon = strchr (line, ':');
               if (firstcolon)
               {
                  char *secondcolon = strchr (firstcolon+1, ':');
                  if (secondcolon)
                  {
                     char *thirdcolon = strchr (secondcolon+1, ':');
                     if (thirdcolon)
                     {
                        int linenum = strtol (firstcolon+1, 0, 10);
                        int colnum = strtol (secondcolon+1, 0, 10);
                        if (*noOfErrors < ERRORS_TRACKED)
                        {
                           errorList[(*noOfErrors)*2] = linenum;
                           errorList[(*noOfErrors)*2 + 1] = colnum;
                           (*noOfErrors)++;
                        }
                     }
                  }
               }
               line[0] = 0;
            }
            else
            {
               t[0] = achar;
               strcat (line, t);
            }
         }
      }
      fclose (rawf);
   }
   cout << "------- End of Xerces Schema Validation Report  -------\n";
}

// ----------------------------------------------------------------------
//  Function   : SlashEscape
// ----------------------------------------------------------------------
//  Purpose    : Insert backslashes before each double quote mark
//  Parameters : string to be escaped
//  Result     : pointer to escaped string
// ----------------------------------------------------------------------

char * SlashEscape ( char *archiveurl )
{
   char *t = tempstring;
   char *p = archiveurl;
   while (*p != 0)
   {
      if (*p == 34)
      {
         *t = 92;
         t++;
      }
      *t = *p;
      p++;  t++;
   }
   *t = 0;
   
   return tempstring;
}


// ----------------------------------------------------------------------
// Function:   : getOAIResponse
// Purpose     : do an HTTP GET operation using url
// Result      : 0 on success / 1 on fail

int getOAIResponse(char *oaiurl, char *datafilename, char *errorfilename,
  char *xsvfilename) {

   CURL *curl = (CURL *)NULL;
   CURLcode res = (CURLcode) 0;
   FILE *outfile = (FILE *)NULL;
   FILE *headerfile = (FILE *)NULL;
   long respCode = 0;
   char statusMessage[100];

   curl = curl_easy_init();

   if (curl) {
     outfile = fopen(datafilename, "wb");
     headerfile = fopen(errorfilename, "wb");
     curl_easy_setopt(curl, CURLOPT_WRITEDATA, outfile);
     curl_easy_setopt(curl, CURLOPT_URL, oaiurl);
     curl_easy_setopt(curl, CURLOPT_HEADERDATA, headerfile);
     if (httpproxy != (char *)NULL && strlen(httpproxy) != 0) {
       curl_easy_setopt(curl, CURLOPT_PROXY, httpproxy);
     }
     curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
     res = curl_easy_perform(curl);
     fclose(outfile);
     fclose(headerfile);
     if (res == CURLE_OK)
       curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &respCode);
     else {
       cout << oaiurl << ": " << curl_easy_strerror(res) << "\n";
     }

     curl_easy_cleanup(curl);
     if (res != CURLE_OK) {
       respCode = 502;
       strcpy(statusMessage, "Proxy error");
     }

     if (respCode != 200) {

       headerfile = fopen(errorfilename, "r");
       if (headerfile != NULL) {
	 fgets(statusMessage, 100, headerfile);
	 fclose(headerfile);
       }

       static char str[2048];
       sprintf (str, "**** [%s] %s : %d / %s", Translate ("ERROR"), Translate ("Unexpected HTTP Error"), respCode, statusMessage);
       errormsg = str;
       DumpFile (datafilename);
       unlink (xsvfilename);
       unlink (datafilename);
       unlink (errorfilename);
       return 1;
     }
   }
   return 0;
}

// ----------------------------------------------------------------------
//  Function   : Process2
// ----------------------------------------------------------------------
//  Purpose    : Perform a test by issuing a request and checking the
//               results
//  Parameters : 1. base url of archive
//               2. verb being tested
//               3. parameters to be used for testing
//               4. do we allow errors ?
//               5. do we update global variables ?
//               6. what error to look for
//  Result     : Time/date string
// ----------------------------------------------------------------------

int Process2 ( char *archiveurl, char *verb, char *parameters, int error, int first, char *errorcode )
{
   char shellcommand[2048], oaiurl[2048], xsvcommand[2048];
//   char datafilename[L_tmpnam];
//   char errorfilename[L_tmpnam];
//   char xsvfilename[L_tmpnam];

//   tmpnam (datafilename);
//   tmpnam (errorfilename);
//   tmpnam (xsvfilename);

   char datafilename[128];
   char errorfilename[128];
   char xsvfilename[128];
   
   strcpy (datafilename, "/tmp/re.XXXXXX");
   strcpy (errorfilename, "/tmp/re.XXXXXX");
   strcpy (xsvfilename, "/tmp/re.XXXXXX");
   
   close (mkstemp (datafilename));
   close (mkstemp (errorfilename));
   close (mkstemp (xsvfilename));
   
   if (first == UPDATE_VARIABLES)
   {
      if (strcmp (verb, "Identify") == 0)
      {
         repositoryName[0] = 0;
         protocolVersion[0] = 0;
         baseURL[0] = 0;
         adminEmail[0] = 0;
         strcpy (granularity, "YYYY-MM-DD");
      }
      else if (strcmp (verb, "ListMetadataFormats") == 0)
      {
         metadataFormat[0] = 0;
      }
      else if (strcmp (verb, "ListSets") == 0)
      {
         set[0] = 0;
         //set_resumptionToken[0] = 0;
      }
      else if (strcmp (verb, "ListIdentifiers") == 0)
      {
         identifier[0] = 0;
         //identifier_resumptionToken[0] = 0;
      }
      else if (strcmp (verb, "ListRecords") == 0)
      {
         //record_resumptionToken[0] = 0;
      }
      else if (strcmp (verb, "GetRecord") == 0)
      {
         setSpec[0] = 0;
      }
   }
   
   parameters = SlashEscape (parameters);

   sprintf (oaiurl, "%s?verb=%s%s", archiveurl, verb, parameters);

   cout << "URL : " << oaiurl << "\n"; 
   cout.flush ();

   int getStatus = getOAIResponse(oaiurl, datafilename, errorfilename, xsvfilename);
   if (getStatus == 1)
     return 0;
   
   char *metadataPrefix;
   if (strstr (parameters, "metadataPrefix=oai_dc") != NULL)
      metadataPrefix = "oai_dc";
   else
      metadataPrefix = "";

   if (ValidateXerces ("2.0", datafilename, xsvfilename, verb, metadataPrefix, 0))
   {
      static char str[2048];
      int noOfErrors = 0;
      long errorList[ERRORS_TRACKED * 2];
      
      sprintf (str, "**** [%s] %s", Translate ("ERROR"), Translate ("XML Schema validation failed"));
      errormsg = str;
      DumpSchemaCheckFile (xsvfilename, &noOfErrors, errorList);
      DumpFile (datafilename, noOfErrors, errorList);
      unlink (xsvfilename);
      unlink (datafilename);
      unlink (errorfilename);
      return 0;
   }

   TagList tl, *p;
   if (!(runparser (&tl, datafilename)))
   {
      static char str[2048];
      sprintf (str, "**** [%s] %s", Translate ("ERROR"), Translate ("XML parsing errors"));
      errormsg = str;
      DumpFile (datafilename);
      unlink (xsvfilename);
      unlink (datafilename);
      unlink (errorfilename);
      return 0;
   }
   else
   {
      // check for error code
      p = tl.head->head;
      if (error == YES_ERROR)
      {
         while (p != NULL)
         {
            if (p->type == TAG)
            {
               if (strcmp (p->tag, "error") == 0)
               {
                  if (strstr (errorcode, p->attr->Search ("code")) != NULL)
                  {
                     unlink (xsvfilename);
                     unlink (datafilename);
                     unlink (errorfilename);  
                     return 1;
                  }
               } 
            }
            p = p->next;
         }

         static char str[2048];
         sprintf (str, "**** [%s] %s : %s", Translate ("ERROR"), Translate ("Error tag expected but not found"), errorcode);
         errormsg = str;
         DumpFile (datafilename);
         unlink (xsvfilename);
         unlink (datafilename);
         unlink (errorfilename);
         return 0;
      }
      else if (error == YES_OR_NO_ERROR)
      {
         int founderror = 0;
         
         while (p != NULL)
         {
            if (p->type == TAG)
            {
               if (strcmp (p->tag, "error") == 0)
               {
                  if (strstr (errorcode, p->attr->Search ("code")) != NULL)
                  {
                     founderror = 1;
                  }
                  else
                  {
                     static char str[2048];
                     sprintf (str, "**** [%s] %s : %s", Translate ("ERROR"), Translate ("Error tag found but not expected"), p->attr->Search ("code"));
                     errormsg = str;
                     DumpFile (datafilename);
                     unlink (xsvfilename);
                     unlink (datafilename);
                     unlink (errorfilename);
                     return 0;
                  }
               }
            }
            p = p->next;
         }
         
         if (founderror == 1)
         {
            unlink (xsvfilename);
            unlink (datafilename);
            unlink (errorfilename);
            return 1;
         }
      }
      else if (error == NO_ERROR)
      {
         while (p != NULL)
         {
            if (p->type == TAG)
            {
               if (strcmp (p->tag, "error") == 0)
               {
                  if (p->attr->Search ("code") != NULL)
                  {
                     static char str[2048];
                     sprintf (str, "**** [%s] %s : %s", Translate ("ERROR"), Translate ("Error tag found but not expected"), p->attr->Search ("code"));
                     errormsg = str;
                     DumpFile (datafilename);
                     unlink (xsvfilename);
                     unlink (datafilename);
                     unlink (errorfilename);
                     return 0;
                  }
               }
            }
            p = p->next;
         }
      }
      
      TagList *q = tl.head->Search (verb);
      if (q == NULL)
      {
         static char str[2048];
         sprintf (str, "**** [%s] %s : %s", Translate ("ERROR"), Translate ("Cannot find container corresponding to the verb"), verb);
         errormsg = str;
         DumpFile (datafilename);
         unlink (xsvfilename);
         unlink (datafilename);
         unlink (errorfilename);
         return 0;
      }
   
      if (first == UPDATE_VARIABLES)
      {
         if (strcmp (verb, "Identify") == 0)
         {
            p = q->Search ("repositoryName");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (repositoryName, p->head->tag);
            p = q->Search ("protocolVersion");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (protocolVersion, p->head->tag);
            p = q->Search ("baseURL");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (baseURL, p->head->tag);
            p = q->Search ("adminEmail");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (adminEmail, p->head->tag);
            p = q->Search ("granularity");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (granularity, p->head->tag);
            p = q->Search ("earliestDatestamp");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (earliestDatestamp, p->head->tag);            
         }
         else if (strcmp (verb, "ListMetadataFormats") == 0)
         {
            p = q->head;
            while (p)
            {
               if ((p->type == TAG) && (strcmp (p->tag, "metadataFormat") == 0))
               {
                  p = p->Search ("metadataPrefix");
                  if ((p) && (p->head) && (p->head->type == TEXT) && 
                      ((strlen (metadataFormat) == 0) || (strcmp (metadataFormat, "oai_dc") == 0)))
                     strcpy (metadataFormat, p->head->tag);
               }
               p = p->next;
            }
         
//            p = q->Search ("metadataFormat");
//            if (p)
//            {
//               p = p->Search ("metadataPrefix");
//               if ((p) && (p->head) && (p->head->type == TEXT))
//                  strcpy (metadataFormat, p->head->tag);
//            }            
         }
         else if (strcmp (verb, "ListSets") == 0)
         {
            p = q->Search ("set");
            if (p)
            {
               p = p->Search ("setSpec");
               if ((p) && (p->head) && (p->head->type == TEXT))
                  strcpy (set, p->head->tag);
            }
            p = q->Search ("resumptionToken");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (set_resumptionToken, p->head->tag);
         }
         else if (strcmp (verb, "ListIdentifiers") == 0)
         {
//            p = q->Search ("header");
//            if (p)
//            {
//               p = p->Search ("identifier");
//               if ((p) && (p->head) && (p->head->type == TEXT))
//               {
//                  strcpy (identifier, p->head->tag);
//               }
//            }
            p = q->head;
            while (p)
            {
               if ((p->type == TAG) && (strcmp (p->tag, "header") == 0) &&
                   ((p->attr->Search ("status") == NULL) || 
                    (strcmp (p->attr->Search ("status"), "deleted") != 0)) )
               {
                  p = p->Search ("identifier");
                  if ((p) && (p->head) && (p->head->type == TEXT))
                  {
                     strcpy (identifier, p->head->tag);
                  }
                  break;
               }
               p = p->next;
            }
            p = q->Search ("resumptionToken");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (identifier_resumptionToken, p->head->tag);
         }
         else if (strcmp (verb, "ListRecords") == 0)
         {
            p = q->Search ("resumptionToken");
            if ((p) && (p->head) && (p->head->type == TEXT))
               strcpy (record_resumptionToken, p->head->tag);
         }
         else if (strcmp (verb, "GetRecord") == 0)
         {
            p = q->Search ("record");
            if (p)
            {
               p = p->Search ("header");
               if (p)
               {
                  p = p->head;
                  while (p != NULL)
                  {
                     if ((p->type == TAG) && (strcmp (p->tag, "setSpec") == 0))
                     {
                        if ((p->head) && (p->head->type == TEXT))
                        {
                           strcat (setSpec, p->head->tag);
                           strcat (setSpec, ",");
                        }
                     }
                     p = p->next;
                  }
               }
            }
         }
      }
   }
   
   unlink (xsvfilename);
   unlink (datafilename);
   unlink (errorfilename);  
   
   return 1;
}


// ----------------------------------------------------------------------
//  Function   : Process
// ----------------------------------------------------------------------
//  Purpose    : perform test and output errors
//  Parameters : 1. test number 
//               2. description of verb being tested
//               3. base url of archive
//               4. verb being tested
//               5. parameters to be used for testing
//               6. do we allow errors ?
//               7. do we update global variables ?
//               8. error code string
//  Result     : 0 on success / 1 on fail
// ----------------------------------------------------------------------

int Process ( int testnum, char *descript, char *archiveurl, char *verb, char *parameters, int error, int first, char *errorcode )
{   
   cout << "\n(" << testnum << ") " << Translate ("Testing") << " : " << descript << "\n";
   
   errormsg = NULL;
   
   int status = Process2 (archiveurl, verb, parameters, error, first, errorcode);

   if (status)
   {
      cout << Translate ("Test Result") << " : " << Translate ("OK") << "\n";
      cout.flush ();
      return 0;
   }
   else
   {
      cout << Translate ("Test Result") << " : " << Translate ("FAIL!") << "\n";
      if (errormsg)
         cout << errormsg << "\n";
      cout.flush ();
      return 1;
   }
}


// ----------------------------------------------------------------------
//  Function   : ProcessSkip
// ----------------------------------------------------------------------
//  Purpose    : skip test and output just a header to say so
//  Parameters : 1. test number 
//               2. description of verb being skipped
//  Result     : (none)
// ----------------------------------------------------------------------

void ProcessSkip ( int testnum, char *descript )
{
   cout << "\n(" << testnum << ") " << Translate ("Skipping") << " : " << descript << "\n";
   cout << Translate ("This test is being skipped because it cannot or should not be performed.") << "\n";
}


// --------------------------------------------------------------------------
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------

int main ( int argc, char **argv )
{
   char str[2048];
   int totalerrors = 0;
   int result;
   
   if (argc != 3)
   {
      cout << "Usage: comply <oai-url> <language>\n\n";
      exit (2);
   }
   
   SetLanguage (argv[2]);

   cout << "Open Archives Initiative :: " << Translate ("Protocol for Metadata Harvesting")
        << " v" << reprotocolversion << "\n" 
        << Translate ("RE Protocol Tester") << " " << reversion << " :: UCT AIM :: "
        << Translate (redate) << "\n";

// IDENTIFY

   result = Process (1, "Identify", argv[1], "Identify", "", NO_ERROR, UPDATE_VARIABLES, "");
   totalerrors += result;
   if (result == 0)
   {
      if (strlen (repositoryName) > 0)
         cout << "---- [ " << Translate ("Repository Name") 
              << " = " << repositoryName << " ]\n";
      else
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] " 
              << Translate ("Repository Name missing !") << "\n";
      }
      if (strlen (protocolVersion) > 0)
         cout << "---- [ " << Translate ("Protocol Version")
              << " = " << protocolVersion << " ]\n";
      else
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Protocol Version missing !") << "\n";
      }
      if (strlen (baseURL) > 0)
         cout << "---- [ " << Translate ("Base URL") 
              << " = " << baseURL << " ]\n";
      else
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Base URL missing !") << "\n";
      }
      if (strlen (adminEmail) > 0)
         cout << "---- [ " << Translate ("Admin Email") 
              << " = " << adminEmail << " ]\n";
      else
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Admin Email missing !") << "\n";
      }
      if (strlen (granularity) > 0)
         cout << "---- [ " << Translate ("Granularity") 
              << " = " << granularity << " ]\n";
      else
      {
         strcpy (granularity, "YYYY-MM-DD");
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Granularity missing !") << "\n";
      }
      if (strlen (earliestDatestamp) > 0)
         cout << "---- [ " << Translate ("Earliest Datestamp") 
              << " = " << earliestDatestamp << " ]\n";
      else
      {
         strcpy (earliestDatestamp, "1970-01-01");
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("earliestDatestamp missing !") << "\n";
      }
      
      if (strlen (granularity) != strlen (earliestDatestamp))
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Granularity mismatch in earliestDatestamp !") << "\n";
      }
   }
   
   totalerrors += Process (2, "Identify (illegal_parameter)", argv[1], "Identify", "&test=test", YES_ERROR, NO_UPDATE, "badArgument");

// LIST METADATA FORMATS 1

   result = Process (3, "ListMetadataFormats", argv[1], "ListMetadataFormats", "", NO_ERROR, UPDATE_VARIABLES, "");
   totalerrors += result;
   if (result == 0)
   {
      if (strcmp (metadataFormat, "oai_dc") == 0)
         cout << "---- [ " << Translate ("Only oai_dc supported")
              << " ]\n";
      else if (strlen (metadataFormat) > 0)
         cout << "---- [ " << Translate ("Sample Metadata Format")
              << " = " << metadataFormat << " ]\n";
      else
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Metadata format missing !") << "\n";
      }
   }

// LIST SETS

   result = Process (4, "ListSets", argv[1], "ListSets", "", YES_OR_NO_ERROR, UPDATE_VARIABLES, "noSetHierarchy,badArgument");
   totalerrors += result;
   if (result == 0)
   {
      if (strlen (set) > 0)
         cout << "---- [ " << Translate ("Sample Set Spec") 
              << " = " << set << " ]\n";

      if (strlen (set_resumptionToken) > 0)
      {
         cout << "---- [ " << Translate ("Set Resumption Token") 
              << " = " << set_resumptionToken << " ]\n";
         sprintf (str, "&resumptionToken=%s", set_resumptionToken);
         totalerrors += Process (5, "ListSets (resumptionToken)", argv[1], "ListSets", str, NO_ERROR, NO_UPDATE, "");
      }
      else
      {
         ProcessSkip (5, "ListSets (resumptionToken)");
      }
   }
   else
   {
      ProcessSkip (5, "ListSets (resumptionToken)");
   }

// LIST IDENTIFIERS

   strcpy (identifier_resumptionToken, "a_junk_token");
   result = Process (6, "ListIdentifiers (oai_dc)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc", NO_ERROR, UPDATE_VARIABLES, "");
   totalerrors += result;
   if (result == 0)
   {
      if (strlen (identifier) > 0)
         cout << "---- [ " << Translate ("Sample Identifier") 
              << " = " << identifier << " ]\n";
      else
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Identifier missing !") << "\n";
      }

      if (strlen (identifier_resumptionToken) > 0)
      {
         if (strcmp (identifier_resumptionToken, "a_junk_token") == 0)
         {
            ProcessSkip (7, "ListIdentifiers (resumptionToken)");
            ProcessSkip (8, "ListIdentifiers (resumptionToken, oai_dc)");
         }
         else
         {
            cout << "---- [ " << Translate ("Identifier Resumption Token")
                 << " = " << identifier_resumptionToken << " ]\n";
            sprintf (str, "&resumptionToken=%s", identifier_resumptionToken);
            totalerrors += Process (7, "ListIdentifiers (resumptionToken)", argv[1], "ListIdentifiers", str, NO_ERROR, NO_UPDATE, "");

            sprintf (str, "&resumptionToken=%s&metadataPrefix=oai_dc", identifier_resumptionToken);
            totalerrors += Process (8, "ListIdentifiers (resumptionToken, oai_dc)", argv[1], "ListIdentifiers", str, YES_ERROR, NO_UPDATE, "badArgument,badResumptionToken");
         }
      }
      else
      {
          totalerrors++;
          cout << "**** [" << Translate ("ERROR") << "] "
               << Translate ("Empty resumptionToken in initial ListIdentifiers response !")
               << "\n";
         ProcessSkip (7, "ListIdentifiers (resumptionToken)");
         ProcessSkip (8, "ListIdentifiers (resumptionToken, oai_dc)");
      }
   }
   else
   {
      ProcessSkip (7, "ListIdentifiers (resumptionToken)");
      ProcessSkip (8, "ListIdentifiers (resumptionToken, oai_dc)");
   }

   totalerrors += Process (9, "ListIdentifiers (oai_dc, from/until)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&from=2000-01-01&until=2000-01-01", YES_OR_NO_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");

   if (strlen (set) > 0)
   {
      sprintf (str, "&metadataPrefix=oai_dc&set=%s&from=2000-01-01&until=2000-01-01", set);
      totalerrors += Process (10, "ListIdentifiers (oai_dc, set, from/until)", argv[1], "ListIdentifiers", str, YES_OR_NO_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");
   }
   else
   {
      ProcessSkip (10, "ListIdentifiers (oai_dc, set, from/until)");
   }
 
   sprintf (str, "&metadataPrefix=oai_dc&set=really_wrong_set&from=some_random_date&until=some_random_date");
   totalerrors += Process (11, "ListIdentifiers (oai_dc, illegal_set, illegal_from/until)", argv[1], "ListIdentifiers", str, YES_ERROR, NO_UPDATE, "badArgument,noSetHierarchy");

   totalerrors += Process (12, "ListIdentifiers (oai_dc, from granularity != until granularity)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&from=2001-01-01&until=2002-01-01T00:00:00Z", YES_ERROR, NO_UPDATE, "badArgument");

   totalerrors += Process (13, "ListIdentifiers (oai_dc, from > until)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&from=2000-01-01&until=1999-01-01", YES_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");

   totalerrors += Process (14, "ListIdentifiers ()", argv[1], "ListIdentifiers", "", YES_ERROR, NO_UPDATE, "badArgument");

   if ((strlen (metadataFormat) > 0) && (strcmp (metadataFormat, "oai_dc") != 0))
   {
      sprintf (str, "&metadataPrefix=%s", metadataFormat);
      totalerrors += Process (15, "ListIdentifiers (metadataPrefix)", argv[1], "ListIdentifiers", str, YES_OR_NO_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");
   }
   else
   {
      ProcessSkip (15, "ListIdentifiers (metadataPrefix)");
   }

   totalerrors += Process (16, "ListIdentifiers (illegal_mdp)", argv[1], "ListIdentifiers", "&metadataPrefix=illegal_mdp", YES_ERROR, NO_UPDATE, "cannotDisseminateFormat,badArgument");

   totalerrors += Process (17, "ListIdentifiers (mdp, mdp)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&metadataPrefix=oai_dc", YES_ERROR, NO_UPDATE, "badArgument");

   totalerrors += Process (18, "ListIdentifiers (illegal_resumptiontoken)", argv[1], "ListIdentifiers", "&resumptionToken=junktoken", YES_ERROR, NO_UPDATE, "badResumptionToken");
   
// GRANULARITY TESTS

   totalerrors += Process (19, "ListIdentifiers (oai_dc, from YYYY-MM-DD)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&from=2001-01-01", YES_OR_NO_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");
   
   if (strcmp (granularity, "YYYY-MM-DDThh:mm:ssZ") == 0)
   {
      totalerrors += Process (20, "ListIdentifiers (oai_dc, from YYYY-MM-DDThh:mm:ssZ)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&from=2001-01-01T00:00:00Z", YES_OR_NO_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");
   }
   else
   {
      totalerrors += Process (20, "ListIdentifiers (oai_dc, from YYYY-MM-DDThh:mm:ssZ)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&from=2001-01-01T00:00:00Z", YES_ERROR, NO_UPDATE, "badArgument");
   }
   
   totalerrors += Process (21, "ListIdentifiers (oai_dc, from YYYY)", argv[1], "ListIdentifiers", "&metadataPrefix=oai_dc&from=2001", YES_ERROR, NO_UPDATE, "badArgument");
   

// LIST METADATA FORMATS 2

   if (strlen (identifier) > 0)
   {
      sprintf (str, "&identifier=%s", identifier);
      result = Process (22, "ListMetadataFormats (identifier)", argv[1], "ListMetadataFormats", str, NO_ERROR, UPDATE_VARIABLES, "");
      totalerrors += result;

      if (result == 0)
      {
         if (strcmp (metadataFormat, "oai_dc") == 0)
            cout << "---- [ " << Translate ("Only oai_dc supported")
                 << " ]\n";
         else if (strlen (metadataFormat) > 0)
            cout << "---- [ " << Translate ("Sample Metadata Format")
                 << " = " << metadataFormat << " ]\n";
         else
         {
            totalerrors++;
            cout << "**** [" << Translate ("ERROR") << "] "
                 << Translate ("Metadata format missing !") << "\n";
         }
      }
   }
   else
   {
      ProcessSkip (22, "ListMetadataFormats (identifier)");
   }

   sprintf (str, "&identifier=really_wrong_id");
   totalerrors += Process (23, "ListMetadataFormats (illegal_id)", argv[1], "ListMetadataFormats", str, YES_ERROR, NO_UPDATE, "idDoesNotExist,badArgument");

// GET RECORD

   if (strlen (identifier) > 0)
   {
      if ((strlen (metadataFormat) > 0) && (strcmp (metadataFormat, "oai_dc") != 0))
      {
         sprintf (str, "&identifier=%s&metadataPrefix=%s", identifier, metadataFormat);
         totalerrors += Process (24, "GetRecord (identifier, metadataPrefix)", argv[1], "GetRecord", str, NO_ERROR, NO_UPDATE, "");
      }
      else
      {
         ProcessSkip (24, "GetRecord (identifier, metadataPrefix)");
      }

      sprintf (str, "&identifier=%s&metadataPrefix=oai_dc", identifier);
      totalerrors += Process (25, "GetRecord (identifier, oai_dc)", argv[1], "GetRecord", str, NO_ERROR, NO_UPDATE, "");

      sprintf (str, "&identifier=%s", identifier);
      totalerrors += Process (26, "GetRecord (identifier)", argv[1], "GetRecord", str, YES_ERROR, NO_UPDATE, "badArgument");

      sprintf (str, "&identifier=%s&metadataPrefix=really_wrong_mdp", identifier);
      totalerrors += Process (27, "GetRecord (identifier, illegal_mdp)", argv[1], "GetRecord", str, YES_ERROR, NO_UPDATE, "cannotDisseminateFormat,badArgument");
   }
   else
   {
      ProcessSkip (24, "GetRecord (identifier, metadataPrefix)");
      ProcessSkip (25, "GetRecord (identifier, oai_dc)");
      ProcessSkip (26, "GetRecord (identifier)");
      ProcessSkip (27, "GetRecord (identifier, illegal_mdp)");
   }

   totalerrors += Process (28, "GetRecord (oai_dc)", argv[1], "GetRecord", "&metadataPrefix=oai_dc", YES_ERROR, NO_UPDATE, "badArgument");

   totalerrors += Process (29, "GetRecord (illegal_id, oai_dc)", argv[1], "GetRecord", "&identifier=really_wrong_id&metadataPrefix=oai_dc", YES_ERROR, NO_UPDATE, "idDoesNotExist,badArgument");

   totalerrors += Process (30, "GetRecord (invalid_id, oai_dc)", argv[1], "GetRecord", "&identifier=invalid%5C%22id&metadataPrefix=oai_dc", YES_ERROR, NO_UPDATE, "badArgument,idDoesNotExist");

// LIST RECORDS

   result = Process (31, "ListRecords (oai_dc, from/until)", argv[1], "ListRecords", "&metadataPrefix=oai_dc&from=2000-01-01&until=2000-01-01", YES_OR_NO_ERROR, UPDATE_VARIABLES, "noRecordsMatch,badArgument");
   totalerrors += result;

   if (result == 0)
   {
      if (strlen (record_resumptionToken) > 0)
      {
         cout << "---- [ " << Translate ("Record Resumption Token") 
              << " = " << record_resumptionToken << " ]\n";
         sprintf (str, "&resumptionToken=%s", record_resumptionToken);
         totalerrors += Process (32, "ListRecords (resumptionToken)", argv[1], "ListRecords", str, NO_ERROR, NO_UPDATE, "");
      }
      else
      {
         ProcessSkip (32, "ListRecords (resumptionToken)");
      }
   }
   else
   {
      ProcessSkip (32, "ListRecords (resumptionToken)");
   }
   
   if ((strlen (metadataFormat) > 0) && (strcmp (metadataFormat, "oai_dc") != 0))
   {
      sprintf (str, "&metadataPrefix=%s&from=2000-01-01&until=2000-01-01", metadataFormat);
      totalerrors += Process (33, "ListRecords (metadataPrefix, from/until)", argv[1], "ListRecords", str, YES_OR_NO_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");
   }
   else
   {
      ProcessSkip (33, "ListRecords (metadataPrefix, from/until)");
   }

   sprintf (str, "&metadataPrefix=oai_dc&set=really_wrong_set&from=some_random_date&until=some_random_date");
   totalerrors += Process (34, "ListRecords (oai_dc, illegal_set, illegal_from/until)", argv[1], "ListRecords", str, YES_ERROR, NO_UPDATE, "badArgument,noSetHierarchy");

   totalerrors += Process (35, "ListRecords", argv[1], "ListRecords", "", YES_ERROR, NO_UPDATE, "badArgument");

   totalerrors += Process (36, "ListRecords (oai_dc, from granularity != until granularity)", argv[1], "ListRecords", "&metadataPrefix=oai_dc&from=2001-01-01&until=2002-01-01T00:00:00Z", YES_ERROR, NO_UPDATE, "badArgument");

   int edate = (earliestDatestamp[0]-48) * 1000 +
               (earliestDatestamp[1]-48) * 100 + 
               (earliestDatestamp[2]-48) * 10 +
               (earliestDatestamp[3]-48);
   edate--;

   // original had %04U
   sprintf (str, "&metadataPrefix=oai_dc&until=%04u%s", edate, earliestDatestamp+4);

   totalerrors += Process (37, "ListRecords (oai_dc, until before earliestDatestamp)", argv[1], "ListRecords", str, YES_ERROR, NO_UPDATE, "noRecordsMatch,badArgument");
   
   strcpy (record_resumptionToken, "a_junk_token");
   result = Process (38, "ListRecords (oai_dc)", argv[1], "ListRecords", "&metadataPrefix=oai_dc", NO_ERROR, UPDATE_VARIABLES, "");
   totalerrors += result;
   if (result == 0)
   {
      if (strlen (record_resumptionToken) == 0)
      {
         totalerrors++;
         cout << "**** [" << Translate ("ERROR") << "] "
              << Translate ("Empty resumptionToken in initial ListRecords response !")
              << "\n";
      }
   }
      
   totalerrors += Process (39, "ListRecords (illegal_resumptiontoken)", argv[1], "ListRecords", "&resumptionToken=junktoken", YES_ERROR, NO_UPDATE, "badResumptionToken");

// SET CORRESPONDENCE

   if (strlen (set) > 0)
   {
      sprintf (str, "&metadataPrefix=oai_dc&set=%s", set);
      result = Process (40, "ListIdentifiers (oai_dc, set)", argv[1], "ListIdentifiers", str, YES_OR_NO_ERROR, UPDATE_VARIABLES, "noRecordsMatch,badArgument");
      totalerrors += result;
 
      if (result == 0)
      {
         if (strlen (identifier) > 0)
         {
            sprintf (str, "&identifier=%s&metadataPrefix=oai_dc", identifier);
            result = Process (41, "GetRecord (identifier, oai_dc)", argv[1], "GetRecord", str, NO_ERROR, UPDATE_VARIABLES, "");
            totalerrors += result;

            if (result == 0)
            {
               if (strstr (setSpec, set) != NULL)
                  cout << "---- [ " << Translate ("Found setSpec in header") 
                       << " ]\n";
               else
               {
                  totalerrors++;
                  cout << "**** [" << Translate ("ERROR") << "] "
                       << Translate ("Could not find setSpec in header !")
                       << " set = " << set << ", setSpecs=" << setSpec << "\n";
               }
            }
         }
         else
         {
            ProcessSkip (41, "GetRecord (identifier, oai_dc)");
         }
      }
      else
      {
         ProcessSkip (41, "GetRecord (identifier, oai_dc)");
      }
   }
   else
   {
      ProcessSkip (40, "ListIdentifiers (oai_dc, set)");
      ProcessSkip (41, "GetRecord (identifier, oai_dc)");
   }

// ILLEGAL VERB

   totalerrors += Process (42, "IllegalVerb", argv[1], "IllegalVerb", "", YES_ERROR, NO_UPDATE, "badVerb");

   cout << "\n\n---- " << Translate ("Total Errors") << " : "
        << totalerrors << "\n";
         
   if (totalerrors > 0)
      exit (1);
   else
      exit (0);
}
	
