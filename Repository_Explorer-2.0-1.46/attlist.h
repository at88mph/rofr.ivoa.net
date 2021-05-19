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
//  Module     : attlist
//  Purpose    : Linked list of attributes
// ======================================================================

#ifndef _ATTLIST_

// ======================================================================
//  Class      : ALNode
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Single node of linked list
// ======================================================================

class ALNode
{
public:
   char *key, *value;
   ALNode *next;
   ALNode ( char *akey, char *avalue );
   ~ALNode ();
};

// ======================================================================
//  Class      : AttList
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Linked list of attributes
// ======================================================================

class AttList
{
public:
   ALNode *head;
   AttList ();
   ~AttList ();
   void Add ( char *akey, char *avalue );
   void Add ( ALNode *p );
   char *Search ( char *akey );
   void List ();
};

#define _ATTLIST_
#endif
