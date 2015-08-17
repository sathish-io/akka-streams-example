package sample.stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Source, Sink, Tcp }

/**
 * Created by skandasa on 8/14/15.
 */
object TcpProxyServer extends App {

  implicit val system = ActorSystem("on-to-one-proxy")
  implicit val materializer = ActorMaterializer()

  val serverBinding = Tcp().bind("localhost", 6000)

  val sink = Sink.foreach[Tcp.IncomingConnection] { connection =>
    println(s"Client connected from: ${connection.remoteAddress}")
    connection.handleWith(Tcp().outgoingConnection("localhost", 7000))
  }
  val materializedServer = serverBinding.to(sink).run()

}
