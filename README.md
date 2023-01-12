# Java Microbenchmark Harness (JMH)

JMH is a Java harness for building, running, and analysing nano/micro/milli/macro benchmarks
written in Java and other languages targeting the JVM.

## Usage

### Basic Considerations

The recommended way to run a JMH benchmark is to use Maven to setup a standalone project
that depends on the jar files of your application. This approach is preferred to ensure that
the benchmarks are correctly initialized and produce reliable results. It is possible to run
benchmarks from within an existing project, and even from within an IDE, however setup is more
complex and the results are less reliable.

In all cases, the key to using JMH is enabling the annotation- or bytecode-processors to
generate the synthetic benchmark code. Maven archetypes are the primary mechanism used
to bootstrap the project that has the proper build configuration. We strongly recommend new
users make use of the archetype to setup the correct environment.

### Samples

In order to understand JMH tests and maybe write your own, it might be useful
to work through the [JMH Samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples). See the test comments for run instructions.

### Preferred Usage: Command Line

**Step 1. Setting up the benchmarking project.** The following command will generate the new JMH-driven
project in `test` folder:

    $ mvn archetype:generate \
      -DinteractiveMode=false \
      -DarchetypeGroupId=org.openjdk.jmh \
      -DarchetypeArtifactId=jmh-java-benchmark-archetype \
      -DgroupId=org.sample \
      -DartifactId=test \
      -Dversion=1.0

If you want to benchmark an alternative JVM language, use another archetype artifact ID from the
[list of existing ones](https://repo.maven.apache.org/maven2/org/openjdk/jmh/), it usually amounts
to replacing `java` to another language in the artifact ID given above. Using alternative archetypes
may require additional changes in the build configuration, see the `pom.xml` in the generated project.

**Step 2. Building the benchmarks.** After the project is generated, you can build it with the following
Maven command:

    $ cd test/
    $ mvn clean verify

**Step 3. Running the benchmarks.** After the build is done, you will get the self-contained executable JAR,
which holds your benchmark, and all essential JMH infrastructure code:

    $ java -jar target/benchmarks.jar

Run with `-h` to see the command line options available.

When dealing with large projects, it is customary to keep the benchmarks in a separate sub-project,
which then depends on the tested modules via the usual build dependencies.

### IDE Support

While the command line approach is the suggested one, some people prefer to use IDEs.
The user experience varies between different IDEs, but we will outline the general things here.
Running benchmarks from the IDE is generally not recommended due to generally uncontrolled environment
in which the benchmarks run.

**Step 1. Setting up the benchmarking project.** Some IDEs provide the GUI to create the Maven project
from the given archetype. Make sure your IDE knows about Central archetype catalog, and look for
`org.openjdk.jmh:jmh-${lang}-benchmark-archetype` there. Alternatively, you can use the command line
to generate the benchmark project, see above.

_NOTE: JMH is not intended to be used in the same way as a typical testing library such as JUnit.
Simply adding the `jmh-core` jar file to your build is not enough to be able to run benchmarks._

**Step 2. Building the benchmarks.** Most IDEs are able to open/import Maven projects, and infer
the build configuration from Maven project configuration. IDEA and NetBeans are able to build
JMH benchmark projects with little to no effort. Eclipse build configuration may need to set up
JMH annotation processors to run.

**Step 3. Running the benchmarks.** There is no direct support for JMH benchmarks in the IDE, but
one can use JMH Java API to invoke the benchmark. It usually amounts to having the `main` method,
which will then call into JMH. See [JMH Samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples)
for the examples of this approach. Before you run any benchmark, the project build is required.
Most IDEs do this automatically, but some do require explicit build action to be added before
the run: adding Maven target `verify` should help there.

### Other Build Systems

JMH project does not ship the build scripts for build systems other that Maven. But there are
community-supported bindings:
 - [Gradle JMH Plugin](https://github.com/melix/jmh-gradle-plugin)
 - [Scala SBT JMH Plugin](https://github.com/ktoso/sbt-jmh)

If you want to build with an alternative build system, you may want to see the
[Ant sample](https://github.com/openjdk/jmh/tree/master/jmh-ant-sample) which describes
the steps to build JMH benchmark projects.

## Support and Development

### Pre-Requisite Steps

Make sure you did this before publishing the benchmark, and/or requesting the JMH feature:

 - **JMH annotations Javadocs and Samples are essential reading.** Follow the [JMH Samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples)
   to get familiar with the API, use cases, culprits, and pitfalls in building the benchmarks
   and using JMH.
 - **Your benchmarks should be peer-reviewed.** Do not assume that a nice harness will magically
   free you from considering benchmarking pitfalls. We only promise to make avoiding them easier,
   not avoiding them completely.

Make sure you tried these things before getting support:

 - **Archetypes provide the golden build configuration.** Try to generate the clean JMH benchmark
   project and transplant the benchmark there. This is important to try when upgrading to the newer
   JMH versions, since the minute differences in the build configurations may attribute to the
   failures you are seeing.
 - **Current development code is usually leaner, meaner, and better.** Make sure you are running
   the latest JMH version, and/or try to run with bleeding edge JMH to see if the issue is already
   fixed.
 - **See if your question/issue was discussed already.** Look around mailing list archives to see if
   there is already an answer.

If all these did not help, you are welcome to report the issue.

### Reporting Harness and Test Bugs

If you have the access to [OpenJDK Bug System](https://bugs.openjdk.org/browse/CODETOOLS-7902762?jql=project%20%3D%20CODETOOLS%20AND%20resolution%20%3D%20Unresolved%20AND%20component%20%3D%20tools%20AND%20Subcomponent%20%3D%20jmh), please submit the bug there:
 * Project: CODETOOLS
 * Component: tools
 * Sub-component: jmh

Alternatively, you can join the [JMH Mailing List](https://mail.openjdk.org/mailman/listinfo/jmh-dev) and report the bugs there.

## Development

JMH project accepts pull requests, like other OpenJDK projects.
If you have never contributed to OpenJDK before, then bots would require you to [sign OCA first](http://openjdk.org/contribute).
Normally, you don't need to post patches anywhere else, or post to mailing lists, etc.
If you do want to have a wider discussion about JMH, please refer to [jmh-dev](https://mail.openjdk.org/mailman/listinfo/jmh-dev).

Short instructions to build, test bleeding-edge JMH, and install its JAR to local Maven repo:

    $ mvn clean install

If you already have the benchmark project, then it is enough to change JMH dependencies version
to the latest `SNAPSHOT` version (look up the actual latest version in [root `pom.xml`](https://github.com/openjdk/jmh/blob/master/pom.xml)). If not, create the JMH benchmark project and change the version there.

GitHub workflow "JMH Pre-Integration Tests" should pass on the changes. It would be triggered
for PRs. You can also trigger it manually for your branch.

## Related projects

These projects are supported by community, not by OpenJDK/JMH developers.

 - [IntelliJ IDEA JMH Plugin](https://github.com/artyushov/idea-jmh-plugin)
 - [Jenkins JMH Plugin](https://github.com/brianfromoregon/jmh-plugin)
 - [Teamcity JMH Plugin](https://github.com/presidentio/teamcity-plugin-jmh)
 - [Visualize JMH Benchmarks](https://github.com/jzillmann/jmh-visualizer)
