//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 1.0-1.1       |
// | Hussein Suleman                                                      |
// | April 2001                                                           |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : escape
//  Purpose    : Perform XML escaping
// ======================================================================

//#include <iostream.h>
#include <iostream>

// added pnh
using namespace std;

// ----------------------------------------------------------------------
//  Function   : Escape
// ----------------------------------------------------------------------
//  Purpose    : Escapes and outputs strings with <>&
//  Parameters : string to output
//  Result     : None
// ----------------------------------------------------------------------

void Escape ( char *s )
{
   long int i = 0;
   while (s[i] != 0)
   {
      if (s[i] == '<')
         cout << "&lt;";
      else if (s[i] == '>')
         cout << "&gt;";
      else if ((s[i] == '&') && (s[i+1] != 0) && (s[i+1] != '#')) 
         cout << "&amp;";
      else
         cout << s[i];
      i++;
   }
}
