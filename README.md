# Web document clustering and keyword generating using link mining
The application implemented as a part of my bachelor's thesis.<br>
 Full text: <a href="#tood">@todo</a>

The application is implemented in Java 1.8 and uses maven.

## C++ integration
This application uses C++ integration for Java so it can use <a href="https://nlp.fi.muni.cz/ma/free.html">Majka</a> implemented in C++ optimally.
All C++ code is located in directory majka4j with Cmake for compilation. I also provided already compiled version of this class in file libmajkaj.so.
If this file do not work for you, you need to compile these sources yourself.<br>
 
To use this library you need to specify path for in when running
```
java -jar -Djava.library.path=majka4j
```
Or put this library into Java library path.