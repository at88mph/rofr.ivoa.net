//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 1.0-1.1       |
// | Hussein Suleman                                                      |
// | November 2000                                                        |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : xml
//  Purpose    : Create parse tree from XML file
// ======================================================================

// #include <fstream.h>
// #include <iostream.h>
#include <fstream>
#include <iostream>
#include <string.h>

// added pnh
using namespace std;

#include <stdio.h>
#include <stdlib.h>

#include "expat/lib/expat.h"

#include "taglist.h"

TagList *LastText = NULL;

// ----------------------------------------------------------------------
//  Function   : trim
// ----------------------------------------------------------------------
//  Purpose    : Remove leading and trailing whitespace
//  Parameters : String
//  Result     : (none)
// ----------------------------------------------------------------------

void trim ( char *s )
{
   int numfront = 0;
   int pos = strlen (s);
   while ((numfront < pos) && 
          ((s[numfront] == '\n') || (s[numfront] == '\r') || (s[numfront] == ' ') || (s[numfront] == '\t')))
      numfront++;
   for ( int i=0; i<(pos-numfront); i++ )
      s[i] = s[i+numfront];
   s[strlen (s) - numfront] = 0;
   pos = strlen (s);
   while ((pos > 0) && 
          ((s[pos-1] == '\n') || (s[pos-1] == '\r') || (s[pos-1] == ' ') || (s[pos-1] == '\t')))
      pos--;
   s[pos] = 0;
}

// ----------------------------------------------------------------------
//  Function   : startElement
// ----------------------------------------------------------------------
//  Purpose    : Handler for starting XML elements
//  Parameters : 1. TagStack
//               2. Name of tag
//               3. Attribute list
//  Result     : (none)
// ----------------------------------------------------------------------

void startElement ( void *ts, const char *name, const char **atts )
{
   if (LastText)
   {
      LastText->Trim ();
      TagList *tl = ((TagStack *)(ts))->Top ();
      if (strlen (LastText->tag) > 0)
         tl->Add (LastText);
      LastText = NULL;
   }
   AttList *att = new AttList ();
   while (*atts != 0)
   {
      char *aname = (char *)*atts;
      atts++;
      char *avalue = (char *)*atts;
      atts++;
      trim (aname);
      trim (avalue);
      att->Add (aname, avalue);
   }
   TagList *tl = ((TagStack *)(ts))->Top ();
   trim ((char *)name);
   TagList *ntl = new TagList ((char *)name, TAG, att);
   tl->Add (ntl);
   ((TagStack *)(ts))->Push (ntl);
}

// ----------------------------------------------------------------------
//  Function   : endElement
// ----------------------------------------------------------------------
//  Purpose    : Handler for ending XML elements
//  Parameters : 1. TagStack
//               2. Name of tag
//  Result     : (none)
// ----------------------------------------------------------------------

void endElement ( void *ts, const char * )
{
   if (LastText)
   {
      LastText->Trim ();
      TagList *tl = ((TagStack *)(ts))->Top ();
      if (strlen (LastText->tag) > 0)
         tl->Add (LastText);
      LastText = NULL;
   }
   ((TagStack *)(ts))->Pop ();
}

// ----------------------------------------------------------------------
//  Function   : cdata
// ----------------------------------------------------------------------
//  Purpose    : Handler for CDATA elements
//  Parameters : 1. TagStack
//               2. Data string
//               3. Length of data string
//  Result     : (none)
// ----------------------------------------------------------------------

void cdata ( void *ts, const char *data, int len )
{
// *   char *text = (char *)malloc(len+1);
   char *text = (char *)malloc((len*10)+1);

   int newlen = len;
   int j = 0;
   // UTF-8 conversion to EASCII // nay, unicode
   for ( int i=0; i<len; i++ )
   {
      if (((unsigned char)data[i] > 0x7f) && (i<(len-1)) && 
          ((unsigned char)data[i] <= 0xdf))
      {
         unsigned char highbyte = (data[i] & 0x03);
         unsigned char lowbyte = (data[i+1] & 0x3f);
         unsigned char fullvalue = (highbyte << 6) | (lowbyte);
         unsigned char leftdigit = (fullvalue & 0xf0) >> 4;
         unsigned char rightdigit = (fullvalue & 0x0f);
         unsigned char h_lowbyte = (data[i] & 0x1c);
         unsigned char h_fullvalue = h_lowbyte >> 2; 
         unsigned char h_rightdigit = h_fullvalue;
         text[j++] = '&';
         text[j++] = '#';
         text[j++] = 'x';
         text[j++] = '0';
         if (h_rightdigit <= 9)
            text[j++] = '0'+h_rightdigit;
         else
            text[j++] = 'a'+(h_rightdigit-10);
         if (leftdigit <= 9)
            text[j++] = '0'+leftdigit;
         else
            text[j++] = 'a'+(leftdigit-10);
         if (rightdigit <= 9)
            text[j++] = '0'+rightdigit;
         else
            text[j++] = 'a'+(rightdigit-10);
         text[j++] = ';';
         i++;
// * old version for eascii
// *         text[i++] = fullvalue;
// *         newlen--;
      }
      else if (((unsigned char)data[i] > 0xdf) && (i<(len-1)) && 
          ((unsigned char)data[i] <= 0xef))
      {
         unsigned char highbyte = (data[i+1] & 0x03);
         unsigned char lowbyte = (data[i+2] & 0x3f);
         unsigned char fullvalue = (highbyte << 6) | (lowbyte);
         unsigned char leftdigit = (fullvalue & 0xf0) >> 4;
         unsigned char rightdigit = (fullvalue & 0x0f);
         unsigned char h_highbyte = (data[i] & 0x0f);
         unsigned char h_lowbyte = (data[i+1] & 0x3c);
         unsigned char h_fullvalue = (h_highbyte << 4) | (h_lowbyte >> 2);
         unsigned char h_leftdigit = (h_fullvalue & 0xf0) >> 4;
         unsigned char h_rightdigit = (h_fullvalue & 0x0f);
         text[j++] = '&';
         text[j++] = '#';
         text[j++] = 'x';
         if (h_leftdigit <= 9)
            text[j++] = '0'+h_leftdigit;
         else
            text[j++] = 'a'+(h_leftdigit-10);
         if (h_rightdigit <= 9)
            text[j++] = '0'+h_rightdigit;
         else
            text[j++] = 'a'+(h_rightdigit-10);
         if (leftdigit <= 9)
            text[j++] = '0'+leftdigit;
         else
            text[j++] = 'a'+(leftdigit-10);
         if (rightdigit <= 9)
            text[j++] = '0'+rightdigit;
         else
            text[j++] = 'a'+(rightdigit-10);
         text[j++] = ';';
         i++;
      }
      else
         text[j++] = data[i];
   }
   text[j] = 0;
      
   TagList *tl = ((TagStack *)(ts))->Top ();
   if (strlen (text) > 0)
   {
      if (LastText)
         LastText->Append (text);
      else
      {
         TagList *ntl = new TagList (text, TEXT, NULL);
         //tl->Add (ntl);
         LastText = ntl;
      }
   }
   free (text);
}

// ----------------------------------------------------------------------
//  Function   : runparser
// ----------------------------------------------------------------------
//  Purpose    : Run parser and create parse tree
//  Parameters : 1. TagList pointer
//               2. Input process command-line
//  Result     : Success?
// ----------------------------------------------------------------------

int runparser ( TagList *tl, char *datafilename )
{
   char buf[16384];
   
   XML_Parser parser = XML_ParserCreate (NULL);
   
   int done;
   TagStack ts;
   ts.Push (tl);
     
   XML_SetUserData (parser, &ts);
   XML_SetElementHandler (parser, startElement, endElement);
   XML_SetCharacterDataHandler (parser, cdata);
   
   FILE *afile = fopen (datafilename, "rb");
   do
   {
      size_t len = fread (buf, 1, sizeof (buf), afile);
      done = len < sizeof (buf);
      if (!XML_Parse (parser, buf, len, done))
      {
         cout << "XML format error in line " << XML_GetCurrentLineNumber (parser);
         cout << " [" << XML_ErrorString (XML_GetErrorCode (parser)) << "]<p>";
         buf[len-1]=0;
         cout << buf;
         
         XML_ParserFree (parser);
         fclose (afile);
         return 0;
      }
   } while (!done);

   XML_ParserFree (parser);
   fclose (afile);
   return 1;
}
