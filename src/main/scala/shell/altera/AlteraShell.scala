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

  def addPackagePin(io: IOPin, pin: String) {
    addConstraint(s"set_location_assignment ${pin} -to ${io.name}")
  }
  def addIOStandard(io: IOPin, standard: String) {
    addConstraint(s"set_instance_assignment -name IO_STANDARD \"${standard}\" -to ${io.name}")
  }
  def addPullup(io: IOPin) {
    addConstraint(s"set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to ${io.name}")
  }
  def addTermination(io: IOPin, kind: String) {
    if(io.isInput) addConstraint(s"set_instance_assignment -name INPUT_TERMINATION \"PARALLEL ${kind}\" -to ${io.name}")
    if(io.isOutput) addConstraint(s"set_instance_assignment -name OUTPUT_TERMINATION \"SERIES ${kind}\" -to ${io.name}")
  }
  def addDriveStrength(io: IOPin, drive: String): Unit = {
    addConstraint(s"set_instance_assignment -name CURRENT_STRENGTH_NEW \"${drive}\" -to ${io.name}")
  }
  def addGroup(from: IOPin, to: IOPin, group: String): Unit = {
    addConstraint(s"set_instance_assignment -name DQ_GROUP ${group} -from ${from.name} -to ${to.name}")
  }
  def addInterfaceDelay(io: IOPin, value: String = "FLEXIBLE_TIMING"): Unit = {
    addConstraint(s"set_instance_assignment -name MEM_INTERFACE_DELAY_CHAIN_CONFIG ${value} -to ${io.name}")
  }
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

  def addGeneratedClock(name: => String, pinin: => IOPin, opath: String, ratio: Rational, phaseDeg: => Double = 0.0): Unit = {
    // TODO: This can be addDerivedClock() but if supported by multiply
    addRawClock(s"create_generated_clock -add -name \"${name}\" -source ${pinin.sdcPin} -multiply_by ${ratio.num} -divide_by ${ratio.den} -phase ${phaseDeg} ${opath}")
  }

  def addGroupOnlyNames(clocks: => Seq[String] = Nil, pins: => Seq[IOPin] = Nil) {
    def thunk = {
      val clocksList = clocks
      val (pinsList, portsList) = pins.map(_.name.replace('/', '|')).partition(_.contains("|"))
      val sep = " \\\n      "
      val clocksStr = (" [get_clocks " +: clocksList).mkString(sep) + " \\\n    ]"
      val pinsStr   = (" [get_clocks -of_objects [get_pins {"  +: pinsList ).mkString(sep) + " \\\n    }]]"
      val portsStr  = (" [get_clocks -of_objects [get_ports {" +: portsList).mkString(sep) + " \\\n    }]]"
      val str = s"  -group [list${if (clocksList.isEmpty) "" else clocksStr}${if (pinsList.isEmpty) "" else pinsStr}${if (portsList.isEmpty) "" else portsStr}]"
      if (clocksList.isEmpty && pinsList.isEmpty && portsList.isEmpty) "" else str
    }
    addRawGroup(thunk)
  }
}

abstract class AlteraShell()(implicit p: Parameters) extends IOShell
{
  val sdc = new AlteraSDC("shell.sdc")
  val tdc = new AlteraShellTcl("shell.quartus.tcl")
  def pllReset: ModuleValue[Bool]

  // TODO: Do we need to add another shell.quartus.tcl?
}