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
//  Module     : taglist
//  Purpose    : Parse tree of XML tags
// ======================================================================

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "taglist.h"

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : Initialize pointers and set tag type/string
//  Parameters : 1. Tag string
//               2. Tag type
//               3. Attribute list
//  Result     : (none)
// ----------------------------------------------------------------------

TagList::TagList ( char *atag, char atype, AttList *anattr )
{
   head = NULL;
   tail = NULL;
   next = NULL;
   type = atype;
   
   // skip over namespace prefix
   if (atype == TAG)
   {
      int a=0;
      while ((atag[a] != 0) && (atag[a] != ':'))
         a++;
      if (atag[a] == ':')
         atag += (a+1);
   }
      
   tag = (char *)malloc(strlen (atag)+1);
   strcpy (tag, atag);
   attr = anattr;
}

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : Initialize empty tag
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

TagList::TagList ()
{
   head = NULL;
   tail = NULL;
   next = NULL;
   tag = NULL;
   attr = NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : Destroy tree of tags
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

TagList::~TagList ()
{
   while (head != NULL)
   {
      TagList *p = head;
      head = head->next;
      delete p;
   }
   if (tag)
      free (tag);
   if (attr)
      delete attr;
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : Add given tag to tree
//  Parameters : Tag to add
//  Result     : (none)
// ----------------------------------------------------------------------

void TagList::Add ( TagList *tl )
{
   if (head == NULL)
      head = tl;
   else
      tail->next = tl;
   tail = tl;
}

// ----------------------------------------------------------------------
//  Method     : Append
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : Append text string to existing tag
//  Parameters : Text string
//  Result     : (none)
// ----------------------------------------------------------------------

void TagList::Append ( char *s )
{
   char *newtag = (char *)malloc(strlen (tag) + strlen (s) + 1);
   strcpy (newtag, tag);
   strcat (newtag, s);
   free (tag);
   tag = newtag;
}

// ----------------------------------------------------------------------
//  Method     : List
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : List contents of tree
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

int globaldots = 0;

void TagList::List ()
{
   globaldots++;
   TagList *tl = head;
   while (tl != NULL)
   {
      for ( int i=0; i<globaldots; i++ )
         printf (".");
      if (tl->type == TEXT)
         printf ("TEXT .... %s\n", tl->tag);
      else
      {
         printf ("TAG .... %s\n", tl->tag);
         if (tl->attr)
            tl->attr->List ();
      }
      tl -> List ();
      tl = tl->next;
   }
   globaldots--;
}

// ----------------------------------------------------------------------
//  Method     : Search
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : Searches for a tag in a taglist
//  Parameters : Text string
//  Result     : *TagList
// ----------------------------------------------------------------------

TagList *TagList::Search ( char *akey )
{
   TagList *p = head;
   while (p != NULL)
   {
      if (strcmp (akey, p->tag) == 0)
         return p;
      p = p->next;
   }
   return NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : TagStackNode
// ----------------------------------------------------------------------
//  Purpose    : Set value of node
//  Parameters : TagList pointer
//  Result     : (none)
// ----------------------------------------------------------------------

TagStackNode::TagStackNode ( TagList *atl )
{
   tl = atl;
}

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : TagStack
// ----------------------------------------------------------------------
//  Purpose    : Initialize stack
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

TagStack::TagStack ()
{
   head = NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : TagStack
// ----------------------------------------------------------------------
//  Purpose    : Destroy stack
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

TagStack::~TagStack ()
{
   while (head != NULL)
   {
      TagStackNode *p = head;
      head = head->next;
      delete p;
   }
}

// ----------------------------------------------------------------------
//  Method     : Push
// ----------------------------------------------------------------------
//  Class      : TagStack
// ----------------------------------------------------------------------
//  Purpose    : Push Taglist onto stack
//  Parameters : Taglist pointer
//  Result     : (none)
// ----------------------------------------------------------------------

void TagStack::Push ( TagList *tl )
{
   TagStackNode *p = new TagStackNode (tl);
   p->next = head;
   head = p;
}

// ----------------------------------------------------------------------
//  Method     : Pop
// ----------------------------------------------------------------------
//  Class      : TagStack
// ----------------------------------------------------------------------
//  Purpose    : Pops off node at top of stack
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

void TagStack::Pop ()
{
   if (head != NULL)
   {
      TagStackNode *p = head;
      head = head->next;
      delete p;
   }
}

// ----------------------------------------------------------------------
//  Method     : Top
// ----------------------------------------------------------------------
//  Class      : TagStack
// ----------------------------------------------------------------------
//  Purpose    : Returns node at top of stack
//  Parameters : (none)
//  Result     : TagList pointer
// ----------------------------------------------------------------------

TagList *TagStack::Top ()
{
   if (head != NULL)
      return head->tl;
   else
      return NULL;
}

// ----------------------------------------------------------------------
//  Method     : trim
// ----------------------------------------------------------------------
//  Class      : TagList
// ----------------------------------------------------------------------
//  Purpose    : Remove leading and trailing whitespace
//  Parameters : String
//  Result     : (none)
// ----------------------------------------------------------------------

void TagList::Trim ()
{
   int numfront = 0;
   int pos = strlen (tag);
   while ((numfront < pos) && 
          ((tag[numfront] == '\n') || (tag[numfront] == '\r') || (tag[numfront] == ' ') || (tag[numfront] == '\t')))
      numfront++;
   for ( int i=0; i<(pos-numfront); i++ )
      tag[i] = tag[i+numfront];
   tag[strlen (tag) - numfront] = 0;
   pos = strlen (tag);
   while ((pos > 0) && 
          ((tag[pos-1] == '\n') || (tag[pos-1] == '\r') || (tag[pos-1] == ' ') || (tag[pos-1] == '\t')))
      pos--;
   tag[pos] = 0;
}
