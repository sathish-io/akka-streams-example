package sample.stream

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import akka.stream.actor.ActorSubscriberMessage.{ OnError, OnComplete, OnNext }
import akka.stream.actor._
import akka.stream.scaladsl.{ Sink, Source }

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

/**
 * Created by skandasa on 8/15/15.
 */

object StreamSenderActor {
  val props: Props = Props[StreamSenderActor]
}

class StreamSenderActor extends ActorPublisher[String] with ActorLogging {

  val buf = new mutable.Queue[String]()

  override def receive: Receive = {
    case message: String =>
      log.info(s"[StreamSenderActor]Received message: ${message}")
      buf += message
      log.info(s"Buffer size ${buf.size}")
    case Request(count) =>
      log.info("Received 'Request' message")
      sendItem()
    case Cancel =>
      log.info("StreamSenderActor cancelled")
      context.stop(self)
    case _ =>
  }
  def sendItem(): Unit = {
    while (isActive && totalDemand > 0) {
      if (buf.size > 0) {
        log.info("Sending item to down stream....")
        onNext(buf.dequeue())
      }
    }
  }
}


class StreamReceiverActor extends ActorSubscriber with ActorLogging {
  //  override protected def requestStrategy: RequestStrategy = new MaxInFlightRequestStrategy(max = 100) {
  //    override def inFlightInternally: Int = 100
  //  }

  val requestStrategy = WatermarkRequestStrategy(50)

  override def receive: Actor.Receive = {
    case OnNext(message: String) =>
      log.info(s"Stream Message Received ${message}")
      Thread.sleep(200)
    case OnError(ex: Exception) =>
      log.error(ex, "Exception occoured in StreamReceiverActor")
      context.stop(self)
    case OnComplete =>
      log.info("StreamReceiverActor completed")
    case _ =>
  }
}

object StreamSenderMain extends App {

  implicit val system = ActorSystem("my-stream-system")
  implicit val mat = ActorMaterializer()
  implicit val exec = ExecutionContext

  println("Started")

  val pubRef = system.actorOf(Props[StreamSenderActor])
  val publisher = ActorPublisher[String](pubRef)

  val subRef = system.actorOf(Props[StreamReceiverActor])
  val subscriber = ActorSubscriber[String](subRef)

  pubRef ! "Hello"

  Source(publisher).map(_.toUpperCase).to(Sink(subscriber)).run()

  //  publisher.subscribe(subscriber)

  pubRef ! "World"
  //  val pubRef = Source.actorPublisher[String](StreamSenderActor.props)
  //  val subRef = Sink.actorSubscriber[String](Props[StreamReceiverActor])
  //
  //  //  val publisher = ActorPublisher[String](pubRef)
  //
  //  val ref = Flow[String].map(_.toUpperCase())
  //    .map(x => { println(x); x })
  //    .to(subRef).runWith(pubRef)

  //  val ref = Source.actorPublisher[String](Props[StreamSenderActor]).map(x => {
  //    println(x); x.toUpperCase()
  //  }).runWith(Sink.actorSubscriber[String](Props[StreamSenderActor]))
  //
  //  println("Sending message to actor")
  //  ref ! "Hello World!"
  //  ref ! "Sathish"
  //  ref ! "Kumar"

  //  val source = Source(1 to 10)
  ////  val sink = Sink.fold[Int, Int](0)(_ + _)
  //  val sink = Sink.actorSubscriber[String](Props[StreamSenderActor])
  //  val sink = Sink.foreach(println)
  //  val runnable = source.runWith(sink)
  //  val runnable = source.toMat(sink)
  //
  //  val sum: Future[Int] = runnable.run()
  //
  //  println("Result: " + sum)
  //
  //  //  val result = Await.result(sum, Duration(10))
  //  //  println(result)
  //
  //  sum.onComplete {
  //    case Success(x) => {
  //      println(x)
  //    }
  //    case Failure(exception) => {
  //      //Do something with my error
  //    }
  //  }

  //
  //  sum.onComplete{
  //    case Success
  //  }

  //  val result = Source[Int](1 to 100).map(i => i%2 ==0).toMat(Sink.fold[Int, Int](0)(_ + _))(Keep.right).run()
  //  print(s"Result ${result}")

  println("Ended")
}
