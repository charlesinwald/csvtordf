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

A sample script `buildrun.sh` is provided. Alternatively, this can be built manually by:
```
1. Setting CLASSPATH to include the lib/ directory and the src/ directory
2. javac src/csvtordf/main/!(CsvPlugin).java
```

## Run <a name="run"></a>
> If you are seeing this, there currently is no JAR file to run. Please build and run manually.

If building manually, the Application can be ran by:
```
java -Dprism.order=sw csvtordf.main.CsvWizard
```

## Features <a name="features"></a>
### Import CSV File
> Select a CSV file to convert to RDF data. The headers will define the properties, and each subsequent row will be considered a new resource. A resource prefix may be specified as well.
![Import GIF](https://github.com/charlesinwald/csvtordf/images/import.gif)

### Multithreading
> Increase number of threads to potentially reduce conversion time on large data sets
![MultiThreading GIF](https://github.com/charlesinwald/csvtordf/images/multithreading.gif)

### Saving
> Save the generated RDF data to an XML file
![Saving GIF](https://github.com/charlesinwald/csvtordf/images/saving.gif)

### Augmenting Input
> Augment the input data to apply to all imported resources.

## Protege <a name="protege"></a>
> If you are seeing this, there currently is no JAR file to install. Please build and run manually.

### Build Plugin <a name="buildplugin"></a>
#### Prerequisites
- JDK8 is installed. Later versions are NOT supported!
- Protege OWL Editor is installed (only tested with Protege 5.5.0)
- ANT is installed.

A sample script `buildplugin.sh` is provided. Alternatively, this can be built manually by:
```
1. Setting Environment Variables
    a. PROTEGE_HOME - Home directory of Protege installation
    b. ANT_HOME - Home directory of Ant installation
    c. JAVA_HOME - Home directory of Java installation
    d. PATH - should include JDK8 and Ant in PATH
2. Copy jfxrt.jar from Protege to lib/
    - On Mac, this is typically in Protege.app/Contents/Plugins/JRE/Contents/Home/jre/lib/ext/
    - On Windows and Linux, this is typically in jre/lib/ext/

$ ant install -v
```

This will create the Plugin Jar and place it in the Protege plugins directory, available
when Protege is next launched.

## Plugin Features <a name="pluginfeatures"></a>
> The Plugin will be available under the Tools menu
![Tools GIF](https://github.com/charlesinwald/csvtordf/images/tools.gif)

The generated RDF triples will be imported to the current Protege Ontology, creating new instances and properties as needed.

## Team <a name="team"></a>
- Cody D'Ambrosio
- Paul Grocholske
- Charles Inwald
