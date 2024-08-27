LIB    		= lib
JSON      	= $(LIB)/etc/gson.jar
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

#wildcards for java files

LIB_SOURCES 		= $(wildcard $(LIB)/**/*.java)
SERVLIB_SOURCES 	= $(filter-out $(LIB)/api%, $(LIB_SOURCES))
SERVUTIL_SOURCES 	= $(wildcard $(SERVER_SRC)/*.java)

SERVLIB_CLASSES 	= $(patsubst %.java, $(CLASS)/%.class,$(SERVLIB_SOURCES))
SERVUTIL_CLASSES 	= $(patsubst $(SERVER_SRC)/%.java, $(CLASS)/serverUtil/%.class, $(SERVUTIL_SOURCES))

all: $(SERVER_JAR)
	

test:
	@echo $(SERVLIB_SOURCES)
	@echo
	@echo $(LIB_SOURCES)
	@echo
	@echo $(SERVLIB_CLASSES)
	@echo
	@echo $(SERVUTIL_SOURCES)
	@echo
	@echo $(SERVUTIL_CLASSES)

$(SERVLIB_CLASSES): $(SERVLIB_SOURCES) 
	@echo "╔════════════════════════╗"
	@echo "║ Compiling libraries... ║"
	@echo "╚════════════════════════╝"
	mkdir -p $(CLASS)
	$(JAVAC) $(JFLAGS) -d $(CLASS) $(SERVLIB_SOURCES)

# Rule to compile server utility code
$(SERVUTIL_CLASSES): $(SERVUTIL_SOURCES) $(SERVLIB_CLASSES)
	@echo "╔══════════════════════════╗"
	@echo "║ Compiling Server Core... ║"
	@echo "╚══════════════════════════╝"
	$(JAVAC) $(JFLAGS) -d $(CLASS) -cp $(JSON):$(CLASS) $(SERVER_SRC)/*.java


$(SERVER_JAR): $(SERVUTIL_CLASSES) $(SERVLIB_CLASSES)
	@echo "╔══════════════════════════╗"
	@echo "║ Building Server.jar ...  ║"
	@echo "╚══════════════════════════╝"
	$(JAR) $(JAR_FLAGS) $(SERVER_JAR) $(SERVER_META) $(SERVER_DEPENDENCIES)

extract: $(SERVER_JAR)
	@rm -r $(EXTRACT_DIR) && mkdir -p $(EXTRACT_DIR) && cp $(SERVER_JAR) $(EXTRACT_DIR)
	@cd $(EXTRACT_DIR) && $(JAR) -xf $(SERVER_JAR) && rm $(SERVER_JAR)


runServer: $(SERVER_JAR)
	@echo "╔════════════════════════╗"
	@echo "║ Running Server.jar ... ║"
	@echo "╚════════════════════════╝"
	@java -jar $(SERVER_JAR) || true

clean: 
	rm -rf $(CLASS) extract