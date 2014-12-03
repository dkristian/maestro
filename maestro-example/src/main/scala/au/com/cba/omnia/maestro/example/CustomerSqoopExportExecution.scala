//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package au.com.cba.omnia.maestro.example

import com.twitter.scalding.{TextLine, TypedPipe, TypedPsv}

import au.com.cba.omnia.maestro.api.exec._
import au.com.cba.omnia.maestro.api.exec.Maestro._
import au.com.cba.omnia.maestro.example.thrift.Customer

import au.com.cba.omnia.parlour.SqoopSyntax.ParlourExportDsl

/** Configuration for `CustomerSqoopExportExecution` */
case class CustomerExportConfig(config: Config) extends MacroSupport[Customer] {
  val maestro = MaestroConfig(
    conf         = config,
    source       = "customer",
    domain       = "customer",
    tablename    = "customer"
  )
  val rawDataDir = s"${maestro.hdfsRoot}/customers"
  val exportDir  = s"${maestro.hdfsRoot}/processed-customers"
  val export     = maestro.sqoopExport[ParlourExportDsl](
    dbTablename  = "customer_export"
  )
}

/** Customer execution, exporting data to a database via Sqoop */
object CustomerSqoopExportExecution extends MacroSupport[Customer] {
  def execute: Execution[Unit] = {
    for {
      conf <- Execution.getConfig.map(CustomerExportConfig(_))
      _    <- TypedPipe.from(TextLine(conf.rawDataDir))
                       .map(Splitter.delimited("|").run(_).init.mkString("|"))
                       .writeExecution(TypedPsv(conf.exportDir))
      _    <- sqoopExport(conf.export, conf.exportDir)
    } yield ()
  }
}