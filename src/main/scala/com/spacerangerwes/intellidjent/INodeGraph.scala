package com.spacerangerwes.intellidjent

import java.io.PrintWriter
import java.util

import sys.process._
import com.spacerangerwes.INode

import scalax.collection.GraphPredef._
import scalax.collection.{Graph, GraphTraversal}
import scalax.collection.io.dot._
import implicits._
import scala.collection.immutable.Iterable
import scala.collection.{immutable, mutable}
import scala.xml.Node
import scalax.collection.GraphEdge.DiEdge

/**
  * Created by wesleyhoffman on 3/1/17.
  */

case class INodeGraph(filePath: String, searchKey: String) {

  val iNodeKeyMap: Map[String,INode] = createKeyMap(filePath)._1
  val dependencyModuleMap: Map[String, String] = createKeyMap(filePath)._2

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

  def createKeyMap(string: String): (Map[String, INode], Map[String, String]) = {
    val iNodeArr: Array[INode] = LocalPomRetriever.pomToINode(string)
    collapseMultiModuleINodeMap(
      iNodeArr.map{ node =>
        createKey(node) -> node
      }.toMap)
  }

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

  def createDependencyModuleMap(key: String, value: INode): Map[String, String] = {
    if (value.children.isFailure) return Map.empty

    value.children.get.map{ (child: Node) =>
      val childKey: String = child.text.stripSuffix("/%s".format("pom.xml"))
      childKey -> key
    }.toMap
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
