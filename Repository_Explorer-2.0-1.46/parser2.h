//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 2.0a1-1.4     |
// | Hussein Suleman                                                      |
// | March 2002                                                           |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : parser
//  Purpose    : Parse valid XML
// ======================================================================

#ifndef _PARSER2_
#define _PARSER2_

#include "taglist.h"

void ProcessOAIPMH ( TagList *tl, char **requestURL, char **responseDate, char *verb, char *identifier, char *buffer );

#endif
