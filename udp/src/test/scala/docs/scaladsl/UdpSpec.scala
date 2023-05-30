/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.scaladsl

import java.net.InetSocketAddress
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.io.UdpSO
import pekko.stream.Materializer
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.connectors.udp.Datagram
import pekko.stream.connectors.udp.scaladsl.Udp
import pekko.stream.scaladsl.{ Flow, Keep, Source }
import pekko.stream.testkit.scaladsl.{ TestSink, TestSource }
import pekko.testkit.TestKit
import pekko.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Future
import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UdpSpec
    extends TestKit(ActorSystem("UdpSpec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with LogCapturing {

  implicit val mat: Materializer = Materializer(system)
  implicit val pat: PatienceConfig = PatienceConfig(3.seconds, 50.millis)

  // #bind-address
  val bindToLocal = new InetSocketAddress("localhost", 0)
  // #bind-address

  private def msg(msg: String, destination: InetSocketAddress) =
    Datagram(ByteString(msg), destination)

  override def afterAll() =
    TestKit.shutdownActorSystem(system)

  "UDP stream" must {
    "send and receive messages" in {

      // #bind-flow
      val bindFlow: Flow[Datagram, Datagram, Future[InetSocketAddress]] =
        Udp.bindFlow(bindToLocal)
      // #bind-flow

      val ((pub, bound), sub) = TestSource
        .probe[Datagram](system)
        .viaMat(bindFlow)(Keep.both)
        .toMat(TestSink.probe)(Keep.both)
        .run()

      val destination = bound.futureValue

      {
        // #send-datagrams
        val destination = new InetSocketAddress("my.server", 27015)
        // #send-datagrams
        destination
      }

      // #send-datagrams
      val messagesToSend = 100

      // #send-datagrams

      sub.ensureSubscription()
      sub.request(messagesToSend)

      // #send-datagrams
      Source(1 to messagesToSend)
        .map(i => ByteString(s"Message $i"))
        .map(Datagram(_, destination))
        .runWith(Udp.sendSink())
      // #send-datagrams

      (1 to messagesToSend).foreach { _ =>
        sub.requestNext()
      }
      sub.cancel()
    }

    "send and receive messages with options" in {

      val bindFlow: Flow[Datagram, Datagram, Future[InetSocketAddress]] =
        Udp.bindFlow(bindToLocal, List(UdpSO.broadcast(true)))

      val ((pub, bound), sub) = TestSource
        .probe[Datagram](system)
        .viaMat(bindFlow)(Keep.both)
        .toMat(TestSink.probe)(Keep.both)
        .run()

      val destination = bound.futureValue

      {
        val destination = new InetSocketAddress("my.server", 27015)
        destination
      }

      val messagesToSend = 100

      sub.ensureSubscription()
      sub.request(messagesToSend)

      Source(1 to messagesToSend)
        .map(i => ByteString(s"Message $i"))
        .map(Datagram(_, destination))
        .runWith(Udp.sendSink(List(UdpSO.broadcast(true))))

      (1 to messagesToSend).foreach { _ =>
        sub.requestNext()
      }
      sub.cancel()
    }

    "ping-pong messages" in {
      val ((pub1, bound1), sub1) = TestSource
        .probe[Datagram](system)
        .viaMat(Udp.bindFlow(bindToLocal))(Keep.both)
        .toMat(TestSink.probe)(Keep.both)
        .run()

      val ((pub2, bound2), sub2) = TestSource
        .probe[Datagram](system)
        .viaMat(Udp.bindFlow(bindToLocal))(Keep.both)
        .toMat(TestSink.probe)(Keep.both)
        .run()

      val boundAddress1 = bound1.futureValue
      val boundAddress2 = bound2.futureValue

      sub1.ensureSubscription()
      sub2.ensureSubscription()

      sub2.request(1)
      pub1.sendNext(msg("Hi!", boundAddress2))
      sub2.requestNext().data.utf8String shouldBe "Hi!"

      sub1.request(1)
      pub2.sendNext(msg("Hello!", boundAddress1))
      sub1.requestNext().data.utf8String shouldBe "Hello!"

      sub2.request(1)
      pub1.sendNext(msg("See ya.", boundAddress2))
      sub2.requestNext().data.utf8String shouldBe "See ya."

      sub1.request(1)
      pub2.sendNext(msg("Bye!", boundAddress1))
      sub1.requestNext().data.utf8String shouldBe "Bye!"

      sub1.cancel()
      sub2.cancel()
    }
  }

}
