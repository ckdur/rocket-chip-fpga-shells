package sifive.fpgashells.ip.xilinx.xdma

import chisel3._
import chisel3.util._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.cde.config._

trait HasXDMAJunkDMA {
  val usr_irq_req = Input(UInt(1.W))
  val usr_irq_ack  = Output(UInt(1.W))

  val msi_enable     = Output(Bool())
  val msi_vector_width = Output(UInt(3.W))

  val user_lnk_up = Output(Bool())

  val cfg_mgmt_addr = Input(UInt(19.W))
  val cfg_mgmt_write = Input(Bool())
  val cfg_mgmt_write_data = Input(UInt(32.W))
  val cfg_mgmt_byte_enable = Input(UInt(4.W))
  val cfg_mgmt_read = Input(Bool())
  val cfg_mgmt_read_data = Output(UInt(32.W))
  val cfg_mgmt_read_write_done = Output(Bool())
  val cfg_mgmt_type1_cfg_reg_access = Input(Bool())

  val int_qpll1lock_out = Output(UInt(2.W))
  val int_qpll1outrefclk_out = Output(UInt(2.W))
  val int_qpll1outclk_out = Output(UInt(2.W))
}

trait HasXDMABusDMA {
  def mbus:  XDMABusParams

  // M.AW
  val m_axi_awready = Input(Bool())
  val m_axi_awvalid = Output(Bool())
  val m_axi_awid    = Output(UInt(mbus.idBits.W))
  val m_axi_awaddr  = Output(UInt(mbus.addrBits.W))
  val m_axi_awlen   = Output(UInt(8.W))
  val m_axi_awsize  = Output(UInt(3.W))
  val m_axi_awburst = Output(UInt(2.W))
  val m_axi_awprot  = Output(UInt(3.W))
  val m_axi_awcache = Output(UInt(4.W))
  val m_axi_awlock  = Output(Bool())

  // M.AR
  val m_axi_arready = Input(Bool())
  val m_axi_arvalid = Output(Bool())
  val m_axi_arid    = Output(UInt(mbus.idBits.W))
  val m_axi_araddr  = Output(UInt(mbus.addrBits.W))
  val m_axi_arlen   = Output(UInt(8.W))
  val m_axi_arsize  = Output(UInt(3.W))
  val m_axi_arburst = Output(UInt(2.W))
  val m_axi_arprot  = Output(UInt(3.W))
  val m_axi_arcache = Output(UInt(4.W))
  val m_axi_arlock  = Output(Bool())

  // M.W
  val m_axi_wready = Input(Bool())
  val m_axi_wvalid = Output(Bool())
  val m_axi_wdata  = Output(UInt(mbus.dataBits.W))
  val m_axi_wstrb  = Output(UInt(mbus.dataBytes.W))
  val m_axi_wlast  = Output(Bool())

  // M.B
  val m_axi_bready = Output(Bool())
  val m_axi_bvalid = Input(Bool())
  val m_axi_bid    = Input(UInt(mbus.idBits.W))
  val m_axi_bresp  = Input(UInt(2.W))

  // M.R
  val m_axi_rready = Output(Bool())
  val m_axi_rvalid = Input(Bool())
  val m_axi_rid    = Input(UInt(mbus.idBits.W))
  val m_axi_rdata  = Input(UInt(mbus.dataBits.W))
  val m_axi_rresp  = Input(UInt(2.W))
  val m_axi_rlast  = Input(Bool())
}

class XDMADMABlackBoxIO(
  val lanes: Int,
  val mbus:  XDMABusParams) extends Bundle
  with HasXDMAPads
  with HasXDMAClocks
  with HasXDMAJunkDMA
  with HasXDMABusDMA

class XDMADMABlackBox(c: XDMAParams) extends BlackBox
{
  override def desiredName = c.name

  val mbus  = XDMABusParams(c.mIDBits, c.addrBits, c.busBytes)

  val io = IO(new XDMADMABlackBoxIO(c.lanes, mbus))
  val pcieGTs = c.gen match {
    case 1 => "2.5_GT/s"
    case 2 => "5.0_GT/s"
    case 3 => "8.0_GT/s"
    case _ => "wrong"
  }

  // 62.5, 125, 250 (no trailing zeros)
  val formatter = new java.text.DecimalFormat("0.###")
  val axiMHzStr = formatter.format(c.axiMHz)

  val bars = c.bars.zip(c.basesFull).zipWithIndex.map { case ((a, b), i) =>
    f"""  CONFIG.axibar_${i}			{0x${a.base}%X}				\\
       |  CONFIG.axibar_highaddr_${i}		{0x${a.max}%X}				\\
       |  CONFIG.axibar2pciebar_${i}		{0x${b}%X}				\\
       |""".stripMargin
  }

  ElaborationArtefacts.add(s"${desiredName}.vivado.tcl",
    s"""create_ip -vendor xilinx.com -library ip -version 4.1 -name xdma -module_name ${desiredName} -dir $$ipdir -force
       |set_property -dict [list 							\\
       |  CONFIG.functional_mode		{DMA}				\\
       |  CONFIG.pcie_blk_locn			{${c.location}}				\\
       |  CONFIG.device_port_type		{PCI_Express_Endpoint_device}	\\
       |  CONFIG.pf0_bar0_enabled		{true}					\\
       |  CONFIG.pf0_sub_class_interface_menu	{16450_compatible_serial_controller}			\\
       |  CONFIG.ref_clk_freq			{100_MHz}				\\
       |  CONFIG.pl_link_cap_max_link_width	{X${c.lanes}}				\\
       |  CONFIG.pl_link_cap_max_link_speed	{${pcieGTs}}				\\
       |  CONFIG.msi_rx_pin_en			{false}					\\
       |  CONFIG.axisten_freq			{${axiMHzStr}}				\\
       |  CONFIG.axi_addr_width			{${c.addrBits}}				\\
       |  CONFIG.axi_data_width			{${c.busBytes*8}_bit}			\\
       |  CONFIG.axi_id_width			{${c.mIDBits}}				\\
       |  CONFIG.s_axi_id_width			{${c.sIDBits}}				\\
       |  CONFIG.axibar_num			{${c.bars.size}}			\\
       |${bars.mkString}] [get_ips ${desiredName}]
       |""".stripMargin)
}

class DiplomaticXDMADMA(c: XDMAParams)(implicit p:Parameters) extends LazyModule
{
  val master = AXI4MasterNode(Seq(AXI4MasterPortParameters(
    masters = Seq(AXI4MasterParameters(
      name    = c.name,
      id      = IdRange(0, 1 << c.mIDBits),
      aligned = false)))))

  lazy val module = new Impl
  class Impl extends LazyRawModuleImp(this) {
    // Must have the right bus width
    require (master.edges.out(0).slave.beatBytes == c.busBytes)

    val io = IO(new Bundle {
      val pads   = new XDMAPads(c.lanes)
      val clocks = new XDMAClocks
    })

    val blackbox = Module(new XDMADMABlackBox(c))

    val (m, _) = master.out(0)

    // Junk
    blackbox.io.usr_irq_req := false.B  // TODO: We probably do not need this
    blackbox.io.cfg_mgmt_addr := 0.U
    blackbox.io.cfg_mgmt_write := false.B
    blackbox.io.cfg_mgmt_write_data := 0.U
    blackbox.io.cfg_mgmt_byte_enable := 0.U
    blackbox.io.cfg_mgmt_read := false.B
    blackbox.io.cfg_mgmt_type1_cfg_reg_access := false.B

    // Pads
    io.pads.pci_exp_txp := blackbox.io.pci_exp_txp
    io.pads.pci_exp_txn := blackbox.io.pci_exp_txn
    blackbox.io.pci_exp_rxp := io.pads.pci_exp_rxp
    blackbox.io.pci_exp_rxn := io.pads.pci_exp_rxn

    // Clocks
    blackbox.io.sys_clk    := io.clocks.sys_clk
    blackbox.io.sys_clk_gt := io.clocks.sys_clk_gt
    blackbox.io.sys_rst_n  := io.clocks.sys_rst_n
    io.clocks.axi_aclk     := blackbox.io.axi_aclk
    io.clocks.axi_aresetn  := blackbox.io.axi_aresetn

    // M.AW
    blackbox.io.m_axi_awready := m.aw.ready
    m.aw.valid := blackbox.io.m_axi_awvalid
    m.aw.bits.id    := blackbox.io.m_axi_awid
    m.aw.bits.addr  := blackbox.io.m_axi_awaddr
    m.aw.bits.len   := blackbox.io.m_axi_awlen
    m.aw.bits.size  := blackbox.io.m_axi_awsize
    m.aw.bits.burst := blackbox.io.m_axi_awburst
    m.aw.bits.prot  := blackbox.io.m_axi_awprot
    m.aw.bits.cache := blackbox.io.m_axi_awcache
    m.aw.bits.lock  := blackbox.io.m_axi_awlock
    m.aw.bits.qos   := 0.U

    // M.AR
    blackbox.io.m_axi_arready := m.ar.ready
    m.ar.valid := blackbox.io.m_axi_arvalid
    m.ar.bits.id    := blackbox.io.m_axi_arid
    m.ar.bits.addr  := blackbox.io.m_axi_araddr
    m.ar.bits.len   := blackbox.io.m_axi_arlen
    m.ar.bits.size  := blackbox.io.m_axi_arsize
    m.ar.bits.burst := blackbox.io.m_axi_arburst
    m.ar.bits.prot  := blackbox.io.m_axi_arprot
    m.ar.bits.cache := blackbox.io.m_axi_arcache
    m.ar.bits.lock  := blackbox.io.m_axi_arlock
    m.ar.bits.qos   := 0.U

    // M.W
    blackbox.io.m_axi_wready := m.w.ready
    m.w.valid := blackbox.io.m_axi_wvalid
    m.w.bits.data := blackbox.io.m_axi_wdata
    m.w.bits.strb := blackbox.io.m_axi_wstrb
    m.w.bits.last := blackbox.io.m_axi_wlast

    // M.B
    m.b.ready := blackbox.io.m_axi_bready
    blackbox.io.m_axi_bvalid := m.b.valid
    blackbox.io.m_axi_bid   := m.b.bits.id
    blackbox.io.m_axi_bresp := m.b.bits.resp

    // M.R
    m.r.ready := blackbox.io.m_axi_rready
    blackbox.io.m_axi_rvalid := m.r.valid
    blackbox.io.m_axi_rid   := m.r.bits.id
    blackbox.io.m_axi_rdata := m.r.bits.data
    blackbox.io.m_axi_rresp := m.r.bits.resp
    blackbox.io.m_axi_rlast := m.r.bits.last
  }
}