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

#ifndef _TAGLIST_

#include "attlist.h"

#define TAG 0
#define TEXT 1

// ======================================================================
//  Class      : TagList
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Tree of XML tags
// ======================================================================

class TagList
{
public:
   TagList *head, *tail, *next;
   AttList *attr;
   char *tag;
   char type;
   TagList ( char *atag, char atype, AttList *anattr );
   TagList ();
   ~TagList ();
   void Add ( TagList *tl );
   void Append ( char *s );
   TagList* Search ( char *akey );
   void List ();
   void Trim ();
};

// ======================================================================
//  Class      : TagStackNode
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Single node in stack of tags
// ======================================================================

class TagStackNode
{
public:
   TagList *tl;
   TagStackNode *next;
   TagStackNode ( TagList *tl );
};

// ======================================================================
//  Class      : TagStack
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Stack of tags
// ======================================================================

class TagStack
{
public:
   TagStackNode *head;
   TagStack ();
   ~TagStack ();
   void Push ( TagList *tl );
   void Pop ();
   TagList *Top ();
};

#define _TAGLIST_
#endif
