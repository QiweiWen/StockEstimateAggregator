#ifndef CIRBUF_H
#define CIRBUF_H
#include <stdlib.h>
#include <semaphore.h>
#include <sys/sem.h>

/*
 *  thread safe circular buffer
 */
template <typename T>
struct cirbuff{
public:
    typedef bool (*donefunc)(const T&);
    cirbuff(size_t size): _size (size), _num(0),
                          _head (0), _tail(0)
    {
        _storage = (T*) malloc (sizeof (T) * _size);
        sem_init (&_lock,0,1);
        sem_init (&_full,0,0);
        sem_init (&_empty,0,_size);
    }
    ~cirbuff (){
        free (_storage);
        sem_destroy (&_lock);
        sem_destroy (&_full);
        sem_destroy (&_empty);
    }
    void put (const T& stuff);
    T    get (donefunc, int*);
private:
    T* _storage;
    size_t _size, _num, _head, _tail;
    sem_t _lock, _full, _empty; 
};

#include "cirbuff.impl"
#endif
