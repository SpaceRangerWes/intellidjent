package com.spacerangerwes

import java.io.File

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node, NodeSeq, XML}


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

  def getGroupDependencies(groupId: String): Option[NodeSeq] = {
    if (dependencies.isFailure) None
    else {
      if ((dependencies.get \\ "groupId").text contains groupId) Some(dependencies.get)
      else None
    }
  }

  def appendNodeDependencies(iNode: INode): Unit = {
    dependencies = Try(dependencies.get ++ iNode.dependencies.get)
  }


}


