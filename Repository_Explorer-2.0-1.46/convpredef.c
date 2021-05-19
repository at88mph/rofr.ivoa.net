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
//  Module     : convpredef
//  Purpose    : Convert old predef file to new format
// ======================================================================

#include <fstream>
#include <iostream>

using namespace std;

int main ()
{
   char aname[1024];
   char aurl[1024];
   char adesc[1024];
   char separator = 250;

   // create stored list of archives in javascript
   fstream * predef = new fstream ("predef", ios_base::in);
   fstream * newpredef = new fstream ("predef2", ios_base::out);
   while (!(predef->eof ()))
   {
      predef->getline (aname, sizeof (aname));
      predef->getline (aurl, sizeof (aurl));
      predef->getline (adesc, sizeof (adesc));
      
      *newpredef << aname << separator << aurl << separator << adesc << endl;
   }
   
   predef->close ();
   newpredef->close ();
}
	
