package sifive.fpgashells.shell.xilinx

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.experimental.dataview._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.prci._
import org.chipsalliance.cde.config._
import sifive.fpgashells.clocks._
import sifive.fpgashells.devices.xilinx.xdma._
import sifive.fpgashells.devices.xilinx.xilinxvcu108mig._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.ip.xilinx.xxv_ethernet._
import sifive.fpgashells.ip.xilinx.vcu108mig._
import sifive.fpgashells.shell._

class SysClockVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 300, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "G31")
    shell.xdc.addPackagePin(io.n, "F31")
    shell.xdc.addIOStandard(io.p, "LVDS")
    shell.xdc.addIOStandard(io.n, "LVDS")
  } }
}
class SysClockVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[VCU108ShellBasicOverlays]
{
  def place(designInput: ClockInputDesignInput) = new SysClockVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class RefClockVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput) {
  val node = shell { ClockSourceNode(freqMHz = 125, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "BC9")
    shell.xdc.addPackagePin(io.n, "BC8")
    shell.xdc.addIOStandard(io.p, "LVDS")
    shell.xdc.addIOStandard(io.n, "LVDS")
  } }
}
class RefClockVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: ClockInputDesignInput) = new RefClockVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SDIOVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SDIOXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(("BB16", IOPin(io.spi_clk)), // PMOD3
      ("BA10", IOPin(io.spi_cs)),      // Actually CMD, PMOD1
      ("AW16", IOPin(io.spi_dat(0))),  // Actually DAT0, PMOD2
      ("BC13", IOPin(io.spi_dat(1))),  // Actually DAT1, PMOD4
      ("BF7", IOPin(io.spi_dat(2))),   // Actually DAT2, PMOD5
      ("BC14", IOPin(io.spi_dat(3))))  // Actually DAT3, PMOD0

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
    } }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    } }
  } }
}
class SDIOVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: SPIDesignInput) = new SDIOVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class UARTVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTXilinxPlacedOverlay(name, designInput, shellInput, true)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(("BF24", IOPin(io.ctsn.get)),
      ("BD22", IOPin(io.rtsn.get)),
      ("BC24", IOPin(io.rxd)),
      ("BE24", IOPin(io.txd)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
      shell.xdc.addIOB(io)
    } }
  } }
}
class UARTVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: UARTDesignInput) = new UARTVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class QSFP1VCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: EthernetDesignInput, val shellInput: EthernetShellInput)
  extends EthernetUltraScalePlacedOverlay(name, designInput, shellInput, XXVEthernetParams(name = name, speed   = 10, dclkMHz = 125))
{
  val dclkSource = shell { BundleBridgeSource(() => Clock()) }
  val dclkSink = dclkSource.makeSink()
  InModuleBody {
    dclk := dclkSink.bundle
  }
  shell { InModuleBody {
    dclkSource.bundle := shell.ref_clock.get.get.overlayOutput.node.out(0)._1.clock
    shell.xdc.addPackagePin(io.tx_p, "AK42")
    shell.xdc.addPackagePin(io.tx_n, "AK43")
    shell.xdc.addPackagePin(io.rx_p, "AG45")
    shell.xdc.addPackagePin(io.rx_n, "AG46")
    shell.xdc.addPackagePin(io.refclk_p, "AF38")
    shell.xdc.addPackagePin(io.refclk_n, "AF39")
  } }
}
class QSFP1VCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: EthernetShellInput)(implicit val valName: ValName)
  extends EthernetShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: EthernetDesignInput) = new QSFP1VCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object LEDVCU108PinConstraints {
  val pins = Seq("AT32", "AV34", "AY30", "BB32", "BF32", "AV36", "AY35", "BA37")
}
class LEDVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDVCU108PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class LEDVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: LEDDesignInput) = new LEDVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object ButtonVCU108PinConstraints {
  val pins = Seq("E34", "A10", "M22", "D9", "AW27")
}
class ButtonVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonVCU108PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class ButtonVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: ButtonDesignInput) = new ButtonVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object SwitchVCU108PinConstraints {
  val pins = Seq("BC40", "L19", "C37", "C38")
}
class SwitchVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchVCU108PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class SwitchVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: SwitchDesignInput) = new SwitchVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// TODO: JTAG is untested
class JTAGDebugVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: JTAGDebugDesignInput, val shellInput: JTAGDebugShellInput)
  extends JTAGDebugXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val pin_locations = Map(
      // TODO: The previous version works in FMC also
      // Order is       PMOD2         PMOD5       PMOD4        PMOD0        PMOD1
      "PMOD_J52" -> Seq("AW16",       "BF7",      "BC13",      "BC14",      "BA10"),
      "PMOD_J53" -> Seq( "J20",       "T23",       "J24",       "P22",       "N22"))
    val pins      = Seq(io.jtag_TCK, io.jtag_TMS, io.jtag_TDI, io.jtag_TDO, io.srst_n)

    shell.sdc.addClock("JTCK", IOPin(io.jtag_TCK), 10)
    shell.sdc.addGroup(clocks = Seq("JTCK"))
    shell.xdc.clockDedicatedRouteFalse(IOPin(io.jtag_TCK))

    val pin_voltage:String = if(shellInput.location.get == "PMOD_J53") "LVCMOS12" else "LVCMOS18"

    (pin_locations(shellInput.location.get) zip pins) foreach { case (pin_location, ioport) =>
      val io = IOPin(ioport)
      shell.xdc.addPackagePin(io, pin_location)
      shell.xdc.addIOStandard(io, pin_voltage)
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    }
  } }
}
class JTAGDebugVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: JTAGDebugShellInput)(implicit val valName: ValName)
  extends JTAGDebugShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: JTAGDebugDesignInput) = new JTAGDebugVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class JTAGDebugBScanVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: JTAGDebugBScanDesignInput, val shellInput: JTAGDebugBScanShellInput)
  extends JTAGDebugBScanXilinxPlacedOverlay(name, designInput, shellInput)
class JTAGDebugBScanVCU108ShellPlacer(val shell: VCU108ShellBasicOverlays, val shellInput: JTAGDebugBScanShellInput)(implicit val valName: ValName)
  extends JTAGDebugBScanShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: JTAGDebugBScanDesignInput) = new JTAGDebugBScanVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

case object VCU108DDRSize extends Field[BigInt](0x40000000L * 2) // 2GB
class DDRVCU108PlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
  extends DDRPlacedOverlay[XilinxVCU108MIGPads](name, designInput, shellInput)
{
  val size = p(VCU108DDRSize)

  val migParams = XilinxVCU108MIGParams(address = AddressSet.misaligned(di.baseAddress, size))
  val mig = LazyModule(new XilinxVCU108MIG(migParams))
  val ddrUI     = shell { ClockSourceNode(freqMHz = 200) }
  val areset    = shell { ClockSinkNode(Seq(ClockSinkParameters())) }
  areset := designInput.wrangler := ddrUI

  def overlayOutput = DDROverlayOutput(ddr = mig.node)
  def ioFactory = new XilinxVCU108MIGPads(size)

  shell { InModuleBody {
    require (shell.sys_clock.get.isDefined, "Use of DDRVCU108Overlay depends on SysClockVCU108Overlay")
    val (sys, _) = shell.sys_clock.get.get.overlayOutput.node.out(0)
    val (ui, _) = ddrUI.out(0)
    val (ar, _) = areset.in(0)
    val port = mig.module.io.port
    io <> port.viewAsSupertype(new VCU108MIGIODDR(mig.depth))
    ui.clock := port.c0_ddr4_ui_clk
    ui.reset := /*!port.mmcm_locked ||*/ port.c0_ddr4_ui_clk_sync_rst
    port.c0_sys_clk_i := sys.clock.asUInt
    port.sys_rst := sys.reset // pllReset
    port.c0_ddr4_aresetn := !(ar.reset.asBool)

    val allddrpins = Seq(
      "C30",	"D32",	"B30",	"C33",	"E32",	"A29",	"C29",	"E29",	"A30",	"C32",	"A31",	"A33",	"F29",	"B32",
      "D29",	"B31",	"B33",	"F33",
      "G30",	"F30",
      "M28",	"E33",	"D31",	"E31",	"K29",	"D30",	"J31",
      "J37",	"H40",	"F38",	"H39",	"K37",	"G40",	"F39",	"F40",	"F36",	"J36",	"F35",	"J35",	"G37",	"H35",	"G36",	"H37",
      "C39",	"A38",	"B40",	"D40",	"E38",	"B38",	"E37",	"C40",	"C34",	"A34",	"D34",	"A35",	"A36",	"C35",	"B35",	"D35",
      "N27",	"R27",	"N24",	"R24",	"P24",	"P26",	"P27",	"T24",	"K27",	"L26",	"J27",	"K28",	"K26",	"M25",	"J26",	"L28",
      "E27",	"E28",	"E26",	"H27",	"F25",	"F28",	"G25",	"G27",	"B28",	"A28",	"B25",	"B27",	"D25",	"C27",	"C25",	"D26",
      "G38",	"G35",	"A40",	"B37",	"N25",	"L25",	"G28",	"A26",
      "H38",	"H34",	"A39",	"B36",	"P25",	"L24",	"H28",	"B26",
      "J39",	"F34",	"E39",	"D37",	"T26",	"M27",	"G26",	"D27",
    )

    (IOPin.of(io) zip allddrpins) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
  } }

  shell.sdc.addGroup(pins = Seq(mig.island.module.blackbox.io.c0_ddr4_ui_clk))
}
class DDRVCU108ShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: DDRDesignInput) = new DDRVCU108PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class PCIeVCU108EdgePlacedOverlay(val shell: VCU108ShellBasicOverlays, name: String, val designInput: PCIeDesignInput, val shellInput: PCIeShellInput)
  extends PCIeUltraScalePlacedOverlay(name, designInput, shellInput, XDMAParams(
    name     = "edge_xdma",
    location = "X1Y2",
    bars     = designInput.bars,
    control  = designInput.ecam,
    bases    = designInput.bases,
    lanes    = 8))
{
  shell { InModuleBody {
    // Work-around incorrectly pre-assigned pins
    IOPin.of(io).foreach { shell.xdc.addPackagePin(_, "") }

    // PCIe Edge connector U2
    //   Lanes 00-03 Bank 227
    //   Lanes 04-07 Bank 226
    //   Lanes 08-11 Bank 225
    //   Lanes 12-15 Bank 224

    val ref = Seq("A13", "A14")  /* [pn] PCIE_CLK_QO_*/

    // PCIe Edge connector U2 : Bank 227, 226
    val rxp = Seq("AJ4", "AK2", "AM2", "AP2", "AT2", "AV2", "AY2", "BB2") // [0-7]
    val rxn = Seq("AJ3", "AK1", "AM1", "AP1", "AT1", "AV1", "AY1", "BB1") // [0-7]
    val txp = Seq("AP7", "AR5", "AT7", "AU5", "AW5", "BA5", "BC5", "BE5") // [0-7]
    val txn = Seq("AP6", "AR4", "AT6", "AU4", "AW4", "BA4", "BC4", "BE4") // [0-7]

    def bind(io: Seq[IOPin], pad: Seq[String]) {
      (io zip pad) foreach { case (io, pad) => shell.xdc.addPackagePin(io, pad) }
    }

    bind(IOPin.of(io.refclk), ref)
    // We do these individually so that zip falls off the end of the lanes:
    bind(IOPin.of(io.lanes.pci_exp_txp), txp)
    bind(IOPin.of(io.lanes.pci_exp_txn), txn)
    bind(IOPin.of(io.lanes.pci_exp_rxp), rxp)
    bind(IOPin.of(io.lanes.pci_exp_rxn), rxn)
  } }
}
class PCIeVCU108EdgeShellPlacer(shell: VCU108ShellBasicOverlays, val shellInput: PCIeShellInput)(implicit val valName: ValName)
  extends PCIeShellPlacer[VCU108ShellBasicOverlays] {
  def place(designInput: PCIeDesignInput) = new PCIeVCU108EdgePlacedOverlay(shell, valName.name, designInput, shellInput)
}

abstract class VCU108ShellBasicOverlays()(implicit p: Parameters) extends UltraScaleShell{
  // PLL reset causes
  val pllReset = InModuleBody { Wire(Bool()) }

  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockVCU108ShellPlacer(this, ClockInputShellInput()))
  val ref_clock = Overlay(ClockInputOverlayKey, new RefClockVCU108ShellPlacer(this, ClockInputShellInput()))
  val led       = Seq.tabulate(8)(i => Overlay(LEDOverlayKey, new LEDVCU108ShellPlacer(this, LEDShellInput(color = "red", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchVCU108ShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(5)(i => Overlay(ButtonOverlayKey, new ButtonVCU108ShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))
  val ddr       = Overlay(DDROverlayKey, new DDRVCU108ShellPlacer(this, DDRShellInput()))
  val qsfp     = Overlay(EthernetOverlayKey, new QSFP1VCU108ShellPlacer(this, EthernetShellInput()))
  //val spi_flash = Overlay(SPIFlashOverlayKey, new SPIFlashVCU108ShellPlacer(this, SPIFlashShellInput()))
  //SPI Flash not functional
}

case object VCU108ShellPMOD extends Field[String]("JTAG")
case object VCU108ShellPMOD2 extends Field[String]("JTAG")

class WithVCU108ShellPMOD(device: String) extends Config((site, here, up) => {
  case VCU108ShellPMOD => device
})

// Change JTAG pinouts to VCU108 J53
// Due to the level shifter is from 1.2V to 3.3V, the frequency of JTAG should be slow down to 1Mhz
class WithVCU108ShellPMOD2(device: String) extends Config((site, here, up) => {
  case VCU108ShellPMOD2 => device
})

class WithVCU108ShellPMODJTAG extends WithVCU108ShellPMOD("JTAG")

// Reassign JTAG pinouts location to PMOD J53
class WithVCU108ShellPMOD2JTAG extends WithVCU108ShellPMOD2("PMODJ53_JTAG")

class VCU108Shell()(implicit p: Parameters) extends VCU108ShellBasicOverlays
{
  val pmod_is_sdio  = p(VCU108ShellPMOD) == "SDIO"
  val pmod_j53_is_jtag = p(VCU108ShellPMOD2) == "PMODJ53_JTAG"
  val jtag_location = Some(if (pmod_is_sdio) (if (pmod_j53_is_jtag) "PMOD_J53" else "FMC_J2") else "PMOD_J52")

  // Order matters; ddr depends on sys_clock
  val uart      = Overlay(UARTOverlayKey, new UARTVCU108ShellPlacer(this, UARTShellInput()))
  val sdio      = if (pmod_is_sdio) Some(Overlay(SPIOverlayKey, new SDIOVCU108ShellPlacer(this, SPIShellInput()))) else None
  val jtag      = Overlay(JTAGDebugOverlayKey, new JTAGDebugVCU108ShellPlacer(this, JTAGDebugShellInput(location = jtag_location)))
  val jtagBScan = Overlay(JTAGDebugBScanOverlayKey, new JTAGDebugBScanVCU108ShellPlacer(this, JTAGDebugBScanShellInput()))
  val edge      = Overlay(PCIeOverlayKey, new PCIeVCU108EdgeShellPlacer(this, PCIeShellInput()))

  val topDesign = LazyModule(p(DesignKey)(designParameters))

  // Place the sys_clock at the Shell if the user didn't ask for it
  designParameters(ClockInputOverlayKey).foreach { unused =>
    val source = unused.place(ClockInputDesignInput()).overlayOutput.node
    val sink = ClockSinkNode(Seq(ClockSinkParameters()))
    sink := source
  }

  override lazy val module = new LazyRawModuleImp(this) {
    val reset = IO(Input(Bool()))
    xdc.addPackagePin(reset, "L19")
    xdc.addIOStandard(reset, "LVCMOS12")

    val reset_ibuf = Module(new IBUF)
    reset_ibuf.io.I := reset

    val sysclk: Clock = sys_clock.get() match {
      case Some(x: SysClockVCU108PlacedOverlay) => x.clock
    }

    val powerOnReset: Bool = PowerOnResetFPGAOnly(sysclk)
    sdc.addAsyncPath(Seq(powerOnReset))

    pllReset := (reset_ibuf.io.O || powerOnReset)
  }
}

/*
   Copyright 2016 SiFive, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
