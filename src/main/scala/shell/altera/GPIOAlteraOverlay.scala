package sifive.fpgashells.shell.altera

import chisel3._
import chisel3.experimental.attach
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class GPIOAlteraPlacedOverlay(name: String, di: GPIODesignInput, si: GPIOShellInput)
  extends GPIOPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    tlgpioSink.bundle.pins.zipWithIndex.foreach{ case (tlpin, idx) => {
      val m = Module(new ALT_IOBUF)
      m.fromBase(tlpin.toBasePin())
      attach(m.io.io, io.gpio(idx))
    } }
  } }
}