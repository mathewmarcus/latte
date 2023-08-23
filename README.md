# latte (Locals Table Enrichment)
latte is a CLI utility which parses the bytecode of a stripped Java class file and attempts to re-construct a reasonble fascimile of each missing [Local Variable Table](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.13).


## Usage
```
usage: java -jar latte.jar INPUT_CLASS_OR_JAR [-c <arg>] [-f] [-h] [-i <arg>] [-j] [-o
       <arg>]
Rebuild the local variable tables in a class file
 -c,--class-path <arg>   A : separated list of directories, JAR
                         archives,and ZIP archives to search for class
                         files.
 -f,--force              do not prompt before overwriting an existing
                         output file
 -h,--help
 -i,--include <arg>      list of classes to modify (default: all)
 -j,--is-jar             whether the input file is a JAR
 -o,--output <arg>       output file
```


## TODO:
* investigate whether ASM event-based API would be better than object-based API
* code cleanup
* more unit tests