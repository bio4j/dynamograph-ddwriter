package ddwriter

import ohnosequences.nisperon.bundles.NisperonMetadataBuilder
import ohnosequences.nisperon.console.Server
import ohnosequences.nisperon._
import ohnosequences.nisperon.ListMonoid
import ohnosequences.nisperon.JsonSerializer
import ohnosequences.nisperon.NisperoConfiguration
import ohnosequences.nisperon.queues.{unitQueue, ProductQueue}
import com.bio4j.dynamograph.parser.{PullGoParser, SingleElement}
import com.bio4j.dynamograph.ServiceProvider
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import ohnosequences.nisperon.logging.S3Logger
import scala.io.Source
import ohnosequences.awstools.ec2.{InstanceType, InstanceSpecs}
import java.io.File
import ohnosequences.awstools.autoscaling.OnDemand

object mapSingleElementInstructions extends Instructions[List[SingleElement], List[PutItemRequest]] {

  type Context = Unit

  override def prepare() {}

  override def solve(input: List[SingleElement], logger: S3Logger, context: Context): List[List[PutItemRequest]] = {
    logger.info("current folder: " + new File(".").getAbsolutePath)
    logger.info("input: " + input)
    logger.uploadFile(new File("/etc/fstab"))
    List(ServiceProvider.mapper.map(input.head))
  }
}

object uploadInstructions extends Instructions[List[PutItemRequest], Unit] {

  type Context = Unit

  override def prepare() {}

  override def solve(input: List[PutItemRequest], logger: S3Logger, context: Context): List[Unit] = {
    logger.info("input: " + input)
    logger.info("parent: " + Tasks.parent("parent.test2"))
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
    serializer = new JsonSerializer[List[PutItemRequest]]
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
    singleElements.initWrite()
    val parser = new PullGoParser(Source.fromFile("/home/ec2-user/go.owl"))
    for (output <- parser){
      putItemRequest.put("0", "", List(List(output)))
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