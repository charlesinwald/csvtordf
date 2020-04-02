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
junit="junit"

srcdir=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)

export CLASSPATH="${srcdir}/${jenaloc}/lib/*:${srcdir}/${junit}/*:${srcdir}"
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
javac ${srcdir}/${package}/main/*.java
res=$?
if [ ${res} -ne 0 ]; then
    exit ${res}
fi

# run
echo "Build Complete! Testing..."
echo "---------------------------------------------"
javac ${srcdir}/${package}/test/*.java
java -jar ${srcdir}/${junit}/junit-platform-console-standalone-1.6.1.jar --classpath ${srcdir}/${package}/test/ --scan-classpath
res=$?
if [ ${res} -ne 0 ]; then
    exit ${res}
fi

echo "Testing Complete! Running..."
echo "---------------------------------------------"

java ${package}.main.${exe} ${appargs}
res=$?

exit ${res}
