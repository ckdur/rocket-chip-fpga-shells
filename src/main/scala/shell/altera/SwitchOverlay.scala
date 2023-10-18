package sifive.fpgashells.shell.altera

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class SwitchAlteraPlacedOverlay(name: String, di: SwitchDesignInput, si: SwitchShellInput, boardPin: Option[String] = None, packagePin: Option[String] = None, ioStandard: String = "2.5V")
  extends SwitchPlacedOverlay(name, di, si)
{
  def shell: AlteraShell

  shell { InModuleBody {
    val bwire = Wire(Bool())
    switchWire := bwire.asUInt
    val ibuf = Module(new ALT_IOBUF)
    ibuf.suggestName(s"switch_ibuf_${si.number}")
    ibuf.io.io := io
    bwire := ibuf.asInput

    val ios = IOPin.of(io)
    (packagePin.toSeq zip ios) foreach { case (pin, io) =>
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, ioStandard)
    }
  } }
}
