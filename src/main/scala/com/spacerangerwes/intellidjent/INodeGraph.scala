package com.spacerangerwes.intellidjent

import java.io.PrintWriter
import java.util

import sys.process._
import com.spacerangerwes.INode
import com.spacerangerwes.intellidjent.INodeGraph.srwGraph

import scalax.collection.GraphPredef._
import scalax.collection.Graph
import scalax.collection.io.dot._
import implicits._
import scala.collection.immutable
import scala.xml.Node
import scalax.collection.GraphEdge.DiEdge

/**
  * Created by wesleyhoffman on 3/1/17.
  */

object INodeGraph extends App {

  var iNodeKeyMap: Map[String,INode] = Map()
  var dependencyModuleMap: Map[String, String] = Map()

  def createKey(iNode: INode): String = {
    List(iNode.groupId.get.text, iNode.artifactId.get.text).mkString(".")
  }

  def createKey(str1: String, str2: String): String = {
    List(str1, str2).mkString(".")
  }

  def getEdges(string: String, iNode: INode): Seq[(String, String)] = {
    var dependencies = None: Option[Seq[(String, String)]]
    var parent = None: Option[Seq[(String, String)]]
    if (iNode.dependencies.isSuccess) {
      dependencies = Some(
        iNode.dependencies.get.map{ dep =>
          val groupId = (dep \ "groupId").text
          val artifactId = (dep \ "artifactId").text
          val key = createKey(groupId,artifactId)
          if(dependencyModuleMap.contains(key)) (string, dependencyModuleMap(key))
          else (string, createKey(groupId,artifactId))
        }
      )
    }
    if (iNode.parent.isSuccess) {
      parent = Some(
        iNode.parent.get.map{ p =>
          val groupId = (p \ "groupId").text
          val artifactId = (p \ "artifactId").text
          (string, createKey(groupId,artifactId))
        }
      )
    }
    dependencies.get ++ parent.get
  }

  def collapseMultiModuleINodeMap(iNodeKeyMap: Map[String, INode]): Map[String, INode] = {
    var tempCopyMap: Map[String, INode] = iNodeKeyMap
    tempCopyMap.foreach{ pair =>
      if(pair._2.children.isSuccess){
        pair._2.children.get.foreach{ (child: Node) =>
          val childKey: String = List(pair._2.groupId.get.text, child.text).mkString(".")
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

  iNodeKeyMap = createKeyMap("/Users/wh035505/Repositories")


  val edgePairs: List[(String, String)] = iNodeKeyMap.map(k => getEdges(k._1,k._2)).toList.flatten


  val srwEdges = edgePairs
    .filter{ pair =>
      pair._1.contains("com.cerner.pophealth.analytics") && pair._2.contains("com.cerner.pophealth.analytics")
    }
    .map{ pair =>
      pair.swap._1~>pair.swap._2
    }
  val srwNodes = iNodeKeyMap.keys
  val srwGraph: Graph[String, DiEdge] = Graph.from(srwNodes, srwEdges)


  /**
    * Generate GraphX of the dependency DAG
    */
  val root = DotRootGraph(true, id = Some(Id("Dot")))
  def edgeTransformer(graph: Graph[String, DiEdge], innerEdge: Graph[String,DiEdge]#EdgeT): Option[(DotGraph,DotEdgeStmt)] = innerEdge match {
    case graph.EdgeT(source, target) =>
        Some((root, DotEdgeStmt(source.toString, target.toString)))
  }
  val dot = srwGraph.toDot(root, edgeTransformer(srwGraph, _))
  println(dot)
  val dotFile = new PrintWriter("graph.dot")
  dotFile.println(dot.toString)
  dotFile.close()
  "dot -Tpng graph.dot -o graph.png" !

  /**
    * Test scala-graph methods for future implementations
    */
  val someMiddleNode: srwGraph.NodeT = srwGraph get "com.cerner.pophealth.analytics.analytics-transformation-populous"
  val x = getDirectAncestry(someMiddleNode)
  val y = getAllDescendants(someMiddleNode)
  println(x)
  println(y)


  /**
    * Helper method for getDirectAncestry
    * @param node
    *             A Graph[String, DiEdge]#NodeT object
    */
  def getDirectAncestry(node: Graph[String, DiEdge]#NodeT): Set[String] = {
    if (node.diPredecessors.nonEmpty) {
      node.diPredecessors.map(p => p.toString) ++ node.diPredecessors.flatMap(getDirectAncestry)
    } else {
      Set(node.toString)
    }
  }

  def getAllDescendants(node: Graph[String, DiEdge]#NodeT): Set[String] = {
    if (node.diSuccessors.nonEmpty) {
      node.diSuccessors.map(p => p.toString()) ++ node.diSuccessors.flatMap(getAllDescendants)
    } else {
      Set(node.toString)
    }
  }
}
