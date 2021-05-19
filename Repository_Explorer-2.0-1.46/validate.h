//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 2.0a3-1.42    |
// | Hussein Suleman                                                      |
// | April 2001                                                           |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : validate
//  Purpose    : Perform XSD validation on XML file
// ======================================================================

#ifndef _VALIDATE_
#define _VALIDATE_

char *ValidateXerces ( char *protocolVersion, char *datafilename, 
                       char *xsvfilename, char *verb, char *metadataPrefix, int local = 1 );

char *ValidateXSV ( char *protocolVersion, char *datafilename, 
                    char *xsvfilename, char *verb, char *metadataPrefix, int local = 1 );

void LocalTransform ( char *protocolVersion, char *datafilename,
                      char *localfilename, char *verb, char *metadataPrefix, int local = 1 );

#endif 
