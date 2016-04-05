# aXent
Java agent for eXist-db testing

To run java client and set mini HTTP server port to 19999 (default 12251) so that you can access the information from your browser, do the followind and open http://localhost:19999/

java -javaagent:path/to/broker-leak-detector-1.0-jar-with-dependencies.jar=http=19999 ...your usual Java args follows...
