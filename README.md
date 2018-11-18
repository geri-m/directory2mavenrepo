# D2M2 (Directory-2-Maven2)

With this tool you can import a directory structure (eg. from your local disc) into a maven repo (eg Nexus, Artifactory). 

There are three parameters required:
- the directory root on our disk/network drrive
- the URL of the repo (check your settings.xml, repository element)
- the ID for the repo (check your settings.xml, repository element)


## Build

In order to create the JAR file simple use maven and the execute

```
mvn clean install
```

The output is created in ./target and is named 'd2m2.jar'

## Usage

In order to create create the maven deploy statements use

```
java -jar d2m2.jar <Path> <RepositoryId> <Url> dryrun
```

If this give you the statements you want, you can directly execute the commands

```
java -jar d2m2.jar <Path> <RepositoryId> <Url> 
```

In order to run the maven command, the "mvn" binary needs to be part of the $PATH

## Platforms

The Script has been tested on OSX and Ubuntu so far. It is very likely that there is an issue on Windows as the Path separation is different on Windows.