package sifive.fpgashells.shell.lattice

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.experimental.dataview._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.prci._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import org.chipsalliance.diplomacy._
import org.chipsalliance.diplomacy.lazymodule._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._
import sifive.fpgashells.devices.common.{TLSDRAM, SDRAMConfig, SDRAMIf, sdram_bb_cfg}

case object GPIO0OverlayKey extends Field[Seq[DesignPlacer[GPIODirectLatticeDesignInput, GPIOShellInput, GPIODirectLatticeOverlayOutput]]](Nil)

class SysClockULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends SingleEndedClockInputLatticePlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 25.0, jitterPS = 50) }

  shell { InModuleBody {
    val clk: Clock = io
    shell.lpf.addPackagePin(clk, "G2")
    shell.lpf.addIOStandard(clk, "LVCMOS33")
    shell.lpf.addFrequency(clk, 25.0)
  } }
}

class SysClockULX3SShellPlacer(val shell: LatticeShell, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[LatticeShell] {
  def place(designInput: ClockInputDesignInput) = new SysClockULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// LEDs
object LEDULX3SPinConstraints{
  val pins = Seq("H3", "E1", "E2", "D1", "D2", "C1", "C2", "B2")
}
class LEDULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDLatticePlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDULX3SPinConstraints.pins(shellInput.number)))
class LEDULX3SShellPlacer(val shell: LatticeShell, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[LatticeShell] {
  def place(designInput: LEDDesignInput) = new LEDULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// SWs
object SwitchULX3SPinConstraints{
  val pins = Seq("E8", "D8", "D7", "E7")
}
class SwitchULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchLatticePlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchULX3SPinConstraints.pins(shellInput.number)))
class SwitchULX3SShellPlacer(val shell: LatticeShell, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[LatticeShell] {
  def place(designInput: SwitchDesignInput) = new SwitchULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// Buttons
object ButtonULX3SPinConstraints {
  // NOTE: Pin D6 is used for reset
  val pins = Seq("R1", "T1", "R18", "V1", "U1", "H16")
}
class ButtonULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonLatticePlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonULX3SPinConstraints.pins(shellInput.number)))
class ButtonULX3SShellPlacer(val shell: LatticeShell, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[LatticeShell] {
  def place(designInput: ButtonDesignInput) = new ButtonULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

object GPIOULX3SPinConstraints{
  val gpio_locations = Seq(
    // p ,   n  ,   p  ,   n  ,   p  ,   n  ,   p  ,   n  ,
    "B11", "C11", "A10", "A11", "A9" , "B10", "B9" , "C10", // gX0-3
    "A7" , "A8" , "C8" , "B8" , "C6" , "C7" , "A6" , "B6" , // gX4-7
    "A4" , "A5" , "A2" , "B1" , "C4" , "B4" , "F4" , "E3" , // gX8-11
    "G3" , "F3" , "H4" , "G5" , "U18", "U17", "N17", "P16", // gX12-15
    "N16", "M17", "L16", "L17", "H18", "H17", "F17", "G18", // gX16-19
    "D18", "E17", "C18", "D17", "B15", "C15", "B17", "C17", // gX20-23
    "C16", "D16", "D14", "E14", "B13", "C13", "D13", "E13"  // gX24-27
  )
  def gpio(np: Int, i: Int) = gpio_locations(i*2 + np)
  val p = 0
  val n = 1
}

trait ULX3SElem {
  def GetBindings: Seq[String]
  def GetStandard: String
  def GetDrive: Option[Int]
}

case class ULX3SGPIOGroup(elem: Seq[(Int, Int)]) extends ULX3SElem {
  elem.foreach{ case (i, j) =>
    require(i < 2, s"Invalid N-P for the GPIO (${i}) of only possible 2 (0->P, 1->N)")
    require(j < 28, s"Indexing a no existent GPIO ${j} from group ${i} (of 28)")
  }
  val GetBindings = elem.map{case (i,j) => GPIOULX3SPinConstraints.gpio(i, j)}
  val GetStandard = "LVCMOS33"
  val GetDrive = Some(4)
}

// GPIO
class GPIOPeripheralULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SElem, name: String, val designInput: GPIODesignInput, val shellInput: GPIOShellInput)
  extends GPIOLatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    require(io.gpio.length == which.GetBindings.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, which.GetStandard, drive = which.GetDrive)
    } }
  } }
}

class GPIOPeripheralULX3SShellPlacer(val shell: LatticeShell, val which: ULX3SElem, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIOShellPlacer[LatticeShell] {

  def place(designInput: GPIODesignInput) = new GPIOPeripheralULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

class GPIO0ULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SElem, name: String, di: GPIODirectLatticeDesignInput, si: GPIOShellInput)
  extends GPIODirectLatticePlacedOverlay(name, di, si)
{
  shell { InModuleBody {
    require(io.gpio.length == which.GetBindings.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, which.GetStandard, drive = which.GetDrive)
    } }
  } }
}

class GPIO0ULX3SShellPlacer(val shell: LatticeShell, val which: ULX3SElem, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIODirectLatticeShellPlacer[LatticeShell] {

  def place(designInput: GPIODirectLatticeDesignInput) = new GPIO0ULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// SPI Flash
class SPIFlashULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SElem, name: String, val designInput: SPIFlashDesignInput, val shellInput: SPIFlashShellInput)
  extends SPIFlashLatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.qspi_sck),
      IOPin(io.qspi_cs),
      IOPin(io.qspi_dq(0)),
      IOPin(io.qspi_dq(1)),
      IOPin(io.qspi_dq(2)),
      IOPin(io.qspi_dq(3)))
    val packagePinsWithPackageIOs = iopins.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs.zipWithIndex.foreach { case ((pin, io), i) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, which.GetStandard, pullMode = if(i == 0) "NONE" else "UP", drive = Some(4))
    }
  } }
}

class SPIFlashULX3SShellPlacer(val shell: LatticeShell, val which: ULX3SElem, val shellInput: SPIFlashShellInput)(implicit val valName: ValName)
  extends SPIFlashShellPlacer[LatticeShell] {

  def place(designInput: SPIFlashDesignInput) = new SPIFlashULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// SPIMedia (Fixed to the SD card)
class SPIMediaULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SPILatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(
      ("H2", IOPin(io.spi_clk)),
      ("K2", IOPin(io.spi_cs)),
      ("J1", IOPin(io.spi_dat(0))),
      ("J3", IOPin(io.spi_dat(1))),
      ("H1", IOPin(io.spi_dat(2))),
      ("K1", IOPin(io.spi_dat(3))))

    packagePinsWithPackageIOs.zipWithIndex.foreach { case ((pin, io), i) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, "LVCMOS33", pullMode = if(i == 0) "NONE" else "UP", drive = Some(4))
    }
  } }
}

class SPIMediaULX3SShellPlacer(val shell: LatticeShell, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[LatticeShell] {

  def place(designInput: SPIDesignInput) = new SPIMediaULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// SPI
class SPIULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SElem, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SPILatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.spi_clk),
      IOPin(io.spi_cs),
      IOPin(io.spi_dat(0)),
      IOPin(io.spi_dat(1)),
      IOPin(io.spi_dat(2)),
      IOPin(io.spi_dat(3)))
    val packagePinsWithPackageIOs = iopins.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs.zipWithIndex.foreach { case ((pin, io), i) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, which.GetStandard, pullMode = if(i == 0) "NONE" else "UP", drive = Some(4))
    }
  } }
}

class SPIULX3SShellPlacer(val shell: LatticeShell, val which: ULX3SElem, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[LatticeShell] {

  def place(designInput: SPIDesignInput) = new SPIULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// JTAG Debug
class JTAGDebugULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SElem, name: String, val designInput: JTAGDebugDesignInput, val shellInput: JTAGDebugShellInput)
  extends JTAGDebugLatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    shell.sdc.addClock("JTCK", IOPin(io.jtag_TCK), 10)
    shell.sdc.addGroup(clocks = Seq("JTCK"))
    val iopins = Seq(IOPin(io.jtag_TDI),
      IOPin(io.jtag_TDO),
      IOPin(io.jtag_TCK),
      IOPin(io.jtag_TMS),
      IOPin(io.srst_n))
    val packagePinsWithPackageIOs = iopins.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, which.GetStandard, pullMode = "UP")
    }
  } }
}

class JTAGDebugULX3SShellPlacer(val shell: LatticeShell, val which: ULX3SElem, val shellInput: JTAGDebugShellInput)(implicit val valName: ValName)
  extends JTAGDebugShellPlacer[LatticeShell] {

  def place(designInput: JTAGDebugDesignInput) = new JTAGDebugULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// UART
class UARTULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTLatticePlacedOverlay(name, designInput, shellInput, false)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(
      (IOPin(io.rxd), "L4"),
      (IOPin(io.txd), "M1"))

    packagePinsWithPackageIOs.zipWithIndex.foreach { case ((io, pin), i) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOStandard(io, "LVCMOS33", pullMode = "UP", drive = Some(4))
    }
  } }
}

class UARTULX3SShellPlacer(val shell: LatticeShell, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[LatticeShell] {

  def place(designInput: UARTDesignInput) = new UARTULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

object ULX3SSDRAMLocs {
  val clk = "F19"
  val cke = "F20"
  val cs = "P20"
  val we = "T20"
  val ras = "R20"
  val cas = "T19"
  val addr = Seq(
    "M20", "L19", "L20", "L19", "K20", "K19",
    "K18", "J20", "J19", "H20", "N19", "G20",
    "G19")
  val ba = Seq("P19", "N20")
  val dqm = Seq("U19", "E20")
  val data = Seq(
    "J16", "L18", "M18", "N18", "P18", "T18", "T17", "U20",
    "E19", "D20", "D19", "C20", "E18", "F18", "J18", "J17")
}

abstract class ULX3SShell()(implicit p: Parameters) extends LatticeShell
{
  val pllReset = InModuleBody { Wire(Bool()) }
  val resetPin = InModuleBody { Wire(Bool()) }
  val ndreset = InModuleBody { WireInit(false.B) }

  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockULX3SShellPlacer(this, ClockInputShellInput()))
  val led       = Seq.tabulate(8)(i => Overlay(LEDOverlayKey, new LEDULX3SShellPlacer(this, LEDShellInput(color = "green", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchULX3SShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(7)(i => Overlay(ButtonOverlayKey, new ButtonULX3SShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))

  // Place the sys_clock at the Shell if the user didn't ask for it
  p(ClockInputOverlayKey).foreach(_.place(ClockInputDesignInput()))
  override lazy val module = new ULX3SShellImpl(this)
}

class ULX3SShellImpl(outer: ULX3SShell) extends LazyRawModuleImp(outer) {
  override def provideImplicitClockToLazyChildren = true

  val reset = IO(Analog(1.W))
  outer.lpf.addPackagePin(IOPin(reset), "D6")
  outer.lpf.addIOStandard(IOPin(reset), "LVCMOS33")
  val reset_ibuf = Module(new BB)
  attach(reset_ibuf.io.B, reset)
  outer.resetPin := !reset_ibuf.asInput() || outer.ndreset
  outer.pllReset := outer.resetPin
}

