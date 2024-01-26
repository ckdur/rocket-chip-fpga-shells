package sifive.fpgashells.shell.altera

import chisel3._
import chisel3.experimental.Analog
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

case class AlteraHSMCShellInput(assign: String)
case class AlteraHSMCDesignInput(hsmcParam: AlteraHSMCParams)(implicit val p: Parameters)
case class AlteraHSMCOverlayOutput(hsmc: ModuleValue[AlteraHSMC])
case object AlteraHSMCOverlayKey extends Field[Seq[DesignPlacer[AlteraHSMCDesignInput, AlteraHSMCShellInput, AlteraHSMCOverlayOutput]]](Nil)
trait AlteraHSMCShellPlacer[Shell] extends ShellPlacer[AlteraHSMCDesignInput, AlteraHSMCShellInput, AlteraHSMCOverlayOutput]

trait HasAlteraHSMCLocs {
  val CLKIN0: String
  val CLKIN_n1: String
  val CLKIN_n2: String
  val CLKIN_p1: String
  val CLKIN_p2: String
  val D: Seq[String]
  val OUT0: String
  val OUT_n1: String
  val OUT_p1: String
  val OUT_n2: String
  val OUT_p2: String
  val RX_n: Seq[String]
  val RX_p: Seq[String]
  val TX_n: Seq[String]
  val TX_p: Seq[String]
}

class AlteraHSMCPlacedOverlay(val shell: AlteraShell, val locs: HasAlteraHSMCLocs, val name: String, val designInput: AlteraHSMCDesignInput, val shellInput: AlteraHSMCShellInput, val ioStandard: String = "2.5V")
  extends IOPlacedOverlay[AlteraHSMC, AlteraHSMCDesignInput, AlteraHSMCShellInput, AlteraHSMCOverlayOutput]
{
  implicit val p = designInput.p

  def ioFactory = new AlteraHSMC(designInput.hsmcParam)
  val hsmcWire = shell { InModuleBody { Wire(new AlteraHSMC(designInput.hsmcParam)) } }
  def overlayOutput = AlteraHSMCOverlayOutput(hsmcWire)

  shell { InModuleBody {
    io <> hsmcWire

    shell.tdc.addPackagePin(IOPin(io.CLKIN0), locs.CLKIN0); shell.tdc.addIOStandard(IOPin(io.CLKIN0), ioStandard)
    shell.tdc.addPackagePin(IOPin(io.CLKIN_n1), locs.CLKIN_n1); shell.tdc.addIOStandard(IOPin(io.CLKIN_n1), ioStandard)
    shell.tdc.addPackagePin(IOPin(io.CLKIN_n2), locs.CLKIN_n2); shell.tdc.addIOStandard(IOPin(io.CLKIN_n2), ioStandard)
    shell.tdc.addPackagePin(IOPin(io.CLKIN_p1), locs.CLKIN_p1); shell.tdc.addIOStandard(IOPin(io.CLKIN_p1), ioStandard)
    shell.tdc.addPackagePin(IOPin(io.CLKIN_p2), locs.CLKIN_p2); shell.tdc.addIOStandard(IOPin(io.CLKIN_p2), ioStandard)
    io.D.zipWithIndex.foreach{case(elem, i) => shell.tdc.addPackagePin(IOPin(elem), locs.D(i)); shell.tdc.addIOStandard(IOPin(elem), ioStandard)}
    shell.tdc.addPackagePin(IOPin(io.OUT0), locs.OUT0); shell.tdc.addIOStandard(IOPin(io.OUT0), ioStandard)

    if (designInput.hsmcParam.on1) {
      shell.tdc.addPackagePin(IOPin(io.OUT_n1.get), locs.OUT_n1); shell.tdc.addIOStandard(IOPin(io.OUT_n1.get), ioStandard)
      shell.tdc.addPackagePin(IOPin(io.OUT_p1.get), locs.OUT_p1); shell.tdc.addIOStandard(IOPin(io.OUT_p1.get), ioStandard)
    }
    if (designInput.hsmcParam.on2) {
      shell.tdc.addPackagePin(IOPin(io.OUT_n2.get), locs.OUT_n2); shell.tdc.addIOStandard(IOPin(io.OUT_n2.get), ioStandard)
      shell.tdc.addPackagePin(IOPin(io.OUT_p2.get), locs.OUT_p2); shell.tdc.addIOStandard(IOPin(io.OUT_p2.get), ioStandard)
    }
    io.RX_n.zipWithIndex.foreach{case(elem, i) => shell.tdc.addPackagePin(IOPin(elem), locs.RX_n(i)); shell.tdc.addIOStandard(IOPin(elem), ioStandard)}
    io.RX_p.zipWithIndex.foreach{case(elem, i) => shell.tdc.addPackagePin(IOPin(elem), locs.RX_p(i)); shell.tdc.addIOStandard(IOPin(elem), ioStandard)}
    io.TX_n.zipWithIndex.foreach{case(elem, i) => shell.tdc.addPackagePin(IOPin(elem), locs.TX_n(i)); shell.tdc.addIOStandard(IOPin(elem), ioStandard)}
    io.TX_p.zipWithIndex.foreach{case(elem, i) => shell.tdc.addPackagePin(IOPin(elem), locs.TX_p(i)); shell.tdc.addIOStandard(IOPin(elem), ioStandard)}

  } }
}

class AlteraHSMCTR4ShellPlacer(val shell: AlteraShell, val locs: HasAlteraHSMCLocs, val shellInput: AlteraHSMCShellInput)(implicit val valName: ValName)
  extends AlteraHSMCShellPlacer[AlteraShell] {

  def place(designInput: AlteraHSMCDesignInput) = new AlteraHSMCPlacedOverlay(shell, locs, valName.name, designInput, shellInput)
}
