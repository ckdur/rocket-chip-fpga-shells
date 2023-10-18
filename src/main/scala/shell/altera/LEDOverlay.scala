package sifive.fpgashells.shell.altera

import freechips.rocketchip.diplomacy._
import sifive.fpgashells.shell._

abstract class LEDAlteraPlacedOverlay(name: String, di: LEDDesignInput, si: LEDShellInput, boardPin: Option[String] = None, packagePin: Option[String] = None, ioStandard: String = "LVCMOS33")
  extends LEDPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    io := ledWire // could/should put OBUFs here?

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, ioStandard)
    }
  } }
}