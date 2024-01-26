package sifive.fpgashells.shell.altera

import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class JTAGDebugAlteraPlacedOverlay(name: String, di: JTAGDebugDesignInput, si: JTAGDebugShellInput)
  extends JTAGDebugPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    jtagDebugSink.bundle.TCK := ALT_IOBUF(io.jtag_TCK).asBool.asClock
    jtagDebugSink.bundle.TMS := ALT_IOBUF(io.jtag_TMS)
    jtagDebugSink.bundle.TDI := ALT_IOBUF(io.jtag_TDI)
    ALT_IOBUF(jtagDebugSink.bundle.TDO.data,io.jtag_TDO,jtagDebugSink.bundle.TDO.driven)
    jtagDebugSink.bundle.srst_n := ALT_IOBUF(io.srst_n)
  } }
}