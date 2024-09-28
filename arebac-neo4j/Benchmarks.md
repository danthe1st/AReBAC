# Running benchmarks

Before running the benchmarks, run the following command to build the project and setting up everything for the benchmarks:
```bash
mvn clean package -DskipTests -Pbenchmarks
```

After that, all benchmarks can be run using
```bash
java -cp target/classes:target/test-classes:target/all-dependencies/*:target/arebac-neo4j-0.0.1-SNAPSHOT.jar:../arebac-core/target/classes/ org.openjdk.jmh.Main
```

In order to run a single benchmark, the name of the benchmark needs to be added as an additional argument:
```bash
java -cp target/classes:target/test-classes:target/all-dependencies/*:target/arebac-neo4j-0.0.1-SNAPSHOT.jar:../arebac-core/target/classes/ org.openjdk.jmh.Main io.github.danthe1st.arebac.neo4j.tests.stackoverflow.SOBenchmark.gpEval
```

It is also possible to run all benchmarks in a specific class or package.

The following command can be used for checking whether all benchmarks run successfully. This command does not yield relevant performance information.
```bash
# WARNING: THIS DOES NOT RUN BENCHMARKS IN A USEFUL WAY - ONLY FOR TESTING
java -cp target/classes:target/test-classes:target/all-dependencies/*:target/arebac-neo4j-0.0.1-SNAPSHOT.jar:../arebac-core/target/classes/ org.openjdk.jmh.Main -f 1 -i 1 -wi 0 -r 100ms -foe true
```

## profiling

In order to profile a specific benchmark using [`async-profiler`](https://github.com/async-profiler/async-profiler), first download and extract `async-profiler` and then use the following command (replace `/PATH/TO/async-profiler` with the path you extracted `async-profiler` to):
```java
java -cp target/classes:target/test-classes:target/all-dependencies/*:target/arebac-neo4j-0.0.1-SNAPSHOT.jar org.openjdk.jmh.Main AirbnbBenchmark.scenario1GetReviewsFromHostGPEvalWithoutWeaving -prof 'async:libPath=/PATH/TO/async-profiler/lib/libasyncProfiler.so;output=flamegraph;dir=profile' -f 2
```

This configures JMH to only use only two forks and attaches async-profiler to create a CPU flamegraph in a `profile.html` file. The above example runs the `AirbnbBenchmark.scenario1GetReviewsFromHostGPEvalWithoutWeaving` benchmark.