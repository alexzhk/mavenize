# pentaho-mavenize #
Project that run moving from ant to maven - for now has setup for pentaho-hadoop-shims
run convert for all project from mvn to ant
If you develop shim for 7.0 branch using ant you can convert it to maven structure using  project https://github.com/VasilinaTerehova/mavenize, branch https://github.com/VasilinaTerehova/mavenize/tree/one_shim, run mvn clean install in cloned repo

To convert shim please point 2 arguments for program 

1) the path to ant project pentaho-hadoop-shims

2) the path to shims folder for new pentaho-hadoop-shims

In main method just change the shimName to needed and run from main class

#### Pre-requisites for building the project:
* Maven, version 3+
* Java JDK 1.8


#### Building it

__Build for nightly/release__

```
$ mvn clean install
```

This will build, unit test, and package the whole project (all of the sub-modules). Every submodule in pentaho-hadoop-shims is independent set of libraries and resources for one hadoop vendor.

