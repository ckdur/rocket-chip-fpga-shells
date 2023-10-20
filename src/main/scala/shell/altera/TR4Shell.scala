package sifive.fpgashells.shell.altera

import chisel3._
import chisel3.experimental.{Analog, attach}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import sifive.fpgashells.clocks._
//import sifive.fpgashells.devices.xilinx.xilinxvc707mig._
import sifive.fpgashells.ip.altera._
//import sifive.fpgashells.ip.xilinx.vc707mig._
import sifive.fpgashells.shell._

case object GPIO0OverlayKey extends Field[Seq[DesignPlacer[GPIODirectAlteraDesignInput, GPIOShellInput, GPIODirectAlteraOverlayOutput]]](Nil)

class SysClockTR4PlacedOverlay(val shell: AlteraShell, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends SingleEndedClockInputAlteraPlacedOverlay(name, designInput, shellInput)
{
  val bank = 1
  val bank_map = Map(
    1 -> "PIN_AB34",
    3 -> "PIN_AW22",
    4 -> "PIN_AV19",
    7 -> "PIN_A21",
    8 -> "PIN_B23",
  )
  def bank_freq(b: Int): Double = 50
  val node = shell { ClockSourceNode(freqMHz = bank_freq(bank), jitterPS = 50) }

  shell { InModuleBody {
    val clk: Clock = io
    shell.tdc.addPackagePin(clk, bank_map(bank))
    shell.tdc.addIOStandard(clk, "2.5V")
  } }
}

// LEDs
object LEDTR4PinConstraints{
  val pins = Seq("PIN_B19", "PIN_A18", "PIN_D19", "PIN_C19")
}
class LEDTR4PlacedOverlay(val shell: TR4Shell, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDAlteraPlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDTR4PinConstraints.pins(shellInput.number)))
class LEDTR4ShellPlacer(val shell: TR4Shell, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[TR4Shell] {
  def place(designInput: LEDDesignInput) = new LEDTR4PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SysClockTR4ShellPlacer(val shell: AlteraShell, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[AlteraShell] {
  def place(designInput: ClockInputDesignInput) = new SysClockTR4PlacedOverlay(shell, valName.name, designInput, shellInput)
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

case class TR4GPIOGroup(elem: Seq[(Int, Int)]) {
  elem.foreach{ case (i, j) =>
    require(i < GPIOTR4PinConstraints.gpio.length, s"Indexing a no existent GPIO group ${i} (of ${GPIOTR4PinConstraints.gpio.length})")
    require(j < GPIOTR4PinConstraints.gpio(i).length, s"Indexing a no existent GPIO ${j} from group ${i} (of ${GPIOTR4PinConstraints.gpio(i).length})")
  }
}

// GPIO
class GPIOPeripheralTR4PlacedOverlay(val shell: TR4Shell, val which: TR4GPIOGroup, name: String, val designInput: GPIODesignInput, val shellInput: GPIOShellInput)
  extends GPIOAlteraPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    require(io.gpio.length == which.elem.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOTR4PinConstraints.gpio(i)(j), IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, "1.8 V")
    } }
  } }
}

class GPIOPeripheralTR4ShellPlacer(val shell: TR4Shell, val which: TR4GPIOGroup, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIOShellPlacer[TR4Shell] {

  def place(designInput: GPIODesignInput) = new GPIOPeripheralTR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

class GPIO0TR4PlacedOverlay(val shell: TR4Shell, val which: TR4GPIOGroup, name: String, di: GPIODirectAlteraDesignInput, si: GPIOShellInput)
  extends GPIODirectAlteraPlacedOverlay(name, di, si)
{
  shell { InModuleBody {
    require(io.gpio.length == which.elem.length)
    val packagePinsWithPackageIOs = io.gpio.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOTR4PinConstraints.gpio(i)(j), IOPin(io))
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, "1.8 V")
    } }
  } }
}

class GPIO0TR4ShellPlacer(val shell: TR4Shell, val which: TR4GPIOGroup, val shellInput: GPIOShellInput)(implicit val valName: ValName)
  extends GPIODirectAlteraShellPlacer[TR4Shell] {

  def place(designInput: GPIODirectAlteraDesignInput) = new GPIO0TR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// SPI Flash
class SPIFlashTR4PlacedOverlay(val shell: TR4Shell, val which: TR4GPIOGroup, name: String, val designInput: SPIFlashDesignInput, val shellInput: SPIFlashShellInput)
  extends SPIFlashAlteraPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.qspi_sck),
      IOPin(io.qspi_cs),
      IOPin(io.qspi_dq(0)),
      IOPin(io.qspi_dq(1)),
      IOPin(io.qspi_dq(2)),
      IOPin(io.qspi_dq(3)))
    val packagePinsWithPackageIOs = iopins.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOTR4PinConstraints.gpio(i)(j), io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, "1.8 V")
    }
  } }
}

class SPIFlashTR4ShellPlacer(val shell: TR4Shell, val which: TR4GPIOGroup, val shellInput: SPIFlashShellInput)(implicit val valName: ValName)
  extends SPIFlashShellPlacer[TR4Shell] {

  def place(designInput: SPIFlashDesignInput) = new SPIFlashTR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// SPI
class SPITR4PlacedOverlay(val shell: TR4Shell, val which: TR4GPIOGroup, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SPIAlteraPlacedOverlay(name, designInput, shellInput)
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
        (GPIOTR4PinConstraints.gpio(i)(j), io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, "1.8 V")
    }
  } }
}

class SPITR4ShellPlacer(val shell: TR4Shell, val which: TR4GPIOGroup, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[TR4Shell] {

  def place(designInput: SPIDesignInput) = new SPITR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// JTAG Debug
class JTAGDebugTR4PlacedOverlay(val shell: TR4Shell, val which: TR4GPIOGroup, name: String, val designInput: JTAGDebugDesignInput, val shellInput: JTAGDebugShellInput)
  extends JTAGDebugAlteraPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    shell.sdc.addClock("JTCK", IOPin(io.jtag_TCK), 10)
    shell.sdc.addGroup(clocks = Seq("JTCK"))
    val iopins = Seq(IOPin(io.jtag_TDI),
      IOPin(io.jtag_TDO),
      IOPin(io.jtag_TCK),
      IOPin(io.jtag_TMS))
    val packagePinsWithPackageIOs = iopins.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOTR4PinConstraints.gpio(i)(j), io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, "1.8 V")
    }
  } }
}

class JTAGDebugTR4ShellPlacer(val shell: TR4Shell, val which: TR4GPIOGroup, val shellInput: JTAGDebugShellInput)(implicit val valName: ValName)
  extends JTAGDebugShellPlacer[TR4Shell] {

  def place(designInput: JTAGDebugDesignInput) = new JTAGDebugTR4PlacedOverlay(shell, which, valName.name, designInput, shellInput)
}

// UART
class UARTTR4PlacedOverlay(val shell: TR4Shell, val which: TR4GPIOGroup, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTAlteraPlacedOverlay(name, designInput, shellInput, false)
{
  shell { InModuleBody {
    val iopins = Seq(IOPin(io.rxd),
      IOPin(io.txd))
    val packagePinsWithPackageIOs = iopins.zip(which.elem).map {
      case (io, (i, j)) =>
        (GPIOTR4PinConstraints.gpio(i)(j), io)
    }
    println(packagePinsWithPackageIOs)

    packagePinsWithPackageIOs foreach { case (pin, io) =>
      shell.tdc.addPackagePin(io, pin)
      shell.tdc.addIOStandard(io, "1.8 V")
    }
  } }
}

class UARTTR4ShellPlacer(val shell: TR4Shell, val which: TR4GPIOGroup, val shellInput: UARTShellInput)(implicit val valName: ValName)
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
  val OUT_n1 = "PIN_AP10"
  val OUT_p1 = "PIN_AN10"
  val OUT_n2 = "PIN_AJ10"
  val OUT_p2 = "PIN_AH10"
  val RX_n = Seq("PIN_AW10", "PIN_AU9", "PIN_AW7", "PIN_AW5", "PIN_AW4", "PIN_AW8", "PIN_AT5", "PIN_AU6", "PIN_AR8",
    "PIN_AU8", "PIN_AU10", "PIN_AV11", "PIN_AT13", "PIN_AK13", "PIN_AJ14", "PIN_AF14", "PIN_AM13")
  val RX_p = Seq("PIN_AV10", "PIN_AT9", "PIN_AV7", "PIN_AW6", "PIN_AV5", "PIN_AV8", "PIN_AR5", "PIN_AT6", "PIN_AP8",
    "PIN_AT8", "PIN_AT10", "PIN_AU11", "PIN_AR13", "PIN_AJ13", "PIN_AH14", "PIN_AE14", "PIN_AL13")
  val TX_n = Seq("PIN_AL15", "PIN_AU14", "PIN_AW11", "PIN_AM14", "PIN_AU12", "PIN_AN14", "PIN_AG15", "PIN_AP9",
    "PIN_AM8", "PIN_AL9", "PIN_AM10", "PIN_AJ11", "PIN_AH12", "PIN_AE12", "PIN_AG13", "PIN_AD12", "PIN_AP7")
  val TX_p = Seq("PIN_AN13", "PIN_AT14", "PIN_AW12", "PIN_AL14", "PIN_AT12", "PIN_AP13", "PIN_AG14", "PIN_AN9",
    "PIN_AL8", "PIN_AK9", "PIN_AL10", "PIN_AH11", "PIN_AG12", "PIN_AE13", "PIN_AF13", "PIN_AD13", "PIN_AN7")
}

abstract class TR4Shell()(implicit p: Parameters) extends AlteraShell
{
  val pllFactory = new PLLFactory(this, 10, p => Module(new QsysALTPLL(PLLCalcParameters(p))))
  override def designParameters = super.designParameters.alterPartial {
    case PLLFactoryKey => pllFactory
  }

  val pllReset = InModuleBody { Wire(Bool()) }
  val resetPin = InModuleBody { Wire(Bool()) }
  val topDesign = LazyModule(p(DesignKey)(designParameters))

  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockTR4ShellPlacer(this, ClockInputShellInput()))
  val led       = Seq.tabulate(4)(i => Overlay(LEDOverlayKey, new LEDTR4ShellPlacer(this, LEDShellInput(color = "green", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchTR4ShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(3)(i => Overlay(ButtonOverlayKey, new ButtonTR4ShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))
  val uartseq   = Seq(1 -> 22, 1 -> 23)
  val uart      = Overlay(UARTOverlayKey, new UARTTR4ShellPlacer(this, TR4GPIOGroup(uartseq), UARTShellInput()))
  val spiseq    = Seq(
    Seq(1 ->  6, 1 ->  7, 1 ->  4, 1 ->  5, 1 -> 30, 1 -> 31),
    Seq(1 -> 14, 1 -> 15, 1 -> 12, 1 -> 13, 1 -> 32, 1 -> 33),
    Seq(1 -> 10, 1 -> 11, 1 ->  8, 1 ->  9, 1 -> 34, 1 -> 35))
  val spi       = spiseq.zipWithIndex.map{case (s, i) => Overlay(SPIOverlayKey, new SPITR4ShellPlacer(this, TR4GPIOGroup(s), SPIShellInput(i))(ValName(s"spi_${i}")))}
  val jtagseq   = Seq(1 ->  0, 1 ->  1, 1 ->  2, 1 ->  3)
  val jtag      = Overlay(JTAGDebugOverlayKey, new JTAGDebugTR4ShellPlacer(this, TR4GPIOGroup(jtagseq), JTAGDebugShellInput()))
  val qspiseq   = Seq(1 -> 20, 1 -> 21, 1 -> 18, 1 -> 19, 1 -> 28, 1 -> 29)
  val qspi      = Overlay(SPIFlashOverlayKey, new SPIFlashTR4ShellPlacer(this, TR4GPIOGroup(qspiseq), SPIFlashShellInput())(ValName(s"qspi")))
  val gpioseq   = Seq(1 -> 24, 1 -> 25, 1 -> 26, 1 -> 27)
  val gpio      = Overlay(GPIOOverlayKey, new GPIOPeripheralTR4ShellPlacer(this, TR4GPIOGroup(gpioseq), GPIOShellInput()))
  val hsmc      = Seq(
    Overlay(AlteraHSMCOverlayKey, new AlteraHSMCTR4ShellPlacer(this, new TR4HSMCA, AlteraHSMCShellInput("A"))(ValName("HSMC_A"))),
    Overlay(AlteraHSMCOverlayKey, new AlteraHSMCTR4ShellPlacer(this, new TR4HSMCB, AlteraHSMCShellInput("B"))(ValName("HSMC_B")))
  )
  val gpio0seq  = Seq.tabulate(36)(i => 0 -> i)
  val gpio0     = Overlay(GPIO0OverlayKey, new GPIO0TR4ShellPlacer(this, TR4GPIOGroup(gpio0seq), GPIOShellInput()))

  /*def memEnable: Boolean = true
  val mem_a = memEnable.option(IO(Output(Bits((15 + 1).W))))
  val mem_ba = memEnable.option(IO(Output(Bits((2 + 1).W))))
  val mem_cas_n = memEnable.option(IO(Output(Bool())))
  val mem_cke = memEnable.option(IO(Output(Bits((1 + 1).W))))
  val mem_ck = memEnable.option(IO(Output(Bits((0 + 1).W)))) // NOTE: Is impossible to do [0:0]
  val mem_ck_n = memEnable.option(IO(Output(Bits((0 + 1).W)))) // NOTE: Is impossible to do [0:0]
  val mem_cs_n = memEnable.option(IO(Output(Bits((1 + 1).W))))
  val mem_dm = memEnable.option(IO(Output(Bits((7 + 1).W))))
  val mem_dq = memEnable.option(IO(Analog((63 + 1).W)))
  val mem_dqs = memEnable.option(IO(Analog((7 + 1).W)))
  val mem_dqs_n = memEnable.option(IO(Analog((7 + 1).W)))
  val mem_odt = memEnable.option(IO(Output(Bits((1 + 1).W))))
  val mem_ras_n = memEnable.option(IO(Output(Bool())))
  val mem_reset_n = memEnable.option(IO(Output(Bool())))
  val mem_we_n = memEnable.option(IO(Output(Bool())))
  val mem_oct_rdn = memEnable.option(IO(Input(Bool())))
  val mem_oct_rup = memEnable.option(IO(Input(Bool())))*/

  // Place the sys_clock at the Shell if the user didn't ask for it
  p(ClockInputOverlayKey).foreach(_.place(ClockInputDesignInput()))
  override lazy val module = new TR4ShellImpl(this)
}

class TR4ShellImpl(outer: TR4Shell) extends LazyRawModuleImp(outer) {
  val FAN_CTRL = IO(Output(Bool()))
  FAN_CTRL := true.B
  outer.tdc.addPackagePin(FAN_CTRL, "PIN_B17")
  outer.tdc.addIOStandard(FAN_CTRL, "1.5V")

  val reset = IO(Analog(1.W))
  outer.tdc.addPackagePin(IOPin(reset), "PIN_L19")
  outer.tdc.addIOStandard(IOPin(reset), "1.5V")
  val reset_ibuf = Module(new ALT_IOBUF)
  attach(reset_ibuf.io.io, reset)
  outer.resetPin := ~reset_ibuf.asInput()
  outer.pllReset := outer.resetPin

  /*val HSMA = IO(new AlteraHSMC)
  val HSMB = IO(new AlteraHSMC)
  val GPIO0_D = IO(Vec(35 + 1, Analog(1.W)))
  val GPIO1_D = IO(Vec(35 + 1, Analog(1.W)))
  val HSMD = IO(new AlteraHSMC(on1 = false))
  val HSME = IO(new AlteraHSMC(on1 = false))
  val HSMF = IO(new AlteraHSMC(on1 = false, on2 = false))

  HSMA := DontCare
  HSMB := DontCare
  HSMD := DontCare
  HSME := DontCare
  HSMF := DontCare*/
}



