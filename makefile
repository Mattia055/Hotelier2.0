LIB    		= lib
JSON      	= $(LIB)/gson.jar
JLINE	  	= $(LIB)/jline.jar
JANSI	  	= $(LIB)/jansi.jar
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

SERVER_DEPENDENCIES = -C $(CLASS) serverUtil -C $(CLASS) $(LIB)/share -C $(CLASS) $(LIB)/server -C $(CONF) server.properties
CLIENT_DEPENDENCIES = -C $(CLASS) clientUtil -C $(CLASS) $(LIB)/share -C $(CLASS) $(LIB)/client -C $(CONF) client.properties
EXTRACT_DIR = ./extract

#wildcards for java files

LIB_SOURCES 		= $(wildcard $(LIB)/**/**/*.java)
SERVLIB_SOURCES 	= $(filter $(LIB)/server%, $(LIB_SOURCES))
SERVUTIL_SOURCES 	= $(wildcard $(SERVER_SRC)/*.java)

CLIENTLIB_SOURCES 	= $(filter $(LIB)/client%, $(LIB_SOURCES))
CLIENTLIB_CLASSES 	= $(patsubst %.java, $(CLASS)/%.class, $(CLIENTLIB_SOURCES))

SHARELIB_SOURCES 	= $(filter $(LIB)/share%, $(LIB_SOURCES))
SHARELIB_CLASSES 	= $(patsubst %.java, $(CLASS)/%.class, $(SHARELIB_SOURCES))

SERVLIB_CLASSES 	= $(patsubst %.java, $(CLASS)/%.class,$(SERVLIB_SOURCES))
SERVUTIL_CLASSES 	= $(patsubst $(SERVER_SRC)/%.java, $(CLASS)/serverUtil/%.class, $(SERVUTIL_SOURCES))

CLIENT_SOURCES 		= $(wildcard $(CLIENT_SRC)/*.java)
CLIENT_CLASSES 		= $(patsubst $(CLIENT_SRC)/%.java, $(CLASS)/clientUtil/%.class, $(CLIENT_SOURCES))

#colors

COLOR = "\033[4m\033[36m"
RESET = "\033[0m"

all: $(SERVER_JAR) $(CLIENT_JAR)

$(SHARELIB_CLASSES): $(SHARELIB_SOURCES)
	@echo -e $(COLOR)Compiling Shared Libs ...$(RESET)
	@mkdir -p $(CLASS)
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON) $(SHARELIB_SOURCES)	

$(CLIENTLIB_CLASSES): $(CLIENTLIB_SOURCES)  $(SHARELIB_CLASSES)
	@echo -e $(COLOR)Compiling Client Libs ...$(RESET)
	@mkdir -p $(CLASS)
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON):$(CLASS):$(JLINE):$(JANSI) $(CLIENTLIB_SOURCES)

$(CLIENT_CLASSES): $(CLIENT_SOURCES) $(CLIENTLIB_CLASSES) $(SHARELIB_CLASSES)
	@echo -e $(COLOR)Compiling Client Core ...$(RESET)
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON):$(CLASS):$(JLINE):$(JANSI) $(CLIENT_SRC)/*.java

$(CLIENT_JAR): $(CLIENT_CLASSES) $(CLIENTLIB_CLASSES) $(SHARELIB_CLASSES) $(CONF)/client.properties
	@echo -e $(COLOR)Compiling Client.jar ...$(RESET)
	$(JAR) $(JAR_FLAGS) $(CLIENT_JAR) $(CLIENT_META) $(CLIENT_DEPENDENCIES)


# SERVER COMPILATION

$(SERVLIB_CLASSES): $(SERVLIB_SOURCES) $(SHARELIB_CLASSES)
	@echo -e $(COLOR)Compiling Server Libs ...$(RESET)
	@mkdir -p $(CLASS)
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON) $(SERVLIB_SOURCES)

# Rule to compile server utility code
$(SERVUTIL_CLASSES): $(SERVUTIL_SOURCES) $(SERVLIB_CLASSES) $(SHARELIB_CLASSES)
	@echo -e $(COLOR)Compiling Server Core ...$(RESET)
	$(JAVAC) $(JFLAGS) -cp $(JSON):$(CLASS) -d $(CLASS) $(SERVER_SRC)/*.java


$(SERVER_JAR): $(SERVUTIL_CLASSES) $(SERVLIB_CLASSES) $(SHARELIB_CLASSES) $(CONF)/server.properties
	@echo -e $(COLOR)Building Server.jar ...$(RESET)
	$(JAR) $(JAR_FLAGS) $(SERVER_JAR) $(SERVER_META) $(SERVER_DEPENDENCIES)

# Custom method for debugging and execution
extractServer: $(SERVER_JAR) 
	@mkdir -p $(EXTRACT_DIR) && rm -rf $(EXTRACT_DIR)/** && cp $(SERVER_JAR) $(EXTRACT_DIR)
	@cd $(EXTRACT_DIR) && $(JAR) -xf $(SERVER_JAR) && rm $(SERVER_JAR)

extractClient: $(CLIENT_JAR)
	@mkdir -p $(EXTRACT_DIR) && rm -rf $(EXTRACT_DIR)/** && cp $(CLIENT_JAR) $(EXTRACT_DIR)
	@cd $(EXTRACT_DIR) && $(JAR) -xf $(CLIENT_JAR) && rm $(CLIENT_JAR)


runServer: $(SERVER_JAR)
	@clear
	@echo -e $(COLOR)Running Server.jar$(RESET)
	@java -jar $(SERVER_JAR) || true

runClient: $(CLIENT_JAR)
	@clear
	@echo -e $(COLOR)Running Client.jar$(RESET)
	@java -jar $(CLIENT_JAR) || true

clear: 
	rm -rf $(CLASS) $(EXTRACT_DIR ) $(SERVER_JAR) $(CLIENT_JAR) data/users.json data/reviews.json data/*tmp data/**/*tmp
	clear

runTest: $(SERVER_JAR) $(CLIENT_JAR)
	@gnome-terminal  -- bash -c "make runServer; exec bash"
	@sleep 0.5 
	@gnome-terminal  -- bash -c "make runClient; exec bash"
