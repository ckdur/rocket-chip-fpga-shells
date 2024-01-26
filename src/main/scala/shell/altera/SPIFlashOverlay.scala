package sifive.fpgashells.shell.altera

import chisel3._
import chisel3.experimental.attach
import chisel3.util.Cat
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class SPIFlashAlteraPlacedOverlay(name: String, di: SPIFlashDesignInput, si: SPIFlashShellInput)
  extends SPIFlashPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    val sck = ALT_IOBUF.apply.suggestName(s"${name}_sck_buf")
    attach(io.qspi_sck, sck.io.io)
    sck.io.i := tlqspiSink.bundle.sck
    sck.io.oe := true.B
    // UIntToAnalog(tlqspiSink.bundle.sck, io.qspi_sck, true.B)

    val cs = ALT_IOBUF.apply.suggestName(s"${name}_cs_buf")
    attach(io.qspi_cs, cs.io.io)
    cs.io.i := tlqspiSink.bundle.cs(0)
    cs.io.oe := true.B
    // UIntToAnalog(tlqspiSink.bundle.cs(0), io.qspi_cs, true.B)

    tlqspiSink.bundle.dq.zip(io.qspi_dq).zipWithIndex.foreach { case ((design_dq, io_dq), i) =>
      val dq = Module(new ALT_IOBUF).suggestName(s"${name}_dq_${i}_buf")
      dq.io.i := design_dq.o
      dq.io.oe := design_dq.oe
      design_dq.i := dq.io.o
      attach(io_dq, dq.io.io)
      // UIntToAnalog(design_dq.o, io_dq, design_dq.oe)
      // design_dq.i := AnalogToUInt(io_dq)
    }

    /*val sck = Module(new ALT_IOBUF).suggestName(s"${name}_sck_buf")
    sck.asOutput(tlqspiSink.bundle.sck)
    attach(sck.io.io, io.qspi_sck)
    val cs = Module(new ALT_IOBUF).suggestName(s"${name}_sck_buf")
    cs.asOutput(tlqspiSink.bundle.cs(0))
    attach(cs.io.io, io.qspi_cs)

    tlqspiSink.bundle.dq.zip(io.qspi_dq).zipWithIndex.foreach { case ((design_dq, io_dq), i) =>
      val dq = Module(new ALT_IOBUF).suggestName(s"${name}_dq_${i}_buf")
      dq.io.i := design_dq.o
      dq.io.oe := design_dq.oe
      design_dq.i := dq.io.o
      attach(sck.io.io, io_dq)
    }*/
  } }
}
