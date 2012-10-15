package com.github.brianmartin

import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.util.duration._
import scala.concurrent.Await

import scala.collection.mutable.HashSet

case class Node(node: ActorRef, id: Int)
case class Neighbors(children: Seq[ActorRef], parent: ActorRef)
case class NumNodes(n: Int)

class TreeMaster extends Actor with ActorLogging {

  var expectedSize = 0
  var numRegistered = 0
  lazy val nodes = Array.ofDim[ActorRef](expectedSize)
  val senders = new HashSet[ActorRef]

  def receive = {
    case Node(node, id) if numRegistered == expectedSize - 1 => {
      log.info("Recieved last id: {}; node: {}", id, node)
      nodes(id) = node
      senders += sender
      nodes(0) ! Neighbors(nodes.drop(1).take(2), null)
      (1 until nodes.size).foreach { i =>
        val parent = nodes((i - 1) / 2)
        val children = Seq(2*i+1, 2*i+2).filter(_ < nodes.size).map(j => nodes(j)) // include the node in children, important for the broadcast
        nodes(i) ! Neighbors(children, parent)
      }
      senders.foreach { sender => sender ! 1 }
    }
    case Node(node, id) => {
      log.info("Recieved id: {}; node: {}", id, node)
      numRegistered += 1
      nodes(id) = node
      senders += sender
    }
    case NumNodes(n) => {
      expectedSize = n
      log.info("Expected size set to {}", n)
    }
  }

}


case class Up(a: Array[Double])
case class Side(a: Array[Double])
case class Down(a: Array[Double])

class ARActor extends Actor with ActorLogging {

  implicit val timeout = Timeout(5 seconds)

  var children: Seq[ActorRef] = null
  var parent: ActorRef = null
  var side: ActorRef = null
  var totalBeforePassingUp = 0 //children.size + 1
  var recievedFrom = 0
  var workingBuffer: Array[Double] = null

  private def sumIntoBuffer(a: Array[Double]): Unit = {
    if (workingBuffer eq null)
      workingBuffer = Array.ofDim[Double](a.size)
    assert(a.length == workingBuffer.length)
    var i = 0
    while (i < workingBuffer.length) {
      workingBuffer(i) += a(i)
      i += 1
    }
  }

  def receive = {
    case Up(a) if (recievedFrom == totalBeforePassingUp - 1) => { 
      log.info("Passing up to parent...")
      recievedFrom += 1
      sumIntoBuffer(a)
      if (parent != self)
        parent ! Up(workingBuffer)
      else 
        self ! Down(workingBuffer)
    }
    case Up(a) => { 
      log.info("Recieved Up, waiting for the other children...")
      recievedFrom += 1
      sumIntoBuffer(a)
    }
    case Side(a) => {
      log.info("Recieved Side...")
      side = sender
      self ! Up(a)
    }
    case Down(a) => { 
      log.info("Recieved down, passing along to children...")
      val m = Down(a)
      side ! m
      children.foreach { c => c ! m}
      workingBuffer = null
      recievedFrom = 0
    }
    case Neighbors(c, p) => {
      parent = if (p eq null) self else p
      children = c
      totalBeforePassingUp = children.size + 1
      log.info("Set parent: {}; children: {}; childrenSize: {}", parent, children, children.size)
    }
  }

}

class Allreduce(masterHostAndPort: (String, Int), id: Int) {

  implicit val timeout = Timeout(5 seconds)

  var me: ActorRef = null

  private def init(): Unit = {
    val system = ActorSystem("AllreduceSystem",
        ConfigFactory
            .parseString("akka.remote.netty.hostname=\"" + masterHostAndPort._1 + "\"\n akka.remote.netty.port=\"" + (masterHostAndPort._2 + id + 1) + "\"")
            .withFallback(ConfigFactory.load()))

    val master = system.actorFor("akka://AllreduceSystem@" + masterHostAndPort._1 + ":" + masterHostAndPort._2 + "/user/TreeMaster")

    me = system.actorOf(Props[ARActor], name = "AllreduceActor" + id)
    Await.result(master ? Node(me, id), 5 seconds)
    println("Recieved reponse from TreeMaster..")
  }

  def allReduce(a: Array[Double]): Array[Double] = {
    if (me eq null)
      init()
    val future = (me ? Side(a))
    Await.result(future, atMost = 5 seconds).asInstanceOf[Down].a
  }

}

object Runner {

  def main(args: Array[String]): Unit = {

    if (args.size < 2) {
      println("Usage worker: ./run hostname port id")
      println("Usage master: ./run master hostname port numWorkers")
      System.exit(0)
    }

    if (args.size == 4) {

      val host = args(1)
      val port = args(2).toInt
      val n = args(3).toInt

      val system = ActorSystem("AllreduceSystem",
          ConfigFactory
              .parseString("akka.remote.netty.hostname=\"" + host + "\"\n akka.remote.netty.port=\"" + port + "\"")
              .withFallback(ConfigFactory.load()))

      val master = system.actorOf(Props[TreeMaster], name = "TreeMaster")

      master ! NumNodes(n)

    }
    else {

      val host = args(0)
      val port = args(1).toInt
      val id = args(2).toInt

      val a = new Allreduce((host, port), id)

      var i = 1
      while (true) {
        val v = Array.fill[Double](5)(id.toDouble * i)
        println("v: " + v.mkString(", "))
        println(a.allReduce(v).mkString(", "))
        Thread.sleep(300)
        i += 1
      }

    }
  }

}

