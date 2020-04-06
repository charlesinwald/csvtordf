#!/bin/bash

###################################
#                                 #
# Script to clean, build, and     #
# run CsvToRdf application        #
# in linux environment.           #
#                                 #
# NOTE: Java 9 or later required! #
#                                 #
###################################
package="csvtordf"
exe="CsvToRdf"
jenaloc="apache-jena-3.14.0"
javafxloc="javafx-sdk-11.0.2"
javafxmods="controls"
junit="junit"

srcdir=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)

export CLASSPATH="${srcdir}/${jenaloc}/lib/*:${srcdir}/${junit}/*:${srcdir}"
echo "CLASSPATH=${CLASSPATH}"
MODULEPATH="${srcdir}/${javafxloc}/lib"

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
modules=""
for module in ${javafxmods}; do
    modules="javafx.${module} ${modules}"
done
javac --module-path ${MODULEPATH} --add-modules ${modules} ${srcdir}/${package}/main/*.java
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

#TODO this should eventually be combined into a single graphical app
#Command line
java --module-path ${MODULEPATH} --add-modules ${modules} -Dprism.order=sw ${package}.main.${exe} ${appargs}
#GUI
java --module-path ${MODULEPATH} --add-modules ${modules} -Dprism.order=sw ${package}.main.CsvWizard ${appargs}

res=$?

exit ${res}
