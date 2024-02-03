package sifive.fpgashells.shell.lattice

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.util.Cat
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._
import sifive.blocks.devices.spi._

class ShellLatticeSPIPortIO extends Bundle {
  val spi_clk = Analog(1.W)
  val spi_cs = Analog(1.W)
  val spi_dat = Vec(4, Analog(1.W))
  val spi_wp = Analog(1.W)
  val spi_cdn = Analog(1.W)
}

abstract class SPILatticePlacedOverlay
(val name: String, val di: SPIDesignInput, val si: SPIShellInput)
  extends IOPlacedOverlay[ShellLatticeSPIPortIO, SPIDesignInput, SPIShellInput, SPIOverlayOutput]
{
  implicit val p = di.p

  def ioFactory = new ShellLatticeSPIPortIO
  val tlspiSink = di.node.makeSink

  val spiSource = BundleBridgeSource(() => new SPIPortIO(di.spiParam))
  val spiSink = sinkScope { spiSource.makeSink }
  def overlayOutput = SPIOverlayOutput()

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

    // The actual spi_dat is the first two spi_dat and the wp dat
    val io_truedat: Seq[Analog] = Seq(io.spi_dat(0), io.spi_dat(1), io.spi_wp, io.spi_cdn)

    // IMPORTANT NOTE: We only take the first 2. dp(2) and dp(3) are not connected at all
    tlspiSink.bundle.dq.zip(io_truedat).take(2).zipWithIndex.foreach { case ((design_dq, io_dq), i) =>
      val dat = Module(new BB).suggestName(s"${name}_dat_${i}_buf")
      dat.io.I := design_dq.o
      dat.io.T := !design_dq.oe
      design_dq.i := dat.io.O
      attach(io_dq, dat.io.B)
    }
  } }
}
