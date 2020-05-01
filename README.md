# CSV To RDF
There is an abundance of widely available data sets across the web. Unfortunately, most data sets contain their information in comma-separated values (CSV) format. The goal of this project is to create a tool for performing the migration of CSV formatted data into an RDF formatted ontology. This entails reading the CSV file, parsing headers into properties, instantiating rows as resources, and outputting the computed ontology to an RDF XML file.

## Table of Contents

- [Build](#build)
- [Run](#run)
- [Features](#features)
- [Protege](#protege)
    - [Build Plugin](#buildplugin)
    - [Plugin Features](#pluginfeatures)
- [Team](#team)

## Build <a name="build"></a>
### Prerequisites
- JDK 8 is installed, which includes JavaFX. If using a new JDK, JavaFX libraries will need to be added separately 

A sample script 'buildrun.sh' is provided. Alternatively, this can be built manually by:
'''
1. Setting CLASSPATH to include the lib/ directory and the src/ directory
2. javac src/csvtordf/main/!(CsvPlugin).java
'''

## Run <a name="run"></a>
> If you are seeing this, there currently is no JAR file to run. Please build and run manually.

If building manually, the Application can be ran by:
'''
java -Dprism.order=sw csvtordf.main.CsvWizard
'''

## Features <a name="features"></a>

## Protege <a name="protege"></a>
> If you are seeing this, there currently is no JAR file to install. Please build and run manually.

### Build Plugin <a name="buildplugin"></a>
#### Prerequisites
- JDK8 is installed. Later versions are NOT supported!
- Protege OWL Editor is installed (only tested with Protege 5.5.0)
- ANT is installed.

A sample script 'buildplugin.sh' is provided. Alternatively, this can be built manually by:
'''
1. Setting Environment Variables
    a. PROTEGE_HOME - Home directory of Protege installation
    b. ANT_HOME - Home directory of Ant installation
    c. JAVA_HOME - Home directory of Java installation
    d. PATH - should include JDK8 and Ant in PATH
2. Copy jfxrt.jar from Protege to lib/
    - On Mac, this is typically in Protege.app/Contents/Plugins/JRE/Contents/Home/jre/lib/ext/
    - On Windows and Linux, this is typically in jre/lib/ext/

$ ant install -v
'''

This will create the Plugin Jar and place it in the Protege plugins directory, available
when Protege is next launched.

### Plugin Features <a name="pluginfeatures"></a>
There are special features for the plugin

## Team <a name="team"></a>
- Cody D'Ambrosio
- Paul Grocholske
- Charles Inwald
