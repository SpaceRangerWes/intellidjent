package com.spacerangerwes.intellidjent

import java.io.File

import org.apache.commons.io.FileUtils

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

/**
  * Utility object to create a Jenkins Pipeline definition from a scala-graph
  */
object JenkinsPipelineBuilder {
  /**
    * Creates a map of the iNodeKeyMap and its sequential run index
    * @param graph
    *              scala-graph of maven module keys
    * @param iNodeKeyMap
    *                    map of string keys identifying an INode
    * @return
    *         Map[String, (Option[Int], Option[INode])]
    */
  def pipelineMapFromDiGraph(graph: Graph[String, DiEdge], iNodeKeyMap: Map[String, INode]): Map[String, (Option[Int], Option[INode])] = {
    val nodeOrderWithIndex: Seq[(String, Int)] = graph.topologicalSort.right.get.toList.map(_.value).zipWithIndex
    val nodeWithUrlMap: Map[String, (Option[Int], Option[INode])] = mergeMaps(nodeOrderWithIndex.toMap, iNodeKeyMap)

    nodeWithUrlMap
  }

  /**
    * Merges two maps by key and pairs the map values as a tuple
    * @param left
    *             primary map
    * @param right
    *              secondary map
    * @tparam A
    *           type of value in primary map
    * @tparam B
    *           type of value in secondary map
    * @return
    *         Map[String, (Option[A], Option[B])]
    */
  def mergeMaps[A, B](left: Map[String, A], right: Map[String, B]): Map[String, (Option[A], Option[B])] = {
    left.map{ pair =>
      if(right contains pair._1) (pair._1, (Some(pair._2), Some(right(pair._1))))
      else (pair._1, (Some(pair._2), None))
    }
  }

  /**
    * Create a Jenkins Pipeline stage
    * @param name
    *             name of the stage
    * @param gitUrl
    *               http url of the git repository
    * @return
    *         String
    */
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

  /**
    * Create a Jenkins Pipeline made of sequential stages that can be copied as a groovy script to a job
    * @param nodeWithUrlMap
    *                       map containing the index a stage belongs in and an INode
    * @return
    *         String
    */
  def createPipeline(nodeWithUrlMap: Map[String, (Option[Int], Option[INode])]): String = {
    val prefix: String = ""
    val indexedStageMap: Map[Int, String] = nodeWithUrlMap.map{ pair =>
      (pair._2._1.get, createStage(pair._1,pair._2._2.get.scmUrl.get.text))
    }
    prefix concat indexedStageMap.toSeq.sortWith(_._1 < _._1).map(_._2).reduce(_ concat _)
  }

  /**
    * Write a string that contains a pipeline groovy script to a file
    * @param pipeline
    *                 groovy pipeline script
    * @param file
    *             file name to write to
    */
  def writeToFile(pipeline: String, file: String): Unit = {
    FileUtils.writeStringToFile(new File(file), pipeline)
  }
}
