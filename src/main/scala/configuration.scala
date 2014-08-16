package ddwriter

import ohnosequences.nisperon.bundles.NisperonMetadataBuilder
import ohnosequences.nisperon.console.Server
import ohnosequences.nisperon._
import ohnosequences.nisperon.JSON
import ohnosequences.nisperon.queues.{unitQueue, ProductQueue}
import com.bio4j.dynamograph.parser.SingleElement
import com.bio4j.dynamograph.parser.go._
import com.bio4j.dynamograph.ServiceProvider
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest}
import ohnosequences.nisperon.logging.S3Logger
import scala.io.Source
import ohnosequences.awstools.ec2.{InstanceType, InstanceSpecs}
import java.io._
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import ohnosequences.awstools.autoscaling.OnDemand
import com.bio4j.dynamograph.parser.SingleElement
import scala.Some
import ohnosequences.nisperon.SingleGroup

case class PutItemR(val name: String, attributes : Map[String,String], val returnedValues : String, val returnedConsumedCapacity :String, val conditionalOperator : String)

object PutItemRequestSerializer extends Serializer[List[PutItemRequest]] {

  def toStringMap(map : Map[String,AttributeValue]) = map.mapValues[String](_.getS)

  def fromStringMap(map : Map[String,String]) = map.mapValues(new AttributeValue().withS(_))

  def fromString(s: String): List[PutItemRequest] = {
    val putItems = JSON.extract[List[PutItemR]](s)
    val result = putItems.map(x => {
      new PutItemRequest().withTableName(x.name).withItem(fromStringMap(x.attributes)).withReturnValues(x.returnedValues).withReturnConsumedCapacity(x.returnedConsumedCapacity).withConditionalOperator(x.conditionalOperator)
    })
    result
  }

  def toString(request: List[PutItemRequest]): String = {
    val  converted = request.map(x => PutItemR(x.getTableName,toStringMap(x.getItem.toMap),x.getReturnValues, x.getReturnConsumedCapacity, x.getConditionalOperator))
    JSON.toJSON(converted)
  }
}


object mapSingleElementInstructions extends Instructions[List[SingleElement], List[PutItemRequest]] {

  type Context = Unit

  override def prepare() {}

  override def solve(input: List[SingleElement], logger: S3Logger, context: Context): List[List[PutItemRequest]] = {
    logger.info("current folder: " + new File(".").getAbsolutePath)
    logger.info("input: " + input)
    List(ServiceProvider.mapper.map(input.head))
  }
}

object uploadInstructions extends Instructions[List[PutItemRequest], Unit] {

  type Context = Unit

  override def prepare() {}

  override def solve(input: List[PutItemRequest], logger: S3Logger, context: Context): List[Unit] = {
    logger.info("input: " + input)
    ServiceProvider.dynamoDbExecutor.execute(input) :: Nil
  }
}

object DynamograpDistributedWriting extends Nisperon{

  override val nisperonConfiguration = NisperonConfiguration(
    metadataBuilder = new NisperonMetadataBuilder(new generated.metadata.ddwriter()),
    email = "alberskib@gmail.com",
    metamanagerGroupConfiguration = SingleGroup(InstanceType.t1_micro, OnDemand),
    password = "3da585db8",
    autoTermination = false,
    defaultInstanceSpecs = InstanceSpecs(
      instanceType = InstanceType.T1Micro,
      amiId = "",
      securityGroups = List("nispero"),
      keyName = "nispero",
      instanceProfile = Some("compota"),
      deviceMapping = Map("/dev/xvdb" -> "ephemeral0")
      )
  )

  val singleElements = s3queue(
    name = "singleElements",
    monoid = new ListMonoid[SingleElement],
    serializer = new JsonSerializer[List[SingleElement]]
  )

  val putItemRequest = s3queue(
    name = "putItemRequests",
    monoid = new ListMonoid[PutItemRequest],
    serializer = PutItemRequestSerializer
  )

  val mapNispero = nispero(
    inputQueue = singleElements,
    outputQueue = putItemRequest,
    instructions = mapSingleElementInstructions,
    nisperoConfiguration = NisperoConfiguration(nisperonConfiguration, "map")
  )

  val uploadNispero = nispero(
    inputQueue = putItemRequest,
    outputQueue = unitQueue,
    instructions = uploadInstructions,
    nisperoConfiguration = NisperoConfiguration(nisperonConfiguration, "upload")
  )

  override def addTasks(): Unit = {
    singleElements.init()
    singleElements.initWrite()
    var t = 0
    val namespaceParser = new PullGoNamespaceParser(Source.fromFile("/home/ec2-user/go_namespace.txt"))
    for (output <- namespaceParser){
      singleElements.put(s"n_$t", "", List(List(output)))
      t = t+1
    }
    t = 0
    val parser = new PullGoParser(Source.fromFile("/home/ec2-user/go.owl"))
    for (output <- parser){
      singleElements.put(s"g_$t", "", List(List(output)))
      t = t+1
    }
  }

  override def undeployActions(solved: Boolean): Option[String] = {None}

  override def checks(): Unit = {}

  override def additionalHandler(args: List[String]): Unit = {
    args match {
      case "console" :: Nil => {
        logger.info("running console")
        new Server(DynamograpDistributedWriting).start()
      }
    }
  }

}