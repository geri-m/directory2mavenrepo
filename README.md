# General

With this tool you can import a directory structure into a maven repo. There are three
parameters required:
- the directory root
- the URL of the repo
- the ID for the repo



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