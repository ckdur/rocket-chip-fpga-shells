package sifive.fpgashells.shell.altera

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.experimental.dataview._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.prci._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.devices.altera.altera_mem_if._
import sifive.fpgashells.ip.altera.altera_mem_if._
import sifive.fpgashells.shell._

case object GPIO0OverlayKey extends Field[Seq[DesignPlacer[GPIODirectAlteraDesignInput, GPIOShellInput, GPIODirectAlteraOverlayOutput]]](Nil)

class SysClockTR4PlacedOverlay(val shell: AlteraGenericShell, val bank: Int, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends SingleEndedClockInputAlteraGenericPlacedOverlay(name, designInput, shellInput)
{
  val bank_map = Map(
    1 -> "PIN_AB34",
    3 -> "PIN_AW22",
    4 -> "PIN_AV19",
    7 -> "PIN_A21",
    8 -> "PIN_B23",
  )
  def bank_freq(b: Int): Double = 50.0
  val node = shell { ClockSourceNode(freqMHz = bank_freq(bank), jitterPS = 50) }

  shell { InModuleBody {
    val clk: Clock = io
    shell.io_tcl.addPackagePin(clk, bank_map(bank))
    shell.io_tcl.addIOStandard(clk, "2.5V")
  } }
}

// LEDs
object LEDTR4PinConstraints{
  val pins = Seq("PIN_B19", "PIN_A18", "PIN_D19", "PIN_C19")
}
class LEDTR4PlacedOverlay(val shell: TR4Shell, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDAlteraGenericPlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDTR4PinConstraints.pins(shellInput.number)))
class LEDTR4ShellPlacer(val shell: TR4Shell, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[TR4Shell] {
  def place(designInput: LEDDesignInput) = new LEDTR4PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SysClockTR4ShellPlacer(val shell: AlteraGenericShell, val bank: Int, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[AlteraGenericShell] {
  def place(designInput: ClockInputDesignInput) = new SysClockTR4PlacedOverlay(shell, bank, valName.name, designInput, shellInput)
}

//SWs
object SwitchTR4PinConstraints{
  val pins = Seq("PIN_AH18", "PIN_AH19", "PIN_D6", "PIN_C6")
}
class SwitchTR4PlacedOverlay(val shell: TR4Shell, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchAlteraPlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchTR4PinConstraints.pins(shellInput.number)))
class SwitchTR4ShellPlacer(val shell: TR4Shell, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[TR4Shell] {
  def place(designInput: SwitchDesignInput) = new SwitchTR4PlacedOverlay(shell, valName.name, designInput, shellInput)
}

//Buttons
object ButtonTR4PinConstraints {
  // NOTE: Pin PIN_L19 is used for reset
  val pins = Seq("PIN_M19", "PIN_A19", "PIN_P20")
}
class ButtonTR4PlacedOverlay(val shell: TR4Shell, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonAlteraPlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonTR4PinConstraints.pins(shellInput.number)))
class ButtonTR4ShellPlacer(val shell: TR4Shell, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[TR4Shell] {
  def place(designInput: ButtonDesignInput) = new ButtonTR4PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// GPIO listing of the TR4
object GPIOTR4PinConstraints{
  val gpio0 = Seq(
    "PIN_AF34", "PIN_AG34", "PIN_AE35", "PIN_AG35", "PIN_AC31", "PIN_AH32",
    "PIN_AC32", "PIN_AH33", "PIN_AH34", "PIN_AJ34", "PIN_AH35", "PIN_AJ35",
    "PIN_AK34", "PIN_AL34", "PIN_AK35", "PIN_AL35", "PIN_AM34", "PIN_AN34",
    "PIN_AM35", "PIN_AN35", "PIN_AJ32", "PIN_AJ26", "PIN_AK33", "PIN_AK26",
    "PIN_AF25", "PIN_AV29", "PIN_AG25", "PIN_AW30", "PIN_AV32", "PIN_AT28",
    "PIN_AW32", "PIN_AU28", "PIN_AV28", "PIN_AP28", "PIN_AW29", "PIN_AR28")
  val gpio1 = Seq(
    "PIN_AB27", "PIN_AE25", "PIN_AB28", "PIN_AD25", "PIN_AP27", "PIN_AU29",
    "PIN_AN27", "PIN_AT29", "PIN_AL25", "PIN_AW33", "PIN_AP26", "PIN_AW34",
    "PIN_AW31", "PIN_AH24", "PIN_AV31", "PIN_AG24", "PIN_AL27", "PIN_AW27",
    "PIN_AH26", "PIN_AW28", "PIN_AK27", "PIN_AD30", "PIN_AE24", "PIN_AD31",
    "PIN_AB30", "PIN_AE30", "PIN_AB31", "PIN_AE31", "PIN_AG31", "PIN_AE28",
    "PIN_AG32", "PIN_AE29", "PIN_AF29", "PIN_AD28", "PIN_AG30", "PIN_AD29")
  val gpio = Seq(gpio0, gpio1)
}

trait TR4Elem {
  def GetBindings: Seq[String]
  def GetStandard: String
}

case class TR4GPIOGroup(elem: Seq[(Int, Int)]) extends TR4Elem {
  elem.foreach{ case (i, j) =>
    require(i < GPIOTR4PinConstraints.gpio.length, s"Indexing a no existent GPIO group ${i} (of ${GPIOTR4PinConstraints.gpio.length})")
    require(j < GPIOTR4PinConstraints.gpio(i).length, s"Indexing a no existent GPIO ${j} from group ${i} (of ${GPIOTR4PinConstraints.gpio(i).length})")
  }
  val GetBindings = elem.map{case (i,j) => GPIOTR4PinConstraints.gpio(i)(j)}
  val GetStandard = "1.8 V"
}

// GPIO
class GPIOPeripheralTR4PlacedOverlay(val shell: TR4Shell, val which: TR4Elem, name: String, val designInput: GPIODesignInput, val shellInput: GPIOShellInput)
  extends GPIOAlteraPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    require(io.gpio.length == which.GetBindings.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.io_tcl.addPackagePin(io, pin)
      shell.io_tcl.addIOStandard(io, which.GetStandard)
    } }
  } }
}

class GPIOPeripheralTR4ShellPlacer(val shell: TR4Shell, val which: TR4Elem, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIOShellPlacer[TR4Shell] {

  def place(designInput: GPIODesignInput) = new GPIOPeripheralTR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

class GPIO0TR4PlacedOverlay(val shell: TR4Shell, val which: TR4Elem, name: String, di: GPIODirectAlteraDesignInput, si: GPIOShellInput)
  extends GPIODirectAlteraPlacedOverlay(name, di, si)
{
  shell { InModuleBody {
    require(io.gpio.length == which.GetBindings.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.io_tcl.addPackagePin(io, pin)
      shell.io_tcl.addIOStandard(io, which.GetStandard)
    } }
  } }
}

class GPIO0TR4ShellPlacer(val shell: TR4Shell, val which: TR4Elem, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIODirectAlteraShellPlacer[TR4Shell] {

  def place(designInput: GPIODirectAlteraDesignInput) = new GPIO0TR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// SPI Flash
class SPIFlashTR4PlacedOverlay(val shell: TR4Shell, val which: TR4Elem, name: String, val designInput: SPIFlashDesignInput, val shellInput: SPIFlashShellInput)
  extends SPIFlashAlteraPlacedOverlay(name, designInput, shellInput)
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

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.io_tcl.addPackagePin(io, pin)
      shell.io_tcl.addIOStandard(io, which.GetStandard)
    }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.io_tcl.addPullup(io)
    } }
  } }
}

class SPIFlashTR4ShellPlacer(val shell: TR4Shell, val which: TR4Elem, val shellInput: SPIFlashShellInput)(implicit val valName: ValName)
  extends SPIFlashShellPlacer[TR4Shell] {

  def place(designInput: SPIFlashDesignInput) = new SPIFlashTR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// SPI
class SPITR4PlacedOverlay(val shell: TR4Shell, val which: TR4Elem, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SPIAlteraPlacedOverlay(name, designInput, shellInput)
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

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.io_tcl.addPackagePin(io, pin)
      shell.io_tcl.addIOStandard(io, which.GetStandard)
    }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.io_tcl.addPullup(io)
    } }
  } }
}

class SPITR4ShellPlacer(val shell: TR4Shell, val which: TR4Elem, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[TR4Shell] {

  def place(designInput: SPIDesignInput) = new SPITR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// JTAG Debug
class JTAGDebugTR4PlacedOverlay(val shell: TR4Shell, val which: TR4Elem, name: String, val designInput: JTAGDebugDesignInput, val shellInput: JTAGDebugShellInput)
  extends JTAGDebugAlteraPlacedOverlay(name, designInput, shellInput)
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
      shell.io_tcl.addPackagePin(io, pin)
      shell.io_tcl.addIOStandard(io, which.GetStandard)
      shell.io_tcl.addPullup(io)
    }
  } }
}

class JTAGDebugTR4ShellPlacer(val shell: TR4Shell, val which: TR4Elem, val shellInput: JTAGDebugShellInput)(implicit val valName: ValName)
  extends JTAGDebugShellPlacer[TR4Shell] {

  def place(designInput: JTAGDebugDesignInput) = new JTAGDebugTR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// UART
class UARTTR4PlacedOverlay(val shell: TR4Shell, val which: TR4Elem, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTAlteraPlacedOverlay(name, designInput, shellInput, false)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.rxd),
      IOPin(io.txd))
    val packagePinsWithPackageIOs = iopins.zip(which.GetBindings).map {
      case (io, elem) =>
        (elem, io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.io_tcl.addPackagePin(io, pin)
      shell.io_tcl.addIOStandard(io, which.GetStandard)
    }
  } }
}

class UARTTR4ShellPlacer(val shell: TR4Shell, val which: TR4Elem, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[TR4Shell] {

  def place(designInput: UARTDesignInput) = new UARTTR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// HSMC Pin definitions
class TR4HSMCA extends HasAlteraHSMCLocs {
  val CLKIN0 = "PIN_C10"
  val CLKIN_n1 = "PIN_AE5"
  val CLKIN_n2 = "PIN_AC5"
  val CLKIN_p1 = "PIN_AF6"
  val CLKIN_p2 = "PIN_AC6"
  val D = Seq("PIN_AK8", "PIN_AP6", "PIN_AK7", "PIN_AP5")
  val OUT0 = "PIN_D10"
  val OUT_n1 = "PIN_R11"
  val OUT_p1 = "PIN_R12"
  val OUT_n2 = "PIN_G10"
  val OUT_p2 = "PIN_H10"
  val RX_n = Seq("PIN_AN5", "PIN_AM5", "PIN_AL5", "PIN_AK5", "PIN_AJ5", "PIN_AH5", "PIN_AG5", "PIN_AC8", "PIN_E10",
    "PIN_F9", "PIN_C9", "PIN_F6", "PIN_F5", "PIN_E7", "PIN_C8", "PIN_C5", "PIN_C7")
  val RX_p = Seq("PIN_AN6", "PIN_AM6", "PIN_AL6", "PIN_AK6", "PIN_AJ6", "PIN_AH6", "PIN_AG6", "PIN_AB9", "PIN_F10",
    "PIN_G9", "PIN_D9", "PIN_G6", "PIN_G5", "PIN_F7", "PIN_D8", "PIN_D5", "PIN_D7")
  val TX_n = Seq("PIN_AG9", "PIN_AH8", "PIN_AG7", "PIN_AF10", "PIN_AD9", "PIN_AB12", "PIN_AB10", "PIN_T12", "PIN_P13",
    "PIN_N10", "PIN_M12", "PIN_L10", "PIN_L11", "PIN_J8", "PIN_J9", "PIN_G7", "PIN_J10")
  val TX_p = Seq("PIN_AG10", "PIN_AH9", "PIN_AG8", "PIN_AF11", "PIN_AD10", "PIN_AB13", "PIN_AB11", "PIN_T13", "PIN_R13",
    "PIN_N11", "PIN_N12", "PIN_M10", "PIN_M11", "PIN_K8", "PIN_K9", "PIN_H7", "PIN_K10")
}

class TR4HSMCB extends HasAlteraHSMCLocs {
  val CLKIN0 = "PIN_AP15"
  val CLKIN_n1 = "PIN_AU7"
  val CLKIN_n2 = "PIN_AW14"
  val CLKIN_p1 = "PIN_AT7"
  val CLKIN_p2 = "PIN_AV14"
  val D = Seq("PIN_AD15", "PIN_AV13", "PIN_AE15", "PIN_AW13")
  val OUT0 = "PIN_AN15"
  val OUT_n1 = "PIN_"
  val OUT_p1 = "PIN_"
  val OUT_n2 = "PIN_AJ10"
  val OUT_p2 = "PIN_AH10"
  val RX_n = Seq("PIN_AW10", "PIN_AU9", "PIN_AW7", "PIN_AW5", "PIN_AW4", "PIN_AW8", "PIN_AT5", "PIN_AU6", "PIN_AR8",
    "PIN_AU8", "PIN_AU10", "PIN_AV11", "PIN_AT13", "PIN_AK13", "PIN_AJ14", "PIN_AF14", "PIN_AM13")
  val RX_p = Seq("PIN_AV10", "PIN_AT9", "PIN_AV7", "PIN_AW6", "PIN_AV5", "PIN_AV8", "PIN_AR5", "PIN_AT6", "PIN_AP8",
    "PIN_AT8", "PIN_AT10", "PIN_AU11", "PIN_AR13", "PIN_AJ13", "PIN_AH14", "PIN_AE14", "PIN_AL13")
  val TX_n = Seq("PIN_AL15", "PIN_AU14", "PIN_AW11", "PIN_AM14", "PIN_AU12", "PIN_AN14", "PIN_AG15", "PIN_AP9", "PIN_AM8",
    "PIN_AL9", "PIN_AM10", "PIN_AJ11", "PIN_AH12", "PIN_AE12", "PIN_AG13", "PIN_AD12", "PIN_AP7")
  val TX_p = Seq("PIN_AN13", "PIN_AT14", "PIN_AW12", "PIN_AL14", "PIN_AT12", "PIN_AP13", "PIN_AG14", "PIN_AN9", "PIN_AL8",
    "PIN_AK9", "PIN_AL10", "PIN_AH11", "PIN_AG12", "PIN_AE13", "PIN_AF13", "PIN_AD13", "PIN_AN7")
}

class TR4HSMCC extends HasAlteraHSMCLocs {
  val CLKIN0 = "PIN_AU27"
  val CLKIN_n1 = "PIN_AE35"
  val CLKIN_n2 = "PIN_AC35"
  val CLKIN_p1 = "PIN_AF34"
  val CLKIN_p2 = "PIN_AC34"
  val D = Seq("PIN_AB27", "PIN_AJ25", "PIN_AB28", "PIN_AK25")
  val OUT0 = "PIN_AT27"
  val OUT_n1 = "PIN_AG35"
  val OUT_p1 = "PIN_AG34"
  val OUT_n2 = "PIN_AD25"
  val OUT_p2 = "PIN_AE25"
  val RX_n = Seq("PIN_AN26", "PIN_AR28", "PIN_AU28", "PIN_AW29", "PIN_AW30", "PIN_AW32", "PIN_AK26", "PIN_AG25", "PIN_AK33",
    "PIN_AN35", "PIN_AM35", "PIN_AL35", "PIN_AK35", "PIN_AJ35", "PIN_AH35", "PIN_AH33", "PIN_AC32")
  val RX_p = Seq("PIN_AM26", "PIN_AP28", "PIN_AT28", "PIN_AV28", "PIN_AV29", "PIN_AV32", "PIN_AJ26", "PIN_AF25", "PIN_AJ32",
    "PIN_AN34", "PIN_AM34", "PIN_AL34", "PIN_AK34", "PIN_AJ34", "PIN_AH34", "PIN_AH32", "PIN_AC31")
  val TX_n = Seq("PIN_AC29", "PIN_AD29", "PIN_AE29", "PIN_AG30", "PIN_AE31", "PIN_AG32", "PIN_AD31", "PIN_AB31", "PIN_AH26",
    "PIN_AE24", "PIN_AW28", "PIN_AG24", "PIN_AV31", "PIN_AW34", "PIN_AP26", "PIN_AT29", "PIN_AN27")
  val TX_p = Seq("PIN_AC28", "PIN_AD28", "PIN_AE28", "PIN_AF29", "PIN_AE30", "PIN_AG31", "PIN_AD30", "PIN_AB30", "PIN_AL27",
    "PIN_AK27", "PIN_AW27", "PIN_AH24", "PIN_AW31", "PIN_AW33", "PIN_AL25", "PIN_AU29", "PIN_AP27")
}

class TR4HSMCD extends HasAlteraHSMCLocs {
  val CLKIN0 = "PIN_AA35"
  val CLKIN_n1 = "PIN_W35"
  val CLKIN_n2 = "PIN_J35"
  val CLKIN_p1 = "PIN_W34"
  val CLKIN_p2 = "PIN_J34"
  val D = Seq("PIN_AJ29", "PIN_AR31", "PIN_AK29", "PIN_AT30")
  val OUT0 = "PIN_P19"
  val OUT_n1 = "PIN_W33"
  val OUT_p1 = "PIN_W32"
  val OUT_n2 = "PIN_L32"
  val OUT_p2 = "PIN_M32"
  val RX_n = Seq("PIN_AU31", "PIN_AU32", "PIN_AU33", "PIN_AV34", "PIN_AP34", "PIN_AR34", "PIN_AR35", "PIN_AP33", "PIN_AN31",
    "PIN_AP30", "PIN_AR32", "PIN_U35", "PIN_V31", "PIN_N34", "PIN_M34", "PIN_L35", "PIN_K35")
  val RX_p = Seq("PIN_AT31", "PIN_AT32", "PIN_AT33", "PIN_AU34", "PIN_AN33", "PIN_AT34", "PIN_AP35", "PIN_AN32", "PIN_AM31",
    "PIN_AN30", "PIN_AP32", "PIN_V34", "PIN_U31", "PIN_N33", "PIN_M33", "PIN_L34", "PIN_K34")
  val TX_n = Seq("PIN_AM29", "PIN_AL30", "PIN_AL32", "PIN_AH30", "PIN_AH27", "PIN_AH29", "PIN_AH28", "PIN_AE27", "PIN_AD26",
    "PIN_AF26", "PIN_V30", "PIN_V28", "PIN_T31", "PIN_R33", "PIN_P32", "PIN_R31", "PIN_AL31")
  val TX_p = Seq("PIN_AL29", "PIN_AK30", "PIN_AK32", "PIN_AJ31", "PIN_AG27", "PIN_AG29", "PIN_AG28", "PIN_AD27", "PIN_AC26",
    "PIN_AE26", "PIN_V29", "PIN_W28", "PIN_T30", "PIN_R32", "PIN_P31", "PIN_R30", "PIN_AK31")
}

class TR4HSMCE extends HasAlteraHSMCLocs {
  val CLKIN0 = "PIN_C13"
  val CLKIN_n1 = "PIN_W5"
  val CLKIN_n2 = "PIN_"
  val CLKIN_p1 = "PIN_W6"
  val CLKIN_p2 = "PIN_AB6"
  val D = Seq("PIN_V12", "PIN_W8", "PIN_V11", "PIN_W7")
  val OUT0 = "PIN_C12"
  val OUT_n1 = "PIN_W11"
  val OUT_p1 = "PIN_W12"
  val OUT_n2 = "PIN_AA5"
  val OUT_p2 = "PIN_R14"
  val RX_n = Seq("PIN_U5", "PIN_R5", "PIN_P6", "PIN_N5", "PIN_N7", "PIN_L5", "PIN_K5", "PIN_J5", "PIN_N14",
    "PIN_K13", "PIN_K14", "PIN_G13", "PIN_E13", "PIN_A11", "PIN_E14", "PIN_A13", "PIN_C14")
  val RX_p = Seq("PIN_V6", "PIN_R6", "PIN_R7", "PIN_N6", "PIN_N8", "PIN_M6", "PIN_K6", "PIN_J6", "PIN_P14",
    "PIN_L13", "PIN_L14", "PIN_H13", "PIN_F13", "PIN_B11", "PIN_F14", "PIN_B13", "PIN_D14")
  val TX_n = Seq("PIN_V9", "PIN_R10", "PIN_T9", "PIN_R8", "PIN_P8", "PIN_M7", "PIN_L7", "PIN_J7", "PIN_M13",
    "PIN_K12", "PIN_B10", "PIN_C11", "PIN_J13", "PIN_D13", "PIN_A14", "PIN_G14", "PIN_J15")
  val TX_p = Seq("PIN_V10", "PIN_T10", "PIN_U10", "PIN_R9", "PIN_N9", "PIN_M8", "PIN_L8", "PIN_K7", "PIN_N13",
    "PIN_M14", "PIN_D11", "PIN_A10", "PIN_J12", "PIN_F12", "PIN_B14", "PIN_H14", "PIN_K15")
}

class TR4HSMCF extends HasAlteraHSMCLocs {
  val CLKIN0 = "PIN_AV22"
  val CLKIN_n1 = "PIN_AW21"
  val CLKIN_n2 = "PIN_AT21"
  val CLKIN_p1 = "PIN_AW20"
  val CLKIN_p2 = "PIN_AR22"
  val D = Seq("PIN_AU25", "PIN_AV26", "PIN_AT25", "PIN_AW26")
  val OUT0 = "PIN_AP20"
  val OUT_n1 = "PIN_AP21"
  val OUT_p1 = "PIN_AN21"
  val OUT_n2 = "PIN_AJ20"
  val OUT_p2 = "PIN_AH20"
  val RX_n = Seq("PIN_AU26", "PIN_AU24", "PIN_AP24", "PIN_AU23", "PIN_AT20", "PIN_AU22", "PIN_AV20", "PIN_AU19", "PIN_AU18",
    "PIN_AV17", "PIN_AN22", "PIN_AP18", "PIN_AL23", "PIN_AJ23", "PIN_AG22", "PIN_AF20", "PIN_AF19")
  val RX_p = Seq("PIN_AT26", "PIN_AT24", "PIN_AN24", "PIN_AT23", "PIN_AR20", "PIN_AT22", "PIN_AU20", "PIN_AT19", "PIN_AT18",
    "PIN_AU17", "PIN_AM22", "PIN_AN18", "PIN_AK23", "PIN_AH23", "PIN_AF22", "PIN_AE20", "PIN_AE19")
  val TX_n = Seq("PIN_AW25", "PIN_AP25", "PIN_AW23", "PIN_AR23", "PIN_AN23", "PIN_AM25", "PIN_AL21", "PIN_AP19", "PIN_AW18",
    "PIN_AM19", "PIN_AK24", "PIN_AH22", "PIN_AE22", "PIN_AE21", "PIN_AG20", "PIN_AE18", "PIN_AG19")
  val TX_p = Seq("PIN_AV25", "PIN_AR25", "PIN_AV23", "PIN_AP23", "PIN_AM23", "PIN_AN25", "PIN_AL22", "PIN_AR19", "PIN_AT17",
    "PIN_AN19", "PIN_AJ22", "PIN_AE23", "PIN_AF23", "PIN_AG21", "PIN_AD21", "PIN_AG18", "PIN_AD19")
}

object HSMCEnums {
  val CLKIN0 = 0
  val CLKIN_n1 = 1
  val CLKIN_n2 = 2
  val CLKIN_p1 = 3
  val CLKIN_p2 = 4
  val D = 5
  val OUT0 = 6
  val OUT_n1 = 7
  val OUT_p1 = 8
  val OUT_n2 = 9
  val OUT_p2 = 10
  val RX_n = 11
  val RX_p = 12
  val TX_n = 13
  val TX_p = 14
}

case class TR4HSMCGroup(hsmc: () => HasAlteraHSMCLocs,  elem: Seq[(Int, Int)]) extends TR4Elem {
  val hsmcimpl = hsmc()
  val gpio = Seq(
    Seq(hsmcimpl.CLKIN0),
    Seq(hsmcimpl.CLKIN_n1),
    Seq(hsmcimpl.CLKIN_n2),
    Seq(hsmcimpl.CLKIN_p1),
    Seq(hsmcimpl.CLKIN_p2),
    hsmcimpl.D,
    Seq(hsmcimpl.OUT0),
    Seq(hsmcimpl.OUT_n1),
    Seq(hsmcimpl.OUT_n2),
    Seq(hsmcimpl.OUT_p1),
    Seq(hsmcimpl.OUT_p2),
    hsmcimpl.RX_n,
    hsmcimpl.RX_p,
    hsmcimpl.TX_n,
    hsmcimpl.TX_p,
  )
  elem.foreach{ case (i, j) =>
    require(i < gpio.length, s"Indexing a no existent GPIO group ${i} (of ${gpio.length})")
    require(j < gpio(i).length, s"Indexing a no existent GPIO ${j} from group ${i} (of ${gpio(i).length})")
  }
  val GetBindings = elem.map{case (i,j) => gpio(i)(j)}
  val GetStandard = "2.5V"
}

case class TR4HSMC2GPIOGroup(hsmc: () => HasAlteraHSMCLocs,  elem: Seq[(Int, Int)]) extends TR4Elem {
  import HSMCEnums._
  val bind = Seq(
    Seq(
      CLKIN_n2 -> 0, RX_n -> 16,
      CLKIN_p2 -> 0, RX_p -> 16,
      TX_n -> 16, RX_n -> 15,
      TX_p -> 16, RX_p -> 15,
      TX_n -> 15, RX_n -> 14,
      // 5V, GND
      TX_p -> 15, RX_p -> 14,
      TX_n -> 14, RX_n -> 13,
      TX_p -> 14, RX_p -> 13,
      OUT_n2 -> 0, RX_n -> 12,
      OUT_p2 -> 0, RX_p -> 12,
      TX_n -> 13, RX_n -> 11,
      TX_p -> 13, RX_p -> 11,
      TX_n -> 12, RX_n -> 10,
      // 3.3V, GND
      TX_p -> 12, RX_p -> 10,
      TX_n -> 11, RX_n -> 9,
      TX_p -> 11, RX_p -> 9,
      TX_n -> 10, TX_n -> 9,
      TX_p -> 10, TX_p -> 9
    ),
    Seq(
      CLKIN_n1 -> 0, RX_n -> 7,
      CLKIN_p1 -> 0, RX_p -> 7,
      TX_n -> 7, RX_n -> 6,
      TX_p -> 7, RX_p -> 6,
      TX_n -> 6, RX_n -> 5,
      // 5V, GND
      TX_p -> 6, RX_p -> 5,
      TX_n -> 5, RX_n -> 4,
      TX_p -> 5, RX_p -> 4,
      OUT_n1 -> 0, RX_n -> 3,
      OUT_p1 -> 0, RX_p -> 3,
      TX_n -> 4, RX_n -> 2,
      TX_p -> 4, RX_p -> 2,
      TX_n -> 3, RX_n -> 1,
      // 3.3V, GND
      TX_p -> 3, RX_p -> 1,
      TX_n -> 2, RX_n -> 0,
      TX_p -> 2, RX_p -> 0,
      TX_n -> 1, TX_n -> 0,
      TX_p -> 1, TX_p -> 0
    )
  )
  elem.foreach{ case (i, j) =>
    require(i < bind.length, s"Indexing a no existent GPIO group ${i} (of ${bind.length})")
    require(j < bind(i).length, s"Indexing a no existent GPIO ${j} from group ${i} (of ${bind(i).length})")
  }
  val transform = elem.map{case(i, j) => bind(i)(j)}
  val internal = TR4HSMCGroup(hsmc, transform)
  val GetBindings = internal.GetBindings
  val GetStandard = internal.GetStandard
}

// DDR3
object TR4DDR3Locs {
  val mem_a = Seq("PIN_N23", "PIN_C22", "PIN_M22", "PIN_D21", "PIN_P24", "PIN_A24", "PIN_M21", "PIN_D17", "PIN_A25",
    "PIN_N25", "PIN_C24", "PIN_N21", "PIN_M25", "PIN_K26"/*, "PIN_F16", "PIN_R20"*/)
  val mem_dm = Seq("PIN_G16", "PIN_N16", "PIN_P23", "PIN_B29", "PIN_H28", "PIN_E17", "PIN_C26", "PIN_E23")
  val mem_dq = Seq("PIN_G15",
    "PIN_F15", "PIN_C16", "PIN_B16", "PIN_G17", "PIN_A16", "PIN_D16", "PIN_E16", "PIN_N17", "PIN_M17", "PIN_K17",
    "PIN_L16", "PIN_P16", "PIN_P17", "PIN_J17", "PIN_H17", "PIN_N22", "PIN_M23", "PIN_J25", "PIN_M24", "PIN_R22",
    "PIN_P22", "PIN_K24", "PIN_J24", "PIN_A27", "PIN_A28", "PIN_C29", "PIN_C30", "PIN_C27", "PIN_D27", "PIN_A31",
    "PIN_B31", "PIN_G27", "PIN_G29", "PIN_F28", "PIN_F27", "PIN_E28", "PIN_D28", "PIN_H26", "PIN_J26", "PIN_F19",
    "PIN_G19", "PIN_F20", "PIN_G20", "PIN_C17", "PIN_F17", "PIN_C18", "PIN_D18", "PIN_D25", "PIN_C25", "PIN_G24",
    "PIN_G25", "PIN_B25", "PIN_A26", "PIN_D26", "PIN_F24", "PIN_F23", "PIN_G23", "PIN_J22", "PIN_H22", "PIN_K22",
    "PIN_D22", "PIN_G22", "PIN_E22")
  val mem_dqs = Seq("PIN_D15", "PIN_K16", "PIN_L23", "PIN_C28", "PIN_E29", "PIN_G18", "PIN_F25", "PIN_J23")
  val mem_dqs_n = Seq("PIN_C15", "PIN_J16", "PIN_K23", "PIN_B28", "PIN_D29", "PIN_F18", "PIN_E25", "PIN_H23")
  val mem_ba = Seq("PIN_B26", "PIN_A29", "PIN_R24")
  val mem_cke = Seq("PIN_P25", "PIN_M16")
  val mem_ck = Seq("PIN_K27", "PIN_L25")
  val mem_ck_n = Seq("PIN_J27", "PIN_K28")
  val mem_cs_n = Seq("PIN_D23", "PIN_G28")
  val mem_odt = Seq("PIN_F26", "PIN_G26")
  val mem_cas_n = "PIN_L26"
  val mem_event_n = "PIN_R18"
  val mem_ras_n = "PIN_D24"
  val mem_reset_n = "PIN_J18"
  val mem_scl = "PIN_H19"
  val mem_sda = "PIN_P18"
  val mem_we_n = "PIN_M27"
  val mem_oct_rdn = "PIN_N26"
  val mem_oct_rup = "PIN_P26"
}

class DDRTR4PlacedOverlay(val shell: TR4Shell, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
  extends DDRPlacedOverlay[AlteraMemIfIODDR3](name, designInput, shellInput)
{
  val ddrParams = AlteraMemIfDDR3Config()
  val memifParams = AlteraMemIfParams(address = AddressSet.misaligned(di.baseAddress, 0x40000000)) // Always 1GB
  val memif     = LazyModule(new AlteraMemIf(c = memifParams, ddrc = ddrParams))
  val ddrUI     = shell { ClockSourceNode(freqMHz = 150) }
  val areset    = shell { ClockSinkNode(Seq(ClockSinkParameters())) }
  areset := designInput.wrangler := ddrUI

  def overlayOutput = DDROverlayOutput(ddr = memif.node)
  def ioFactory = new AlteraMemIfIODDR3(ddrParams)

  val getStatus = shell { InModuleBody { Wire(new Bundle with AlteraMemIfDDR3User) } }

  shell { InModuleBody {
    require (shell.ddr_clock.get.isDefined, "Use of DDRTR4PlacedOverlay depends on SysClockTR4PlacedOverlay")
    val (sys, _) = shell.ddr_clock.get.get.overlayOutput.node.out(0)
    val (ui, _) = ddrUI.out(0)
    val (ar, _) = areset.in(0)
    val port = memif.module.io.port
    io <> port.viewAsSupertype(new AlteraMemIfIODDR3(ddrParams))
    ui.clock := port.mem_afi_clk_clk
    ui.reset := !port.mem_afi_reset_reset_n  // TODO: Get the locked also?
    port.mem_pll_ref_clk_clk := sys.clock
    port.mem_global_reset_reset_n := !sys.reset.asBool // pllReset
    port.mem_soft_reset_reset := sys.reset

    getStatus.mem_status_local_init_done := port.mem_status_local_init_done
    getStatus.mem_status_local_cal_success := port.mem_status_local_cal_success
    getStatus.mem_status_local_cal_fail := port.mem_status_local_cal_fail

    TR4DDR3Locs.mem_a.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_a, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_a, i), "SSTL-15 CLASS I")
      shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_a, i), "MAXIMUM CURRENT")
    }
    TR4DDR3Locs.mem_dm.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_dm, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_dm, i), "SSTL-15 CLASS I")
      shell.io_tcl.addTermination(IOPin(io.memory_mem_dm, i), "50 OHM WITH CALIBRATION")
      shell.io_tcl.addGroup(
        IOPin(io.memory_mem_dqs, i),
        IOPin(io.memory_mem_dm, i),
        "9"
      )
      shell.io_tcl.addInterfaceDelay(IOPin(io.memory_mem_dm, i))
    }
    TR4DDR3Locs.mem_dq.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_dq, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_dq, i), "SSTL-15 CLASS I")
      shell.io_tcl.addTermination(IOPin(io.memory_mem_dq, i), "50 OHM WITH CALIBRATION")
      shell.io_tcl.addGroup(
        IOPin(io.memory_mem_dqs, i / 8),
        IOPin(io.memory_mem_dq, i),
        "9"
      )
      shell.io_tcl.addInterfaceDelay(IOPin(io.memory_mem_dq, i))
    }
    TR4DDR3Locs.mem_dqs.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_dqs, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_dqs, i), "DIFFERENTIAL 1.5-V SSTL CLASS I")
      shell.io_tcl.addTermination(IOPin(io.memory_mem_dqs, i), "50 OHM WITH CALIBRATION")
      shell.io_tcl.addInterfaceDelay(IOPin(io.memory_mem_dqs, i))
    }
    TR4DDR3Locs.mem_dqs_n.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_dqs_n, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_dqs_n, i), "DIFFERENTIAL 1.5-V SSTL CLASS I")
      shell.io_tcl.addTermination(IOPin(io.memory_mem_dqs_n, i), "50 OHM WITH CALIBRATION")
      shell.io_tcl.addInterfaceDelay(IOPin(io.memory_mem_dqs_n, i))
    }
    TR4DDR3Locs.mem_ba.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_ba, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_ba, i), "SSTL-15 CLASS I")
      shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_ba, i), "MAXIMUM CURRENT")
    }
    TR4DDR3Locs.mem_cke.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_cke, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_cke, i), "SSTL-15 CLASS I")
      shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_cke, i), "MAXIMUM CURRENT")
    }
    TR4DDR3Locs.mem_ck.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_ck, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_ck, i), "DIFFERENTIAL 1.5-V SSTL CLASS I")
      shell.io_tcl.addTermination(IOPin(io.memory_mem_ck, i), "50 OHM WITHOUT CALIBRATION")
    }
    TR4DDR3Locs.mem_ck_n.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_ck_n, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_ck_n, i), "DIFFERENTIAL 1.5-V SSTL CLASS I")
      shell.io_tcl.addTermination(IOPin(io.memory_mem_ck_n, i), "50 OHM WITHOUT CALIBRATION")
    }
    TR4DDR3Locs.mem_cs_n.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_cs_n, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_cs_n, i), "SSTL-15 CLASS I")
      shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_cs_n, i), "MAXIMUM CURRENT")
    }
    TR4DDR3Locs.mem_odt.zipWithIndex.foreach { case (pin, i) =>
      shell.io_tcl.addPackagePin(IOPin(io.memory_mem_odt, i), pin)
      shell.io_tcl.addIOStandard(IOPin(io.memory_mem_odt, i), "SSTL-15 CLASS I")
      shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_odt, i), "MAXIMUM CURRENT")
    }
    io.oct.rdn.foreach { rdn =>
      shell.io_tcl.addPackagePin(IOPin(rdn), TR4DDR3Locs.mem_oct_rdn)
      shell.io_tcl.addIOStandard(IOPin(rdn), "1.5V")
    }
    io.oct.rup.foreach { rup =>
      shell.io_tcl.addPackagePin(IOPin(rup), TR4DDR3Locs.mem_oct_rup)
      shell.io_tcl.addIOStandard(IOPin(rup), "1.5V")
    }
    shell.io_tcl.addPackagePin(IOPin(io.memory_mem_cas_n), TR4DDR3Locs.mem_cas_n)
    shell.io_tcl.addIOStandard(IOPin(io.memory_mem_cas_n), "SSTL-15 CLASS I")
    shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_cas_n), "MAXIMUM CURRENT")
    shell.io_tcl.addPackagePin(IOPin(io.memory_mem_ras_n), TR4DDR3Locs.mem_ras_n)
    shell.io_tcl.addIOStandard(IOPin(io.memory_mem_ras_n), "SSTL-15 CLASS I")
    shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_ras_n), "MAXIMUM CURRENT")
    io.memory_mem_reset_n.foreach { reset_n =>
      shell.io_tcl.addPackagePin(IOPin(reset_n), TR4DDR3Locs.mem_reset_n)
      shell.io_tcl.addIOStandard(IOPin(reset_n), "1.5V")
    }
    shell.io_tcl.addPackagePin(IOPin(io.memory_mem_we_n), TR4DDR3Locs.mem_we_n)
    shell.io_tcl.addIOStandard(IOPin(io.memory_mem_we_n), "SSTL-15 CLASS I")
    shell.io_tcl.addDriveStrength(IOPin(io.memory_mem_we_n), "MAXIMUM CURRENT")
  } }

  // TODO: Highly cursed. The qip will create this clock, and there is no known way to retrieve it
  // NOTE: If there is a way to retrieve the .sdc from this IP, maybe there is a possibility

  //shell.sdc.addGroupOnlyNames(clocks = Seq("memif|island|blackbox|mem|pll0|pll_afi_clk"))
  //shell.sdc.addGroupOnlyNames(clocks = Seq("memif|island|blackbox|mem|pll0|pll_mem_clk"))
  //shell.sdc.addGroupOnlyNames(clocks = Seq("memif|island|blackbox|mem|pll0|pll_write_clk"))
  //shell.sdc.addGroupOnlyNames(clocks = Seq("memif|island|blackbox|mem|pll0|pll_addr_cmd_clk"))
  //shell.sdc.addGroupOnlyNames(clocks = Seq("memif|island|blackbox|mem|pll0|pll_avl_clk"))
  //shell.sdc.addGroupOnlyNames(clocks = Seq("memif|island|blackbox|mem|pll0|pll_config_clk"))
}

class DDRTR4ShellPlacer(val shell: TR4Shell, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[TR4Shell] {
  def place(designInput: DDRDesignInput) = new DDRTR4PlacedOverlay(shell, valName.name, designInput, shellInput)
}

abstract class TR4Shell()(implicit p: Parameters) extends AlteraGenericShell
{
  // NOTE: The new AlteraShell now put the PLLFactory. Is a shame but we need to use our own Shell
  val pllFactory = new PLLFactory(this, 10, p => Module(new QsysALTPLL(PLLCalcParameters(p))))
  override def designParameters = super.designParameters.alterPartial {
    case PLLFactoryKey => pllFactory
  }

  val pllReset = InModuleBody { Wire(Bool()) }
  val resetPin = InModuleBody { Wire(Bool()) }
  val ndreset = InModuleBody { WireInit(false.B) }
  val topDesign = LazyModule(p(DesignKey)(designParameters))

  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockTR4ShellPlacer(this, 1, ClockInputShellInput()))
  val ddr_clock = Overlay(ClockInputOverlayKey, new SysClockTR4ShellPlacer(this, 3, ClockInputShellInput()))
  val led       = Seq.tabulate(4)(i => Overlay(LEDOverlayKey, new LEDTR4ShellPlacer(this, LEDShellInput(color = "green", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchTR4ShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(3)(i => Overlay(ButtonOverlayKey, new ButtonTR4ShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))
  val ddr       = Overlay(DDROverlayKey, new DDRTR4ShellPlacer(this, DDRShellInput()))

  // Place the sys_clock at the Shell if the user didn't ask for it
  p(ClockInputOverlayKey).foreach(_.place(ClockInputDesignInput()))
  override lazy val module = new TR4ShellImpl(this)
}

class TR4ShellImpl(outer: TR4Shell) extends LazyRawModuleImp(outer) {
  override def provideImplicitClockToLazyChildren = true
  val FAN_CTRL = IO(Output(Bool()))
  FAN_CTRL := true.B
  outer.io_tcl.addPackagePin(FAN_CTRL, "PIN_B17")
  outer.io_tcl.addIOStandard(FAN_CTRL, "1.5V")

  val reset = IO(Analog(1.W))
  outer.io_tcl.addPackagePin(IOPin(reset), "PIN_L19")
  outer.io_tcl.addIOStandard(IOPin(reset), "1.5V")
  val reset_ibuf = Module(new ALT_IOBUF)
  attach(reset_ibuf.io.io, reset)
  outer.resetPin := !reset_ibuf.asInput() || outer.ndreset
  outer.pllReset := outer.resetPin
}



