package sifive.fpgashells.shell.altera

import freechips.rocketchip.diplomacy._
import sifive.fpgashells.shell._

abstract class JTAGDebugAlteraPlacedOverlay(name: String, di: JTAGDebugDesignInput, si: JTAGDebugShellInput)
  extends JTAGDebugPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    jtagDebugSink.bundle.TCK := AnalogToUInt(io.jtag_TCK).asBool.asClock
    jtagDebugSink.bundle.TMS := AnalogToUInt(io.jtag_TMS)
    jtagDebugSink.bundle.TDI := AnalogToUInt(io.jtag_TDI)
    UIntToAnalog(jtagDebugSink.bundle.TDO.data,io.jtag_TDO,jtagDebugSink.bundle.TDO.driven)
    jtagDebugSink.bundle.srst_n := AnalogToUInt(io.srst_n)
  } }
}