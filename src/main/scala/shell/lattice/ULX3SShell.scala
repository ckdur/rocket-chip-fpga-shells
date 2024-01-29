package sifive.fpgashells.shell.lattice

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.experimental.dataview._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import freechips.rocketchip.prci._
import org.chipsalliance.cde.config._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

class SysClockULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends SingleEndedClockInputLatticePlacedOverlay(name, designInput, shellInput)
{
  def bank_freq: Double = 25.0
  val node = shell { ClockSourceNode(freqMHz = bank_freq, jitterPS = 50) }

  shell { InModuleBody {
    val clk: Clock = io
    shell.lpf.addPackagePin(clk, "G2")
    shell.lpf.addIOBUF(clk, drive=Some(4))
  } }
}

class SysClockLatticeShellPlacer(val shell: LatticeShell, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[LatticeShell] {
  def place(designInput: ClockInputDesignInput) = new SysClockULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// LEDs
object LEDULX3SPinConstraints{
  val pins = Seq("B2", "C2", "C1", "D2", "D1", "E2", "E1", "H3")
}
class LEDULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDLatticePlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDULX3SPinConstraints.pins(shellInput.number)))
class LEDLatticeShellPlacer(val shell: LatticeShell, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[LatticeShell] {
  def place(designInput: LEDDesignInput) = new LEDULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

//SWs
object SwitchULX3SPinConstraints{
  val pins = Seq("E8", "D8", "D7", "E7")
}
class SwitchULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchLatticePlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchULX3SPinConstraints.pins(shellInput.number)))
class SwitchLatticeShellPlacer(val shell: LatticeShell, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[LatticeShell] {
  def place(designInput: SwitchDesignInput) = new SwitchULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

//Buttons
object ButtonULX3SPinConstraints {
  // NOTE: Pin D6 is used for reset
  val pins = Seq("R1", "T1", "R18", "V1", "U1", "H16")
}
class ButtonULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonLatticePlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonULX3SPinConstraints.pins(shellInput.number)))
class ButtonLatticeShellPlacer(val shell: LatticeShell, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[LatticeShell] {
  def place(designInput: ButtonDesignInput) = new ButtonULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// GPIO listing of the ULX3S
object GPIOULX3SPinConstraints{
  // The format is in pairs p, n
  val gp: Seq[Seq[String]] = Seq(
    Seq("B11", "C11"), Seq("A10", "A11"), Seq("A9", "B10"), Seq("B9", "C10"), Seq("A7", "A8"),
    Seq("C8", "B8"), Seq("C6", "C7"), Seq("A6", "B6"), Seq("A4", "A5"), Seq("A2", "B1"),
    Seq("C4", "B4"), Seq("F4", "E3"), Seq("G3", "F3"), Seq("H4", "G5"), Seq("U18", "U17"),
    Seq("N17", "P16"), Seq("N16", "M17"), Seq("L16", "L17"), Seq("H18", "H17"), Seq("F17", "G18"),
    Seq("D18", "E17"), Seq("C18", "D17"), Seq("B15", "C15"), Seq("B17", "C17"), Seq("C16", "D16"),
    Seq("D14", "E14"), Seq("B13", "C13"), Seq("D13", "E13"))
}

case class ULX3SGPIOGroup(elem: Seq[(Int, Int)]) {
  elem.foreach{ case (i, j) =>
    require(i < GPIOULX3SPinConstraints.gp.length, s"Indexing a no existent GPIO group ${i} (of ${GPIOULX3SPinConstraints.gp.length})")
    require(j < GPIOULX3SPinConstraints.gp(i).length, s"Indexing a no existent GPIO ${j} from group ${i} (of ${GPIOULX3SPinConstraints.gp(i).length})")
  }
}

// GPIO
class GPIOPeripheralULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SGPIOGroup, name: String, val designInput: GPIODesignInput, val shellInput: GPIOShellInput)
  extends GPIOLatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    require(io.gpio.length == which.elem.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOULX3SPinConstraints.gp(i)(j), IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOBUF(io, drive=Some(4))
    } }
  } }
}

class GPIOPeripheralLatticeShellPlacer(val shell: LatticeShell, val which: ULX3SGPIOGroup, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIOShellPlacer[LatticeShell] {

  def place(designInput: GPIODesignInput) = new GPIOPeripheralULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

class GPIO0ULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SGPIOGroup, name: String, di: GPIODirectLatticeDesignInput, si: GPIOShellInput)
  extends GPIODirectLatticePlacedOverlay(name, di, si)
{
  shell { InModuleBody {
    require(io.gpio.length == which.elem.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOULX3SPinConstraints.gp(i)(j), IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOBUF(io, drive=Some(4))
    } }
  } }
}

class GPIO0LatticeShellPlacer(val shell: LatticeShell, val which: ULX3SGPIOGroup, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIODirectLatticeShellPlacer[LatticeShell] {

  def place(designInput: GPIODirectLatticeDesignInput) = new GPIO0ULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

object SPIULX3SPinConstraints{
  val pins = Seq("U3", "R2", "W2", "V2", "W1", "Y2", // Normal pins (SCK, CS, MOSI, MISO, HOLDN, WPN)
    "AJ3", "AG4", "AJ4", "AH4", "AM4", "AL4", "AK4") // csspin, initn, done, programn, cfg1, cfg2, cfg3
}

// SPI Flash
class SPIFlashULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: SPIFlashDesignInput, val shellInput: SPIFlashShellInput)
  extends SPIFlashLatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.qspi_sck),
      IOPin(io.qspi_cs),
      IOPin(io.qspi_dq(0)),
      IOPin(io.qspi_dq(1)),
      IOPin(io.qspi_dq(2)),
      IOPin(io.qspi_dq(3)))
    val packagePinsWithPackageIOs = iopins.zip(SPIULX3SPinConstraints.pins).map {
      case (io, pin) =>
        (pin, io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
    }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.lpf.addIOBUF(io, pullmode = "UP", drive=Some(4))
    } }
    packagePinsWithPackageIOs take 1 foreach { case (pin, io) => {
      shell.lpf.addIOBUF(io, drive=Some(4))
    } }
  } }
}

class SPIFlashLatticeShellPlacer(val shell: LatticeShell, val shellInput: SPIFlashShellInput)(implicit val valName: ValName)
  extends SPIFlashShellPlacer[LatticeShell] {

  def place(designInput: SPIFlashDesignInput) = new SPIFlashULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// SPI
class SPIULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SGPIOGroup, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SPILatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.spi_clk),
      IOPin(io.spi_cs),
      IOPin(io.spi_dat(0)),
      IOPin(io.spi_dat(1)),
      IOPin(io.spi_dat(2)),
      IOPin(io.spi_dat(3)))
    val packagePinsWithPackageIOs = iopins.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOULX3SPinConstraints.gp(i)(j), io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
    }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.lpf.addIOBUF(io, pullmode = "UP", drive=Some(4))
    } }
    packagePinsWithPackageIOs take 1 foreach { case (pin, io) => {
      shell.lpf.addIOBUF(io, drive=Some(4))
    } }
  } }
}

class SPILatticeShellPlacer(val shell: LatticeShell, val which: ULX3SGPIOGroup, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[LatticeShell] {

  def place(designInput: SPIDesignInput) = new SPIULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// JTAG Debug
class JTAGDebugULX3SPlacedOverlay(val shell: LatticeShell, val which: ULX3SGPIOGroup, name: String, val designInput: JTAGDebugDesignInput, val shellInput: JTAGDebugShellInput)
  extends JTAGDebugLatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    shell.sdc.addClock("JTCK", IOPin(io.jtag_TCK), 10)
    val iopins = Seq(IOPin(io.jtag_TDI),
      IOPin(io.jtag_TDO),
      IOPin(io.jtag_TCK),
      IOPin(io.jtag_TMS),
      IOPin(io.srst_n))
    val packagePinsWithPackageIOs = iopins.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOULX3SPinConstraints.gp(i)(j), io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOBUF(io, pullmode = "UP", drive=Some(4))
    }
  } }
}

class JTAGDebugLatticeShellPlacer(val shell: LatticeShell, val which: ULX3SGPIOGroup, val shellInput: JTAGDebugShellInput)(implicit val valName: ValName)
  extends JTAGDebugShellPlacer[LatticeShell] {

  def place(designInput: JTAGDebugDesignInput) = new JTAGDebugULX3SPlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

object UARTULX3SPinConstraints{
  val pins = Seq("M1", "L4", // Normal pins (rxd, txd)
    "M3", "N1", "L3") // nrts, ndtr, txden
}

// UART
class UARTULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTLatticePlacedOverlay(name, designInput, shellInput, false)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.rxd),
      IOPin(io.txd))
    val packagePinsWithPackageIOs = iopins.zip(UARTULX3SPinConstraints.pins).map {
      case (io, pin) =>
        (pin, io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
    }

    packagePinsWithPackageIOs take 1 foreach { case (pin, io) =>
      shell.lpf.addIOBUF(io, pullmode = "UP", drive=Some(4))
    }

    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) =>
      shell.lpf.addIOBUF(io, pullmode = "UP")
    }
  } }
}

class UARTLatticeShellPlacer(val shell: LatticeShell, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[LatticeShell] {

  def place(designInput: UARTDesignInput) = new UARTULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

object SDULX3SPinConstraints{
  val pins = Seq("H2", "K2", "J1", "J3", "H1", "K1")
}

// SD
class SDULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SPILatticePlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.spi_clk),
      IOPin(io.spi_cs),
      IOPin(io.spi_dat(0)),
      IOPin(io.spi_dat(1)),
      IOPin(io.spi_dat(2)),
      IOPin(io.spi_dat(3)))
    val packagePinsWithPackageIOs = iopins.zip(SDULX3SPinConstraints.pins).map {
      case (io, pins) =>
        (pins, io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.lpf.addPackagePin(io, pin)
      shell.lpf.addIOBUF(io, pullmode = "UP", drive=Some(4))
    }
  } }
}

class SDLatticeShellPlacer(val shell: LatticeShell, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[LatticeShell] {

  def place(designInput: SPIDesignInput) = new SDULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}

// SDRAM3
object ULX3SSDRAMLocs {
  val mem_a = Seq("M20", "M19", "L20", "L19", "K20", "K19", "K18", "J20", "J19", "H20", "N19", "G20", "G19")
  val mem_dqm = Seq("U19", "E20")
  val mem_data = Seq(
    "J16", "L18", "M18", "N18", "P18", "T18", "T17", "U20",
    "E19", "D20", "D19", "C20", "E18", "F18", "J18", "J17")
  val mem_ba = Seq("P19", "N20")
  val mem_clk = "F19"
  val mem_cke = "F20"
  val mem_cs = "P20"
  val mem_we = "T20"
  val mem_ras = "R20"
  val mem_cas = "T19"
}

class SDRAMULX3SPlacedOverlay(val shell: LatticeShell, name: String, val designInput: SDRAMDesignInput, val shellInput: SDRAMShellInput)
  extends SDRAMPlacedOverlay[ULX3SSDRAM](name, designInput, shellInput)
{
  def ioFactory = new ULX3SSDRAM
  shell { InModuleBody {
    io.sdram_clk_o := port.sdram_clk_o
    io.sdram_cke_o := port.sdram_cke_o
    io.sdram_cs_o := port.sdram_cs_o
    io.sdram_ras_o := port.sdram_ras_o
    io.sdram_cas_o := port.sdram_cas_o
    io.sdram_we_o := port.sdram_we_o
    io.sdram_dqm_o := port.sdram_dqm_o
    io.sdram_addr_o := port.sdram_addr_o
    io.sdram_ba_o := port.sdram_ba_o
    port.sdram_data_i := VecInit((port.sdram_data_o.asBools zip io.sdram_data_io).map{
      case (o, an) =>
        val b = Module(new BB)
        b.io.T := !port.sdram_drive_o
        b.io.I := o
        attach(b.io.B, an)
        b.io.O
    }).asUInt
    
    ULX3SSDRAMLocs.mem_a.zipWithIndex.foreach { case (pin, i) =>
      shell.lpf.addPackagePin(IOPin(io.sdram_addr_o, i), pin)
      shell.lpf.addIOBUF(IOPin(io.sdram_addr_o, i), drive=Some(4))
    }
    ULX3SSDRAMLocs.mem_dqm.zipWithIndex.foreach { case (pin, i) =>
      shell.lpf.addPackagePin(IOPin(io.sdram_dqm_o, i), pin)
      shell.lpf.addIOBUF(IOPin(io.sdram_dqm_o, i), drive=Some(4))
    }
    ULX3SSDRAMLocs.mem_data.zipWithIndex.foreach { case (pin, i) =>
      shell.lpf.addPackagePin(IOPin(io.sdram_data_io(i)), pin)
      shell.lpf.addIOBUF(IOPin(io.sdram_data_io(i), i), drive=Some(4))
    }
    ULX3SSDRAMLocs.mem_ba.zipWithIndex.foreach { case (pin, i) =>
      shell.lpf.addPackagePin(IOPin(io.sdram_ba_o, i), pin)
      shell.lpf.addIOBUF(IOPin(io.sdram_ba_o, i), drive=Some(4))
    }
    shell.lpf.addPackagePin(IOPin(io.sdram_clk_o), ULX3SSDRAMLocs.mem_clk)
    shell.lpf.addIOBUF(IOPin(io.sdram_clk_o), drive=Some(4))
    shell.lpf.addPackagePin(IOPin(io.sdram_cke_o), ULX3SSDRAMLocs.mem_cke)
    shell.lpf.addIOBUF(IOPin(io.sdram_cke_o), drive=Some(4))
    shell.lpf.addPackagePin(IOPin(io.sdram_cs_o), ULX3SSDRAMLocs.mem_cs)
    shell.lpf.addIOBUF(IOPin(io.sdram_cs_o), drive=Some(4))
    shell.lpf.addPackagePin(IOPin(io.sdram_ras_o), ULX3SSDRAMLocs.mem_ras)
    shell.lpf.addIOBUF(IOPin(io.sdram_ras_o), drive=Some(4))
    shell.lpf.addPackagePin(IOPin(io.sdram_cas_o), ULX3SSDRAMLocs.mem_cas)
    shell.lpf.addIOBUF(IOPin(io.sdram_cas_o), drive=Some(4))
    shell.lpf.addPackagePin(IOPin(io.sdram_we_o), ULX3SSDRAMLocs.mem_we)
    shell.lpf.addIOBUF(IOPin(io.sdram_we_o), drive=Some(4))
  } }
}

class SDRAMLatticeShellPlacer(val shell: LatticeShell, val shellInput: SDRAMShellInput)(implicit val valName: ValName)
  extends SDRAMShellPlacer[LatticeShell] {
  def place(designInput: SDRAMDesignInput) = new SDRAMULX3SPlacedOverlay(shell, valName.name, designInput, shellInput)
}
