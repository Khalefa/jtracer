


 javac  -cp /Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home/lib/tools.jar:. edu/suny/jdi/JDebugger.java


 javac  -g examples/JDIExampleDebuggee.java
 javac  -g examples/Debuggee.java

`
java -cp /Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home/lib/tools.jar:. edu.suny.jdi.JDebugger JDIExampleDebuggee
`

The idea is to record and output each steps along the way


# Tasks
- Add maven (done)
- Tools.jar has been migrated to a module in java 11 Module jdk.jdi (as far as i know)
- add sqlite (or duckdb) orhttp://jsondb.io/
-run rest api 
-Java allows debugging a running program and connect to it. Check jhsdb https://static.rainfocus.com/oracle/oow16/sess/14627958356770011JJj/ppt/JavaOne2016_CON3733.pdf
-WatchpointRequest (I think this may be more efficient to monitor fields)
-Test with lambda, multiple threads and asynchronous functions (callbacks)
# Usage

in target folder, make sure to have 


`
java -cp ../target/jracer-1.0-SNAPSHOT.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home/lib/tools.jar  edu.suny.jdi.JDebugger Test
`

`
 java -cp jracer-1.0-SNAPSHOT-jar-with-dependencies.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home/lib/tools.jar  edu.suny.jdi.JDebugger Test &> ../test_tace_1.json
 `

More easily,
`
mvn exec:java -Dexec.mainClass="edu.suny.jdi.JDebugger" -Dexec.args="Test"
`

Always make sure class in the current directory and it has been compiled with 
-g





 

# How to Inspect Stack Variables:

Stack variables, and specifically their values at your breakpoint, are associated with the JDI StackFrame and Frame classes. They are referred to as LocalVariables in JDI, you get reach them through the BreakpointEvent, by accessing its ThreadReference, then the StackFrame at the top level (that is, element #0) of the stack. You can then query the visible variables on the StackFrame by name to find the target variable. In other words, you could do something like this:

 StackFrame stackFrame = breakpointEvt.thread().frame(0);
 LocalVariable localVar = stackFrame.visibleVariableByName(varName);
 Value val = stackFrame.getValue(localVar);

# Working with classes

https://stackoverflow.com/questions/59010599/jdi-how-to-get-the-objectreference-value
https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jdi/com/sun/jdi/ObjectReference.html


https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jdi/com/sun/jdi/LocalVariable.html

