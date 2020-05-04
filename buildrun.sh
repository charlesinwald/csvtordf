#!/bin/bash

#######################################
#                                     #
# Script to clean, build, and         #
# run CsvToRdf application            #
# in linux environment.               #
#                                     #
# NOTE: To build complete JAR         #
# standalone file, use Ant:           #
#  $ ant fatjar                       #
# The build.xml file and manifest     #
# is setup to build a standlone JAR   #
# and place it in builds/             #
#                                     #
#######################################
package="csvtordf"
exe="${package}.main.CsvWizard"

# Need to be in directory
wdir="$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)"
cd $wdir
srcdir="src"
libdir="lib"

if [[ "$OSTYPE" == "linux-gnu" ]]; then
  export CLASSPATH="${libdir}/*:${srcdir}"
else
  export CLASSPATH="${libdir}/*;${srcdir}"
fi

echo "CLASSPATH=${CLASSPATH}"

# clean
echo "Cleaning ${srcdir}/${package}"
rm -rf ${srcdir}/${package}/*/*.class

# build
echo "Building ${package}/*.java"
shopt -s extglob
javac ${srcdir}/${package}/main/!(CsvPlugin).java
res=$?
if [ ${res} -ne 0 ]; then
    exit ${res}
fi

# test
echo "Build Complete! Testing..."
echo "---------------------------------------------"
javac ${srcdir}/${package}/test/*.java
java -jar lib/junit/junit-platform-console-standalone-1.6.1.jar --classpath ${srcdir}/${package}/test/ --scan-classpath
res=$?
if [ ${res} -ne 0 ]; then
    exit ${res}
fi

# run
echo "Testing Complete! Running..."
echo "---------------------------------------------"
java -Dprism.order=sw ${exe}
res=$?

exit ${res}
