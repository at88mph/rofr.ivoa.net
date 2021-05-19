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
//  Module     : about
//  Purpose    : information and acknowledgements
// ======================================================================

// #include <iostream.h>
#include <iostream>

// add pnh
using namespace std;
#include <string.h>

#include "blocks.h"
#include "cgi.h"
#include "config.h"
#include "language.h"

// ----------------------------------------------------------------------
//  Function   : HelpScreen
// ----------------------------------------------------------------------
//  Purpose    : generate help screen
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

void HelpScreen ()
{
   cout << "<h2>About</h2>"
        << "<p>The Repository Explorer was initally developed at the <a href=\""
        << "http://www.dlib.vt.edu\">Digital Library Research Laboratory</a>"
        << " at <a href=\"http://www.vt.edu\">Virginia Tech</a>. "
        << "It is currently hosted and maintained at the <a href=\"http://aim.cs.uct.ac.za/\">"
        << "Advanced Information Management Laboratory</a> at <a href=\"http://www.uct.ac.za\">"
        << "University of Cape Town</a>. "
        << "The primary purpose of the RE is to help developers to create new "
        << "Open Archives - it is not meant for users to browse production "
        << "sites, hence the sparse user interface.</p>";
        
   cout << "<h2>Acknowledgements</h2>"
        << "<ul>"
        << "<li>Mann-Ho Lee, for the Korean interface translation</li>"
        << "<li>Jochen Rode, for the German interface translation</li>"
        << "<li>Wensi Xi, for the Chinese interface translation</li>"
        << "<li>All the OAI technical people and users of OAI-compliant "
        << "software for finding and reporting errors and making suggestions</li>"
        << "</ul>";
}

// ----------------------------------------------------------------------
// ----------------------------------------------------------------------
// ----------------------------------------------------------------------

int main ()
{
   CGIProcessor cgi;
   
   char *language = cgi.ParmList.Search ("language");
   char *bgcolor = cgi.ParmList.Search ("bgcolor");
   char *headercolor = cgi.ParmList.Search ("headercolor");
   char *blockcolor = cgi.ParmList.Search ("blockcolor");   
   
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

   HelpScreen ();

   OutputFooters (language, bgcolor, headercolor, blockcolor);
   
   return 0;   
}
