package sifive.fpgashells.shell.lattice

import chisel3._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import org.chipsalliance.diplomacy.lazymodule._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

class IO_LPF(val name: String)
{
  private var constraints: Seq[() => String] = Nil
  protected def addConstraint(command: => String) { constraints = (() => command) +: constraints }
  private val header =
    """BLOCK RESETPATHS;
      |BLOCK ASYNCPATHS;
      |## ULX3S v2.x.x and v3.0.x
      |
      |# JTAG and SPI FLASH voltage 3.3V and options to boot from SPI flash
      |# write to FLASH possible any time from JTAG:
      |#SYSCONFIG CONFIG_IOVOLTAGE=3.3 COMPRESS_CONFIG=ON MCCLK_FREQ=62 MASTER_SPI_PORT=ENABLE SLAVE_SPI_PORT=DISABLE SLAVE_PARALLEL_PORT=DISABLE;
      |# write to FLASH possible from user bitstream:
      |SYSCONFIG CONFIG_IOVOLTAGE=3.3 COMPRESS_CONFIG=ON MCCLK_FREQ=62 MASTER_SPI_PORT=DISABLE SLAVE_SPI_PORT=DISABLE SLAVE_PARALLEL_PORT=DISABLE;
      |
      |""".stripMargin
  ElaborationArtefacts.add(name, header + constraints.map(_()).reverse.mkString("\n") + "\n")

  def addPackagePin(io: IOPin, pin: String) {
    addConstraint(s"LOCATE COMP \"${io.name}\" SITE \"${pin}\";")
  }
  def addIOStandard(io: IOPin, standard: String, pullMode: String = "NONE", drive: Option[Int] = None) {
    val driveStr = drive.map(a => s" DRIVE=$a").getOrElse("")
    addConstraint(s"IOBUF PORT \"${io.name}\" PULLMODE=${pullMode} IO_TYPE=${standard}${driveStr};")
  }
  def addFrequency(io: IOPin, freqMHz: Double) {
    addConstraint(s"FREQUENCY PORT \"${io.name}\" ${freqMHz} MHZ;")
  }
}

abstract class LatticeShell()(implicit p: Parameters) extends IOShell
{
  val sdc = new SDC("shell.sdc")
  val lpf = new IO_LPF("assign.tcl")
  def pllReset: ModuleValue[Bool]

  val pllFactory = new PLLFactory(this, 4, ecp5pll.apply)

  override def designParameters = super.designParameters.alterPartial {
    case PLLFactoryKey => pllFactory
  }
}


/*
   Copyright 2016 SiFive, Inc.
   Copyright 2026 Ckristian Duran

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
