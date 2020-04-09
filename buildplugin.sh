#!/bin/bash

###################################
#                                 #
# Script to clean, build, and     #
# run CsvToRdf application        #
# in linux environment.           #
#                                 #
###################################

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

# ant build
echo "Running ant install..."
rm -rf build/ && ant install
if [ $? -ne 0 ]; then
	exit 1
fi

# Run
runfile=$(cd "${PROTEGE_HOME}" && find . -type f -name run*)
echo "Running Protege ${runfile}"

currdir=$(pwd)
cd "${PROTEGE_HOME}" && ${runfile}
res=$?
cd "${currdir}"

exit ${res}

