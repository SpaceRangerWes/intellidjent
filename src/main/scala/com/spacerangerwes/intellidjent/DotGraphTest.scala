package com.spacerangerwes.intellidjent

/**
  * Created by wesleyhoffman on 3/2/17.
  */
object DotGraphTest extends App {
  import scalax.collection.Graph
  import scalax.collection.edge.LDiEdge, scalax.collection.edge.Implicits._
  import scalax.collection.io.dot._
  import implicits._

  val g = Graph[String, LDiEdge](("A1"~+>"A2")("f"))
  val root = DotRootGraph(directed = true,
    id       = Some("Wikipedia_Example"))
  def edgeTransformer(innerEdge: Graph[String,LDiEdge]#EdgeT):
  Option[(DotGraph,DotEdgeStmt)] = innerEdge.edge match {
    case LDiEdge(source, target, label) => label match {
      case label: String =>
        Some((root,
          DotEdgeStmt(source.toString,
            target.toString,
            if (label.nonEmpty) List(DotAttr("label", label.toString))
            else                Nil)))
    }}
  print(g.toDot(root, edgeTransformer))
}
