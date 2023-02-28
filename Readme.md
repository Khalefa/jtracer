
- Install Docker.
- run the following command

		docker build -t mvn .

-run the following in the project folder

		docker run   -it --mount type=bind,source=$PWD/code,target=/code mvn /bin/bash
		

-OR-

		docker run   -it --mount type=bind,source=%cd%/code,target=/code mvn /bin/bash
		(for Windows cmd)

		cd code
		mvn clean package

then,

		cd examples
		javac -g Test.java

Finally,

		java -cp ../target/jtracer-1.0-SNAPSHOT-jar-with-dependencies.jar:/usr/lib/jvm/java-8-openjdk-amd64/lib/tools.jar edu.suny.jdi.JDebugger Test
	



