# pentaho-mavenize #
Project that run moving from ant to maven - for now has setup for pentaho-hadoop-shims
run convert for all project from mvn to ant
the only argument to main is the folder where project pentaho-hadoop-shims is checked out
for now main.sh, tmp.sh should be there - could be checkout VasilinaTerehova/pentaho-haodop-shims/BAD-570
api folder proccessed separately - just folder to maven structure movement, convert pom, generate versions property section, add parent tag
for every shim the previous actions take part, assembly plugin section put, also script with generating assembly.xml starts which, after script finishes
assembly move to needed folder, add to github


#### Pre-requisites for building the project:
* Maven, version 3+
* Java JDK 1.8


#### Building it

__Build for nightly/release__

```
$ mvn clean install
```

This will build, unit test, and package the whole project (all of the sub-modules). Every submodule in pentaho-hadoop-shims is independent set of libraries and resources for one hadoop vendor.

