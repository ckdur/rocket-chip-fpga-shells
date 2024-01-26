package sifive.fpgashells.ip.altera.altera_mem_if

import chisel3._
import chisel3.experimental.Analog
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.util._

case class AlteraMemIfDDR3Config
(
  size_ck: Int = 2,
  size_cke: Int = 2,
  size_csn: Int = 2,
  size_odt: Int = 2,
  addrbit: Int = 14,
  octmode: Int = 0,
  is_reset: Boolean = false,
  name: String = "main"
)

class AlteraMemIfIODDR3(val c: AlteraMemIfDDR3Config) extends Bundle {
  val memory_mem_a       = Output(Bits(c.addrbit.W))
  val memory_mem_ba      = Output(Bits(3.W))
  val memory_mem_ck      = Output(Bits(c.size_ck.W))
  val memory_mem_ck_n    = Output(Bits(c.size_ck.W))
  val memory_mem_cke     = Output(Bits(c.size_cke.W))
  val memory_mem_cs_n    = Output(Bits(c.size_csn.W))
  val memory_mem_dm      = Output(Bits(8.W))
  val memory_mem_ras_n   = Output(Bool())
  val memory_mem_cas_n   = Output(Bool())
  val memory_mem_we_n    = Output(Bool())
  val memory_mem_reset_n = if(c.is_reset) Some(Output(Bool())) else None
  val memory_mem_dq      = Analog(64.W)
  val memory_mem_dqs     = Analog(8.W)
  val memory_mem_dqs_n   = Analog(8.W)
  val memory_mem_odt     = Output(Bits(c.size_odt.W))
  val oct = new Bundle {
    val rdn              = (c.octmode == 0).option(Input(Bool()))
    val rup              = (c.octmode == 0).option(Input(Bool()))
    val rzqin            = (c.octmode == 1).option(Input(Bool()))
  }
}

trait AlteraMemIfDDR3ClocksReset extends Bundle {
  val mem_pll_ref_clk_clk = Input(Clock())
  val mem_soft_reset_reset = Input(Bool())
  val mem_global_reset_reset_n = Input(Bool())
  val mem_afi_clk_clk = Output(Clock())
  val mem_afi_reset_reset_n = Output(Bool())
}

trait AlteraMemIfDDR3User extends Bundle {
  val mem_status_local_init_done   = Output(Bool())
  val mem_status_local_cal_success = Output(Bool())
  val mem_status_local_cal_fail    = Output(Bool())
}

class AlteraMemIfDDR3IO(c: AlteraMemIfDDR3Config = AlteraMemIfDDR3Config()) extends AlteraMemIfIODDR3(c) with AlteraMemIfDDR3User

class AlteraMemIfDDR3Blackbox(val c: AlteraMemIfDDR3Config = AlteraMemIfDDR3Config())(implicit val p:Parameters) extends BlackBox {
  override def desiredName = c.name
  val moduleName = c.name

  val io = IO(new AlteraMemIfDDR3IO(c) with AlteraMemIfDDR3ClocksReset {
    //axi_s
    //slave interface write address ports
    val axi4_awid = Input(Bits((4).W))
    val axi4_awaddr = Input(Bits((32).W))
    val axi4_awlen = Input(Bits((8).W))
    val axi4_awsize = Input(Bits((3).W))
    val axi4_awburst = Input(Bits((2).W))
    val axi4_awlock = Input(Bits((1).W))
    val axi4_awcache = Input(Bits((4).W))
    val axi4_awprot = Input(Bits((3).W))
    val axi4_awqos = Input(Bits((4).W))
    val axi4_awvalid = Input(Bool())
    val axi4_awready = Output(Bool())
    //slave interface write data ports
    val axi4_wdata = Input(Bits((32).W))
    val axi4_wstrb = Input(Bits((4).W))
    val axi4_wlast = Input(Bool())
    val axi4_wvalid = Input(Bool())
    val axi4_wready = Output(Bool())
    //slave interface write response ports
    val axi4_bready = Input(Bool())
    val axi4_bid = Output(Bits((4).W))
    val axi4_bresp = Output(Bits((2).W))
    val axi4_bvalid = Output(Bool())
    //slave interface read address ports
    val axi4_arid = Input(Bits((4).W))
    val axi4_araddr = Input(Bits((32).W))
    val axi4_arlen = Input(Bits((8).W))
    val axi4_arsize = Input(Bits((3).W))
    val axi4_arburst = Input(Bits((2).W))
    val axi4_arlock = Input(Bits((1).W))
    val axi4_arcache = Input(Bits((4).W))
    val axi4_arprot = Input(Bits((3).W))
    val axi4_arqos = Input(Bits((4).W))
    val axi4_arvalid = Input(Bool())
    val axi4_arready = Output(Bool())
    //slave interface read data ports
    val axi4_rready = Input(Bool())
    val axi4_rid = Output(Bits((4).W))
    val axi4_rdata = Output(Bits((32).W))
    val axi4_rresp = Output(Bits((2).W))
    val axi4_rlast = Output(Bool())
    val axi4_rvalid = Output(Bool())
  })

  ElaborationArtefacts.add(s"${moduleName}.qsys.tcl",
    s"""# qsys scripting (.tcl) file for altera_mem_if
       |package require qsys
       |
       |create_system {main}
       |
       |set_project_property DEVICE_FAMILY {Stratix IV}
       |set_project_property DEVICE {EP4SGX230KF40C2}
       |set_project_property HIDE_FROM_IP_CATALOG {false}
       |
       |# Instances and instance parameters
       |# (disabled instances are intentionally culled)
       |add_instance axi altera_axi_bridge
       |set_instance_parameter_value axi {ADDR_WIDTH} {30}
       |set_instance_parameter_value axi {AXI_VERSION} {AXI4}
       |set_instance_parameter_value axi {COMBINED_ACCEPTANCE_CAPABILITY} {16}
       |set_instance_parameter_value axi {COMBINED_ISSUING_CAPABILITY} {16}
       |set_instance_parameter_value axi {DATA_WIDTH} {32}
       |set_instance_parameter_value axi {M0_ID_WIDTH} {4}
       |set_instance_parameter_value axi {READ_ACCEPTANCE_CAPABILITY} {16}
       |set_instance_parameter_value axi {READ_ADDR_USER_WIDTH} {32}
       |set_instance_parameter_value axi {READ_DATA_REORDERING_DEPTH} {1}
       |set_instance_parameter_value axi {READ_DATA_USER_WIDTH} {32}
       |set_instance_parameter_value axi {READ_ISSUING_CAPABILITY} {16}
       |set_instance_parameter_value axi {S0_ID_WIDTH} {4}
       |set_instance_parameter_value axi {USE_M0_ARBURST} {1}
       |set_instance_parameter_value axi {USE_M0_ARCACHE} {1}
       |set_instance_parameter_value axi {USE_M0_ARID} {1}
       |set_instance_parameter_value axi {USE_M0_ARLEN} {1}
       |set_instance_parameter_value axi {USE_M0_ARLOCK} {1}
       |set_instance_parameter_value axi {USE_M0_ARQOS} {1}
       |set_instance_parameter_value axi {USE_M0_ARREGION} {0}
       |set_instance_parameter_value axi {USE_M0_ARSIZE} {1}
       |set_instance_parameter_value axi {USE_M0_ARUSER} {0}
       |set_instance_parameter_value axi {USE_M0_AWBURST} {1}
       |set_instance_parameter_value axi {USE_M0_AWCACHE} {1}
       |set_instance_parameter_value axi {USE_M0_AWID} {1}
       |set_instance_parameter_value axi {USE_M0_AWLEN} {1}
       |set_instance_parameter_value axi {USE_M0_AWLOCK} {1}
       |set_instance_parameter_value axi {USE_M0_AWQOS} {1}
       |set_instance_parameter_value axi {USE_M0_AWREGION} {0}
       |set_instance_parameter_value axi {USE_M0_AWSIZE} {1}
       |set_instance_parameter_value axi {USE_M0_AWUSER} {0}
       |set_instance_parameter_value axi {USE_M0_BID} {1}
       |set_instance_parameter_value axi {USE_M0_BRESP} {1}
       |set_instance_parameter_value axi {USE_M0_BUSER} {0}
       |set_instance_parameter_value axi {USE_M0_RID} {1}
       |set_instance_parameter_value axi {USE_M0_RLAST} {1}
       |set_instance_parameter_value axi {USE_M0_RRESP} {1}
       |set_instance_parameter_value axi {USE_M0_RUSER} {0}
       |set_instance_parameter_value axi {USE_M0_WSTRB} {1}
       |set_instance_parameter_value axi {USE_M0_WUSER} {0}
       |set_instance_parameter_value axi {USE_PIPELINE} {1}
       |set_instance_parameter_value axi {USE_S0_ARCACHE} {1}
       |set_instance_parameter_value axi {USE_S0_ARLOCK} {1}
       |set_instance_parameter_value axi {USE_S0_ARPROT} {1}
       |set_instance_parameter_value axi {USE_S0_ARQOS} {1}
       |set_instance_parameter_value axi {USE_S0_ARREGION} {0}
       |set_instance_parameter_value axi {USE_S0_ARUSER} {0}
       |set_instance_parameter_value axi {USE_S0_AWCACHE} {1}
       |set_instance_parameter_value axi {USE_S0_AWLOCK} {1}
       |set_instance_parameter_value axi {USE_S0_AWPROT} {1}
       |set_instance_parameter_value axi {USE_S0_AWQOS} {1}
       |set_instance_parameter_value axi {USE_S0_AWREGION} {0}
       |set_instance_parameter_value axi {USE_S0_AWUSER} {0}
       |set_instance_parameter_value axi {USE_S0_BRESP} {1}
       |set_instance_parameter_value axi {USE_S0_BUSER} {0}
       |set_instance_parameter_value axi {USE_S0_RRESP} {1}
       |set_instance_parameter_value axi {USE_S0_RUSER} {0}
       |set_instance_parameter_value axi {USE_S0_WLAST} {1}
       |set_instance_parameter_value axi {USE_S0_WUSER} {0}
       |set_instance_parameter_value axi {WRITE_ACCEPTANCE_CAPABILITY} {16}
       |set_instance_parameter_value axi {WRITE_ADDR_USER_WIDTH} {32}
       |set_instance_parameter_value axi {WRITE_DATA_USER_WIDTH} {32}
       |set_instance_parameter_value axi {WRITE_ISSUING_CAPABILITY} {16}
       |set_instance_parameter_value axi {WRITE_RESP_USER_WIDTH} {32}
       |
       |add_instance clock_bridge_0 altera_clock_bridge
       |set_instance_parameter_value clock_bridge_0 {EXPLICIT_CLOCK_RATE} {0.0}
       |set_instance_parameter_value clock_bridge_0 {NUM_CLOCK_OUTPUTS} {1}
       |
       |add_instance mem altera_mem_if_ddr3_emif
       |set_instance_parameter_value mem {ABSTRACT_REAL_COMPARE_TEST} {0}
       |set_instance_parameter_value mem {ABS_RAM_MEM_INIT_FILENAME} {meminit}
       |set_instance_parameter_value mem {ACV_PHY_CLK_ADD_FR_PHASE} {0.0}
       |set_instance_parameter_value mem {AC_PACKAGE_DESKEW} {0}
       |set_instance_parameter_value mem {AC_ROM_USER_ADD_0} {0_0000_0000_0000}
       |set_instance_parameter_value mem {AC_ROM_USER_ADD_1} {0_0000_0000_1000}
       |set_instance_parameter_value mem {ADDR_ORDER} {0}
       |set_instance_parameter_value mem {ADD_EFFICIENCY_MONITOR} {0}
       |set_instance_parameter_value mem {ADD_EXTERNAL_SEQ_DEBUG_NIOS} {0}
       |set_instance_parameter_value mem {ADVANCED_CK_PHASES} {0}
       |set_instance_parameter_value mem {ADVERTIZE_SEQUENCER_SW_BUILD_FILES} {0}
       |set_instance_parameter_value mem {AFI_DEBUG_INFO_WIDTH} {32}
       |set_instance_parameter_value mem {ALTMEMPHY_COMPATIBLE_MODE} {0}
       |set_instance_parameter_value mem {AP_MODE} {0}
       |set_instance_parameter_value mem {AP_MODE_EN} {0}
       |set_instance_parameter_value mem {AUTO_PD_CYCLES} {0}
       |set_instance_parameter_value mem {AUTO_POWERDN_EN} {0}
       |set_instance_parameter_value mem {AVL_DATA_WIDTH_PORT} {32 32 32 32 32 32}
       |set_instance_parameter_value mem {AVL_MAX_SIZE} {8}
       |set_instance_parameter_value mem {BYTE_ENABLE} {1}
       |set_instance_parameter_value mem {C2P_WRITE_CLOCK_ADD_PHASE} {0.0}
       |set_instance_parameter_value mem {CALIBRATION_MODE} {Skip}
       |set_instance_parameter_value mem {CALIB_REG_WIDTH} {8}
       |set_instance_parameter_value mem {CFG_DATA_REORDERING_TYPE} {INTER_BANK}
       |set_instance_parameter_value mem {CFG_REORDER_DATA} {1}
       |set_instance_parameter_value mem {CFG_TCCD_NS} {2.5}
       |set_instance_parameter_value mem {COMMAND_PHASE} {0.0}
       |set_instance_parameter_value mem {CONTROLLER_LATENCY} {5}
       |set_instance_parameter_value mem {CORE_DEBUG_CONNECTION} {EXPORT}
       |set_instance_parameter_value mem {CPORT_TYPE_PORT} {Bidirectional Bidirectional Bidirectional Bidirectional Bidirectional Bidirectional}
       |set_instance_parameter_value mem {CTL_AUTOPCH_EN} {0}
       |set_instance_parameter_value mem {CTL_CMD_QUEUE_DEPTH} {8}
       |set_instance_parameter_value mem {CTL_CSR_CONNECTION} {INTERNAL_JTAG}
       |set_instance_parameter_value mem {CTL_CSR_ENABLED} {0}
       |set_instance_parameter_value mem {CTL_CSR_READ_ONLY} {1}
       |set_instance_parameter_value mem {CTL_DEEP_POWERDN_EN} {0}
       |set_instance_parameter_value mem {CTL_DYNAMIC_BANK_ALLOCATION} {0}
       |set_instance_parameter_value mem {CTL_DYNAMIC_BANK_NUM} {4}
       |set_instance_parameter_value mem {CTL_ECC_AUTO_CORRECTION_ENABLED} {0}
       |set_instance_parameter_value mem {CTL_ECC_ENABLED} {0}
       |set_instance_parameter_value mem {CTL_ENABLE_BURST_INTERRUPT} {0}
       |set_instance_parameter_value mem {CTL_ENABLE_BURST_TERMINATE} {0}
       |set_instance_parameter_value mem {CTL_HRB_ENABLED} {0}
       |set_instance_parameter_value mem {CTL_LOOK_AHEAD_DEPTH} {4}
       |set_instance_parameter_value mem {CTL_SELF_REFRESH_EN} {0}
       |set_instance_parameter_value mem {CTL_USR_REFRESH_EN} {0}
       |set_instance_parameter_value mem {CTL_ZQCAL_EN} {0}
       |set_instance_parameter_value mem {CUT_NEW_FAMILY_TIMING} {1}
       |set_instance_parameter_value mem {DAT_DATA_WIDTH} {32}
       |set_instance_parameter_value mem {DEBUG_MODE} {0}
       |set_instance_parameter_value mem {DEVICE_DEPTH} {1}
       |set_instance_parameter_value mem {DEVICE_FAMILY_PARAM} {}
       |set_instance_parameter_value mem {DISABLE_CHILD_MESSAGING} {0}
       |set_instance_parameter_value mem {DISCRETE_FLY_BY} {1}
       |set_instance_parameter_value mem {DLL_SHARING_MODE} {None}
       |set_instance_parameter_value mem {DQS_DQSN_MODE} {DIFFERENTIAL}
       |set_instance_parameter_value mem {DQ_INPUT_REG_USE_CLKN} {0}
       |set_instance_parameter_value mem {DUPLICATE_AC} {0}
       |set_instance_parameter_value mem {ED_EXPORT_SEQ_DEBUG} {0}
       |set_instance_parameter_value mem {ENABLE_ABS_RAM_MEM_INIT} {0}
       |set_instance_parameter_value mem {ENABLE_BONDING} {0}
       |set_instance_parameter_value mem {ENABLE_BURST_MERGE} {0}
       |set_instance_parameter_value mem {ENABLE_CTRL_AVALON_INTERFACE} {1}
       |set_instance_parameter_value mem {ENABLE_DELAY_CHAIN_WRITE} {0}
       |set_instance_parameter_value mem {ENABLE_EMIT_BFM_MASTER} {0}
       |set_instance_parameter_value mem {ENABLE_EXPORT_SEQ_DEBUG_BRIDGE} {0}
       |set_instance_parameter_value mem {ENABLE_EXTRA_REPORTING} {0}
       |set_instance_parameter_value mem {ENABLE_ISS_PROBES} {0}
       |set_instance_parameter_value mem {ENABLE_NON_DESTRUCTIVE_CALIB} {0}
       |set_instance_parameter_value mem {ENABLE_NON_DES_CAL} {0}
       |set_instance_parameter_value mem {ENABLE_NON_DES_CAL_TEST} {0}
       |set_instance_parameter_value mem {ENABLE_SEQUENCER_MARGINING_ON_BY_DEFAULT} {0}
       |set_instance_parameter_value mem {ENABLE_USER_ECC} {0}
       |set_instance_parameter_value mem {EXPORT_AFI_HALF_CLK} {0}
       |set_instance_parameter_value mem {EXTRA_SETTINGS} {}
       |set_instance_parameter_value mem {FIX_READ_LATENCY} {8}
       |set_instance_parameter_value mem {FORCED_NON_LDC_ADDR_CMD_MEM_CK_INVERT} {0}
       |set_instance_parameter_value mem {FORCED_NUM_WRITE_FR_CYCLE_SHIFTS} {0}
       |set_instance_parameter_value mem {FORCE_DQS_TRACKING} {AUTO}
       |set_instance_parameter_value mem {FORCE_MAX_LATENCY_COUNT_WIDTH} {0}
       |set_instance_parameter_value mem {FORCE_SEQUENCER_TCL_DEBUG_MODE} {0}
       |set_instance_parameter_value mem {FORCE_SHADOW_REGS} {AUTO}
       |set_instance_parameter_value mem {FORCE_SYNTHESIS_LANGUAGE} {}
       |set_instance_parameter_value mem {HARD_EMIF} {0}
       |set_instance_parameter_value mem {HCX_COMPAT_MODE} {0}
       |set_instance_parameter_value mem {HHP_HPS} {0}
       |set_instance_parameter_value mem {HHP_HPS_SIMULATION} {0}
       |set_instance_parameter_value mem {HHP_HPS_VERIFICATION} {0}
       |set_instance_parameter_value mem {HPS_PROTOCOL} {DEFAULT}
       |set_instance_parameter_value mem {INCLUDE_BOARD_DELAY_MODEL} {0}
       |set_instance_parameter_value mem {INCLUDE_MULTIRANK_BOARD_DELAY_MODEL} {0}
       |set_instance_parameter_value mem {IS_ES_DEVICE} {0}
       |set_instance_parameter_value mem {LOCAL_ID_WIDTH} {8}
       |set_instance_parameter_value mem {LRDIMM_EXTENDED_CONFIG} {0x000000000000000000}
       |set_instance_parameter_value mem {MARGIN_VARIATION_TEST} {0}
       |set_instance_parameter_value mem {MAX_PENDING_RD_CMD} {32}
       |set_instance_parameter_value mem {MAX_PENDING_WR_CMD} {16}
       |set_instance_parameter_value mem {MEM_ASR} {Manual}
       |set_instance_parameter_value mem {MEM_ATCL} {Disabled}
       |set_instance_parameter_value mem {MEM_AUTO_LEVELING_MODE} {1}
       |set_instance_parameter_value mem {MEM_BANKADDR_WIDTH} {3}
       |set_instance_parameter_value mem {MEM_BL} {OTF}
       |set_instance_parameter_value mem {MEM_BT} {Sequential}
       |set_instance_parameter_value mem {MEM_CK_PHASE} {0.0}
       |set_instance_parameter_value mem {MEM_CK_WIDTH} {2}
       |set_instance_parameter_value mem {MEM_CLK_EN_WIDTH} {1}
       |set_instance_parameter_value mem {MEM_CLK_FREQ} {300.0}
       |set_instance_parameter_value mem {MEM_CLK_FREQ_MAX} {533.333}
       |set_instance_parameter_value mem {MEM_COL_ADDR_WIDTH} {10}
       |set_instance_parameter_value mem {MEM_CS_WIDTH} {1}
       |set_instance_parameter_value mem {MEM_DEVICE} {MISSING_MODEL}
       |set_instance_parameter_value mem {MEM_DLL_EN} {1}
       |set_instance_parameter_value mem {MEM_DQ_PER_DQS} {8}
       |set_instance_parameter_value mem {MEM_DQ_WIDTH} {64}
       |set_instance_parameter_value mem {MEM_DRV_STR} {RZQ/6}
       |set_instance_parameter_value mem {MEM_FORMAT} {UNBUFFERED}
       |set_instance_parameter_value mem {MEM_GUARANTEED_WRITE_INIT} {0}
       |set_instance_parameter_value mem {MEM_IF_BOARD_BASE_DELAY} {10}
       |set_instance_parameter_value mem {MEM_IF_DM_PINS_EN} {1}
       |set_instance_parameter_value mem {MEM_IF_DQSN_EN} {1}
       |set_instance_parameter_value mem {MEM_IF_SIM_VALID_WINDOW} {0}
       |set_instance_parameter_value mem {MEM_INIT_EN} {0}
       |set_instance_parameter_value mem {MEM_INIT_FILE} {}
       |set_instance_parameter_value mem {MEM_MIRROR_ADDRESSING} {0}
       |set_instance_parameter_value mem {MEM_NUMBER_OF_DIMMS} {1}
       |set_instance_parameter_value mem {MEM_NUMBER_OF_RANKS_PER_DEVICE} {1}
       |set_instance_parameter_value mem {MEM_NUMBER_OF_RANKS_PER_DIMM} {1}
       |set_instance_parameter_value mem {MEM_PD} {DLL off}
       |set_instance_parameter_value mem {MEM_RANK_MULTIPLICATION_FACTOR} {1}
       |set_instance_parameter_value mem {MEM_ROW_ADDR_WIDTH} {14}
       |set_instance_parameter_value mem {MEM_RTT_NOM} {ODT Disabled}
       |set_instance_parameter_value mem {MEM_RTT_WR} {Dynamic ODT off}
       |set_instance_parameter_value mem {MEM_SRT} {Normal}
       |set_instance_parameter_value mem {MEM_TCL} {7}
       |set_instance_parameter_value mem {MEM_TFAW_NS} {37.5}
       |set_instance_parameter_value mem {MEM_TINIT_US} {200}
       |set_instance_parameter_value mem {MEM_TMRD_CK} {4}
       |set_instance_parameter_value mem {MEM_TRAS_NS} {37.5}
       |set_instance_parameter_value mem {MEM_TRCD_NS} {13.13}
       |set_instance_parameter_value mem {MEM_TREFI_US} {7.8}
       |set_instance_parameter_value mem {MEM_TRFC_NS} {110.0}
       |set_instance_parameter_value mem {MEM_TRP_NS} {13.125}
       |set_instance_parameter_value mem {MEM_TRRD_NS} {7.5}
       |set_instance_parameter_value mem {MEM_TRTP_NS} {7.5}
       |set_instance_parameter_value mem {MEM_TWR_NS} {15.0}
       |set_instance_parameter_value mem {MEM_TWTR} {4}
       |set_instance_parameter_value mem {MEM_USER_LEVELING_MODE} {Leveling}
       |set_instance_parameter_value mem {MEM_VENDOR} {Hynix}
       |set_instance_parameter_value mem {MEM_VERBOSE} {1}
       |set_instance_parameter_value mem {MEM_VOLTAGE} {1.5V DDR3}
       |set_instance_parameter_value mem {MEM_WTCL} {6}
       |set_instance_parameter_value mem {MRS_MIRROR_PING_PONG_ATSO} {0}
       |set_instance_parameter_value mem {MULTICAST_EN} {0}
       |set_instance_parameter_value mem {NEXTGEN} {1}
       |set_instance_parameter_value mem {NIOS_ROM_DATA_WIDTH} {32}
       |set_instance_parameter_value mem {NUM_DLL_SHARING_INTERFACES} {1}
       |set_instance_parameter_value mem {NUM_EXTRA_REPORT_PATH} {10}
       |set_instance_parameter_value mem {NUM_OCT_SHARING_INTERFACES} {1}
       |set_instance_parameter_value mem {NUM_OF_PORTS} {1}
       |set_instance_parameter_value mem {NUM_PLL_SHARING_INTERFACES} {1}
       |set_instance_parameter_value mem {OCT_SHARING_MODE} {None}
       |set_instance_parameter_value mem {P2C_READ_CLOCK_ADD_PHASE} {0.0}
       |set_instance_parameter_value mem {PACKAGE_DESKEW} {0}
       |set_instance_parameter_value mem {PARSE_FRIENDLY_DEVICE_FAMILY_PARAM} {}
       |set_instance_parameter_value mem {PARSE_FRIENDLY_DEVICE_FAMILY_PARAM_VALID} {0}
       |set_instance_parameter_value mem {PHY_CSR_CONNECTION} {INTERNAL_JTAG}
       |set_instance_parameter_value mem {PHY_CSR_ENABLED} {0}
       |set_instance_parameter_value mem {PHY_ONLY} {0}
       |set_instance_parameter_value mem {PINGPONGPHY_EN} {0}
       |set_instance_parameter_value mem {PLL_ADDR_CMD_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_ADDR_CMD_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_ADDR_CMD_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_ADDR_CMD_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_ADDR_CMD_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_ADDR_CMD_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_AFI_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_AFI_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_AFI_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_AFI_HALF_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_HALF_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_AFI_HALF_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_AFI_HALF_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_HALF_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_HALF_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_AFI_PHY_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_PHY_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_AFI_PHY_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_AFI_PHY_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_PHY_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_AFI_PHY_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_C2P_WRITE_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_C2P_WRITE_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_C2P_WRITE_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_C2P_WRITE_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_C2P_WRITE_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_C2P_WRITE_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_CLK_PARAM_VALID} {0}
       |set_instance_parameter_value mem {PLL_CONFIG_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_CONFIG_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_CONFIG_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_CONFIG_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_CONFIG_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_CONFIG_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_DR_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_DR_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_DR_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_DR_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_DR_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_DR_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_HR_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_HR_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_HR_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_HR_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_HR_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_HR_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_LOCATION} {Top_Bottom}
       |set_instance_parameter_value mem {PLL_MEM_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_MEM_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_MEM_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_MEM_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_MEM_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_MEM_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_NIOS_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_NIOS_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_NIOS_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_NIOS_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_NIOS_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_NIOS_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_P2C_READ_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_P2C_READ_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_P2C_READ_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_P2C_READ_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_P2C_READ_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_P2C_READ_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_SHARING_MODE} {None}
       |set_instance_parameter_value mem {PLL_WRITE_CLK_DIV_PARAM} {0}
       |set_instance_parameter_value mem {PLL_WRITE_CLK_FREQ_PARAM} {0.0}
       |set_instance_parameter_value mem {PLL_WRITE_CLK_FREQ_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {PLL_WRITE_CLK_MULT_PARAM} {0}
       |set_instance_parameter_value mem {PLL_WRITE_CLK_PHASE_PS_PARAM} {0}
       |set_instance_parameter_value mem {PLL_WRITE_CLK_PHASE_PS_SIM_STR_PARAM} {}
       |set_instance_parameter_value mem {POWER_OF_TWO_BUS} {0}
       |set_instance_parameter_value mem {PRIORITY_PORT} {1 1 1 1 1 1}
       |set_instance_parameter_value mem {RATE} {Half}
       |set_instance_parameter_value mem {RDIMM_CONFIG} {0000000000000000}
       |set_instance_parameter_value mem {READ_DQ_DQS_CLOCK_SOURCE} {INVERTED_DQS_BUS}
       |set_instance_parameter_value mem {READ_FIFO_SIZE} {8}
       |set_instance_parameter_value mem {REFRESH_BURST_VALIDATION} {0}
       |set_instance_parameter_value mem {REFRESH_INTERVAL} {15000}
       |set_instance_parameter_value mem {REF_CLK_FREQ} {50.0}
       |set_instance_parameter_value mem {REF_CLK_FREQ_MAX_PARAM} {0.0}
       |set_instance_parameter_value mem {REF_CLK_FREQ_MIN_PARAM} {0.0}
       |set_instance_parameter_value mem {REF_CLK_FREQ_PARAM_VALID} {0}
       |set_instance_parameter_value mem {SEQUENCER_TYPE} {NIOS}
       |set_instance_parameter_value mem {SEQ_MODE} {0}
       |set_instance_parameter_value mem {SKIP_MEM_INIT} {1}
       |set_instance_parameter_value mem {SOPC_COMPAT_RESET} {0}
       |set_instance_parameter_value mem {SPEED_GRADE} {2}
       |set_instance_parameter_value mem {STARVE_LIMIT} {10}
       |set_instance_parameter_value mem {TIMING_BOARD_AC_EYE_REDUCTION_H} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_AC_EYE_REDUCTION_SU} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_AC_SKEW} {0.02}
       |set_instance_parameter_value mem {TIMING_BOARD_AC_SLEW_RATE} {1.0}
       |set_instance_parameter_value mem {TIMING_BOARD_AC_TO_CK_SKEW} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_CK_CKN_SLEW_RATE} {2.0}
       |set_instance_parameter_value mem {TIMING_BOARD_DELTA_DQS_ARRIVAL_TIME} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_DELTA_READ_DQS_ARRIVAL_TIME} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_DERATE_METHOD} {AUTO}
       |set_instance_parameter_value mem {TIMING_BOARD_DQS_DQSN_SLEW_RATE} {2.0}
       |set_instance_parameter_value mem {TIMING_BOARD_DQ_EYE_REDUCTION} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_DQ_SLEW_RATE} {1.0}
       |set_instance_parameter_value mem {TIMING_BOARD_DQ_TO_DQS_SKEW} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_ISI_METHOD} {AUTO}
       |set_instance_parameter_value mem {TIMING_BOARD_MAX_CK_DELAY} {0.6}
       |set_instance_parameter_value mem {TIMING_BOARD_MAX_DQS_DELAY} {0.6}
       |set_instance_parameter_value mem {TIMING_BOARD_READ_DQ_EYE_REDUCTION} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_SKEW_BETWEEN_DIMMS} {0.05}
       |set_instance_parameter_value mem {TIMING_BOARD_SKEW_BETWEEN_DQS} {0.02}
       |set_instance_parameter_value mem {TIMING_BOARD_SKEW_CKDQS_DIMM_MAX} {0.01}
       |set_instance_parameter_value mem {TIMING_BOARD_SKEW_CKDQS_DIMM_MIN} {-0.01}
       |set_instance_parameter_value mem {TIMING_BOARD_SKEW_WITHIN_DQS} {0.02}
       |set_instance_parameter_value mem {TIMING_BOARD_TDH} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_TDS} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_TIH} {0.0}
       |set_instance_parameter_value mem {TIMING_BOARD_TIS} {0.0}
       |set_instance_parameter_value mem {TIMING_TDH} {100}
       |set_instance_parameter_value mem {TIMING_TDQSCK} {300}
       |set_instance_parameter_value mem {TIMING_TDQSCKDL} {1200}
       |set_instance_parameter_value mem {TIMING_TDQSCKDM} {900}
       |set_instance_parameter_value mem {TIMING_TDQSCKDS} {450}
       |set_instance_parameter_value mem {TIMING_TDQSQ} {150}
       |set_instance_parameter_value mem {TIMING_TDQSS} {0.25}
       |set_instance_parameter_value mem {TIMING_TDS} {25}
       |set_instance_parameter_value mem {TIMING_TDSH} {0.2}
       |set_instance_parameter_value mem {TIMING_TDSS} {0.2}
       |set_instance_parameter_value mem {TIMING_TIH} {200}
       |set_instance_parameter_value mem {TIMING_TIS} {125}
       |set_instance_parameter_value mem {TIMING_TQH} {0.38}
       |set_instance_parameter_value mem {TIMING_TQSH} {0.38}
       |set_instance_parameter_value mem {TRACKING_ERROR_TEST} {0}
       |set_instance_parameter_value mem {TRACKING_WATCH_TEST} {0}
       |set_instance_parameter_value mem {TREFI} {35100}
       |set_instance_parameter_value mem {TRFC} {350}
       |set_instance_parameter_value mem {USER_DEBUG_LEVEL} {1}
       |set_instance_parameter_value mem {USE_AXI_ADAPTOR} {0}
       |set_instance_parameter_value mem {USE_FAKE_PHY} {0}
       |set_instance_parameter_value mem {USE_MEM_CLK_FREQ} {0}
       |set_instance_parameter_value mem {USE_MM_ADAPTOR} {1}
       |set_instance_parameter_value mem {USE_SEQUENCER_BFM} {0}
       |set_instance_parameter_value mem {WEIGHT_PORT} {0 0 0 0 0 0}
       |set_instance_parameter_value mem {WRBUFFER_ADDR_WIDTH} {6}
       |
       |add_instance reset_bridge_0 altera_reset_bridge
       |set_instance_parameter_value reset_bridge_0 {ACTIVE_LOW_RESET} {0}
       |set_instance_parameter_value reset_bridge_0 {NUM_RESET_OUTPUTS} {1}
       |set_instance_parameter_value reset_bridge_0 {SYNCHRONOUS_EDGES} {deassert}
       |set_instance_parameter_value reset_bridge_0 {USE_RESET_REQUEST} {0}
       |
       |# exported interfaces
       |add_interface axi4 altera_axi4 slave
       |set_interface_property axi4 EXPORT_OF axi.s0
       |add_interface mem_afi_clk clock source
       |set_interface_property mem_afi_clk EXPORT_OF clock_bridge_0.out_clk
       |add_interface mem_afi_reset reset source
       |set_interface_property mem_afi_reset EXPORT_OF mem.afi_reset
       |add_interface mem_global_reset reset sink
       |set_interface_property mem_global_reset EXPORT_OF mem.global_reset
       |add_interface mem_pll_ref_clk clock sink
       |set_interface_property mem_pll_ref_clk EXPORT_OF mem.pll_ref_clk
       |add_interface mem_pll_sharing conduit end
       |set_interface_property mem_pll_sharing EXPORT_OF mem.pll_sharing
       |add_interface mem_soft_reset reset sink
       |set_interface_property mem_soft_reset EXPORT_OF reset_bridge_0.in_reset
       |add_interface mem_status conduit end
       |set_interface_property mem_status EXPORT_OF mem.status
       |add_interface memory conduit end
       |set_interface_property memory EXPORT_OF mem.memory
       |add_interface oct conduit end
       |set_interface_property oct EXPORT_OF mem.oct
       |
       |# connections and connection parameters
       |add_connection axi.m0 mem.avl
       |set_connection_parameter_value axi.m0/mem.avl arbitrationPriority {1}
       |set_connection_parameter_value axi.m0/mem.avl baseAddress {0x0000}
       |set_connection_parameter_value axi.m0/mem.avl defaultConnection {0}
       |add_connection mem.afi_clk axi.clk
       |add_connection mem.afi_clk clock_bridge_0.in_clk
       |add_connection mem.afi_clk reset_bridge_0.clk
       |add_connection reset_bridge_0.out_reset axi.clk_reset
       |add_connection reset_bridge_0.out_reset mem.soft_reset
       |
       |# interconnect requirements
       |set_interconnect_requirement {$$system} {qsys_mm.clockCrossingAdapter} {HANDSHAKE}
       |set_interconnect_requirement {$$system} {qsys_mm.enableEccProtection} {FALSE}
       |set_interconnect_requirement {$$system} {qsys_mm.insertDefaultSlave} {FALSE}
       |set_interconnect_requirement {$$system} {qsys_mm.maxAdditionalLatency} {1}
       |
       |save_system {${moduleName}.qsys}
       |""".stripMargin)
}