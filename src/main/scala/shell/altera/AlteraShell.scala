package sifive.fpgashells.shell.altera

import chisel3._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import org.chipsalliance.diplomacy.lazymodule._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

class IO_TCL(val name: String)
{
  private var constraints: Seq[() => String] = Nil
  protected def addConstraint(command: => String) { constraints = (() => command) +: constraints }
  ElaborationArtefacts.add(name, constraints.map(_()).reverse.mkString("\n") + "\n")

  def addPackagePin(io: IOPin, pin: String) {
    addConstraint(s"set_location_assignment {${pin}} -to ${io.name}")
  }
  def addIOStandard(io: IOPin, standard: String) {
    addConstraint(s"set_instance_assignment -name IO_STANDARD {${standard}} -to ${io.name}")
  }
  def addPullup(io: IOPin) {
    addConstraint(s"set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to ${io.name}")
  }
  // def addSlew(io: IOPin, speed: String) {
  //   addConstraint(s"set_property SLEW {${speed}} ${io.sdcPin}")
  // }
  def addTermination(io: IOPin, kind: String) {
    if(io.isInput) addConstraint(s"set_instance_assignment -name INPUT_TERMINATION \"PARALLEL ${kind}\" -to ${io.name}")
    if(io.isOutput) addConstraint(s"set_instance_assignment -name OUTPUT_TERMINATION \"SERIES ${kind}\" -to ${io.name}")
  }
  def addDriveStrength(io: IOPin, drive: String) {
    addConstraint(s"set_instance_assignment -name CURRENT_STRENGTH_NEW {${drive}} -to ${io.name}")
  }
  def addGroup(from: IOPin, to: IOPin, group: String): Unit = {
    addConstraint(s"set_instance_assignment -name DQ_GROUP ${group} -from ${from.name} -to ${to.name}")
  }
  def addInterfaceDelay(io: IOPin, value: String = "FLEXIBLE_TIMING"): Unit = {
    addConstraint(s"set_instance_assignment -name MEM_INTERFACE_DELAY_CHAIN_CONFIG ${value} -to ${io.name}")
  }
}

abstract class AlteraGenericShell()(implicit p: Parameters) extends IOShell
{
  val sdc = new AlteraGenericSDC("shell.sdc")
  val io_tcl = new IO_TCL("shell.quartus.tcl")
  def pllReset: ModuleValue[Bool]
}

abstract class AlteraShell()(implicit p: Parameters) extends IOShell
{
  val sdc = new AlteraSDC("shell.sdc")
  val io_tcl = new IO_TCL("assign.tcl")
  def pllReset: ModuleValue[Bool]

  val pllFactory = new AlteraPLLFactory(this, 9, p => Module(new AlteraPLL(p)))

  sdc.addSDCDirective("derive_pll_clocks")
  sdc.addSDCDirective("derive_clock_uncertainty")

  override def designParameters = super.designParameters.alterPartial {
    case PLLFactoryKey => pllFactory
  }
}


/*
   Copyright 2016 SiFive, Inc.
   Copyright 2025 Ckristian Duran

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
