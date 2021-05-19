//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 2.0b1-1.43    |
// | Hussein Suleman                                                      |
// | May 2002                                                             |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Library Research Laboratory                                 |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : blocks
//  Purpose    : generate header and footer blocks
// ======================================================================

//#include <iostream.h>
#include <iostream>

// added pnh
#include <string.h>
using namespace std;

#include "config.h"
#include "language.h"

// ----------------------------------------------------------------------
//  Function   : OutputHeaders
// ----------------------------------------------------------------------
//  Purpose    : Output headers for page
//  Parameters : 1. protocol version
//               2. bgcolor
//               3. headercolor
//  Result     : (none)
// ----------------------------------------------------------------------

void OutputHeaders ( char *protocolVersion, char *bgcolor, char *headercolor )
{
   if ((protocolVersion == NULL) || (strlen (protocolVersion) == 0))
   {
      protocolVersion = reallprotocolversions;
   }

   // base output data
   cout << "Content-type: text/html\n\n"
        << "<html><head><title>"
        << "Open Archives Initiative - " << Translate ("Repository Explorer")
        << "</title></head><body bgcolor=\"#" << bgcolor << "\">";
                                 
   cout << "<center><table width=\"90%\" border=0 cellspacing=0 cellpadding=10>"
        << "<tr bgcolor=\"#" << headercolor 
        << "\"><td align=center valign=center rowspan=2>"
        << "<a href=\"http://www.openarchives.org\">"
        << "<img src=\"" << oaiimageprefix << "OA100.gif\"></a>"
        << "</td><td width=\"*\" align=center valign=center>"
        << "<h2>" << "Open Archives Initiative - " 
        << Translate ("Repository Explorer") << "</h2>"
        << "</td></tr><tr bgcolor=\"#" << headercolor 
        << "\"><td align=right valign=center><i>"
        << Translate ("explorer version") << " - " << reversion << " : "
        << Translate ("protocol version") << " - " 
        << protocolVersion << " : "
        << Translate (redate)
        << "</i></td></tr></table></center>";

   cout.flush ();
}

// ----------------------------------------------------------------------
//  Function   : OutputFooters
// ----------------------------------------------------------------------
//  Purpose    : Output footers for page
//  Parameters : 1. language
//               2. bgcolor
//               3. headercolor
//               4. blockcolor
//  Result     : (none)
// ----------------------------------------------------------------------

void OutputFooters ( char *language, char *bgcolor, char *headercolor,
                     char *blockcolor )
{
   cout << "<table width=\"100%\" border=0 cellspacing=0 cellpadding=10><tr bgcolor=\"#" 
        << headercolor << "\">"
        << "<td align=left>" 
        << "<a href=\"testoai?language=" << language 
        << "&bgcolor=" << bgcolor
        << "&headercolor=" << headercolor
        << "&blockcolor=" << blockcolor
        << "\">"
        << Translate ("home") << "</a> <a href=\"about?language=" << language 
        << "&bgcolor=" << bgcolor
        << "&headercolor=" << headercolor
        << "&blockcolor=" << blockcolor
        << "\">about</a>"
        << "</td><td align=right>"
        << Translate ("Send all comments to")
        << " <a href=\"mailto:hussein@cs.uct.ac.za\">hussein@cs.uct.ac.za</a>"
        << " --- <a href=\"http://www.cs.uct.ac.za\">" 
        << Translate ("Dept of Computer Science") << "</a>"
        << "@<a href=\"http://www.uct.ac.za\">U. of Cape Town</a>"
        << "</td></tr></table>";

   cout << "</body></html>";
}
