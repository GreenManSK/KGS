# This Makefile is intended for compilation in MinGW/msys environment.
#
# Warning: on Windows use echo word| majka, not echo word | majka (the input would be "word ")

CXX=g++
LDFLAGS=
CPPFLAGS=-g -O2 --pedantic -Wall -Wextra

all: majka majka.dll

majka.o: majka.cc majka.h
	${CXX} ${CPPFLAGS} -c $< -o $@
majka_bin.o : majka_bin.cc majka.h
	${CXX} ${CPPFLAGS} -c $< -o $@
majka: majka_bin.o majka.o
	${CXX} ${CPPFLAGS} $^ ${LDFLAGS} -s -static -o $@
# majka: majka_bin.cc majka.dll
#	${CXX} ${CPPFLAGS} $^ ${LDFLAGS} -o $@

majka.dll: majka.o
	rm -f $@
	${CXX} -s -static -shared -Wl,-soname,$@ -o $@ $^

clean:
	rm -f majka.o majka_bin.o majka.dll majka

