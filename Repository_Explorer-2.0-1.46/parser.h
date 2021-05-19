//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 1.0-1.1       |
// | Hussein Suleman                                                      |
// | January 2001                                                         |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : parser
//  Purpose    : Parse valid XML
// ======================================================================

#ifndef _PARSER_
#define _PARSER_

#include "taglist.h"

void ProcessIdentify ( TagList *tl, char **requestURL, char **responseDate );
void ProcessListSets ( TagList *tl, char **requestURL, char **responseDate );
void ProcessListMetadataFormats ( TagList *tl, char *identifier, char **requestURL, char **responseDate );
void ProcessListIdentifiers ( TagList *tl, char **requestURL, char **responseDate );
void ProcessGetRecord ( TagList *tl, char **requestURL, char **responseDate );
void ProcessListRecords ( TagList *tl, char **requestURL, char **responseDate );

#endif 
