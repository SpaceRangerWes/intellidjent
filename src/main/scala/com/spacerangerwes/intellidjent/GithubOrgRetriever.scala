package com.spacerangerwes.intellidjent

import java.io._
import java.nio.file.{Files, Paths}
import java.util

import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import org.kohsuke.github.{GHContent, GHRepository, GitHub, PagedIterable}

import scala.collection.parallel.{ParMap, mutable}
import scala.util.Try
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq, XML}


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

  def writeMapToFiles(pomMap: Map[String, GHContent]): Map[String, String] = {
    pomMap.map{ pair =>
      val in = scala.io.Source.fromInputStream(pair._2.read)
      val output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("pom_collection/" + pair._1)))
      in.getLines().foreach(line => output.write(line))
      output.close()
      in.close()

      val outputFile: File = new File("pom_collection/" + pair._1)
      var uglyXml: Elem = XML.loadFile(outputFile)

      def toBeAddedEntry(url: String) = <scm.url>{url}</scm.url>
      def addScmUri(originalXML: Elem, url: String) = {
        originalXML match {
          case e @ Elem(_, _, _, _, configs @ _*) =>
            val changedNodes = configs.map {
              case <properties>{ innerConfigs @ _* }</properties> =>
                <properties> { toBeAddedEntry(url) ++ innerConfigs }</properties>
              case other => other
            }
            e.copy(child = changedNodes)
          case _ => originalXML
        }
      }

      if (Try(uglyXml \\ "scm.url").isFailure) uglyXml = addScmUri(uglyXml, pair._2.getOwner.getHtmlUrl.toString)

      val prettyPrinter = new scala.xml.PrettyPrinter(120, 2)
      val prettyXml: String = prettyPrinter.format(uglyXml)
      FileUtils.writeStringToFile(outputFile, prettyXml)

      (pair._1, pair._2.getUrl)
    }
  }

//  def assertScmUrl(xmlPath: String, scmUrl: String): Unit = {
//    val xml: Elem = XML.loadFile(xmlPath)
//
//    if (xml.attribute("scm.url").isDefined) {
//      xml.attribute("scm.url").get.text = scmUrl
//    }
//  }

}
