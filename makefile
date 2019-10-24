JFLAGS = -g
JCC = javac

default: packet.class client.class server.class

packet.class: packet.java
	$(JCC) $(JFLAGS) packet.java

client.class: client.java
	$(JCC) $(JFLAGS) client.java

server.class: server.java
	$(JCC) $(JFLAGS) server.java

clean: 
	$(RM) packet.class client.class server.class *.txt *.log
