JSON      	= lib/etc/gson.jar
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

SERVER_DEPENDENCIES = -C $(CLASS) serverUtil -C $(CLASS) $(LIB)/etc -C $(CLASS) $(LIB)/struct -C $(CLASS) $(LIB)/packet -C $(CONF) server.properties
EXTRACT_DIR = ./extract

DELIMITER = ==========================

.PHONY: clean

all: $(SERVER_JAR)

	

lib: lib/**/*.java lib/**/*.jar
	@echo Compiling lib ... && echo $(DELIMITER)
	mkdir -p $(CLASS)
	$(JAVAC) $(JFLAGS) -d $(CLASS) $(LIB)/etc/*.java
	$(JAVAC) $(JFLAGS) -d $(CLASS) $(LIB)/packet/*.java
	$(JAVAC) $(JFLAGS) -d $(CLASS) $(LIB)/struct/*.java
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON):$(CLASS) $(LIB)/api/*.java
	@echo $(DELIMITER)

#controllo sull'eventuale modifica dei file
$(SERVER_JAR): lib
	@echo Compiling Server.jar ... && echo $(DELIMITER)
	mkdir -p $(CLASS)
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON):$(CLASS) $(SERVER_SRC)/*.java
	$(JAR) $(JAR_FLAGS) $(SERVER_JAR) $(SERVER_META) $(SERVER_DEPENDENCIES)
	@echo $(DELIMITER)

extract: $(SERVER_JAR)
	@rm -r $(EXTRACT_DIR) && mkdir -p $(EXTRACT_DIR) && cp $(SERVER_JAR) $(EXTRACT_DIR)
	@cd $(EXTRACT_DIR) && $(JAR) -xf $(SERVER_JAR) && rm $(SERVER_JAR)


runServer: $(SERVER_JAR)
	@java -jar $(SERVER_JAR) || true

clean: 
	rm -rf $(CLASS) extract