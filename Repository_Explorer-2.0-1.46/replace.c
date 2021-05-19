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

//#include <iostream.h>
#include <iostream>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "replace.h"

#define START_STATE 1000

// #define FSMDEBUG

/*
   Transitions:
      0..999 = replacement string n
      1000 = pop one and return to start
      1001.. = move to new state
*/

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : TransitionList
// ----------------------------------------------------------------------
//  Purpose    : Initialize linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

TransitionList::TransitionList ()
{
   head = NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : TransitionList
// ----------------------------------------------------------------------
//  Purpose    : Destroy linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

TransitionList::~TransitionList ()
{
   while (head != NULL)
   {
      TransitionNode *p = head;
      head = head->next;
      delete p;
   }
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : TransitionList
// ----------------------------------------------------------------------
//  Purpose    : Add new node to linked list
//  Parameters : Node
//  Result     : (none)
// ----------------------------------------------------------------------

void TransitionList::Add ( TransitionNode *p )
{
   p->next = head;
   head = p;
}

// ----------------------------------------------------------------------
//  Method     : Search
// ----------------------------------------------------------------------
//  Class      : TransitionList
// ----------------------------------------------------------------------
//  Purpose    : Search linked list for value corresponding to key
//  Parameters : Key string
//  Result     : Value string
// ----------------------------------------------------------------------

int TransitionList::Search ( char akey )
{
   TransitionNode *p = head;
   while (p != NULL)
   {
      if (akey == p->key)
         return p->value;
      p = p->next;
   }
   return START_STATE;
}

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : FSMList
// ----------------------------------------------------------------------
//  Purpose    : Initialize linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

FSMList::FSMList ()
{
   head = NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : FSMList
// ----------------------------------------------------------------------
//  Purpose    : Destroy linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

FSMList::~FSMList ()
{
   while (head != NULL)
   {
      FSMNode *p = head;
      head = head->next;
      delete p;
   }
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : FSMList
// ----------------------------------------------------------------------
//  Purpose    : Add new node to linked list
//  Parameters : Node
//  Result     : Transition list
// ----------------------------------------------------------------------

TransitionList * FSMList::Add ( int i )
{
   FSMNode *p = new FSMNode (i);
   p->next = head;
   head = p;
   return p->Transition;
}

// ----------------------------------------------------------------------
//  Method     : Search
// ----------------------------------------------------------------------
//  Class      : FSMList
// ----------------------------------------------------------------------
//  Purpose    : Search linked list for value corresponding to key
//  Parameters : Key string
//  Result     : Value string
// ----------------------------------------------------------------------

TransitionList * FSMList::Search ( int akey )
{
   FSMNode *p = head;
   while (p != NULL)
   {
      if (akey == p->key)
         return p->Transition;
      p = p->next;
   }
   return NULL;
}

// ----------------------------------------------------------------------
//  Method     : (Constructor)
// ----------------------------------------------------------------------
//  Class      : ReplaceList
// ----------------------------------------------------------------------
//  Purpose    : Initialize linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

ReplaceList::ReplaceList ()
{
   head = NULL;
   numberofstrings = 0;
}

// ----------------------------------------------------------------------
//  Method     : (Destructor)
// ----------------------------------------------------------------------
//  Class      : ReplaceList
// ----------------------------------------------------------------------
//  Purpose    : Destroy linked list
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

ReplaceList::~ReplaceList ()
{
   while (head != NULL)
   {
      ReplaceNode *p = head;
      head = head->next;
      delete p;
   }
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : ReplaceList
// ----------------------------------------------------------------------
//  Purpose    : Add new node to linked list
//  Parameters : string to add
//  Result     : index of string just added
// ----------------------------------------------------------------------

int ReplaceList::Add ( char *s )
{
   numberofstrings++;
   ReplaceNode *p = new ReplaceNode (numberofstrings, s);
   p->next = head;
   head = p;
   return numberofstrings;
}

// ----------------------------------------------------------------------
//  Method     : Search
// ----------------------------------------------------------------------
//  Class      : ReplaceList
// ----------------------------------------------------------------------
//  Purpose    : Search linked list for value corresponding to key
//  Parameters : Key string
//  Result     : Value string
// ----------------------------------------------------------------------

char *ReplaceList::Search ( int akey )
{
   ReplaceNode *p = head;
   while (p != NULL)
   {
      if (akey == p->key)
         return p->value;
      p = p->next;
   }
   return NULL;
}

// ----------------------------------------------------------------------
//  Method     : (constructor)
// ----------------------------------------------------------------------
//  Class      : SRFSM
// ----------------------------------------------------------------------
//  Purpose    : initiatize data structures
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

SRFSM::SRFSM ()
{
   M = new FSMList ();
   numberofstates = START_STATE;
   TransitionList *t = M->Add (numberofstates);
   R = new ReplaceList ();
   maxsourcelength = 0;
}

// ----------------------------------------------------------------------
//  Method     : (destructor)
// ----------------------------------------------------------------------
//  Class      : SRFSM
// ----------------------------------------------------------------------
//  Purpose    : destroy data structures
//  Parameters : (none)
//  Result     : (none)
// ----------------------------------------------------------------------

SRFSM::~SRFSM ()
{
   delete M;
   delete R;
}

// ----------------------------------------------------------------------
//  Method     : Add
// ----------------------------------------------------------------------
//  Class      : SRFSM
// ----------------------------------------------------------------------
//  Purpose    : Add a string and its replacement to the FSM
//  Parameters : 1. source string
//               2. replacement string
//  Result     : (none)
// ----------------------------------------------------------------------

void SRFSM::Add ( char *source, char *dest )
{
   int i = 0;
   int currentstate = START_STATE;
   while (i<(strlen (source)-1))
   {
      TransitionList *t = M->Search (currentstate);
      int nextstate = t->Search (source[i]);
      if (nextstate == START_STATE)
      {
         numberofstates++;
         currentstate = numberofstates;
         M->Add (numberofstates);
         t->Add (new TransitionNode (source[i], currentstate));
      }
      else
         currentstate = nextstate;
      i++;
   }
   M->Search (currentstate)->Add (new TransitionNode (source[i], R->Add (dest)));

   if (strlen (source) > maxsourcelength)
      maxsourcelength = strlen (source);
}

// ----------------------------------------------------------------------
//  Method     : SearchAndReplace
// ----------------------------------------------------------------------
//  Class      : SRFSM
// ----------------------------------------------------------------------
//  Purpose    : Search through a file and replace all relevant strings 
//  Parameters : 1. source file
//               2. destination file
//  Result     : (none)
// ----------------------------------------------------------------------

void SRFSM::SearchAndReplace ( char *infilename, char *outfilename )
{
   char *buffer = (char *)malloc(maxsourcelength);
   int bufferp = 0;
   int fsmp = 0;
   int currentstate = START_STATE;

#ifdef FSMDEBUG
   // print out data structure
   FSMNode *a = f->M->head;
   while (a!=NULL)
   {
      cout << a->key << " :  ";
      TransitionNode *b = a->Transition->head;
      while (b!=NULL)
      {
         cout << b->key << "/" << b->value << "  ";
         b = b->next;
      }
      cout << "\n";
      a = a->next;
   }
#endif

   FILE *infile = fopen (infilename, "r");
   char achar;
   FILE *outfile = fopen (outfilename, "w");
   
   while ((!feof (infile)) || (bufferp > fsmp))
   {
      if (bufferp > fsmp)
         fsmp++;
      else
      {
         buffer[bufferp] = getc (infile);
         if (feof (infile))
            continue;
         bufferp++;
         fsmp++;
      }
      int t = M->Search (currentstate)->Search (buffer[fsmp-1]);
      if (t == START_STATE)
      {
#ifdef FSMDEBUG
         cout << "*shiftbuffer[";
#endif 
         putc (buffer[0], outfile);
#ifdef FSMDEBUG
         cout << "]*";
#endif 
         for ( int i=0; i<bufferp-1; i++ )
            buffer[i] = buffer[i+1];         
         bufferp--;
         fsmp = 0;
         currentstate = START_STATE;
      }
      else if (t < START_STATE)
      {
         bufferp = 0;
         fsmp = 0;
         fputs (R->Search (t), outfile);
         currentstate = START_STATE;
#ifdef FSMDEBUG
         cout << "*replace[" << t << "]*";
#endif 
      }
      else
      {
         currentstate = t;  
#ifdef FSMDEBUG
         cout << "*nextstate=t[" << t << "]*";
#endif 
      }
   }
   
   for ( int j=0; j<bufferp; j++ )
      putc (buffer[j], outfile);
      
   fclose (outfile);
   fclose (infile);
   
   free (buffer);
}

// ------------------------------------------------------------------
// ------------------------------------------------------------------
// ------------------------------------------------------------------

//void main ()
//{
//   SRFSM * f = new SRFSM ();
//   f->Add ("http://www.openarchives.org/OAI/1.0/OAI_Identify.xsd", "http://oai.dlib.vt.edu/OAI/1.0/OAI_Identify.xsd");
//   f->Add ("http://www.openarchives.org/OAI/1.0/OAI_ListSets.xsd", "http://oai.dlib.vt.edu/OAI/1.0/OAI_ListSets.xsd");
//   f->SearchAndReplace ("testfile", "testfile2");
//   delete f;   
//}

