//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 1.1-1.3       |
// | Hussein Suleman                                                      |
// | August 2001                                                          |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Library Research Laboratory                                 |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : blocks
//  Purpose    : generate header and footer blocks
// ======================================================================

#ifndef _BLOCKS_
#define _BLOCKS_

void OutputHeaders ( char *protocolVersion, char *bgcolor, char *headercolor );
void OutputFooters ( char *language, char *bgcolor, char *headercolor,
                     char *blockcolor );

#endif 
