package com.spacerangerwes.intellidjent

import com.spacerangerwes.INode
import org.kohsuke.github.{GHContent, GHRepository}

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

/**
  * Created by wh035505 on 3/15/17.
  */
object IntellidjentDriver extends App {
  val retriever: GithubOrgRetriever = GithubOrgRetriever("http_github_v3_api_url", "username", "password")
  val pomName: String = "pom.xml"
  val pomMap: Map[String, GHContent] = retriever.searchOrg("organization_name", pomName).toMap
  val nameUrlMap: Map[String, String] = retriever.writeMapToFiles(pomMap)

  val iNodeGraph: INodeGraph = INodeGraph("pom_collection/", "search_key")
  val graph: Graph[String, DiEdge] = iNodeGraph.generateGraph()

  iNodeGraph.writeToDot(graph, "fullgraph")

  val subGraph: Graph[String, DiEdge] = iNodeGraph.rootToLeafSubGraph(graph, "repository_name", "repository_name")

  iNodeGraph.writeToDot(subGraph, "subgraph")

  val orderMap: Map[String, (Option[Int], Option[INode])] = JenkinsPipelineBuilder.pipelineFromDiGraph(subGraph, iNodeGraph.iNodeKeyMap)

  val jenkinsPipeline: String = JenkinsPipelineBuilder.createPipeline(orderMap)
  JenkinsPipelineBuilder.writeToFile(jenkinsPipeline, "jenkinsPipeline.txt")
}
