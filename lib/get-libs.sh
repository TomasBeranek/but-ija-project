#!/bin/sh

# the script is invoked from the root of the project
cd lib

# download a JSON simple library
wget http://www.java2s.com/Code/JarDownload/json-simple/json-simple-1.1.jar.zip
unzip json-simple-1.1.jar.zip
rm json-simple-1.1.jar.zip
