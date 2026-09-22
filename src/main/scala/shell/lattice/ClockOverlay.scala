package sifive.fpgashells.shell.lattice

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class SingleEndedClockInputLatticePlacedOverlay
(name: String, di: ClockInputDesignInput, si: ClockInputShellInput)
  extends SingleEndedClockInputPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    val (c, _) = node.out(0)
    c.clock := io
    c.reset := shell.pllReset
  } }
}
