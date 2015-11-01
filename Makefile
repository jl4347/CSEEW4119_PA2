# THe Makefile for the Simple TCP Protocol. Automates the build for the
# program, as well as the clean process.
#
# Author: Jialun Liu
# UNI:    jl4347
# Date:   11/01/2015

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