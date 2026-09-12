package sifive.fpgashells.shell.lattice

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

abstract class ButtonLatticePlacedOverlay(name: String, di: ButtonDesignInput, si: ButtonShellInput, boardPin: Option[String] = None, packagePin: Option[String] = None, ioStandard: String = "LVCMOS33")
  extends ButtonPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    buttonWire := io

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, ioStandard)
    }
  } }
}
