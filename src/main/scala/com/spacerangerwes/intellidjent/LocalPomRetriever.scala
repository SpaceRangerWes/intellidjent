package com.spacerangerwes.intellidjent

import java.io.File

import com.spacerangerwes.INode

/**
  * Created by wesleyhoffman on 2/28/17.
  */
object LocalPomRetriever {

  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }

  def findLocalPoms(filePath: File): Array[File] = {
    val fileArr: Array[File] = getRecursiveListOfFiles(filePath)
    fileArr.filter(_.getName.toLowerCase.contains("pom.xml"))
  }

  def pomToINode(filePath: String): Array[INode] = {
    val rootDir = new File(filePath)
    val pomList = findLocalPoms(rootDir)

    pomList.map{ pom => new INode(pom)}
  }
}
