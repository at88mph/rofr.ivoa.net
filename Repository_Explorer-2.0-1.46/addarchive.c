//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 2.0b2-1.44    |
// | Hussein Suleman                                                      |
// | May 2002                                                             |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Library Research Laboratory                                 |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : addarchive
//  Purpose    : add a new archive to list of predefined archives
// ======================================================================

// #include <fstream.h>
// #include <iostream.h>
#include <fstream>
#include <iostream>

// add pnh
using namespace std;
#include <string.h>

#include <stdio.h>
#include <stdlib.h>

#include "blocks.h"
#include "cgi.h"
#include "config.h"
#include "language.h"

// ----------------------------------------------------------------------
//  Function   : ScreenOne
// ----------------------------------------------------------------------
//  Purpose    : generate screen to capture archive url
//  Parameters : 1. language name
//               2. bgcolor
//               3. headercolor
//               4. blockcolor
//  Result     : (none)
// ----------------------------------------------------------------------

void ScreenOne ( char *language, char *bgcolor, char *headercolor, 
                 char *blockcolor )
{
   cout << "<h2>" << Translate ("Test and Add an archive") 
        << "</h2><hr>" 
        << Translate ("Please note that these tests are neither definitive nor exhaustive")
        << ". " << Translate ("They serve not as a proof of correctness of implementation, but rather indicate possible errors if they exist")
        << ". " << Translate ("If you have any questions please contact the author at the address below")
        << ".<hr>";

   cout << Translate ("Enter the base URL of the archive") << " :<p>"
        << "<form method=\"get\" action=\"" << addarchive << "\">"
        << "<input type=\"text\" name=\"archiveurl\" size=\"100\" maxlength=\"200\">"
        << "<input type=\"hidden\" name=\"language\" value=\"" << language << "\">"
        << "<input type=\"hidden\" name=\"bgcolor\" value=\"" << bgcolor << "\">"
        << "<input type=\"hidden\" name=\"headercolor\" value=\"" << headercolor << "\">"
        << "<input type=\"hidden\" name=\"blockcolor\" value=\"" << blockcolor << "\">"
//        << "<input type=\"hidden\" name=\"gotarchive\" value=\"gotarchive\">"
        << "<input type=\"submit\" name=\"submit\" value=\""
        << Translate ("Test the archive") << "\">"
        << "</form>";
}

// ----------------------------------------------------------------------
//  Function   : ScreenTwo
// ----------------------------------------------------------------------
//  Purpose    : test archive and generate screen for more data
//  Parameters : 1. archive url
//               2. language name
//               3. bgcolor
//               4. headercolor
//               5. blockcolor
//  Result     : (none)
// ----------------------------------------------------------------------

void ScreenTwo ( char *archiveurl, char *language, char *bgcolor, 
                 char *headercolor, char *blockcolor )
{
   cout << "<pre>"; cout.flush ();
   
   char complycommand[2048];
   sprintf (complycommand, "%s %s %s", complypath, archiveurl, language);
   int status = system (complycommand) >> 8;
   
   cout << "</pre>";

   if (status == 0)
   {
      cout << "<hr><h3>" << Translate ("Congratulations !") << "</h3><p>"
           << Translate ("Your archive satisfied all the tests we performed")
           << ". " << Translate ("You may now add it to the list of archives on the front page if you wish by filling in the following additional details")
           << ":</p><p><form method=\"get\" action=\"" << addarchive << "\">"
           << Translate ("Base URL of Archive") << " : " << archiveurl
           << "<input type=\"hidden\" name=\"archiveurl\" value=\"" << archiveurl << "\">"
           << "<input type=\"hidden\" name=\"language\" value=\"" << language << "\">"
//           << "<input type=\"hidden\" name=\"gotarchive\" value=\"gotarchive\">"
           << "<input type=\"hidden\" name=\"bgcolor\" value=\"" << bgcolor << "\">"
           << "<input type=\"hidden\" name=\"headercolor\" value=\"" << headercolor << "\">"
           << "<input type=\"hidden\" name=\"blockcolor\" value=\"" << blockcolor << "\">"
           << "</p><p>" << Translate ("Name of Archive") << " : <input type=text name=archivename size=50 maxlength=200>"
           << "</p><p>" << Translate ("Archive website") << " : <input type=text name=archive size=50 maxlength=200>"
           << "</p><p><input type=\"submit\" name=\"submit\" value=\"" << Translate ("Submit Archive to List") << "\">"
           << "</form></p><hr>";
   }
}

// ----------------------------------------------------------------------
//  Function   : ScreenThree
// ----------------------------------------------------------------------
//  Purpose    : save information to predef file
//  Parameters : 1. archive oai url
//               2. archive name
//               3. archive website
//  Result     : (none)
// ----------------------------------------------------------------------

void ScreenThree ( char *archiveurl, char *archivename, char *archive )
{
   fstream predef2 ("predef2", ios::out|ios::app);
   char separator = separatorchar;
   
   predef2 << archivename << separator << archiveurl << separator << archive << endl;

   predef2.close ();
   
   cout << "<h4>" << Translate ("Thank You ! Your archive has been added")
        << "</h4>";
}

int main ()
{
   CGIProcessor cgi;
   
   char *language = cgi.ParmList.Search ("language");
   char *archiveurl = cgi.ParmList.Search ("archiveurl");
   char *archivename = cgi.ParmList.Search ("archivename");
   char *archive = cgi.ParmList.Search ("archive");
   char *bgcolor = cgi.ParmList.Search ("bgcolor");
   char *headercolor = cgi.ParmList.Search ("headercolor");
   char *blockcolor = cgi.ParmList.Search ("blockcolor");   
//   char *gotarchive = cgi.ParmList.Search ("gotarchive");
   
   SetLanguage (language);
   if ((!language) || (strlen (language) == 0))
      language = "enus.lan";
   if ((!bgcolor) || (strlen (bgcolor) == 0))
      bgcolor = bgcolorpreset;
   if ((!headercolor) || (strlen (headercolor) == 0))
      headercolor = headercolorpreset;
   if ((!blockcolor) || (strlen (blockcolor) == 0))
      blockcolor = blockcolorpreset;

   OutputHeaders (reprotocolversion, bgcolor, headercolor);

   if ((archiveurl == NULL) || (strcmp (archiveurl, "") == 0))
//      || (gotarchive == NULL) || (strcmp (gotarchive, "") == 0))
      ScreenOne (language, bgcolor, headercolor, blockcolor);
   else if ((archivename == NULL) || (strcmp (archivename, "") == 0) ||
            (archive == NULL) || (strcmp (archive, "") == 0))
      ScreenTwo (archiveurl, language, bgcolor, headercolor, blockcolor);
   else
      ScreenThree (archiveurl, archivename, archive);

   OutputFooters (language, bgcolor, headercolor, blockcolor);
   
   return 0;   
}
