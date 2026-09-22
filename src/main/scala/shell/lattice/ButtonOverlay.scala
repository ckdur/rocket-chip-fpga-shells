package sifive.fpgashells.shell.lattice

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class ButtonLatticePlacedOverlay
(
  name: String, di: ButtonDesignInput,
  si: ButtonShellInput,
  packagePin: Option[String] = None,
  ioStandard: String = "LVCMOS33",
  pullmode: String = "DOWN",
  drive: Option[Int] = Some(4)
) extends ButtonPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    buttonWire := io

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOBUF(io, ioStandard, pullmode, drive)
    }
  } }
}
