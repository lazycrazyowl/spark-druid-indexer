package io.druid.cli.spark

import io.druid.cli.{CliHadoopIndexer, CliCommandCreator}
import io.airlift.command.{Help, Cli}

class SparkCommandCreator extends CliCommandCreator {

  def addCommands (builder: Cli.CliBuilder[_]) : Unit = {
    val b = builder.asInstanceOf[Cli.CliBuilder[Runnable]]

    b.withGroup("index").withCommand(classOf[CliSparkIndexer])
  }

}
