#!/bin/bash

###################################
#                                 #
# Script to clean, build, and     #
# run CsvToRdf application        #
# in linux environment.           #
#                                 #
###################################
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

# Create a JAR file too
echo "Creating Jar"
# create Manifest file
mfFile="csvtordf.MF"
echo "Manifest-Version: 1.0" > ${mfFile}
echo "Class-Path: src/ " >> ${mfFile}
for libFile in ${libdir}/*.jar; do
  if [[ "${libFile}" == *"csvtordf"* || "${libFile}" == *"junit"* || "${libFile}" == *"jfxrt"* ]]; then
    continue;
  fi
  echo " ${libFile} " >> ${mfFile}
done
echo " src/" >> ${mfFile}
echo "Main-Class: ${exe}" >> ${mfFile}
echo "" >> ${mfFile}

jar cmf ${mfFile} csvtordf.jar ${srcdir}/${package}/main/* lib/*
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
#java -Dprism.order=sw ${exe}
java -jar csvtordf.jar
res=$?

exit ${res}
