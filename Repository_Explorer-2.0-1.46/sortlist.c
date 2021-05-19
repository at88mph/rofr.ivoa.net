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

// #include <iostream.h>
#include <iostream>

#include <stdlib.h>
#include <strings.h>

#include "sortlist.h"

// add pnh
using namespace std;
#include <string.h>

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : SortNode
// ----------------------------------------------------------------------
//  Purpose    : Copy values into node
//  Parameters : 1. Name string
//               2. URL string
//               3. website string
//  Result     : (none)
// ----------------------------------------------------------------------

SortNode::SortNode ( char *aname, char *aurl, char *asite )
{
   name = (char *)malloc(strlen (aname)+1);
   url = (char *)malloc(strlen (aurl)+1);
   site = (char *)malloc(strlen (asite)+1);
   strcpy (name, aname);
   strcpy (url, aurl);
   strcpy (site, asite);
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : SortNode
// ----------------------------------------------------------------------
//  Purpose    : Free memory
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

SortNode::~SortNode ()
{
   free (name);
   free (url);
   free (site);
}

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : SortList
// ----------------------------------------------------------------------
//  Purpose    : Initialize linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

SortList::SortList ()
{
   head = NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : SortList
// ----------------------------------------------------------------------
//  Purpose    : Destroy linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

SortList::~SortList ()
{
   while (head != NULL)
   {
      SortNode *p = head;
      head = head->next;
      delete p;
   }
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : SortList
// ----------------------------------------------------------------------
//  Purpose    : Create and add new node to linked list
//  Parameters : 1. Name string
//               2. URL string
//               3. website string
//  Result     : (none)
// ----------------------------------------------------------------------

void SortList::Add ( char *aname, char *aurl, char *asite )
{
   SortNode *p = new SortNode (aname, aurl, asite);
   if ((head == NULL) || (strcasecmp (p->name, head->name) < 0))
   {
      p->next = head;
      head = p;
   }
   else
   {
      SortNode *q = head;
      while ((q->next != NULL) && (strcasecmp (p->name, q->next->name) > 0))
         q = q->next;
      p->next = q->next;
      q->next = p;
   }
}
