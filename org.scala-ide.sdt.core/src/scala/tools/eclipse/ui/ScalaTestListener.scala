package scala.tools.eclipse.ui

import java.net.ServerSocket
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.xml.Elem
import scala.xml.XML
import org.xml.sax.SAXException
import java.util.Observable
import scala.xml.NodeSeq

class ScalaTestListener extends Observable with Runnable {

  @volatile
  private var stopped: Boolean = false
  private var serverSocket: ServerSocket = null
  private var connection: Socket = null
  private var in: BufferedReader = null
  @volatile
  private var ready: Boolean = false

  def getPort = serverSocket.getLocalPort
  
  def bindSocket() = {
    serverSocket = new ServerSocket(0)
  }
  
  def run() {
    ready = false
    stopped = false
    try {
      connection = serverSocket.accept()
      in = new BufferedReader(new InputStreamReader(connection.getInputStream))
      while (!stopped || in.ready) {
        var eventRawXml = ""
        var eventXml: Elem = null
        while (eventXml == null && (!stopped || in.ready)) {
          val line = in.readLine
          if (line != null) {
            eventRawXml += line
            try {
              eventXml = XML.loadString(eventRawXml)
            }
            catch {
              case e: SAXException => 
                Thread.sleep(10)
            }
          }
          else
            Thread.sleep(10)
        }
        if (eventXml != null) {
          eventXml.label match {
            case "TestStarting" => 
              send(
                TestStarting(
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  (eventXml \ "testName").text,
                  (eventXml \ "testText").text,
                  stringOpt(eventXml \ "decodedTestName"),
                  locationOpt(eventXml \ "location"),
                  stringOpt(eventXml \ "rerunner"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong)
              )
            case "TestSucceeded" => 
              send(
                TestSucceeded(
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  (eventXml \ "testName").text,
                  (eventXml \ "testText").text,
                  stringOpt(eventXml \ "decodedTestName"),
                  longOpt(eventXml \ "duration"),
                  locationOpt(eventXml \ "location"),
                  stringOpt(eventXml \ "rerunner"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
            case "TestFailed" => 
              send(
                TestFailed(
                  (eventXml \ "message").text,
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  (eventXml \ "testName").text,
                  (eventXml \ "testText").text,
                  stringOpt(eventXml \ "decodedTestName"),
                  stringOpt(eventXml \ "throwable" \ "message"),
                  stringOpt(eventXml \ "throwable" \ "stackTraces"),
                  longOpt(eventXml \ "duration"),
                  locationOpt(eventXml \ "location"),
                  stringOpt(eventXml \ "rerunner"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
            case "TestIgnored" => 
              send(
                TestIgnored(
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  (eventXml \ "testName").text,
                  (eventXml \ "testText").text,
                  stringOpt(eventXml \ "decodedTestName"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
            case "TestPending" => 
              send(
                TestPending(
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  (eventXml \ "testName").text,
                  (eventXml \ "testText").text,
                  stringOpt(eventXml \ "decodedTestName"),
                  longOpt(eventXml \ "duration"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )    
              )
            case "TestCanceled" => 
              send(
                TestCanceled(
                  (eventXml \ "message").text,
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  (eventXml \ "testName").text,
                  (eventXml \ "testText").text,
                  stringOpt(eventXml \ "decodedTestName"),
                  stringOpt(eventXml \ "throwable" \ "message"),
                  stringOpt(eventXml \ "throwable" \ "stackTraces"),
                  longOpt(eventXml \ "duration"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
            case "ScopeOpened" => 
              send(
                ScopeOpened(
                  (eventXml \ "message").text,
                  nameInfo(eventXml \ "nameInfo"),
                  booleanOpt(eventXml \ "aboutAPendingTest"),
                  booleanOpt(eventXml \ "aboutACanceledTest"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
            case "ScopeClosed" => 
              send(
                ScopeClosed (
                  (eventXml \ "message").text,
                  nameInfo(eventXml \ "nameInfo"),
                  booleanOpt(eventXml \ "aboutAPendingTest"),
                  booleanOpt(eventXml \ "aboutACanceledTest"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )    
              )
            case "SuiteStarting" => 
              send(
                SuiteStarting (
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  locationOpt(eventXml \ "location"),
                  stringOpt(eventXml \ "rerunner"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )    
              )
            case "SuiteCompleted" => 
              send(
                SuiteCompleted (
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  longOpt(eventXml \ "duration"),
                  locationOpt(eventXml \ "location"),
                  stringOpt(eventXml \ "rerunner"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )    
              )
            case "SuiteAborted" => 
              send(
                SuiteAborted (
                  (eventXml \ "message").text,
                  (eventXml \ "suiteName").text,
                  (eventXml \ "suiteId").text,
                  stringOpt(eventXml \ "suiteClassName"),
                  stringOpt(eventXml \ "decodedSuiteName"),
                  stringOpt(eventXml \ "throwable" \ "message"),
                  stringOpt(eventXml \ "throwable" \ "stackTraces"),
                  longOpt(eventXml \ "duration"),
                  locationOpt(eventXml \ "location"),
                  stringOpt(eventXml \ "rerunner"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )    
              )
            case "RunStarting" => 
              send(
                RunStarting(
                  (eventXml \ "testCount").text.toInt,
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )    
              )
            case "RunCompleted" => 
              send(
                RunCompleted(
                  longOpt(eventXml \ "duration"),
                  summaryOpt(eventXml \ "summary"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )    
              )
              stop()
            case "RunStopped" => 
              send(
                RunStopped (
                  longOpt(eventXml \ "duration"),
                  summaryOpt(eventXml \ "summary"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                ) 
              )
              stop()
            case "RunAborted" => 
              send(
                RunAborted (
                  (eventXml \ "message").text,
                  stringOpt(eventXml \ "throwable" \ "message"),
                  stringOpt(eventXml \ "throwable" \ "stackTraces"),
                  longOpt(eventXml \ "duration"),
                  summaryOpt(eventXml \ "summary"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
              stop()
            case "InfoProvided" => 
              send(
                InfoProvided (
                  (eventXml \ "message").text,
                  nameInfoOpt(eventXml \ "nameInfo"),
                  booleanOpt(eventXml \ "aboutAPendingTest"),
                  booleanOpt(eventXml \ "aboutACanceledTest"),
                  stringOpt(eventXml \ "throwable" \ "message"),
                  stringOpt(eventXml \ "throwable" \ "stackTraces"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
            case "MarkupProvided" => 
              send(
                MarkupProvided (
                  (eventXml \ "text").text,
                  nameInfoOpt(eventXml \ "nameInfo"),
                  booleanOpt(eventXml \ "aboutAPendingTest"),
                  booleanOpt(eventXml \ "aboutACanceledTest"),
                  locationOpt(eventXml \ "location"),
                  (eventXml \ "threadName").text,
                  (eventXml \ "timeStamp").text.toLong
                )
              )
          }
        }
        Thread.sleep(10)
      }
    }
    finally {
      in.close()
      connection.close()
      ready = true
      println("#######stopped successfully!")
    }
  }
  
  def send(event: Event){
    setChanged()
    notifyObservers(event)
  }
  
  def stop() {
    stopped = true
  }
  
  private def stringOpt(nodeSeq: NodeSeq) = {
    if (nodeSeq.text == "")
      None
    else
      Some(nodeSeq.text)
  }
  
  private def longOpt(nodeSeq: NodeSeq) = {
    if (nodeSeq.text == "")
      None
    else
      Some(nodeSeq.text.toLong)
  }
  
  private def booleanOpt(nodeSeq: NodeSeq) = {
    if (nodeSeq.text == "")
      None
    else
      Some(nodeSeq.text.toBoolean)
  }
  
  private def locationOpt(nodeSeq: NodeSeq) = {
    val nodeOpt = nodeSeq.toSeq.find(node => node.label == "TopOfClass" || node.label == "TopOfMethod" || node.label == "LineInFile" || node.label == "SeeStackDepthException")
    nodeOpt match {
      case Some(node) => 
        node.label match {
          case "TopOfClass" => 
            Some(TopOfClass((node \ "className").text))
          case "TopOfMethod" =>
            Some(TopOfMethod((node \ "className").text, (node \ "methodId").text))
          case "LineInFile" =>
            Some(LineInFile((node \ "lineNumber").text.toInt, (node \ "fileName").text))
          case "SeeStackDepthException" => 
            Some(SeeStackDepthException)
          case _ =>
            None
        }
      case None => 
        None
    }
  }
  
  private def nameInfoOpt(nodeSeq: NodeSeq) = {
    if (nodeSeq.text == "")
      None
    else {
      val testName = 
        if ((nodeSeq \ "testName").text == "")
          None
        else
          Some(TestNameInfo((nodeSeq \ "testName" \ "testName").text, stringOpt(nodeSeq \ "testName" \ "decodedTestName")))
      Some(
        NameInfo(
          (nodeSeq \ "suiteName").text, 
          (nodeSeq \ "suiteId").text, 
          stringOpt(nodeSeq \ "suiteClassName"), 
          stringOpt(nodeSeq \ "decodedSuiteName"),  
          testName)
      )
    }
  }
  
  private def nameInfo(nodeSeq: NodeSeq) = {
    nameInfoOpt(nodeSeq) match {
      case Some(nameInfo) =>
        nameInfo
      case None => 
        null
    }
  }
  
  private def summaryOpt(nodeSeq: NodeSeq) = {
    if (nodeSeq.text == "")
      None
    else {
      Some(
        Summary(
          (nodeSeq \ "testsSucceededCount").text.toInt, 
          (nodeSeq \ "testsFailedCount").text.toInt, 
          (nodeSeq \ "testsIgnoredCount").text.toInt, 
          (nodeSeq \ "testsPendingCount").text.toInt, 
          (nodeSeq \ "testsCanceledCount").text.toInt, 
          (nodeSeq \ "suitesCompletedCount").text.toInt, 
          (nodeSeq \ "suitesAbortedCount").text.toInt)    
      )
    }
  }
}