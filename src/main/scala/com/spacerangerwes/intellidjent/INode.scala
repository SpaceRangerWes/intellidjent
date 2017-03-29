package com.spacerangerwes.intellidjent

import java.io.File

import scala.util.Try
import scala.xml.{Elem, NodeSeq, XML}

/**
  * Intellidjent Node that contains relevant information from a POM file to build a graph of INodes
  * @param xmlPath
  *                location of the POM file
  */
case class INode(xmlPath: File) {
  val xml: Elem = XML.loadFile(xmlPath)
  val parent: Try[NodeSeq] = Try(xml \\ "parent")
  val children: Try[NodeSeq] = Try(xml \\ "modules" \ "module")
  val groupId: Try[NodeSeq] = Try(xml \ "groupId")
  val artifactId: Try[NodeSeq] = Try(xml \ "artifactId")
  val version: Try[NodeSeq] = Try(xml \ "version")
  val dependencyManagement: Try[NodeSeq] = Try(xml \\ "dependencyManagement")
  var dependencies: Try[NodeSeq] = Try(xml \\ "dependencies" \ "dependency")
  var scmUrl: Try[NodeSeq] = Try(xml \\ "scm.url")

  /**
    * Retrieve all dependencies that contain the given substring in its groupId
    * @param substring
    *                  substring to search against all dependency groupIds
    * @return
    *         Option[NodeSeq]
    */
  def getGroupDependencies(substring: String): Option[NodeSeq] = {
    if (dependencies.isFailure) None
    else {
      if ((dependencies.get \\ "groupId").text contains substring) Some(dependencies.get)
      else None
    }
  }

  /**
    * Add dependencies of an INode to this INode's dependency variable
    * @param iNode
    *              INode containing dependencies to add
    */
  def appendNodeDependencies(iNode: INode): Unit = {
    dependencies = Try(dependencies.get ++ iNode.dependencies.get)
  }


}


