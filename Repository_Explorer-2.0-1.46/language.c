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
//  Module     : language
//  Purpose    : Translate from base language to secondary language
// ======================================================================

//#include <fstream.h>
//#include <iostream.h>
#include <fstream>
#include <iostream>

// added pnh
#include <string.h>
using namespace std;

#include "attlist.h"
#include "language.h"

AttList TranslationMatrix;

// ----------------------------------------------------------------------
//  Function   : SetLanguage
// ----------------------------------------------------------------------
//  Purpose    : Sets the secondary language
//  Parameters : language identifier
//  Result     : (none)
// ----------------------------------------------------------------------

void SetLanguage ( char *language )
{
   char base[1024];
   char secondary[1024];
   
   if ((language == NULL) || (strlen (language) == 0))
      return;

   ifstream langfile (language, ios::in);

   // skip over language name
   langfile.getline (base, sizeof (base));   
   
   // read in pairs
   while (!langfile.eof ())
   {
      langfile.getline (base, sizeof (base));
      while ((base[0] != 0) && ((base[strlen (base)-1] == ' ') || (base[strlen (base)-1] == 9)))
         base[strlen (base)-1] = 0;      
      while (((base[0] == '#') || (base[0] == 0)) && (!langfile.eof ()))
         langfile.getline (base, sizeof (base));
      if (!langfile.eof ())
      {
         langfile.getline (secondary, sizeof (secondary));
         while ((secondary[0] != 0) && ((secondary[strlen (secondary)-1] == ' ') || (secondary[strlen (secondary)-1] == 9)))
            secondary[strlen (secondary)-1] = 0;      
         while (((secondary[0] == '#') || (secondary[0] == 0)) && (!langfile.eof ()))
            langfile.getline (secondary, sizeof (secondary));
         if (!((secondary[0] == '#') || (secondary[0] == 0)))
            TranslationMatrix.Add (new ALNode (base, secondary));
      }
   }
}

// ----------------------------------------------------------------------
//  Function   : Translate
// ----------------------------------------------------------------------
//  Purpose    : Translate a string
//  Parameters : string
//  Result     : translated string
// ----------------------------------------------------------------------

char *Translate ( char *s )
{
   char *t = TranslationMatrix.Search (s);
   if (t)
      return t;
   else
      return s;
}
