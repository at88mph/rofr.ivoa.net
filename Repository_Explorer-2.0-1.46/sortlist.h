//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer                         |
// | Hussein Suleman                                                      |
// | July 2001                                                            |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : sortlist
//  Purpose    : Sorted linked list of values
// ======================================================================

#ifndef _SORTLIST_

// ======================================================================
//  Class      : SortNode
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Single node of linked list
// ======================================================================

class SortNode
{
public:
   char *name, *url, *site;
   SortNode *next;
   SortNode ( char *aname, char *aurl, char *asite );
   ~SortNode ();
};

// ======================================================================
//  Class      : SortList
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Sorted linked list of values
// ======================================================================

class SortList
{
public:
   SortNode *head;
   SortList ();
   ~SortList ();
   void Add ( char *aname, char *aurl, char *asite );
};

#define _SORTLIST_
#endif
