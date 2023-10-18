package sifive.fpgashells.shell.altera

import chisel3._
import chisel3.experimental.attach
import chisel3.util.Cat
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class SPIAlteraPlacedOverlay(name: String, di: SPIDesignInput, si: SPIShellInput)
  extends SPIPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    val sck = Module(new ALT_IOBUF)
    sck.asOutput(tlspiSink.bundle.sck)
    attach(sck.io.io, io.spi_clk)
    val cs = Module(new ALT_IOBUF)
    cs.asOutput(tlspiSink.bundle.cs(0))
    attach(cs.io.io, io.spi_cs)

    tlspiSink.bundle.dq.zip(io.spi_dat).foreach { case (design_dq, io_dq) =>
      val dq = Module(new ALT_IOBUF)
      dq.io.i := design_dq.o
      dq.io.oe := design_dq.oe
      design_dq.i := dq.io.o
      attach(sck.io.io, io_dq)
    }
  } }
}
