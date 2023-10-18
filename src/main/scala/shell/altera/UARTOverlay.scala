package sifive.fpgashells.shell.altera

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.shell._

abstract class UARTAlteraPlacedOverlay(name: String, di: UARTDesignInput, si: UARTShellInput, flowControl: Boolean)
  extends UARTPlacedOverlay(name, di, si, flowControl)
{
  def shell: AlteraShell

  shell { InModuleBody {
    UIntToAnalog(tluartSink.bundle.txd, io.txd, true.B)
    tluartSink.bundle.rxd := AnalogToUInt(io.rxd)
  } }
}