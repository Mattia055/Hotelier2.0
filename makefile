JSON      	= lib/gson.jar
LIB    		= lib
CONF      	= config
META      	= META-INF
SERVER_META = $(META)/SERVER.MF
CLIENT_META = $(META)/CLIENT.MF
CLASS     	= bin
SERVER_SRC 	= src/serverUtil
CLIENT_SRC 	= src/clientUtil

JAVAC     = javac
JFLAGS    = -Werror -g
JAR       = jar
JAR_FLAGS = -cfm

SERVER_JAR = Server.jar
CLIENT_JAR = Client.jar

LIB_DEPS = packet_lib struct_lib etc_lib

.PHONY: lib

lib:
	@mkdir -p $(CLASS)
	@$(JAVAC) $(JFLAGS) -d $(CLASS) $(LIB)/etc/*.java
	@$(JAVAC) $(JFLAGS) -d $(CLASS) $(LIB)/packet/*.java
	@$(JAVAC) $(JFLAGS) -d $(CLASS) $(LIB)/struct/*.java
	@$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON):$(CLASS) $(LIB)/api/*.java

server: lib
	mkdir -p $(CLASS)
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON):$(CLASS) $(SERVER_SRC)/*.java
	$(JAR) $(JAR_FLAGS) $(SERVER_JAR) $(SERVER_META) -C $(CLASS) serverUtil -C $(CLASS) $(LIB)  $(JSON) -C $(CONF) server.properties

extract: server
	@rm -r extract
	@mkdir -p extract
	@mv $(SERVER_JAR) extract
	@cd extract;
	$(JAR) -xf $(SERVER_JAR)
	@cd ..

runServer: 
	@java -jar $(SERVER_JAR) || true