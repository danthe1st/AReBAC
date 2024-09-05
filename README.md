This repository contains my implementation of the GP-eval and Weaving algorithms presented in [Syed Zain Raza Rizvi and Philip W. L. Fong. 2020. Efficient Authorization of Graph-database Queries in an Attribute-supporting ReBAC Model. ACM Trans. Priv. Secur. 23, 4, Article 18 (November 2020), 33 pages. https://doi.org/10.1145/3401027](https://doi.org/10.1145/3401027).

Another implementation using Neo4J has been [provided by the authors of that paper](https://github.com/szrrizvi/arebac/).

The `arebac-core` module contains the code for the GP-eval and Weaving algorithms as well as data classes from graph patterns and an in-memory graph implementation that can be used with the implementation of GP-eval.

### Graph patterns

Graph patterns are a database-independant representation of a query to a graph database.

A graph pattern consists of
- A directed graph
- Mutual exclusion constraints specifying two nodes must not be the same
- Node attribute requirements specifying specific nodes must have attributes matching a criterion
- Edge attribute requirements specifying specific edges must have attributes matching a criterion
- Nodes to be included in the result of the query
- The category of the query which is used to determine the policy as well as the actors (which nodes the query acts on)

An implementation of a graph pattern is provided in the `GraphPattern` class. This implementation doesn't include categories but only specifies the actors and corresponding nodes as that's all the information from categories that is necessary for the Weaving and GP-Eval algorithms.

### Weaving

Weaving combines multiple graph patterns such that the resulting graph pattern matches if and only if all of the combined patterns match with the constraint that all actors must correspond to the same node in the graph.

This algorithm is implemented in the `Weaving` class.

### GP-Eval

The implementation of the GP-Eval algorithm can be found in the `GPEval` class.

This algorithm matches a graph pattern against an attributed graph.
It attempts to assign a node in the graph for every node in the graph pattern in a way that all edges specified in the graph pattern are also present in the graph and no constraints in the graph pattern are violated.
If there are multiple possible assignments resulting in different values of the specified returned nodes, it should return all of these assignments.

In order to use that algorithm on custom graph implementation/graph databases, one needs to implement the `AttributedGraph` interface.

### arebac-neo4j

The `arebac-neo4j` module contains an implementation of `AttributedGraph` that uses an embedded Neo4J database that can be used to evaluate graph patterns against a Neo4J database with the GP-Eval algorithm.

```java
GraphDatabaseService database = getDatabase();
GraphPattern pattern = createGraphPattern();
try(Transaction tx = database.beginTx()){
	Set<List<Neo4jNode>> results = GPEval.evaluate(dbAsGraph, pattern);
	System.out.println(results);
}
```

### arebac-jfr

The `arebac-jfr` module provides an `AttributedGraph` implementation wrapping an `AttributedGraph` and adding custom [JFR](https://openjdk.org/jeps/328) events on graph accesses. These events can be used to find out which graph accesses are relevant for performance, how often these take place and where.
JFR events can be collected by starting Java with the `-XX:StartFlightRecording:filename=someRecording.jfr` JVM argument and can be analyized with [JDK Mission Control](https://openjdk.org/projects/jmc/).

This module provides the following JFR events:
- `io.github.danthe1st.arebac.jfr.events.FindEdgesEvent`: This event is fired when obtaining the edges of a node.
- `io.github.danthe1st.arebac.jfr.events.FindNodeEvent`: This event is fired when looking up a node by its id.
- `io.github.danthe1st.arebac.jfr.events.GetAttributeEvent`: This event is obtained when accessing an attribute.

Aside from that, the GP-Eval implementation (in the `arebac-core` module) provides these JFR events:
- `io.github.danthe1st.arebac.gpeval.events.FilterMutualExclusionConstraintEvent`: This event is fired during the "filter mutual exclusion constraints" step of GP-Eval.
- `io.github.danthe1st.arebac.gpeval.events.ForwardCheckingEvent`: This event is fired during the forward-checking step of GP-Eval.