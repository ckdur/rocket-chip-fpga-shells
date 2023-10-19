package sifive.fpgashells.shell.altera

import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

class AlteraShellTcl(val name: String)
{
  private var constraints: Seq[() => String] = Nil
  protected def addConstraint(command: => String) { constraints = (() => command) +: constraints }
  ElaborationArtefacts.add(name, constraints.map(_()).reverse.mkString("\n") + "\n")

  def addBoardPin(io: IOPin, pin: String) {} // TODO: Unimplemented
  def addPackagePin(io: IOPin, pin: String) {
    addConstraint(s"set_location_assignment ${pin} -to ${io.name}")
  }
  def addIOStandard(io: IOPin, standard: String) {
    addConstraint(s"set_instance_assignment -name IO_STANDARD \"${standard}\" -to ${io.name}")
  }
  def addPullup(io: IOPin) {} // TODO: Unimplemented
  def addIOB(io: IOPin) {} // TODO: Unimplemented
  def addSlew(io: IOPin, speed: String) {} // TODO: Unimplemented
  def addTermination(io: IOPin, kind: String) {
    if(io.isInput) addConstraint(s"set_instance_assignment -name INPUT_TERMINATION \"SERIES ${kind}\" -to ${io.name}")
    if(io.isOutput) addConstraint(s"set_instance_assignment -name OUTPUT_TERMINATION \"PARALELL ${kind}\" -to ${io.name}")
  }
  def clockDedicatedRouteFalse(io: IOPin) {} // TODO: Unimplemented
  def addDriveStrength(io: IOPin, drive: String) {} // TODO: Unimplemented
  def addIbufLowPower(io: IOPin, value: String) {} // TODO: Unimplemented
}

class AlteraSDC(name: String) extends SDC(name) {
  // A version of the SDC but for Quartus, as SDC quartus does not support some stuff
  override def addClock(name: => String, pin: => IOPin, freqMHz: => Double, jitterNs: => Double = 0.5) {
    addRawClock(s"create_clock -name ${name} -period ${1000 / freqMHz} ${pin.sdcPin}")
    // addRawClock(s"set_input_jitter ${name} ${jitterNs}") // Not supported
  }

  override def addGroup(clocks: => Seq[String] = Nil, pins: => Seq[IOPin] = Nil): Unit = {
    // Unimplemented
  }
}

abstract class AlteraShell()(implicit p: Parameters) extends IOShell
{
  val sdc = new AlteraSDC("shell.sdc")
  val tdc = new AlteraShellTcl("shell.quartus.tcl")
  def pllReset: ModuleValue[Bool]

  // TODO: Do we need to add another shell.quartus.tcl?
}