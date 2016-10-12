CC=g++
CFLAGS= -O2 -Wall -c -std=gnu++11 -O3
LDFLAGS = -lm -lpthread
LD=$(CC)

TARGET=splitter
OBJECTS = $(patsubst %.cpp, %.o, $(wildcard *.cpp))
HEADERS = $(wildcard *.h)

$(TARGET):  $(OBJECTS) $(HEADERS)
	$(LD) -o $@ $< $(LDFLAGS) 

%.o: %.cpp $(HEADERS)
	$(CC) $(CFLAGS) $< -o $@

clean:	
	find . -name "*.o" -o -name "pipetest"|xargs rm -f
	rm -rf stockprices
