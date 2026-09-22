package sifive.fpgashells.shell.lattice

import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

abstract class JTAGDebugLatticePlacedOverlay(name: String, di: JTAGDebugDesignInput, si: JTAGDebugShellInput)
  extends JTAGDebugPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    jtagDebugSink.bundle.TCK := BB(io.jtag_TCK).asBool.asClock
    jtagDebugSink.bundle.TMS := BB(io.jtag_TMS)
    jtagDebugSink.bundle.TDI := BB(io.jtag_TDI)
    BB(jtagDebugSink.bundle.TDO.data,io.jtag_TDO,jtagDebugSink.bundle.TDO.driven)
    jtagDebugSink.bundle.srst_n := BB(io.srst_n)
  } }
}