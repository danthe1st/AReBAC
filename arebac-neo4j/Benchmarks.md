# Running benchmarks

Before running the benchmarks, run the following command to build the project and setting up everything for the benchmarks:
```bash
mvn clean package -DskipTests -Pbenchmarks
```

After that, all benchmarks can be run using
```bash
java -cp target/classes:target/test-classes:target/all-dependencies/*:target/arebac-neo4j-0.0.1-SNAPSHOT.jar org.openjdk.jmh.Main
```

In order to run a single benchmark, the name of the benchmark needs to be added as an additional argument:
```bash
java -cp target/classes:target/test-classes:target/all-dependencies/*:target/arebac-neo4j-0.0.1-SNAPSHOT.jar org.openjdk.jmh.Main io.github.danthe1st.arebac.neo4j.tests.SOBenchmark.gpEval
```

It is also possible to run all benchmarks in a specific class or package.

## profiling

In order to profile a specific benchmark using [`async-profiler`](https://github.com/async-profiler/async-profiler), first download and extract `async-profiler` and then use the following command (replace `/PATH/TO/async-profiler` with the path you extracted `async-profiler` to):
```java
java -cp target/classes:target/test-classes:target/all-dependencies/*:target/arebac-neo4j-0.0.1-SNAPSHOT.jar org.openjdk.jmh.Main io.github.danthe1st.arebac.neo4j.tests.SOBenchmark -f 1 -jvmArgs '-agentpath:/PATH/TO/async-profiler/lib/libasyncProfiler.so=start,event=cpu,file=profile.html'
```

This configures JMH to only use a single fork and attaches async-profiler to create a CPU flamegraph in a `profile.html` file. The above example runs the benchmarks specified in the `SOBenchmark` class.