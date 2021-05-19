//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer                         |
// | Hussein Suleman                                                      |
// | February 2000                                                        |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : cgi
//  Purpose    : CGI parameter processing
// ======================================================================

#ifndef _CGI_
#define _CGI_

#include "attlist.h"

// ======================================================================
//  Class      : CGIProcessor
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Process CGI parameters from GET/PUT
// ======================================================================

class CGIProcessor
{
public:
   AttList ParmList;
   char *QueryString;
   CGIProcessor ();
   ~CGIProcessor ();
   void Clean ( char *s );
   void Process ();
};

#endif


