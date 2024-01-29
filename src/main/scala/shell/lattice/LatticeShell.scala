package sifive.fpgashells.shell.lattice

import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import sifive.fpgashells.shell._

class LatticeShellLpf(val name: String)
{
  private var constraints: Seq[() => String] = Nil
  protected def addConstraint(command: => String) { constraints = (() => command) +: constraints }
  ElaborationArtefacts.add(name, constraints.map(_()).reverse.mkString("\n") + "\n")

  def addPackagePin(io: IOPin, pin: String) {
    addConstraint(s"LOCATE COMP \"${pin}\" SITE \"${io.name}\"")
  }
  def addIOBUF(io: IOPin, standard: String = "LVCMOS33", pullmode: String = "NONE", drive: Option[Int] = None) {
    val drivestr = " " + drive.map(d => s"DRIVE=${d}").getOrElse("")
    addConstraint(s"IOBUF PORT \"${io.name}\" PULLMODE=${pullmode} IO_TYPE=${standard}${drivestr}")
  }
  def addClock(io: => IOPin, freqMHz: => Double) {
    addConstraint(s"FREQUENCY PORT ${io.name} ${freqMHz}")
  }
}

abstract class LatticeShell()(implicit p: Parameters) extends IOShell
{
  val lpf = new LatticeShellLpf("shell.lpf")
  val sdc = new SDC("shell.sdc")
  def pllReset: ModuleValue[Bool]
}
