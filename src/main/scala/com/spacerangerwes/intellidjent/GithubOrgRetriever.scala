package com.spacerangerwes.intellidjent

import java.io._
import java.nio.file.{Files, Paths}
import java.util

import scala.collection.JavaConverters._
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import org.kohsuke.github.{GHContent, GHRepository, GitHub, PagedIterable}

import scala.collection.parallel.{ParMap, mutable}


/**
  * Created by wh035505 on 3/8/17.
  */
case class GithubOrgRetriever(apiUrl: String, userName: String, password: String) {

  val gitHub: GitHub = buildRetriever(apiUrl, userName, password)

  def buildRetriever(apiUrl: String, userName: String, password: String): GitHub = {
    GitHub.connectToEnterprise(apiUrl, userName, password)
  }

  def orgRepositories(name: String): List[GHRepository] = {
    gitHub.getOrganization(name).listRepositories().asScala.toList
  }

  def searchOrg(org: String, searchItem: String): List[(String, GHContent)] = {
    val contentList = gitHub.searchContent.user(org).filename(searchItem).list.asScala.toList
    contentList.map{ content =>
      (content.getOwner.getName concat content.getPath.replaceAll("""[^a-zA-Z\d\.]""",""), content)
    }.filter(_._1.endsWith("xml"))
  }

  def writeMapToFiles(pomMap: Map[String, GHContent]): Unit = {
    pomMap.foreach{ pair =>
      val in = scala.io.Source.fromInputStream(pair._2.read)
      val output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("pom_collection/" + pair._1)))
      in.getLines().foreach(line => output.write(line))
      output.close()
      in.close()
    }
  }

}

object Test extends App {
  val retriever: GithubOrgRetriever = GithubOrgRetriever("https://github.cerner.com/api/v3", "", "")
  val repoList: List[GHRepository] = retriever.orgRepositories("")
  val pomName: String = "pom.xml"
  val pomMap: Map[String, GHContent] = retriever.searchOrg("", pomName).toMap
  retriever.writeMapToFiles(pomMap)
}