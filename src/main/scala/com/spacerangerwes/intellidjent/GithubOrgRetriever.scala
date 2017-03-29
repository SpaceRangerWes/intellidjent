package com.spacerangerwes.intellidjent

import java.io._

import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._
import org.kohsuke.github.{GHContent, GHRepository, GitHub}

import scala.util.Try
import scala.xml.{Elem, XML}


/**
  * Github Utility that communicates to the Github 3.0 API and retains the organization object that can be used for
  * retrieving POM files from the Github servers
  * @param apiUrl
  *               url of the enterprise github url
  * @param userName
  *                 username required for enterprise github account
  * @param password
  *                 password required for enterprise github account
  */
case class GithubOrgRetriever(apiUrl: String, userName: String, password: String) {

  val gitHub: GitHub = buildRetriever(apiUrl, userName, password)
  var pomOutputDirectory: String = "pom_collection/"

  /**
    * Construct the retriever value
    * @param apiUrl
    *               url of the enterprise github url
    * @param userName
    *                 username required for enterprise github account
    * @param password
    *                 password required for enterprise github account
    * @return
    *         GitHub
    */
  def buildRetriever(apiUrl: String, userName: String, password: String): GitHub = {
    GitHub.connectToEnterprise(apiUrl, userName, password)
  }

  /**
    * List of all repositories belonging to a Github Organization
    * @param name
    *             name of the Organization
    * @return
    *         List[GHRepository]
    */
  def orgRepositories(name: String): List[GHRepository] = {
    gitHub.getOrganization(name).listRepositories().asScala.toList
  }

  def searchOrg(org: String, searchItem: String): List[(String, GHContent)] = {
    val contentList = gitHub.searchContent.user(org).filename(searchItem).list.asScala.toList
    contentList.map{ content =>
      (content.getOwner.getName concat content.getPath.replaceAll("""[^a-zA-Z\d\.]""",""), content)
    }.filter(_._1.endsWith("pom.xml"))
  }

  /**
    * Setter for the download location of the collected POM files
    *
    * Default Value: ./pom_collection/
    * @param pomOutputDirectory
    *                           directory path to save files to
    */
  def setOutputDirectory(pomOutputDirectory: String): Unit = {
    this.pomOutputDirectory = pomOutputDirectory
  }

  /**
    * Write the map containing POM contents to files in a download directory
    * @param pomMap
    *               map of unique POM file identifier and the POM file's contents
    * @return
    *         Map[String, String]
    */
  def writeMapToFiles(pomMap: Map[String, GHContent]): Map[String, String] = {
    pomMap.map{ pair =>
      val in = scala.io.Source.fromInputStream(pair._2.read)
      val output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pomOutputDirectory + pair._1)))
      in.getLines().foreach(line => output.write(line))
      output.close()
      in.close()

      val outputFile: File = new File(pomOutputDirectory + pair._1)
      var uglyXml: Elem = XML.loadFile(outputFile)

      if (Try(uglyXml \\ "scm.url").isFailure) uglyXml = addScmUri(uglyXml, pair._2.getOwner.getHtmlUrl.toString)

      val prettyPrinter = new scala.xml.PrettyPrinter(120, 2)
      val prettyXml: String = prettyPrinter.format(uglyXml)
      FileUtils.writeStringToFile(outputFile, prettyXml)

      (pair._1, pair._2.getUrl)
    }
  }

  /**
    * Add XML tag for the POM's Github URL if one is not present
    * @param originalXML
    *                    unedited POM contents
    * @param url
    *            url of Github repository that POM belongs to
    * @return
    *         Elem
    */
  def addScmUri(originalXML: Elem, url: String): Elem = {
    originalXML match {
      case e @ Elem(_, _, _, _, configs @ _*) =>
        val changedNodes = configs.map {
          case <properties>{ innerConfigs @ _* }</properties> =>
            <properties> { <scm.url>{url}</scm.url> ++ innerConfigs }</properties>
          case other => other
        }
        e.copy(child = changedNodes)
      case _ => originalXML
    }
  }
}
