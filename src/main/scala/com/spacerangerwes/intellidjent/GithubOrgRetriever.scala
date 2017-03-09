package com.spacerangerwes.intellidjent

import java.io._
import java.nio.file.{Files, Paths}
import java.util

import scala.collection.JavaConverters._
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import org.kohsuke.github.{GHContent, GHRepository, GitHub, PagedIterable}

import scala.collection.parallel.mutable


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

  def repositoryPoms(repoList: List[GHRepository], pomName: String): mutable.ParHashMap[GHContent,String] = {
    val pomMap = mutable.ParHashMap.empty[GHContent, String]
    repoList.foreach{ repo =>
      repo.getDirectoryContent("").forEach{ dir =>
        if(dir.getName == pomName) pomMap += (dir -> repo.getName)
      }
    }
    pomMap
  }

  def directoryContents(dirContent: GHContent, repoName: String): mutable.ParHashMap[GHContent, String] = {
    val pomMap = mutable.ParHashMap.empty[GHContent, String]
    if(dirContent.isDirectory) {
      val subDirectories = dirContent.listDirectoryContent().asList().asScala
      val mapSeq: Seq[mutable.ParHashMap[GHContent, String]] = subDirectories.map(sd => directoryContents(sd, repoName))
      mapSeq.reduce(_ ++ _)
    }else{
      pomMap += (dirContent -> repoName)
    }
  }
}

object Test extends App {
  val retriever: GithubOrgRetriever = GithubOrgRetriever("https://github.cerner.com/api/v3", "", "")
  val repoList: List[GHRepository] = retriever.orgRepositories("")
  val pomName: String = "pom.xml"
  val pomMap: mutable.ParHashMap[GHContent, String] = retriever.repositoryPoms(repoList,pomName)
  pomMap.foreach{ pair =>
    val fileName: String = List(pair._2, pair._1.getPath).mkString("-")
    val in = scala.io.Source.fromInputStream(pair._1.read)
    val output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))
    in.getLines().foreach(line => output.write(line))
    output.close()
  }
}