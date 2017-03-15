package com.spacerangerwes.intellidjent

import java.io.PrintWriter
import java.util

import sys.process._
import com.spacerangerwes.INode
import com.spacerangerwes.intellidjent.INodeGraph.srwGraph

import scalax.collection.GraphPredef._
import scalax.collection.{Graph, GraphTraversal}
import scalax.collection.io.dot._
import implicits._
import scala.collection.{immutable, mutable}
import scala.xml.Node
import scalax.collection.GraphEdge.DiEdge

/**
  * Created by wesleyhoffman on 3/1/17.
  */

object INodeGraph extends App {

  var iNodeKeyMap: Map[String,INode] = Map()
  var dependencyModuleMap: Map[String, String] = Map()

  def createKey(iNode: INode): String = {
    iNode.artifactId.get.text
  }

  def getEdges(string: String, iNode: INode): Seq[(String, String)] = {
    var dependencies = None: Option[Seq[(String, String)]]
    var parent = None: Option[Seq[(String, String)]]
    if (iNode.dependencies.isSuccess) {
      dependencies = Some(
        iNode.dependencies.get.map{ dep =>
          val artifactId = (dep \ "artifactId").text
          val key = artifactId
          if(dependencyModuleMap.contains(key)) (string, dependencyModuleMap(key))
          else (string, artifactId)
        }
      )
    }
    if (iNode.parent.isSuccess) {
      parent = Some(
        iNode.parent.get.map{ p =>
          val artifactId = (p \ "artifactId").text
          (string, artifactId)
        }
      )
    }
    dependencies.get ++ parent.get
  }


  /**
    * Generate GraphX of the dependency DAG
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

  def collapseMultiModuleINodeMap(iNodeKeyMap: Map[String, INode]): Map[String, INode] = {
    var tempCopyMap: Map[String, INode] = iNodeKeyMap
    tempCopyMap.foreach{ pair =>
      if(pair._2.children.isSuccess){
        pair._2.children.get.foreach{ (child: Node) =>
          val childKey: String = child.text.stripSuffix("/%s".format("pom.xml"))
          val childINode: INode = tempCopyMap(childKey)
          tempCopyMap(pair._1).appendNodeDependencies(childINode)
          tempCopyMap -= childKey
          dependencyModuleMap += (childKey -> pair._1)
        }
      }
    }
    tempCopyMap
  }

  def createKeyMap(string: String): Map[String,INode] = {
    LocalPomRetriever
      .pomToINode(string)
      .foreach{ node =>
        iNodeKeyMap += (createKey(node) -> node)
      }

    collapseMultiModuleINodeMap(iNodeKeyMap)
  }

  iNodeKeyMap = createKeyMap("/Users/wh035505/Repositories/my_sandbox/intellidjent/pom_collection")


  val edgePairs: List[(String, String)] = iNodeKeyMap.map(k => getEdges(k._1,k._2)).toList.flatten


  val srwEdges = edgePairs
    .filter{ pair =>
      pair._1.contains("analytics") && pair._2.contains("analytics")
    }
    .map{ pair =>
      pair.swap._1~>pair.swap._2
    }
  val srwNodes = iNodeKeyMap.keys
  val srwGraph: Graph[String, DiEdge] = Graph.from(srwNodes, srwEdges)

  writeToDot(srwGraph,"fullGraph")
  /**
    * Test scala-graph methods for future implementations
    */
  val someMiddleNode: srwGraph.NodeT = srwGraph get "analytics-transformation-populous"
  val subGraphSet: Set[Graph[String, DiEdge]#NodeT] = getDirectAncestry(someMiddleNode) ++ getAllDescendants(someMiddleNode) ++ Set(someMiddleNode)
  val subGraph: Graph[String, DiEdge] = srwGraph filter srwGraph.having(node = subGraphSet.contains(_))
  writeToDot(subGraph, "trans-pop-build-graph")



  val someMiddleNode2: srwGraph.NodeT = srwGraph get "analytics-transformation-kpi"
  val subGraphSet2: Set[Graph[String, DiEdge]#NodeT] = getDirectAncestry(someMiddleNode2) ++ getAllDescendants(someMiddleNode2) ++ Set(someMiddleNode2)
  val subGraph2: Graph[String, DiEdge] = srwGraph filter srwGraph.having(node = subGraphSet2.contains(_))
  writeToDot(subGraph2, "trans-kpi-build-graph")


  val subUnion: Graph[String, DiEdge] = subGraph union subGraph2
  writeToDot(subUnion, "trans-pop-and-kpi-union")
  /**
    * Helper method for getDirectAncestry
    * @param node
    *             A Graph[String, DiEdge]#NodeT object
    */
  def getDirectAncestry(node: Graph[String, DiEdge]#NodeT): Set[Graph[String, DiEdge]#NodeT] = {
    if (node.diPredecessors.nonEmpty) {
      node.diPredecessors ++ node.diPredecessors.flatMap(getDirectAncestry)
    } else {
      Set(node)
    }
  }

  def getAllDescendants(node: Graph[String, DiEdge]#NodeT): Set[Graph[String, DiEdge]#NodeT] = {
    if (node.diSuccessors.nonEmpty) {
      node.diSuccessors ++ node.diSuccessors.flatMap(getAllDescendants)
    } else {
      Set(node)
    }
  }
}
