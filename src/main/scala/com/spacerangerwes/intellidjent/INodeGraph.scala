package com.spacerangerwes.intellidjent

import java.io.PrintWriter

import sys.process._
import com.spacerangerwes.INode

import scalax.collection.GraphPredef._
import scalax.collection.Graph
import scalax.collection.io.dot._
import implicits._
import scala.xml.Node
import scalax.collection.GraphEdge.DiEdge

/**
  * Class containing all utilities to build a scala-graph of INodes
  * @param filePath
  *                 path to a flat directory containing all necessary maven POM files
  * @param searchKey
  *                  string to use in looking for the subset of dependencies that pertain to an organization
  *                  dependency graph
  */
case class INodeGraph(filePath: String, searchKey: String) {

  val iNodeKeyMap: Map[String,INode] = createKeyMap(filePath)._1
  val dependencyModuleMap: Map[String, String] = createKeyMap(filePath)._2

  /**
    * Using the class value iNodeKeyMap, a graph is created from devising edge pairs and nodes
    * @return
    *         Graph[String, DiEdge]
    */
  def generateGraph(): Graph[String, DiEdge] = {
    val edgePairs: List[(String, String)] = iNodeKeyMap.map(k => getEdges(k._1,k._2)).toList.flatten
    val edges = edgePairs
      .filter{ pair =>
        pair._1.contains(searchKey) && pair._2.contains(searchKey)
      }
      .filterNot{ pair =>
        pair._1 == pair._2
      }
      .map{ pair =>
        pair.swap._1~>pair.swap._2
      }
    val nodes = iNodeKeyMap.keys

    Graph.from(nodes, edges)
  }

  /**
    * Create a key from the artifactId tag contained in the given INode
    * @param iNode
    *              INode that the key is to be created from
    * @return
    *         String
    */
  def createKey(iNode: INode): String = {
    iNode.artifactId.get.text
  }

  /**
    * Creates the edges between an INode and its possible dependencies and parent
    * @param key
    *            key of the parameter iNode
    * @param iNode
    *              iNode to generate the edges from
    * @return
    *         Seq[(String, String)]
    */
  def getEdges(key: String, iNode: INode): Seq[(String, String)] = {
    var dependencies = None: Option[Seq[(String, String)]]
    var parent = None: Option[Seq[(String, String)]]
    if (iNode.dependencies.isSuccess) {
      dependencies = Some(
        iNode.dependencies.get.map{ dep =>
          val artifactId = (dep \ "artifactId").text
          val dependencyKey = artifactId
          if(dependencyModuleMap.contains(dependencyKey)) (key, dependencyModuleMap(dependencyKey))
          else (key, artifactId)
        }
      )
    }
    if (iNode.parent.isSuccess) {
      parent = Some(
        iNode.parent.get.map{ p =>
          val artifactId = (p \ "artifactId").text
          (key, artifactId)
        }
      )
    }
    dependencies.get ++ parent.get
  }

  /**
    * Creates two maps that define how the INodes will map edges to one-another
    * @param pomDirectory
    *                     the directory location of the POM files collected via the GithubRetriever
    * @return
    *         (Map[String, INode], Map[String, String])
    */
  def createKeyMap(pomDirectory: String): (Map[String, INode], Map[String, String]) = {
    val iNodeArr: Array[INode] = LocalPomRetriever.pomToINode(pomDirectory)
    collapseMultiModuleINodeMap(
      iNodeArr.map{ node =>
        createKey(node) -> node
      }.toMap)
  }

  /**
    * Collapses INodes that belong to a single multi-module maven project.
    * @param iNodeKeyMap
    *                    the non-collapsed class value
    * @return
    *         (Map[String, INode], Map[String, String])
    */
  def collapseMultiModuleINodeMap(iNodeKeyMap: Map[String, INode]): (Map[String, INode], Map[String, String]) = {
    var tempCopyMap: Map[String, INode] = iNodeKeyMap
    val moduleMap: Map[String, String] = tempCopyMap.map { (pair: (String, INode)) =>
      createDependencyModuleMap(pair._1, pair._2)
    }.reduce(_ ++ _)

    moduleMap.foreach{ childParentKeyPair =>
      val childINode: INode = tempCopyMap(childParentKeyPair._1)
      tempCopyMap(childParentKeyPair._2).appendNodeDependencies(childINode)
      tempCopyMap -= childParentKeyPair._1
    }
    (tempCopyMap, moduleMap)
  }

  /**
    * Creates a map of keys from the given INode's children to the key of the INode itself. This map provides the
    * necessary information to collapse multi-module POM structures.
    * @param key
    *            the key of the INode given to the parameter value
    * @param value
    *              the INode corresponding to the given parameter for key
    * @return
    *         Map[String, String]
    */
  def createDependencyModuleMap(key: String, value: INode): Map[String, String] = {
    if (value.children.isFailure) return Map.empty

    value.children.get.map{ (child: Node) =>
      val childKey: String = child.text.stripSuffix("/%s".format("pom.xml"))
      childKey -> key
    }.toMap
  }

  /**
    * Writes a graphViz dot file (via a side-effect) to the working directory and generates a .png file of the produced
    * dot file (again via a side-effect)
    * @param graph
    *              scala-graph that contains a dot writable graph
    * @param fileName
    *                 name given to both the produced .dot and .png files
    */
  def writeToDot(graph: Graph[String, DiEdge], fileName: String): Unit = {
    val root = DotRootGraph(true, id = Some(Id("Dot")))
    def edgeTransformer(graph: Graph[String, DiEdge], innerEdge: Graph[String,DiEdge]#EdgeT): Option[(DotGraph,DotEdgeStmt)] = innerEdge match {
      case graph.EdgeT(source, target) =>
        Some((root, DotEdgeStmt(source.toString, target.toString)))
    }
    val dot = graph.toDot(root, edgeTransformer(graph, _))
    val dotFile = new PrintWriter("%s.dot".format(fileName))
    dotFile.println(dot.toString)
    dotFile.close()
    "dot -Tpng %s.dot -o %s.png".format(fileName, fileName) !
  }

  /**
    * The union of the heavy paths from all direct ancestors to leaf nodes that go through the nodes of focus
    * @param graph
    *              scala-graph that contains the full dependency graph
    * @param nodeNames
    *                  key of the nodes of focus
    * @return
    *         Graph[String, DiEdge]
    */
  def rootToLeafSubGraph(graph: Graph[String, DiEdge], nodeNames: String*): Graph[String, DiEdge] = {
    val nodeSeq: Seq[graph.NodeT] = nodeNames.map(graph get _)
    val subGraphSetSeq: Seq[Set[Graph[String, DiEdge]#NodeT]] = nodeSeq.map{ node =>
      getDirectAncestry(node) ++ getAllDescendants(node) ++ Set(node)
    }
    val subGraphSeq: Seq[Graph[String, DiEdge]] = subGraphSetSeq.map{ nodeSet =>
      graph filter graph.having(node = nodeSet.contains(_))
    }

    subGraphSeq.reduce(_ union _)
  }

  /**
    * Retrieves the direct ancestors of the given node as a set of nodes
    * @param node
    *             the node of focus
    * @return
    *         Set[Graph[String, DiEdge]#NodeT]
    */
  def getDirectAncestry(node: Graph[String, DiEdge]#NodeT): Set[Graph[String, DiEdge]#NodeT] = {
    if (node.diPredecessors.nonEmpty) {
      node.diPredecessors ++ node.diPredecessors.flatMap(getDirectAncestry)
    } else {
      Set(node)
    }
  }

  /**
    * Retrieves all descendants that have the given node as a dependency (transitive relation or otherwise)
    * @param node
    *             the node of focus
    * @return
    *         Set[Graph[String, DiEdge]#NodeT]
    */
  def getAllDescendants(node: Graph[String, DiEdge]#NodeT): Set[Graph[String, DiEdge]#NodeT] = {
    if (node.diSuccessors.nonEmpty) {
      node.diSuccessors ++ node.diSuccessors.flatMap(getAllDescendants)
    } else {
      Set(node)
    }
  }
}
