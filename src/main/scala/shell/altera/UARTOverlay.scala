package sifive.fpgashells.shell.altera

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class UARTAlteraPlacedOverlay(name: String, di: UARTDesignInput, si: UARTShellInput, flowControl: Boolean)
  extends UARTPlacedOverlay(name, di, si, flowControl)
{
  def shell: AlteraShell

  shell { InModuleBody {
    ALT_IOBUF(tluartSink.bundle.txd, io.txd, true.B)
    tluartSink.bundle.rxd := ALT_IOBUF(io.rxd)
  } }
}