#!/bin/bash

###################################
#                                 #
# Script to clean, build, and     #
# run CsvToRdf application        #
# in linux environment.           #
#                                 #
###################################

# Need to be in directory
wdir="$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)"
if [ "$(pwd)" != "${wdir}" ]; then
    echo "ERROR - must buildplugin.sh execute from ${wdir}"
    exit 2
fi

# Check for environment variables
echo "Checking environment variables"
if [ -z "${PROTEGE_HOME}" ]; then
	echo "ERROR - must set PROTEGE_HOME"
	exit 2
elif [ -z "${ANT_HOME}" ]; then
	echo "ERROR - must set ANT_HOME"
	exit 2
elif [ -z "${JAVA_HOME}" ]; then
	echo "ERROR - must set JAVA_HOME"
	exit 2
elif [[ "${PATH}" != *"jdk"* ]]; then
	echo "ERROR - must include jdk bin directory in PATH"
	exit 2
elif [[ "${PATH}" != *"ant"* ]]; then
	echo "ERROR - must incude ant bin directory in PATH"
	exit 2
fi

# WORKAROUND - get version of jfxrt bundled with Protege JRE
rm -f lib/jfxrt.jar && cp "${PROTEGE_HOME}/jre/lib/ext/jfxrt.jar" lib/jfxrt.jar
if [ $? -ne 0 ]; then
    echo "ERROR - failed copying jfxrt.jar from ${PROTEGE_HOME}/jre/lib/ext/"
    exit 1
fi

# ant build
echo "Running ant install..."
rm -rf build/ && ant install -v
if [ $? -ne 0 ]; then
	exit 1
fi


#Test
ant -lib lib/junit/ant-junitlauncher-1.10.5.jar test


# Run
runfile=$(cd "${PROTEGE_HOME}" && find . -type f -name run*)
echo "Running Protege ${runfile}"

currdir=$(pwd)
cd "${PROTEGE_HOME}" && ${runfile}
res=$?
cd "${currdir}"

exit ${res}

