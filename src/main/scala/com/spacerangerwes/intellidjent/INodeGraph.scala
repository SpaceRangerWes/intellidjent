package com.spacerangerwes.intellidjent

import com.spacerangerwes.INode

import scala.collection.immutable.Iterable
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq
import scalax.collection.GraphPredef._
import scalax.collection.Graph
import scalax.collection.io.dot._
import implicits._
import scalax.collection.GraphEdge.DiEdge

/**
  * Created by wesleyhoffman on 3/1/17.
  */

object INodeGraph extends App {

  def createKey(iNode: INode): String = {
    List(iNode.groupId.get.text, iNode.artifactId.get.text).mkString(".")
  }

  def createKey(str1: String, str2: String): String = {
    List(str1, str2).mkString(".")
  }

  def getEdges(string: String, iNode: INode): Seq[(String, String)] = {
    if (iNode.dependencies.isFailure) Seq((string, ""))
    else {
      iNode.dependencies.get.map{ dep =>
        val groupId = (dep \ "groupId").text
        val artifactId = (dep \ "artifactId").text
        (string, createKey(groupId,artifactId))
      }
    }
  }

  var iNodeKeyMap: Map[String,INode] = Map()
  LocalPomRetriever
    .pomToINode("/Users/wesleyhoffman/IdeaProjects/")
    .foreach{ node =>
      iNodeKeyMap += (createKey(node) -> node)
    }

  val edgePairs: List[(String, String)] = iNodeKeyMap.map(k => getEdges(k._1,k._2)).toList.flatten


  val srwEdges = edgePairs
    .filter{ pair =>
      pair._1.contains("com.spacerangerwes") && pair._2.contains("com.spacerangerwes")
    }
    .map{ pair =>
      pair.swap._1~>pair.swap._2
    }
  val srwNodes = iNodeKeyMap.map(i => i._1)
  val srwGraph: Graph[String, DiEdge] = Graph.from(srwNodes, srwEdges)



  val root = DotRootGraph (
    directed = false,
    id = Some("Wikipedia_Example"))


  val dot: String = srwGraph.toDot(root, _ => None)
  print(dot)
}
