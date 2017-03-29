# intellidjent (Rhythmic Development Automation)
###### An intelligent Maven Workflow Generator for Jenkins and CI


## Presenting the Problem

* Simplify the act of creating Continuous Integration (CI) tests for large and complex Maven module situations
* Guarantee that all affected modules are tested
* Guarantee an isolated development environment to reduce inter-engineer
conflicts

#### Example
A dependency graph that has 14 Github Repositories that contain 1 or more Maven modules. Each of the leaves is a processing module that will be packaged as a shaded jar to be sent to Hadoop. intellidjent has to understand the module graph structure and return a workflow representation that Jenkins can process.

If it is assumed that this graph given is a cohesive tech stack then all 14 nodes will need to build continuously for our Stable builds.
Intellidjent, alpha version, will build our Stable stack in sequential order by following a topological sorted order (i.e. 0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 9 -> 10 -> 13 -> 11 -> 12)

In *figure 1* we are shown a dependency graph that has 14 Maven modules (assume they are in separate repositories) and each of the leaves (a node with no outgoing edges is a leaf. *i.e. node 6, 7, 9, etc*) is a processing module that requires all of its 3rd-party dependencies and ancestor/parent modules packaged into a [shaded jar](https://maven.apache.org/plugins/maven-shade-plugin/). These processing modules will be assumed to to run on some distributed framework like Spark, Flume, Hadoop, etc. The question for our Stable Dev builds is simply "how do we segment this DAG into discrete build jobs for each processing module?"

Figure 1

![figure1]


#### SNAPSHOT Conflicts Of Interest


##### There are two important questions to ask

What happens when I’m developing on repository 2 and a fellow engineer is developing on repository 0?

Will I consume their development builds that are considered unstable or will I be consuming the latest code from master that is considered stable?

##### Option 1
She could have isolated her snapshot (unique snapshot identifier that I can’t consume unless I purposely try). Because she isolated her module version, I won’t consume her changes. But we can’t guarantee every engineer will be as proactive.

##### Option 2
She could have left the version of repository 0 in her dev branch as X.X.X-Master-Snapshot. Not good news for me because I’m now going to consume her development code from a build 5 minutes ago compared to a true Master branch build from 10 minutes. This is why each developer should have an isolated environment when building versions that will ultimately be hosted on the remote Maven SNAPSHOT repository.

Figure 2

![figure2]

**To reiterate:** Because **module 2** states that it wants to consume the latest development build of **module 0** in the form of `<artifact>version-SNAPSHOT</artifact>`, if the timestamp of **dev-snapshot** is more recent than **master-snapshot** and they are both of version *v*, then **dev-snapshot** will be included in **module 2** and all consuming descendants. Hopefully you can see the problem this causes. Therefore, a Jenkins build of **module 2** must configure and validate that all ancestors are exclusively master builds.

#### Development Situation One
Figure 3

![figure3]

If a developer is working sufficiently close to the top of the tech stack (a leaf is the top in this case), then the only modules that need to be added to a workflow is itself and its descendants. In *figure 3* **module 5** is the only module with a dev change for this example JIRA task. *intellidjent* will create a workflow that ingests master SNAPSHOTs as **module 5** and **module 10's** ancestors and create an isolated development SNAPSHOT of **module 5** and **module 10** so that other intellidjent workflows are not aware of the development changes being made. Focusing on `3 -> 5 -> 10`, the reactive build looks like *figure 4* below.

Figure 4

![figure4]




#### Development Situation Two
Figure 5

![figure5]

If a developer is working near the root of the maven ancestry or across multiple modules, she must be aware of possible side-effects that can occur far away in the descendants. Passivity can only be guaranteed if all consumers are tested. This situation requires that all descendants of **module 1** are rebuilt with that new SNAPSHOT and proper integration testing is ran. At this point, the only module that is not isolated from master is **module 0**.


## Project Architecture

![figure6]


## Flowchart For an Engineer to Follow When Using intellidjent

![figure7]

[figure1]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/test.png
[figure2]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/test_one_node_with_two_sources.png
[figure3]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/test_with_dev_2_colors.png
[figure4]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/Jenkins-Reaction.png
[figure5]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/situation_two.png
[figure6]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/intellidjent_alpha.png
[figure7]: https://raw.githubusercontent.com/SpaceRangerWes/intellidjent/master/docs/intellidjent_use_case_flowchart.png
