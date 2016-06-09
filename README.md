# aXent
Java agent for eXist-db testing

To run java client and set mini HTTP server port to 19999 (default 12251) so that you can access the information from your browser, do the followind and open http://localhost:19999/

java -javaagent:path/to/broker-leak-detector-1.0-jar-with-dependencies.jar=http=19999 ...your usual Java args follows...

java -Xbootclasspath/p:path/to/javagent-1.1-jar-with-dependencies.jar -javaagent:path/to/javagent-1.1-jar-with-dependencies.jar  ...your usual Java args follows...

http://localhost:12251/ - show active brokers;

http://localhost:12251/exceptions - show stack of catched exceptions;

http://localhost:12251/reset - zero stack of catched exceptions;
