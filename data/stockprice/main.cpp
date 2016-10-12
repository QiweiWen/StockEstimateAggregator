#include <stdio.h>
#include <map>
#include <set>
#include <string>
#include "cirbuff.h"
#include <string.h>
#include <iostream>
#include <ostream>
#include <fstream>
#include <pthread.h>
#include <sys/types.h>
#include <sched.h>
#include <unistd.h>
#include <errno.h>
#include <iomanip>

#define BUF_SIZE_BYTES 134217728
#define SNL_DONE (0xdeadbeef)
#define NUM_WRITERS (4)

static int stick_this_thread_to_core(int core_id) {
   int num_cores = sysconf(_SC_NPROCESSORS_ONLN);
   if (core_id < 0)
      return EINVAL;
   core_id %= num_cores;
   cpu_set_t cpuset;
   CPU_ZERO(&cpuset);
   CPU_SET(core_id, &cpuset);

   pthread_t current_thread = pthread_self();    
   return pthread_setaffinity_np(current_thread, sizeof(cpu_set_t), &cpuset);
}

typedef struct tuple tuple_t;

struct tuple{
    char cusip[9];
    char date[9];
    double price;
    size_t outstanding;
    friend std::ostream& operator << (std::ostream& o, const tuple_t& tuppie);
};

std::ostream& operator << 
(std::ostream& o, const tuple_t& tuppie)
{
    o << std::string(tuppie.date);
    double mktval = (tuppie.price * (double)tuppie.outstanding);
    o << ",";
	o<< std::setprecision( 6 )<< mktval;
    return o;
}


bool is_done (const tuple_t& t){
    if (t.outstanding == SNL_DONE){
        return true;
    }else
        return false;
}
static cirbuff <tuple_t> tuplebuff(BUF_SIZE_BYTES/sizeof (tuple_t));

#define errprint(...) {fprintf(stderr,__VA_ARGS__);} 

void* readerfunc (void* arg){
    FILE* ifile = (FILE*) arg;
    char* heapbuf = (char*) malloc (sizeof (char)* 500);
    memset (heapbuf,0, 500);
    size_t size = 500;
    tuple_t tuppy;
    memset (tuppy.cusip,0,9);
    memset (tuppy.date,0,9);
    while (getline (&heapbuf, &size, ifile) != -1){ 
        char* token = strtok (heapbuf, ",");
        int fcnt = 0;
        while (token){
            switch (fcnt){
                case 1:{
                    strncpy (tuppy.date, token, 8);
                    break;
                }
                case 2:{
                    strncpy (tuppy.cusip, token, 8);
                    break;
                }
                case 3:{
                    tuppy.price = atof (token);
                    break;
                }
                case 4:{
                    tuppy.outstanding = atoi (token);
                    break;
                }
                default: break;
            }
            ++fcnt; 
            token = strtok (NULL, ",");
        }     
        tuplebuff.put (tuppy);
    }
    tuppy.outstanding = SNL_DONE;
    for (int i = 0; i < NUM_WRITERS; ++i){
        tuplebuff.put (tuppy);
    }
    return NULL;
}


typedef struct tuplecmp{
public:
    bool operator() (const tuple_t& a, const tuple_t& b) const
    {
        return std::string(a.date) < std::string (b.date);
    }
}tuplecmp_t;

sem_t biglock;
std::map <std::string, std::set <tuple_t, tuplecmp>> writebuffer; 

void *writerfunc (void *arg){
   
    stick_this_thread_to_core ((int)(long)arg);
    int doneflag = 0;
    tuple_t tuppy;
    while (!doneflag){
       // printf ("%s\n", tuppy.cusip);
        tuppy = tuplebuff.get (is_done, &doneflag);
        sem_wait (&biglock);
        writebuffer [tuppy.cusip].insert (tuppy);
        sem_post (&biglock);
    }
    return NULL;
}



int main (int argc, char** argv){
    if (argc != 2){
        errprint( "Look, I came here for an argument!\n");
        return 1;
    }
    FILE* ifile = fopen (argv[1], "r");
    if (!ifile){
        errprint( "Well, an argument isn't just an automatic"
                         " gainsay of whatever the other person is saying!\n");
        return 1;
    }
    pthread_t readerthread;
    //reader is io bound and writer is cpu bound
    pthread_t writerthread[NUM_WRITERS];
    pthread_create (&readerthread, NULL, readerfunc, (void*)ifile);

    sem_init (&biglock,0,1);
    for (int i = 0; i < NUM_WRITERS; ++i){
        pthread_create (&writerthread[i], NULL, writerfunc, (void*)(long)i);
    }
    pthread_join (readerthread,NULL);
    for (int i = 0; i < NUM_WRITERS; ++i){
        pthread_join (writerthread[i],NULL);
    }
    sem_destroy (&biglock);

    char fnamebuf [15] = {'\0'};
    std::ofstream twat;
    
    for (auto itr = writebuffer.begin();
              itr!= writebuffer.end();
              ++itr)
    {
        const std::string& str = itr->first;
        const std::set <tuple_t, tuplecmp>& shit = itr->second;
       // std::cout << str <<std::endl;

        sprintf (fnamebuf, "%s.stock", str.c_str());
        twat.open (fnamebuf);
        for (auto itr = shit.begin(); itr != shit.end(); ++itr){
            twat << *itr << std::endl;
        }
        twat.close ();
    }
    errprint( "main thread terminates\n");
    return 0;
}
