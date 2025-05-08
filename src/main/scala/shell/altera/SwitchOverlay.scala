package sifive.fpgashells.shell.altera

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class SwitchAlteraPlacedOverlay(name: String, di: SwitchDesignInput, si: SwitchShellInput, boardPin: Option[String] = None, packagePin: Option[String] = None, ioStandard: String = "2.5V")
  extends SwitchPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    switchWire := io

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.io_tcl.addPackagePin(io, pin)
      shell.io_tcl.addIOStandard(io, ioStandard)
    }
  } }
}
