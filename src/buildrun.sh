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

# Pass all arguments to java app
appargs="$@"
if [ -z "${appargs}" ]; then
    # default to sample
    appargs="-c sample.csv"
fi

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

java ${package}.${exe} ${appargs}
res=$?

exit ${res}
