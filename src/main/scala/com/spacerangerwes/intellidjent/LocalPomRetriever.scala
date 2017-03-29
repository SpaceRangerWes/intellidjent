package com.spacerangerwes.intellidjent

import java.io.File

/**
  * Utility object used to retrieve POM files on the local file system
  */
object LocalPomRetriever {

  /**
    * Find all files in the directory given and all subdirectories
    * @param dir
    *            root directory in which to start listing files
    * @return
    *         Array[File]
    */
  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these: Array[File] = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }

  /**
    * Find all POM files in the file path given and all possible subdirectories
    * @param filePath
    *                 root path in which to start searching for POM files
    * @return
    *         Array[File]
    */
  def findLocalPoms(filePath: File): Array[File] = {
    val fileArr: Array[File] = getRecursiveListOfFiles(filePath)
    fileArr.filter(_.getName.toLowerCase.endsWith("pom.xml"))
  }

  /**
    * Transform all POM files rooted in the provided path to INodes
    * @param filePath
    *                 root path of POM files
    * @return
    *         Array[INode]
    */
  def pomToINode(filePath: String): Array[INode] = {
    val rootDir = new File(filePath)
    val pomList = findLocalPoms(rootDir)

    pomList.map{ pom => INode(pom)}
  }
}
