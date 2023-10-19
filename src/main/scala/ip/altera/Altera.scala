package sifive.fpgashells.ip.altera

import chisel3._
import chisel3.experimental.{Analog, DoubleParam, IntParam, RawParam, StringParam, attach}
import chisel3.util.HasBlackBoxInline
import freechips.rocketchip.util._
import sifive.fpgashells.clocks._
import sifive.blocks.devices.pinctrl.BasePin

//-------------------------------------------------------------------------
// IO Lib for Altera boards
//-------------------------------------------------------------------------

class ALT_IOBUF extends BlackBox{
  val io = IO(new Bundle{
    val io = Analog(1.W)
    val oe = Input(Bool())
    val i = Input(Bool())
    val o = Output(Bool())
  })

  def asInput() : Bool = {
    io.oe := false.B
    io.i := false.B
    io.o
  }

  def asOutput(o: Bool) : Unit = {
    io.oe := true.B
    io.i := o
  }

  def fromBase(e: BasePin) : Unit = {
    io.oe := e.o.oe
    io.i := e.o.oval
    e.i.ival := io.o
  }

  def attachTo(analog: Analog) : Unit = {
    attach(analog, io.io)
  }
}

object ALT_IOBUF {
  def apply : ALT_IOBUF = {
    Module(new ALT_IOBUF)
  }

  def apply(analog: Analog) : Bool = {
    val m = Module(new ALT_IOBUF)
    m.attachTo(analog)
    m.asInput()
  }

  def apply(analog: Analog, i: Bool) : Unit = {
    val m = Module(new ALT_IOBUF)
    m.attachTo(analog)
    m.asOutput(i)
  }

  def apply(analog: Analog, e: BasePin) : Unit = {
    val m = Module(new ALT_IOBUF)
    m.attachTo(analog)
    m.fromBase(e)
  }
}

class DDR3_PORT extends Bundle {
  val A = Output(Bits(15.W))
  val BA = Output(Bits(3.W))
  val CK_p = Output(Bits(1.W))
  val CK_n = Output(Bits(1.W))
  val CKE = Output(Bits(1.W))
  val CS_n = Output(Bits(1.W))
  val DM = Output(Bits(4.W))
  val RAS_n = Output(Bool())
  val CAS_n = Output(Bool())
  val WE_n = Output(Bool())
  val RESET_n = Output(Bool())
  val DQ = Analog(32.W)
  val DQS_p = Analog(4.W)
  val DQS_n = Analog(4.W)
  val ODT = Output(Bits(1.W))
  val RZQ = Input(Bool())
}

class clkctrl extends BlackBox {
  val io = IO(new Bundle {
    val inclk = Input(Clock())
    val outclk = Output(Clock())
  })
}

class AlteraHSMC(val on1: Boolean = true, val on2: Boolean = true) extends Bundle {
  val CLKIN0 = Input(Bool())
  val CLKIN_n1 = Input(Bool())
  val CLKIN_n2 = Input(Bool())
  val CLKIN_p1 = Input(Bool())
  val CLKIN_p2 = Input(Bool())
  val D = Vec(4, Analog(1.W))
  val OUT0 = Analog(1.W)
  val OUT_n1 = on1.option(Analog(1.W))
  val OUT_p1 = on1.option(Analog(1.W))
  val OUT_n2 = on2.option(Analog(1.W))
  val OUT_p2 = on2.option(Analog(1.W))
  val RX_n = Vec(17, Analog(1.W))
  val RX_p = Vec(17, Analog(1.W))
  val TX_n = Vec(17, Analog(1.W))
  val TX_p = Vec(17, Analog(1.W))
}

class AlteraFMC(val ext: Boolean = false, val xcvr: Boolean = false) extends Bundle {
  val CLK_M2C_p = Vec(2, Analog(1.W))
  val CLK_M2C_n = Vec(2, Analog(1.W))
  val HA_RX_CLK_p = ext.option(Analog(1.W))
  val HA_RX_CLK_n = ext.option(Analog(1.W))
  val HB_RX_CLK_p = ext.option(Analog(1.W))
  val HB_RX_CLK_n = ext.option(Analog(1.W))
  val LA_RX_CLK_p = Analog(1.W)
  val LA_RX_CLK_n = Analog(1.W)
  val HA_TX_CLK_p = ext.option(Analog(1.W))
  val HA_TX_CLK_n = ext.option(Analog(1.W))
  val HB_TX_CLK_p = ext.option(Analog(1.W))
  val HB_TX_CLK_n = ext.option(Analog(1.W))
  val LA_TX_CLK_p = Analog(1.W)
  val LA_TX_CLK_n = Analog(1.W)
  val HA_TX_p = ext.option(Vec(11, Analog(1.W)))
  val HA_TX_n = ext.option(Vec(11, Analog(1.W)))
  val HA_RX_p = ext.option(Vec(11, Analog(1.W)))
  val HA_RX_n = ext.option(Vec(11, Analog(1.W)))
  val HB_TX_p = ext.option(Vec(11, Analog(1.W)))
  val HB_TX_n = ext.option(Vec(11, Analog(1.W)))
  val HB_RX_p = ext.option(Vec(11, Analog(1.W)))
  val HB_RX_n = ext.option(Vec(11, Analog(1.W)))
  val LA_TX_p = Vec(17, Analog(1.W))
  val LA_TX_n = Vec(17, Analog(1.W))
  val LA_RX_p = Vec(15, Analog(1.W))
  val LA_RX_n = Vec(15, Analog(1.W))

  val GBTCLK_M2C_p = xcvr.option(Vec(2, Input(Bool())))
  val ONBOARD_REFCLK_p = xcvr.option(Vec(2, Input(Bool())))
  val DP_C2M_p = xcvr.option(Vec(10, Output(Bool())))
  DP_C2M_p.foreach(_.foreach(_ := false.B))
  val DP_M2C_p = xcvr.option(Vec(10, Input(Bool())))

  val GA = Vec(2, Analog(1.W))
  val SCL = Analog(1.W)
  val SDA = Analog(1.W)
}

class PLLCalcParameters(name: String, input: PLLInClockParameters, req: Seq[PLLOutClockParameters])
  extends PLLParameters(name, input, req) {
  println(s"PLL Calculation Parameters")
  val inputfreq = input.freqMHz
  val outputfreqs = req.map(_.freqMHz)
  println(s"  Output Freqs: ${outputfreqs.map(_.toString).mkString(", ")}")
  val ratios = outputfreqs.map { ofreq => RationalApprox.toRational(inputfreq / ofreq) }
  println(s"  Ratios: ${ratios.map(_.toString).mkString(", ")}")
  val mults = ratios.map(_.num)
  val divs = ratios.map(_.den)
}

object PLLCalcParameters {
  def apply(name: String, input: PLLInClockParameters, req: Seq[PLLOutClockParameters]) = {
    new PLLCalcParameters(name, input, req)
  }

  def apply(from: PLLParameters) = {
    new PLLCalcParameters(from.name, from.input, from.req)
  }
}

class QsysALTPLL(val c: PLLCalcParameters) extends BlackBox with PLLInstance {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val c0 = if (c.req.size >= 1) Some(Output(Clock())) else None
    val c1 = if (c.req.size >= 2) Some(Output(Clock())) else None
    val c2 = if (c.req.size >= 3) Some(Output(Clock())) else None
    val c3 = if (c.req.size >= 4) Some(Output(Clock())) else None
    val c4 = if (c.req.size >= 5) Some(Output(Clock())) else None
    val c5 = if (c.req.size >= 6) Some(Output(Clock())) else None
    val c6 = if (c.req.size >= 7) Some(Output(Clock())) else None
    val c7 = if (c.req.size >= 8) Some(Output(Clock())) else None
    val c8 = if (c.req.size >= 9) Some(Output(Clock())) else None
    val c9 = if (c.req.size >= 10) Some(Output(Clock())) else None
    val areset = Input(Bool())
    val locked = Output(Bool())
    val read = Input(Bool())
    val write = Input(Bool())
    val address = Input(UInt(2.W))
    val writedata = Input(UInt(32.W))
    val readdata = Output(UInt(32.W))
  })

  val moduleName = c.name
  override def desiredName = c.name

  def getClocks = Seq() ++ io.c0 ++ io.c1 ++ io.c2 ++ io.c3 ++ io.c4 ++
    io.c5 ++ io.c6 ++ io.c7 ++ io.c8 ++ io.c9
  def getInput = io.clk
  def getReset = Some(io.areset)
  def getLocked = io.locked
  def getClockNames = Seq.tabulate(c.req.size) { i => // TODO: Implement this ok
    s"${c.name}/altpll_0/c${i}"
  }
  def tieoffextra = {
    io.read := false.B
    io.write := false.B
    io.address := 0.U
    io.writedata := 0.U
    io.reset := false.B
  }

  val used = Seq.tabulate(7) { i =>
    val statement = if(i < c.req.size) "PORT_USED" else "PORT_UNUSED"
    s"set_instance_parameter_value altpll_0 {PORT_clk${i}} {${statement}}\n"
  }.mkString

  val outputs = c.req.zipWithIndex.map { case (r, i) =>
    s"""set_instance_parameter_value altpll_0 {CLK${i}_DUTY_CYCLE} {${r.dutyCycle.toInt}}
       |set_instance_parameter_value altpll_0 {CLK${i}_PHASE_SHIFT} {${r.phaseDeg.toInt}}
       |#set_instance_parameter_value altpll_0 {CLK${i}_MULTIPLY_BY} {${c.mults(i)}}
       |#set_instance_parameter_value altpll_0 {CLK${i}_DIVIDE_BY} {${c.divs(i)}}
       |""".stripMargin
  }.mkString

  val itimeps = 1e6 / c.input.freqMHz

  ElaborationArtefacts.add(s"${moduleName}.qsys.tcl",
    s"""# qsys scripting (.tcl) file for atlpll
       |package require qsys
       |
       |create_system {${moduleName}}
       |
       |set_project_property DEVICE_FAMILY {Stratix IV}
       |set_project_property DEVICE {EP4SGX230KF40C2}
       |set_project_property HIDE_FROM_IP_CATALOG {false}
       |
       |# Instances and instance parameters
       |add_instance altpll_0 altpll
       |puts [get_instance_parameters altpll_0]
       |get_instance_parameters altpll_0
       |set_instance_parameter_value altpll_0 {AVALON_USE_SEPARATE_SYSCLK} {NO}
       |set_instance_parameter_value altpll_0 {BANDWIDTH} {}
       |set_instance_parameter_value altpll_0 {BANDWIDTH_TYPE} {AUTO}
       |${outputs}
       |${used}
       |
       |set_instance_parameter_value altpll_0 {COMPENSATE_CLOCK} {CLK0}
       |set_instance_parameter_value altpll_0 {INCLK0_INPUT_FREQUENCY} {${itimeps.toInt}}
       |set_instance_parameter_value altpll_0 {INCLK1_INPUT_FREQUENCY} {}
       |set_instance_parameter_value altpll_0 {INTENDED_DEVICE_FAMILY} {Stratix IV}
       |
       |# exported interfaces
       |set_instance_property altpll_0 AUTO_EXPORT {true}
       |
       |# interconnect requirements
       |set_interconnect_requirement {$$system} {qsys_mm.clockCrossingAdapter} {HANDSHAKE}
       |set_interconnect_requirement {$$system} {qsys_mm.enableEccProtection} {FALSE}
       |set_interconnect_requirement {$$system} {qsys_mm.insertDefaultSlave} {FALSE}
       |set_interconnect_requirement {$$system} {qsys_mm.maxAdditionalLatency} {1}
       |
       |save_system {${moduleName}.qsys}
       |
       |""".stripMargin)
}