#!/bin/bash

##################################
#                                #
# Script to clean, build, and    #
# run CsvToRdf application       #
# in linux environment.          #
#                                #
##################################
package="csvtordf"
exe="CsvToRdf"
jenaloc="apache-jena-3.14.0"

srcdir=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)

export CLASSPATH="${srcdir}/${jenaloc}/lib/*:${srcdir}"
echo "CLASSPATH=${CLASSPATH}"

# clean
echo "Cleaning ${srcdir}/${package}"
rm -f ${srcdir}/${package}/*.class

# build
echo "Building ${package}/*.java"
javac ${srcdir}/${package}/*.java
res=$?
if [ ${res} -ne 0 ]; then
    exit ${res}
fi

# run
echo "Build Complete! Executing..."
echo "---------------------------------------------"

java ${package}.${exe}
res=$?

exit ${res}
