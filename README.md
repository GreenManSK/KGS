# Web document clustering and keyword generating using link mining
The application implemented as a part of my bachelor's thesis.<br>
 Full text: <a href="https://is.muni.cz/th/vv9kb/">https://is.muni.cz/th/vv9kb/</a>

The application is implemented in Java 1.8 and uses maven. Jar file with dependencies can be found in folder executables.

## C++ integration
This application uses C++ integration for Java so it can use <a href="https://nlp.fi.muni.cz/ma/free.html">Majka</a> implemented in C++ optimally.
All C++ code is located in directory majka4j with Cmake for compilation. I also provided already compiled version of this class in file libmajkaj.so.
If this file do not work for you, you need to compile these sources yourself.<br>
 
To use this library you need to specify path for in when running
```
java -jar -Djava.library.path=majka4j
```
Or put this library into Java library path.

## Usage
This application works as command line tool. Whole process of clustering and extracting keywords is split into modules, each using specific arguments.

### Common arguments
| Param | Description |
| ----- | ----------- |
| -h, --help| Print a list of all parameters|
| -helpm <module name> | Print a list of parameters for the specified module. Module name can be downloader, preprocessing, clustering, linkmining, keywords |
| -L, --console-Log | Print logs into standard output |
| -l <file name>, --log <file name> | Save logs into a file |
| -d <dir name>, --dir <dir name> | Specify the directory for saving and retrieving data used by the application |

### Download module
| Param | Description |
| ----- | ----------- |
| -downloader | Run the download module |
| -u <url>, --url <url> | Starting domain for the web crawle |
| -hops <integer> | The maximal number of domain hops. Default value: 0 |
| -depth <integer> | The maximal depth of crawling. Default value: 1 |

### Preprocessing module
| Param | Description |
| ----- | ----------- |
| -preprocessing | Run the preprocessing module |
| -v <integer>, --vocabulary <integer> | Vocabulary size. Default value: 2000 |
| -redundant <double> | Redundant word percentage.Default value: 0.3 |
| -pruning <double> | Pruning rate.  Default value: 0 |

### Clustering module
| Param | Description |
| ----- | ----------- |
| -clustering | Run the clustering module  |
| -alpha <double> | Scaling parameter for word to document distribution. Default value: 1.0 |
| -beta <double> | Scaling parameter for word to topic distribution. Default value: 0.5 |
| -gamma <double> | Scaling parameter for topics. Default value: 1.5 |

### Link mining module
| Param | Description |
| ----- | ----------- |
| -linkmining | Run the link mining module |
| -distance  <string> | Type of distance comparison used in link mining, **mean** or **average**  |


### Keyword extraction module
| Param | Description |
| ----- | ----------- |
| -keywords | Run the keyword extraction module  |
| -w <integer>, --words <integer> | Number of words extracted for each cluster |
| -skiptr | Skip TextRank algorithm and use previously saved results |
