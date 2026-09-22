package sifive.fpgashells.shell.lattice

import freechips.rocketchip.diplomacy._
import sifive.fpgashells.shell._

abstract class LEDLatticePlacedOverlay
(
  name: String,
  di: LEDDesignInput,
  si: LEDShellInput,
  packagePin: Option[String] = None,
  ioStandard: String = "LVCMOS33",
  pullmode: String = "DOWN",
  drive: Option[Int] = Some(4)
) extends LEDPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    io := ledWire // could/should put OBUFs here?

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOBUF(io, ioStandard, pullmode, drive)
    }
  } }
}