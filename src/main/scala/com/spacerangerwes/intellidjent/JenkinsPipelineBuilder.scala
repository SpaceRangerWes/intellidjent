package com.spacerangerwes.intellidjent

import java.io.File

import com.spacerangerwes.INode
import org.apache.commons.io.FileUtils

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

/**
  * Created by wh035505 on 3/15/17.
  */
object JenkinsPipelineBuilder {
  def pipelineFromDiGraph(graph: Graph[String, DiEdge], iNodeKeyMap: Map[String, INode]): Map[String, (Option[Int], Option[INode])] = {
    val nodeOrderWithIndex: Seq[(String, Int)] = graph.topologicalSort.right.get.toList.map(_.value).zipWithIndex
    val nodeWithUrlMap: Map[String, (Option[Int], Option[INode])] = mergeMaps(nodeOrderWithIndex.toMap, iNodeKeyMap)

    nodeWithUrlMap
//    nodeWithUrlMap.foreach{ pair =>
//      println("%d: %s, %s".format(pair._2._1.getOrElse(-1), pair._1, pair._2._2.get.scmUrl.get.text))
//    }
  }

  def mergeMaps[A, B](left: Map[String, A], right: Map[String, B]): Map[String, (Option[A], Option[B])] = {
    left.map{ pair =>
      if(right contains pair._1) (pair._1, (Some(pair._2), Some(right(pair._1))))
      else (pair._1, (Some(pair._2), None))
    }
  }

  def createStage(name: String, gitUrl: String): String = {
      """
        |stage '%s'
        |node {
        |  git url: '%s'
        |  def mvnHome = tool 'maven-3.0.4'
        |  env.JAVA_HOME="${tool 'java-1.6_37'}"
        |  env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
        |  sh "${mvnHome}/bin/mvn clean install"
        |}
      """.stripMargin.format(name, gitUrl)
  }

  def createPipeline(nodeWithUrlMap: Map[String, (Option[Int], Option[INode])]): String = {
    val prefix: String = ""
    val indexedStageMap: Map[Int, String] = nodeWithUrlMap.map{ pair =>
      (pair._2._1.get, createStage(pair._1,pair._2._2.get.scmUrl.get.text))
    }
    prefix concat indexedStageMap.toSeq.sortWith(_._1 < _._1).map(_._2).reduce(_ concat _)
  }

  def writeToFile(pipeline: String, file: String): Unit = {
    FileUtils.writeStringToFile(new File(file), pipeline)
  }
}
