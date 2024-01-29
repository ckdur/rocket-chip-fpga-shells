package sifive.fpgashells.shell

import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.prci._
import org.chipsalliance.cde.config._
import sifive.fpgashells.shell._
import sifive.fpgashells.clocks._

case class sdram_bb_cfg
(
  SDRAM_HZ: BigInt = 50000000,
  SDRAM_ADDR_W: Int = 24,
  SDRAM_COL_W: Int = 9,
  SDRAM_BANK_W: Int = 2,
  SDRAM_DQM_W: Int = 2,
  SDRAM_DQ_W: Int = 16,
  SDRAM_READ_LATENCY: Int  = 3
) {
  val SDRAM_MHZ = SDRAM_HZ/1000000
  val SDRAM_BANKS = 1 << SDRAM_BANK_W
  val SDRAM_ROW_W = SDRAM_ADDR_W - SDRAM_COL_W - SDRAM_BANK_W
  val SDRAM_REFRESH_CNT = 1 << SDRAM_ROW_W
  val SDRAM_START_DELAY = 100000 / (1000 / SDRAM_MHZ) // 100 uS
  val SDRAM_REFRESH_CYCLES = (64000*SDRAM_MHZ) / SDRAM_REFRESH_CNT-1
}

trait HasSDRAMIf{
  this: Bundle =>
  val cfg: sdram_bb_cfg
  val sdram_clk_o = Output(Bool())
  val sdram_cke_o = Output(Bool())
  val sdram_cs_o = Output(Bool())
  val sdram_ras_o = Output(Bool())
  val sdram_cas_o = Output(Bool())
  val sdram_we_o = Output(Bool())
  val sdram_dqm_o = Output(UInt(cfg.SDRAM_DQM_W.W))
  val sdram_addr_o = Output(UInt(cfg.SDRAM_ROW_W.W))
  val sdram_ba_o = Output(UInt(cfg.SDRAM_BANK_W.W))
  val sdram_data_o = Output(UInt(cfg.SDRAM_DQ_W.W))
  val sdram_data_i = Input(UInt(cfg.SDRAM_DQ_W.W))
  val sdram_drive_o = Output(Bool())
}

class SDRAMIf(val cfg: sdram_bb_cfg = sdram_bb_cfg()) extends Bundle with HasSDRAMIf

case class SDRAMShellInput()
case class SDRAMDesignInput(cfg: sdram_bb_cfg)(implicit val p: Parameters)
case class SDRAMOverlayOutput(port: ModuleValue[SDRAMIf])
trait SDRAMShellPlacer[Shell] extends ShellPlacer[SDRAMDesignInput, SDRAMShellInput, SDRAMOverlayOutput]

case object SDRAMOverlayKey extends Field[Seq[DesignPlacer[SDRAMDesignInput, SDRAMShellInput, SDRAMOverlayOutput]]](Nil)

abstract class SDRAMPlacedOverlay[IO <: Data](val name: String, val di: SDRAMDesignInput, val si: SDRAMShellInput)
  extends IOPlacedOverlay[IO, SDRAMDesignInput, SDRAMShellInput, SDRAMOverlayOutput]
{
  implicit val p = di.p
  val port = shell { InModuleBody { Wire(new SDRAMIf(di.cfg)) } }
  def overlayOutput = SDRAMOverlayOutput(port)
}
