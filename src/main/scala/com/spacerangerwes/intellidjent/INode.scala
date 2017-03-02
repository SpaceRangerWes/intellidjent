package com.spacerangerwes

import java.io.File

import com.sun.net.httpserver.Authenticator.Success

import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}
import scala.xml.{Node, NodeSeq, XML}


class INode(xmlPath: File) {
  val xml = XML.loadFile(xmlPath)
  val parent: Try[NodeSeq] = Try(xml \\ "parent")
  val children: Try[NodeSeq] = Try(xml \\ "modules")
  val groupId: Try[NodeSeq] = Try(xml \ "groupId")
  val artifactId: Try[NodeSeq] = Try(xml \ "artifactId")
  val version: Try[NodeSeq] = Try(xml \ "version")
  val dependencyManagement: Try[NodeSeq] = Try(xml \\ "dependencyManagement")
  val dependencies: Try[NodeSeq] = Try(xml \\ "dependencies" \ "dependency")

  def getGroupDependencies(groupId: String): Option[NodeSeq] = {
    if (dependencies.isFailure) None
    else {
      if ((dependencies.get \\ "groupId").text contains groupId) Some(dependencies.get)
      else None
    }
  }
}


