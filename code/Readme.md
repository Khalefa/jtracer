


Record and output each steps in the executed programs




# Build

        mvn clean package

# Usage

In the main directory, to generate some java classes for test.


                javac  -g examples/JDIExampleDebuggee.java 
                javac  -g examples/Debuggee.java

To run, go to the debugee class directory (i.e.  examples directory to test the examples)  

Then, run the following comand.

                
        java -cp ../target/jtracer-1.0-SNAPSHOT-jar-with-dependencies.jar:../lib/jdk-1.8.0.jar  edu.suny.jdi.JDebugger Test


More easily, you can copy the class file to a the root directory, then you can run
    
    mvn exec:java -Dexec.mainClass="edu.suny.jdi.JDebugger" -Dexec.args="Test"


Note: Always make sure the class file is in the current directory and it has been compiled with 
-g





 

# How to Inspect Stack Variables:

Stack variables, and specifically their values at your breakpoint, are associated with the JDI StackFrame and Frame classes. They are referred to as LocalVariables in JDI, you get reach them through the BreakpointEvent, by accessing its ThreadReference, then the StackFrame at the top level (that is, element #0) of the stack. You can then query the visible variables on the StackFrame by name to find the target variable. In other words, you could do something like this:

 StackFrame stackFrame = breakpointEvt.thread().frame(0);
 LocalVariable localVar = stackFrame.visibleVariableByName(varName);
 Value val = stackFrame.getValue(localVar);

# Working with classes

- https://stackoverflow.com/questions/59010599/jdi-how-to-get-the-objectreference-value
- https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jdi/com/sun/jdi/ObjectReference.html
- https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jdi/com/sun/jdi/LocalVariable.html


# Tasks 

- Add maven (done)
- Tools.jar has been migrated to a module in java 11 Module jdk.jdi (as far as i know)
- add sqlite (or duckdb) or http://jsondb.io/
-run rest api 
-Java allows debugging a running program and connect to it. Check jhsdb https://static.rainfocus.com/oracle/oow16/sess/14627958356770011JJj/ppt/JavaOne2016_CON3733.pdf
-WatchpointRequest (I think this may be more efficient to monitor fields)
-Test with lambda, multiple threads and asynchronous functions (callbacks)

