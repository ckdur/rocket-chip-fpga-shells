package sifive.fpgashells.shell.altera

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class ButtonAlteraPlacedOverlay(name: String, di: ButtonDesignInput, si: ButtonShellInput, boardPin: Option[String] = None, packagePin: Option[String] = None, ioStandard: String = "1.5V")
  extends ButtonPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    buttonWire := io

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, ioStandard)
    }
  } }
}
