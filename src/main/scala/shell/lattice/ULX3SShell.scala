package sifive.fpgashells.shell.lattice

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.experimental.dataview._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.prci._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

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

