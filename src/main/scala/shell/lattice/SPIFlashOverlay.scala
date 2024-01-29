package sifive.fpgashells.shell.lattice

import chisel3._
import chisel3.experimental.attach
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

abstract class SPIFlashLatticePlacedOverlay
(name: String, di: SPIFlashDesignInput, si: SPIFlashShellInput)
  extends SPIFlashPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    val sck = BB.apply.suggestName(s"${name}_sck_buf")
    attach(io.qspi_sck, sck.io.B)
    sck.io.I := tlqspiSink.bundle.sck
    sck.io.T := false.B

    val cs = BB.apply.suggestName(s"${name}_cs_buf")
    attach(io.qspi_cs, cs.io.B)
    cs.io.I := tlqspiSink.bundle.cs(0)
    cs.io.T := false.B

    tlqspiSink.bundle.dq.zip(io.qspi_dq).zipWithIndex.foreach { case ((design_dq, io_dq), i) =>
      val dq = Module(new BB).suggestName(s"${name}_dq_${i}_buf")
      dq.io.I := design_dq.o
      dq.io.T := !design_dq.oe
      design_dq.i := dq.io.O
      attach(io_dq, dq.io.B)
    }
  } }
}
