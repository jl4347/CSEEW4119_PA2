# Compiles all the java files into class files.
.PHONY: compile
compile:
	javac *.java

# Cleans the current directory
.PHONY: clean
clean:
	rm -f *.*~ *.class *~

# 'all' target:
# First cleans the directory then compiles the java file
.PHONY: all
all: clean compile