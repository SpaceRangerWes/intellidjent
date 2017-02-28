# intellidjent
###### An intelligent Maven Workflow Generator for Jenkins and CI


## Presenting the Problem

This project is to simplify the act of creating Continuous Integration tests for large and complex maven build situations.

Let's look at the simple example DAG (Directed Acyclic Graph) given below in *figure 1*

Figure 1
![plain]

**Note**: *When a node in the DAG is not colored, assume it is a master build of the component.*

In *figure 1* we are shown a dependency graph that has 14 Maven modules (assume they are in separate repositories) and each of the leaves (a node with no outgoing edges is a leaf. *i.e. node 6, 7, 9, etc*) is a processing module that requires all of its 3rd-party dependencies and ancestor/parent modules packaged into a [shaded jar](https://maven.apache.org/plugins/maven-shade-plugin/). These processing modules will be assumed to to run on some distributed framework like Spark, Flume, Hadoop, etc. The question for our Stable Dev builds is simply "how do we segment this DAG into discrete build jobs for each processing module?"

#### Partitioning Build Paths and Reduce Duplicating builds

This section is two-fold. At least 7 Jenkins jobs need to be dynamically generated in *figure 1* for each of the processing modules 6, 7, and 9-12. However if we build the path `0 -> 1 -> 2 -> 3` seven times, we are repeating a lot of work, consuming more compute time and resources, and creating new SNAPSHOT versions each time we build that path for each module.
```
Step 1: Each maven module/repository should have its own elementary Jenkins/CI build definition that configures the commands to be run and the setup of the container environment for building that single repository.

Step 2: A master Jenkins/CI job contains the run logic for ordering builds, defining consumed and emitted versions, and any other customized parameters.
```

The purpose for partitioning build paths is any case that an isolated branch needs to be ran. A good example would be if the *master* branch was just updated on **module 8**. In that case **module 8** can consume the last master SNAPSHOT of **module 3** and the paths `8->13`, `8->11`, and `8->12` are in their own reactive build.

#### SNAPSHOT Conflicts Of Interest

If each module pulls in the latest snapshot build of its dependencies, there must be a guarantee that master builds are always consuming master builds of dependencies and not a remote development test. This is because the accidental consumption of development code may present a false-negative when running continuous testing against code that is assumed to be stable. *Figure 2* shows one of these situations.

Figure 2
![figure2]

Because **module 2** states that it wants to consume the latest development build of **module 0** in the form of `<artifact>version-SNAPSHOT</artifact>`, if the timestamp of **dev-snapshot** is more recent than **master-snapshot** and they are both of version *v*, then **dev-snapshot** will be included in **module 2** and all consuming descendants. Hopefully you can see the problem this causes. Therefore, a Jenkins build of **module 2** must configure and validate that all ancestors are exclusively master builds.

#### Development Situation One
Figure 3
![figure3]

If a developer is working sufficiently close to the top of the tech stack (a leaf is the top in this case), then the only modules that need to be added to a workflow is itself and its descendants. In *figure 3* **module 5** is the only module with a dev change for this example JIRA task. *intellidjent* will create a workflow that ingests master SNAPSHOTs as **module 5** and **module 10's** ancestors and create an isolated development SNAPSHOT of **module 5** and **module 10** so that other intellidjent workflows are not aware of the development changes being made. Focusing on `3 -> 5 -> 10`, the reactive build looks like *figure 4* below.

Figure 4
![figure4]


[plain]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/test.png
[figure2]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/test_one_node_with_two_sources.png
[figure3]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/test_with_dev_2_colors.png
[figure4]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/Jenkins-Reaction.png
