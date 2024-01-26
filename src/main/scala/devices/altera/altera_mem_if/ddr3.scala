package sifive.fpgashells.devices.altera.altera_mem_if

import chisel3._
import chisel3.experimental.attach
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config.Parameters
import sifive.fpgashells.ip.altera.altera_mem_if._

case class AlteraMemIfParams(address : Seq[AddressSet])

class AlteraMemIfIO(c: AlteraMemIfDDR3Config = AlteraMemIfDDR3Config())
  extends AlteraMemIfDDR3IO(c) with AlteraMemIfDDR3ClocksReset

class AlteraMemIfIsland(c : AlteraMemIfParams,
                        val crossing: ClockCrossingType = AsynchronousCrossing(8),
                        ddrc: AlteraMemIfDDR3Config = AlteraMemIfDDR3Config()
                       )(implicit p: Parameters) extends LazyModule with CrossesToOnlyOneClockDomain {

  val ranges = AddressRange.fromSets(c.address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val offset = ranges.head.base
  val depth = ranges.head.size

  val device = new MemoryDevice
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    slaves = Seq(AXI4SlaveParameters(
      address       = c.address,
      resources     = device.reg,
      regionType    = RegionType.UNCACHED,
      executable    = true,
      supportsWrite = TransferSizes(1, 64),
      supportsRead  = TransferSizes(1, 64))),
    beatBytes = 4
  )))

  lazy val module = new Impl

  class Impl extends LazyRawModuleImp(this) {
    val io = IO(new Bundle {
      val port = new AlteraMemIfIO(ddrc)
    })

    childClock := io.port.mem_afi_clk_clk
    childReset := !io.port.mem_afi_reset_reset_n

    //MIG black box instantiation
    val blackbox = Module(new AlteraMemIfDDR3Blackbox(ddrc))
    val (axi_async, _) = node.in(0)

    //pins to top level

    //inouts
    attach(io.port.memory_mem_dq,blackbox.io.memory_mem_dq)
    attach(io.port.memory_mem_dqs_n,blackbox.io.memory_mem_dqs_n)
    attach(io.port.memory_mem_dqs,blackbox.io.memory_mem_dqs)

    //outputs
    io.port.memory_mem_a            := blackbox.io.memory_mem_a
    io.port.memory_mem_ba           := blackbox.io.memory_mem_ba
    io.port.memory_mem_ras_n        := blackbox.io.memory_mem_ras_n
    io.port.memory_mem_cas_n        := blackbox.io.memory_mem_cas_n
    io.port.memory_mem_we_n         := blackbox.io.memory_mem_we_n
    if(ddrc.is_reset) io.port.memory_mem_reset_n.get := blackbox.io.memory_mem_reset_n.get
    io.port.memory_mem_ck           := blackbox.io.memory_mem_ck
    io.port.memory_mem_ck_n         := blackbox.io.memory_mem_ck_n
    io.port.memory_mem_cke          := blackbox.io.memory_mem_cke
    io.port.memory_mem_cs_n         := blackbox.io.memory_mem_cs_n
    io.port.memory_mem_dm           := blackbox.io.memory_mem_dm
    io.port.memory_mem_odt          := blackbox.io.memory_mem_odt

    //inputs
    //NO_BUFFER clock
    blackbox.io.mem_pll_ref_clk_clk := io.port.mem_pll_ref_clk_clk
    blackbox.io.mem_global_reset_reset_n := io.port.mem_global_reset_reset_n
    blackbox.io.mem_soft_reset_reset := io.port.mem_soft_reset_reset
    io.port.mem_afi_clk_clk := blackbox.io.mem_afi_clk_clk
    io.port.mem_afi_reset_reset_n := blackbox.io.mem_afi_reset_reset_n
    (blackbox.io.oct.rdn zip io.port.oct.rdn).foreach{case (a,b) => a := b}
    (blackbox.io.oct.rup zip io.port.oct.rup).foreach{case (a,b) => a := b}
    (blackbox.io.oct.rzqin zip io.port.oct.rzqin).foreach{case (a,b) => a := b}
    io.port.mem_status_local_init_done   := blackbox.io.mem_status_local_init_done
    io.port.mem_status_local_cal_success := blackbox.io.mem_status_local_cal_success
    io.port.mem_status_local_cal_fail    := blackbox.io.mem_status_local_cal_fail

    val awaddr = axi_async.aw.bits.addr - offset.U
    val araddr = axi_async.ar.bits.addr - offset.U

    //slave AXI interface write address ports
    blackbox.io.axi4_awid    := axi_async.aw.bits.id
    blackbox.io.axi4_awaddr  := awaddr //truncated
    blackbox.io.axi4_awlen   := axi_async.aw.bits.len
    blackbox.io.axi4_awsize  := axi_async.aw.bits.size
    blackbox.io.axi4_awburst := axi_async.aw.bits.burst
    blackbox.io.axi4_awlock  := axi_async.aw.bits.lock
    blackbox.io.axi4_awcache := "b0011".U
    blackbox.io.axi4_awprot  := axi_async.aw.bits.prot
    blackbox.io.axi4_awqos   := axi_async.aw.bits.qos
    blackbox.io.axi4_awvalid := axi_async.aw.valid
    axi_async.aw.ready        := blackbox.io.axi4_awready

    //slave interface write data ports
    blackbox.io.axi4_wdata   := axi_async.w.bits.data
    blackbox.io.axi4_wstrb   := axi_async.w.bits.strb
    blackbox.io.axi4_wlast   := axi_async.w.bits.last
    blackbox.io.axi4_wvalid  := axi_async.w.valid
    axi_async.w.ready         := blackbox.io.axi4_wready

    //slave interface write response
    blackbox.io.axi4_bready  := axi_async.b.ready
    axi_async.b.bits.id       := blackbox.io.axi4_bid
    axi_async.b.bits.resp     := blackbox.io.axi4_bresp
    axi_async.b.valid         := blackbox.io.axi4_bvalid

    //slave AXI interface read address ports
    blackbox.io.axi4_arid    := axi_async.ar.bits.id
    blackbox.io.axi4_araddr  := araddr // truncated
    blackbox.io.axi4_arlen   := axi_async.ar.bits.len
    blackbox.io.axi4_arsize  := axi_async.ar.bits.size
    blackbox.io.axi4_arburst := axi_async.ar.bits.burst
    blackbox.io.axi4_arlock  := axi_async.ar.bits.lock
    blackbox.io.axi4_arcache := "b0011".U
    blackbox.io.axi4_arprot  := axi_async.ar.bits.prot
    blackbox.io.axi4_arqos   := axi_async.ar.bits.qos
    blackbox.io.axi4_arvalid := axi_async.ar.valid
    axi_async.ar.ready        := blackbox.io.axi4_arready

    //slace AXI interface read data ports
    blackbox.io.axi4_rready  := axi_async.r.ready
    axi_async.r.bits.id       := blackbox.io.axi4_rid
    axi_async.r.bits.data     := blackbox.io.axi4_rdata
    axi_async.r.bits.resp     := blackbox.io.axi4_rresp
    axi_async.r.bits.last     := blackbox.io.axi4_rlast
    axi_async.r.valid         := blackbox.io.axi4_rvalid
  }
}

class AlteraMemIf(c : AlteraMemIfParams, crossing: ClockCrossingType = AsynchronousCrossing(8), ddrc: AlteraMemIfDDR3Config = AlteraMemIfDDR3Config())(implicit p: Parameters) extends LazyModule {
  val ranges = AddressRange.fromSets(c.address)
  val depth = ranges.head.size

  val buffer  = LazyModule(new TLBuffer)
  val toaxi4  = LazyModule(new TLToAXI4(adapterName = Some("mem")))
  val indexer = LazyModule(new AXI4IdIndexer(idBits = 4))
  val deint   = LazyModule(new AXI4Deinterleaver(p(CacheBlockBytes)))
  val yank    = LazyModule(new AXI4UserYanker)
  val island  = LazyModule(new AlteraMemIfIsland(c, crossing, ddrc))

  val node: TLInwardNode =
    island.crossAXI4In(island.node) := yank.node := deint.node := indexer.node := toaxi4.node := buffer.node

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle {
      val port = new AlteraMemIfIO(ddrc)
    })

    io.port <> island.module.io.port
  }
}

