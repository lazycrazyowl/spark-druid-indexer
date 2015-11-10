package io.druid.cli.spark

import com.google.common.base.Joiner
import com.google.common.collect.Lists
import com.google.inject.Inject
import com.metamx.common.logger.Logger
import io.airlift.command.Arguments
import io.airlift.command.Command
import io.airlift.command.Option
import io.druid.cli.Main
import io.druid.guice.ExtensionsConfig
import io.druid.initialization.Initialization
import io.tesla.aether.internal.DefaultTeslaAether
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.{Arrays => jArrays}
import java.util.{List => jList}

/**
  */
@Command (
name = "spark",
description = "Runs the batch Spark Druid Indexer, see " +
  "https://github.com/SparklineData/spark-druid-indexer for a description."
)
class CliSparkIndexer extends Runnable {

  import CliSparkIndexer._

  private val log: Logger = new Logger(classOf[CliSparkIndexer])

  @Arguments(description = "A JSON object or the path to a file that contains a JSON object",
    required = true)
  private var argumentSpec: String = null

  @Option(name = Array("-c", "--coordinate", "hadoopDependencies"),
    description = "extra dependencies to pull down (e.g. non-default hadoop coordinates or extra hadoop jars)")
  private var coordinates: java.util.List[String] = null

  @Option(name = Array("--no-default-hadoop"),
    description = "don't pull down the default hadoop version (currently org.apache.hadoop:hadoop-client:2.3.0)",
    required = false)
  var noDefaultHadoop: Boolean = false

  @Inject private var extensionsConfig: ExtensionsConfig = null

  @SuppressWarnings(Array("unchecked")) def run {
    import scala.collection.JavaConversions._

    try {
      val allCoordinates: jList[String] = Lists.newArrayList[String]

      if (coordinates != null) {
        allCoordinates.addAll(coordinates)
      }
      if (!noDefaultHadoop) {
        allCoordinates.add(DEFAULT_HADOOP_COORDINATES)
      }
      val aetherClient: DefaultTeslaAether = Initialization.getAetherClient(extensionsConfig)
      val extensionURLs: jList[URL] = Lists.newArrayList[URL]

      for (coordinate <- extensionsConfig.getCoordinates) {
        val coordinateLoader: ClassLoader =
          Initialization.getClassLoaderForCoordinates(aetherClient, coordinate,
            extensionsConfig.getDefaultVersion)
        extensionURLs.addAll(jArrays.asList(
          (coordinateLoader.asInstanceOf[URLClassLoader]).getURLs:_*))
      }

      val nonHadoopURLs: jList[URL] = Lists.newArrayList[URL]
      nonHadoopURLs.addAll(jArrays.asList((
        classOf[CliSparkIndexer].getClassLoader.asInstanceOf[URLClassLoader]).getURLs:_*))
      nonHadoopURLs.addAll(jArrays.asList((
        classOf[Main].getClassLoader.asInstanceOf[URLClassLoader]).getURLs:_*))

      val driverURLs: jList[URL] = Lists.newArrayList[URL]
      driverURLs.addAll(nonHadoopURLs)
      // put hadoop dependencies last to avoid jets3t & apache.httpcore version conflicts
      import scala.collection.JavaConversions._
      for (coordinate <- allCoordinates) {
        val hadoopLoader: ClassLoader = Initialization.getClassLoaderForCoordinates(aetherClient,
          coordinate, extensionsConfig.getDefaultVersion)
        driverURLs.addAll(jArrays.asList((hadoopLoader.asInstanceOf[URLClassLoader]).getURLs:_*))
      }

      val loader: URLClassLoader = new URLClassLoader(
        driverURLs.toArray(new Array[URL](driverURLs.size)), null)
      Thread.currentThread.setContextClassLoader(loader)

      val jobUrls: jList[URL] = Lists.newArrayList[URL]
      jobUrls.addAll(nonHadoopURLs)
      jobUrls.addAll(extensionURLs)

      System.setProperty("druid.hadoop.internal.classpath",
        Joiner.on(File.pathSeparator).join(jobUrls))

      val mainClass: Class[_] = loader.loadClass(classOf[Main].getName)
      val mainMethod: Method = mainClass.getMethod("main", classOf[Array[String]])

      val args: Array[String] = Array[String]("internal", "hadoop-indexer", argumentSpec)

      mainMethod.invoke(null, args)

    }
    catch {
      case e: Exception => {
        log.error(e, "failure!!!!")
        System.exit(1)
      }
    }
  }
}

object CliSparkIndexer {
  private val DEFAULT_HADOOP_COORDINATES: String = "org.apache.hadoop:hadoop-client:2.3.0"

}
