//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer                         |
// | Hussein Suleman                                                      |
// | March 2002                                                           |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Libraries Research Laboratory                               |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : attlist
//  Purpose    : Linked list of attributes
// ======================================================================

//#include <iostream.h>
#include <iostream>
#include <stdlib.h>
#include <strings.h>
#include <string.h>

// added pnh
using namespace std;

#include "attlist.h"
#include "escape.h"

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : ALNode
// ----------------------------------------------------------------------
//  Purpose    : Copy values into node
//  Parameters : 1. Key string
//               2. Value string
//  Result     : (none)
// ----------------------------------------------------------------------

ALNode::ALNode ( char *akey, char *avalue )
{
   key = (char *)malloc(strlen (akey)+1);
   value = (char *)malloc(strlen (avalue)+1);
   strcpy (key, akey);
   strcpy (value, avalue);
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : ALNode
// ----------------------------------------------------------------------
//  Purpose    : Free memory
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

ALNode::~ALNode ()
{
   free (key);
   free (value);
}

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : AttList
// ----------------------------------------------------------------------
//  Purpose    : Initialize linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

AttList::AttList ()
{
   head = NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : AttList
// ----------------------------------------------------------------------
//  Purpose    : Destroy linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

AttList::~AttList ()
{
   while (head != NULL)
   {
      ALNode *p = head;
      head = head->next;
      delete p;
   }
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : AttList
// ----------------------------------------------------------------------
//  Purpose    : Create and add new node to linked list
//  Parameters : 1. Key string
//               2. Value string
//  Result     : (none)
// ----------------------------------------------------------------------

void AttList::Add ( char *akey, char *avalue )
{
   ALNode *p = new ALNode (akey, avalue);
   p->next = head;
   head = p;
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : AttList
// ----------------------------------------------------------------------
//  Purpose    : Add new node to linked list
//  Parameters : Node
//  Result     : (none)
// ----------------------------------------------------------------------

void AttList::Add ( ALNode *p )
{
   p->next = head;
   head = p;
}

// ----------------------------------------------------------------------
//  Method     : Search
// ----------------------------------------------------------------------
//  Class      : AttList
// ----------------------------------------------------------------------
//  Purpose    : Search linked list for value corresponding to key
//  Parameters : Key string
//  Result     : Value string
// ----------------------------------------------------------------------

char *AttList::Search ( char *akey )
{
   ALNode *p = head;
   while (p != NULL)
   {
      if (strcmp (akey, p->key) == 0)
         return p->value;
      p = p->next;
   }
   return NULL;
}

// ----------------------------------------------------------------------
//  Method     : List
// ----------------------------------------------------------------------
//  Class      : AttList
// ----------------------------------------------------------------------
//  Purpose    : List all attributes in list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

void AttList::List ()
{
   ALNode *p = head;
   int first = 1;
   while (p != NULL)
   {
      if ((strstr (p->key, "xmlns") != p->key) &&
          (strcmp (p->key, "xsi:schemaLocation") != 0))
      {
         if (first == 1)
            first = 0;
         else
            cout << ", ";
         cout << p->key << "=";
         Escape (p->value);
      }
      p = p->next;
   }
}

