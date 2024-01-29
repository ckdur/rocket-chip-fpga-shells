package sifive.fpgashells.shell.lattice

import chisel3._
import chisel3.experimental.attach
import chisel3.util.Cat
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

abstract class SPILatticePlacedOverlay
(name: String, di: SPIDesignInput, si: SPIShellInput)
  extends SPIPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    val sck = BB.apply.suggestName(s"${name}_clk_buf")
    attach(io.spi_clk, sck.io.B)
    sck.io.I := tlspiSink.bundle.sck
    sck.io.T := false.B
    val cs0 = BB.apply.suggestName(s"${name}_cs0_buf")
    attach(io.spi_cs, cs0.io.B)
    cs0.io.I := tlspiSink.bundle.cs(0)
    cs0.io.T := false.B

    tlspiSink.bundle.dq.zip(io.spi_dat).zipWithIndex.foreach { case ((design_dq, io_dq), i) =>
      val dat = Module(new BB).suggestName(s"${name}_dat_${i}_buf")
      dat.io.I := design_dq.o
      dat.io.T := !design_dq.oe
      design_dq.i := dat.io.O
      attach(io_dq, dat.io.B)
    }
  } }
}
