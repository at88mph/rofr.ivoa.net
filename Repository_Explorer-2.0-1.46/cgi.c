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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "cgi.h"
#include "attlist.h"

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : CGIProcessor
// ----------------------------------------------------------------------
//  Purpose    : Process parameters
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

CGIProcessor::CGIProcessor ()
{
   Process ();
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : CGIProcessor
// ----------------------------------------------------------------------
//  Purpose    : Free memory
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

CGIProcessor::~CGIProcessor ()
{
   free (QueryString);
}

// ----------------------------------------------------------------------
//  Method     : Clean
// ----------------------------------------------------------------------
//  Class      : CGIProcessor
// ----------------------------------------------------------------------
//  Purpose    : Convert special characters in URL string
//  Parameters : URL string
//  Result     : (none)
// ----------------------------------------------------------------------

void CGIProcessor::Clean ( char *s )
{
   int qlen = strlen (s);
   for ( int i=0; i<qlen; i++ )
   {
      if (s[i] == '+')
         s[i] = ' ';
      if (s[i] == '\n')
         s[i] = ' ';
      if ((s[i] == '%') && (i+2 < qlen) && 
         (((s[i+1] >= '0') && (s[i+1] <= '9')) || 
          ((s[i+1] >= 'a') && (s[i+1] <= 'f')) || 
          ((s[i+1] >= 'A') && (s[i+1] <= 'F'))) &&
         (((s[i+2] >= '0') && (s[i+2] <= '9')) || 
          ((s[i+2] >= 'a') && (s[i+2] <= 'f')) || 
          ((s[i+2] >= 'A') && (s[i+2] <= 'F'))))
      {
         unsigned char accum = 0;
         if (s[i+1] >= 'a')
            accum += 16 * (s[i+1] - 'a' + 10);
         else if (s[i+1] >= 'A')
            accum += 16 * (s[i+1] - 'A' + 10);
         else
            accum += 16 * (s[i+1] - '0');
         if (s[i+2] >= 'a')
            accum += s[i+2] - 'a' + 10;
         else if (s[i+2] >= 'A')
            accum += s[i+2] - 'A' + 10;
         else
            accum += s[i+2] - '0';
         s[i] = accum;
         for ( int j=i+1; j<qlen-2; j++ )
            s[j] = s[j+2];
         s[qlen-2] = 0;
         qlen -= 2;
      }
   }
}

// ----------------------------------------------------------------------
//  Method     : Process
// ----------------------------------------------------------------------
//  Class      : CGIProcessor
// ----------------------------------------------------------------------
//  Purpose    : Get, parse URL string and add parameters to list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

void CGIProcessor::Process ()
{
   char *EnvString = getenv ("QUERY_STRING");
   if ((EnvString == NULL) || (*EnvString == 0))
   {
      char *buf = (char *)malloc(16384);
      int len = fread (buf, 1, 16384, stdin);
      buf[len] = 0;
      QueryString = (char *)malloc(strlen (buf)+1);
      strcpy (QueryString, buf);
      free (buf);
   }
   else
   {
      QueryString = (char *)malloc(strlen (EnvString)+1);
      strcpy (QueryString, EnvString);
   }
   while ((QueryString[strlen (QueryString)-1] == '\n') ||
          (QueryString[strlen (QueryString)-1] == '\r'))
      QueryString[strlen (QueryString)-1] = 0;
   unsigned int posamp = 0;
   do
   {
      unsigned int posequal = posamp;
      unsigned int posstart = posamp;
      while ((posamp < strlen (QueryString)) && (QueryString[posamp] != '&'))
         posamp++;
      if (posamp > posstart)
      {
         while ((posequal < posamp) && (QueryString[posequal] != '='))
            posequal++;
         if (posequal < posamp)
         {
            char aname[2048], avalue[2048];
            char t;
            
            t = QueryString[posequal];
            QueryString[posequal] = 0;
            strcpy (aname, QueryString+posstart);
            QueryString[posequal] = t;
            
            t = QueryString[posamp];
            QueryString[posamp] = 0;
            strcpy (avalue, QueryString+posequal+1);
            QueryString[posamp] = t;
            
            Clean (aname);
            Clean (avalue);
            
            ParmList.Add (new ALNode (aname, avalue));
         }
      }
      posamp++;
   } while (posamp < strlen (QueryString));
}   
