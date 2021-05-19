//  ----------------------------------------------------------------------
// | Open Archives Initiative Repository Explorer - version 1.0-1.1       |
// | Hussein Suleman                                                      |
// | April 2001                                                           |
//  ----------------------------------------------------------------------
// |  Virginia Polytechnic Institute and State University                 |
// |  Department of Computer Science                                      |
// |  Digital Library Research Laboratory                                 |
//  ----------------------------------------------------------------------

// ======================================================================
//  Module     : replace
//  Purpose    : String replacement finite state machine
// ======================================================================

#ifndef _REPLACE_
#define _REPLACE_

// ======================================================================
//  Class      : TransitionNode
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Single node of linked list
// ======================================================================

class TransitionNode
{
public:
   char key;
   int value;
   TransitionNode *next;
   TransitionNode (  char akey, int avalue )
   {
      key = akey;
      value = avalue;
   }
};

// ======================================================================
//  Class      : TransitionList
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Linked list of transitions
// ======================================================================

class TransitionList
{
public:
   TransitionNode *head;
   TransitionList ();
   ~TransitionList ();
   void Add ( TransitionNode *p ); 
   int Search ( char akey );
};

// ======================================================================
//  Class      : FSMNode
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Single node of linked list
// ======================================================================

class FSMNode
{
public:
   int key;
   TransitionList *Transition;
   FSMNode *next;
   FSMNode ( int akey )
   {
      key = akey;
      Transition = new TransitionList ();
   }
   ~FSMNode ()
   {
      delete Transition;
   }
};

// ======================================================================
//  Class      : FSMList
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Linked list of FSMNodes
// ======================================================================

class FSMList
{
public:
   FSMNode *head;
   FSMList ();
   ~FSMList ();
   TransitionList * Add ( int i );
   TransitionList * Search ( int akey );
};

// ======================================================================
//  Class      : ReplaceNode
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Single node of linked list
// ======================================================================

class ReplaceNode
{
public:
   int key;
   char *value;
   ReplaceNode *next;
   ReplaceNode ( int akey, char *avalue )
   {
      key = akey;
      value = (char *)malloc(strlen (avalue)+1);
      strcpy (value, avalue);
   }
   ~ReplaceNode ()
   {
      free (value);
   }
};

// ======================================================================
//  Class      : ReplaceList
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Linked list of replacement strings
// ======================================================================

class ReplaceList
{
public:
   int numberofstrings;
   ReplaceNode *head;
   ReplaceList ();
   ~ReplaceList ();
   int Add ( char *s );
   char *Search ( int akey );
};

// ======================================================================
//  Class      : SRFSM
// ======================================================================
//  Base class : (none)
// ======================================================================
//  Purpose    : Finite State Machine for string replacement
// ======================================================================

class SRFSM
{
public:
   FSMList *M;
   ReplaceList *R;
   int numberofstates;
   int maxsourcelength;
   SRFSM ();
   ~SRFSM ();
   void Add ( char *source, char *dest );
   void SearchAndReplace ( char *infilename, char *outfilename );
};

#endif 
