# latte (Locals Table Enrichment)
`latte` is a CLI utility which parses the bytecode of a stripped Java class file and attempts to re-construct a reasonble fascimile of each missing [Local Variable Table](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.13).


## Background
`javac` can optionally include debugging info - including Local Variables Tables - in the compiled Java `.class` file.

Consider the following - extremely contrived - sample Java program to print the contents of a text file.
```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PrintFile {
    public static void main(String args[]) {
	Path filePath = null;
        if (args.length != 1) {
            System.err.println("Must specify a file");
        }
	else {
	    filePath = Paths.get(args[0]);
	    try {
		System.out.println(Files.readString(filePath));
	    }
	    catch (IOException e) {
		String errMsg = String.format("Failed to read file %s: %s", args[0], e.toString());
		System.err.println(errMsg);
	    }
	}

    }
}
```

When compiling, the `-g` option can be specified to `javac` to output debugging info, like so:
```bash
$ javac -g PrintFile.java
$ javap -l PrintFile.class
Compiled from "PrintFile.java"
public class PrintFile {
  public PrintFile();
    LineNumberTable:
      line 6: 0
    LocalVariableTable:
      Start  Length  Slot  Name   Signature
          0       5     0  this   LPrintFile;

  public static void main(java.lang.String[]);
    LineNumberTable:
      line 8: 0
      line 9: 2
      line 10: 8
      line 13: 19
      line 15: 30
      line 20: 40
      line 17: 43
      line 18: 44
      line 19: 67
      line 23: 74
    LocalVariableTable:
      Start  Length  Slot  Name   Signature
         67       7     3 errMsg   Ljava/lang/String;
         44      30     2     e   Ljava/io/IOException;
          0      75     0  args   [Ljava/lang/String;
          2      73     1 filePath   Ljava/nio/file/Path;
```

The Local Variable Tables facilitate debugging; specifically, they allow inspection of the methods arguments and local variables at each step of the method:

```bash
$ jdb PrintFile /etc/issue
Initializing jdb ...
> stop in PrintFile.main
Deferring breakpoint PrintFile.main.
It will be set after the class is loaded.
> run
run PrintFile /etc/issue
Set uncaught java.lang.Throwable
Set deferred uncaught java.lang.Throwable
> 
VM Started: Set deferred breakpoint PrintFile.main

Breakpoint hit: "thread=main", PrintFile.main(), line=8 bci=0
8    	Path filePath = null;

main[1] locals
Method arguments:
args = instance of java.lang.String[1] (id=436)
Local variables:
main[1] dump args
 args = {
"/etc/issue"
}
```

However, if the debugging information is omitted - without access to the original Java source code - debugging becomes much trickier:
```bash
$ javac -g:none PrintFile.java 
$ javap -l PrintFile.class 
public class PrintFile {
  public PrintFile();

  public static void main(java.lang.String[]);
}
$ jdb PrintFile /etc/issue
Initializing jdb ...
> stop in PrintFile.main
Deferring breakpoint PrintFile.main.
It will be set after the class is loaded.
> run
run PrintFile /etc/issue
Set uncaught java.lang.Throwable
Set deferred uncaught java.lang.Throwable
> 
VM Started: Set deferred breakpoint PrintFile.main

Breakpoint hit: "thread=main", PrintFile.main(), line=-1 bci=0

main[1] locals
Local variable information not available.  Compile with -g to generate variable information
```

## Description
`latte` will attempt to build an approximation of the Local Variables Tables by analyzing the bytecode of a Java `.class` - or `.jar` - file.

```bash
$ java -jar app/build/libs/latte-0.1.0.jar PrintFile.class
Examining class PrintFile
	Examining method <init>
	Examining method main
Overwrite the existing file PrintFile.class? (Y/n)
Y
Overwriting existing file PrintFile.class
$ javap -l PrintFile.class 
public class PrintFile {
  public PrintFile();
    LocalVariableTable:
      Start  Length  Slot  Name   Signature
          0       5     0  this   LPrintFile;

  public static void main(java.lang.String[]);
    LocalVariableTable:
      Start  Length  Slot  Name   Signature
          0      75     0  arg1   [Ljava/lang/String;
          2      73     1 local1   Ljava/nio/file/Path;
         44      31     2 local2   Ljava/io/IOException;
         67       8     3 local3   Ljava/lang/String;
}

```

This allows for inspection of the local variables during debugging.

```bash
$ jdb PrintFile /etc/issue
Initializing jdb ...
> stop in PrintFile.main
Deferring breakpoint PrintFile.main.
It will be set after the class is loaded.
> run
run PrintFile /etc/issue
Set uncaught java.lang.Throwable
Set deferred uncaught java.lang.Throwable
> 
VM Started: Set deferred breakpoint PrintFile.main

Breakpoint hit: "thread=main", PrintFile.main(), line=-1 bci=0

main[1] locals
Method arguments:
arg1 = instance of java.lang.String[1] (id=437)
Local variables:
main[1] dump arg1
 arg1 = {
"/etc/issue"
}

```


## Building
```bash
$ git clone https://github.com/mathewmarcus/latte.git
$ cd latte
$ ./gradlew build
```


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

### Positional Arguments
#### INPUT_CLASS_FILE_OR_JAR
The target file; either a `.class` file (default) or a `.jar` file (see [`--jar`](#jar))

### Options

#### --jar
Specifies that the [target file](#input_class_file_or_jar) is a `.jar`

#### --output
Write the modified class(es)/JAR to a different file; by default the input file will be overwritten

#### --force
Don't prompt before overwriting an existing file.

#### --include
Explicitly limit the classes which `latte` should analyze/modify within a JAR file.

##### Example
```bash
$ java -jar latte.jar -j -i com.example.A com.example.B input.jar
```

#### --class-path
In order to handle local variable typing, `latte` will attempt to resolve and load classes. If the target file has any external dependencies, the location to these can be specified via a [class path string](https://docs.oracle.com/en/java/javase/20/docs/specs/man/java.html#standard-options-for-java).

##### Example
```bash
$ java -jar latte.jar --class-path dependency.jar:/path/to/more/jar/dependencies/* input.class
```


## TODO:
* investigate whether ASM event-based API would be better than object-based API
* code cleanup
* more unit tests