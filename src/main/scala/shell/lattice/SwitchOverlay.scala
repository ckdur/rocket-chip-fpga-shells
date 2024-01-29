package sifive.fpgashells.shell.lattice

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class SwitchLatticePlacedOverlay
(
  name: String,
  di: SwitchDesignInput,
  si: SwitchShellInput,
  packagePin: Option[String] = None,
  ioStandard: String = "LVCMOS33",
  pullmode: String = "DOWN",
  drive: Option[Int] = Some(4)
) extends SwitchPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    switchWire := io

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOBUF(io, ioStandard, pullmode, drive)
    }
  } }
}
