// See LICENSE for license details.
package sifive.fpgashells.ip.xilinx.sakuraxmig

import Chisel._
import chisel3.experimental.{Analog,attach}
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.util.GenericParameterizedBundle
import freechips.rocketchip.config._

// IP VLNV: xilinx.com:customize_ip:sakuraxmig:1.0
// Black Box

class SakuraXMIGIODDR(depth : BigInt) extends GenericParameterizedBundle(depth) {
  require((depth<=0x8000000L),"SakuraXMIGIODDR supports up to 128MB depth configuration")
  val ddr3_addr             = Bits(OUTPUT,13)
  val ddr3_ba               = Bits(OUTPUT,3)
  val ddr3_ras_n            = Bool(OUTPUT)
  val ddr3_cas_n            = Bool(OUTPUT)
  val ddr3_we_n             = Bool(OUTPUT)
  val ddr3_reset_n          = Bool(OUTPUT)
  val ddr3_ck_p             = Bits(OUTPUT,1)
  val ddr3_ck_n             = Bits(OUTPUT,1)
  val ddr3_cke              = Bits(OUTPUT,1)
  val ddr3_cs_n             = Bits(OUTPUT,1)
  val ddr3_dm               = Bits(OUTPUT,2)
  val ddr3_odt              = Bits(OUTPUT,1)

  val ddr3_dq               = Analog(16.W)
  val ddr3_dqs_n            = Analog(2.W)
  val ddr3_dqs_p            = Analog(2.W)
}

//reused directly in io bundle for sifive.blocks.devices.xilinxvc707mig
trait SakuraXMIGIOClocksReset extends Bundle {
  //inputs
  //"NO_BUFFER" clock source (must be connected to IBUF outside of IP)
  val sys_clk_i             = Bool(INPUT)
  //user interface signals
  val ui_clk                = Clock(OUTPUT)
  val ui_clk_sync_rst       = Bool(OUTPUT)
  val mmcm_locked           = Bool(OUTPUT)
  val aresetn               = Bool(INPUT)
  //misc
  val init_calib_complete   = Bool(OUTPUT)
  val sys_rst               = Bool(INPUT)
}

//scalastyle:off
//turn off linter: blackbox name must match verilog module
class sakuraxmig(depth : BigInt)(implicit val p:Parameters) extends BlackBox
{
  require((depth<=0x8000000L),"SakuraXMIGIODDR supports up to 128MB depth configuration")

  val io = new SakuraXMIGIODDR(depth) with SakuraXMIGIOClocksReset {
    // User interface signals
    val app_sr_req            = Bool(INPUT)
    val app_ref_req           = Bool(INPUT)
    val app_zq_req            = Bool(INPUT)
    val app_sr_active         = Bool(OUTPUT)
    val app_ref_ack           = Bool(OUTPUT)
    val app_zq_ack            = Bool(OUTPUT)
    //axi_s
    //slave interface write address ports
    val s_axi_awid            = Bits(INPUT,4)
    val s_axi_awaddr          = Bits(INPUT,27)
    val s_axi_awlen           = Bits(INPUT,8)
    val s_axi_awsize          = Bits(INPUT,3)
    val s_axi_awburst         = Bits(INPUT,2)
    val s_axi_awlock          = Bits(INPUT,1)
    val s_axi_awcache         = Bits(INPUT,4)
    val s_axi_awprot          = Bits(INPUT,3)
    val s_axi_awqos           = Bits(INPUT,4)
    val s_axi_awvalid         = Bool(INPUT)
    val s_axi_awready         = Bool(OUTPUT)
    //slave interface write data ports
    val s_axi_wdata           = Bits(INPUT,64)
    val s_axi_wstrb           = Bits(INPUT,8)
    val s_axi_wlast           = Bool(INPUT)
    val s_axi_wvalid          = Bool(INPUT)
    val s_axi_wready          = Bool(OUTPUT)
    //slave interface write response ports
    val s_axi_bready          = Bool(INPUT)
    val s_axi_bid             = Bits(OUTPUT,4)
    val s_axi_bresp           = Bits(OUTPUT,2)
    val s_axi_bvalid          = Bool(OUTPUT)
    //slave interface read address ports
    val s_axi_arid            = Bits(INPUT,4)
    val s_axi_araddr          = Bits(INPUT,27)
    val s_axi_arlen           = Bits(INPUT,8)
    val s_axi_arsize          = Bits(INPUT,3)
    val s_axi_arburst         = Bits(INPUT,2)
    val s_axi_arlock          = Bits(INPUT,1)
    val s_axi_arcache         = Bits(INPUT,4)
    val s_axi_arprot          = Bits(INPUT,3)
    val s_axi_arqos           = Bits(INPUT,4)
    val s_axi_arvalid         = Bool(INPUT)
    val s_axi_arready         = Bool(OUTPUT)
    //slave interface read data ports
    val s_axi_rready          = Bool(INPUT)
    val s_axi_rid             = Bits(OUTPUT,4)
    val s_axi_rdata           = Bits(OUTPUT,64)
    val s_axi_rresp           = Bits(OUTPUT,2)
    val s_axi_rlast           = Bool(OUTPUT)
    val s_axi_rvalid          = Bool(OUTPUT)
    //misc
    val device_temp           = Bits(OUTPUT,12)
  }

  val migprj = """ {<?xml version='1.0' encoding='UTF-8'?>
<!-- IMPORTANT: This is an internal file that has been generated by the MIG software. Any direct editing or changes made to this file may result in unpredictable behavior or data corruption. It is strongly advised that users do not edit the contents of this file. Re-run the MIG GUI with the required settings if any of the options provided below need to be altered. -->
<Project NoOfControllers="1" >
    <ModuleName>SakuraXmig1gb</ModuleName>
    <dci_inouts_inputs>1</dci_inouts_inputs>
    <dci_inputs>1</dci_inputs>
    <Debug_En>OFF</Debug_En>
    <DataDepth_En>1024</DataDepth_En>
    <LowPower_En>ON</LowPower_En>
    <XADC_En>Enabled</XADC_En>
    <TargetFPGA>xc7k160t-fbg676/-1</TargetFPGA>
    <Version>4.0</Version>
    <SystemClock>No Buffer</SystemClock>
    <ReferenceClock>Use System Clock</ReferenceClock>
    <SysResetPolarity>ACTIVE HIGH</SysResetPolarity>
    <BankSelectionFlag>FALSE</BankSelectionFlag>
    <InternalVref>0</InternalVref>
    <dci_hr_inouts_inputs>50 Ohms</dci_hr_inouts_inputs>
    <dci_cascade>0</dci_cascade>
    <Controller number="0" >
        <MemoryDevice>DDR3_SDRAM/Components/MT41J64M16XX-15E</MemoryDevice>
        <TimePeriod>2500</TimePeriod>
        <VccAuxIO>1.8V</VccAuxIO>
        <PHYRatio>4:1</PHYRatio>
        <InputClkFreq>200</InputClkFreq>
        <UIExtraClocks>0</UIExtraClocks>
        <MMCM_VCO>800</MMCM_VCO>
        <MMCMClkOut0> 1.000</MMCMClkOut0>
        <MMCMClkOut1>1</MMCMClkOut1>
        <MMCMClkOut2>1</MMCMClkOut2>
        <MMCMClkOut3>1</MMCMClkOut3>
        <MMCMClkOut4>1</MMCMClkOut4>
        <DataWidth>16</DataWidth>
        <DeepMemory>1</DeepMemory>
        <DataMask>1</DataMask>
        <ECC>Disabled</ECC>
        <Ordering>Normal</Ordering>
        <BankMachineCnt>4</BankMachineCnt>
        <CustomPart>FALSE</CustomPart>
        <NewPartName></NewPartName>
        <RowAddress>13</RowAddress>
        <ColAddress>10</ColAddress>
        <BankAddress>3</BankAddress>
        <MemoryVoltage>1.5V</MemoryVoltage>
        <UserMemoryAddressMap>BANK_ROW_COLUMN</UserMemoryAddressMap>
        <PinSelection>
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AE1" SLEW="" name="ddr3_addr[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AD3" SLEW="" name="ddr3_addr[10]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AC4" SLEW="" name="ddr3_addr[11]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AD1" SLEW="" name="ddr3_addr[12]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AB6" SLEW="" name="ddr3_addr[1]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AE3" SLEW="" name="ddr3_addr[2]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AC6" SLEW="" name="ddr3_addr[3]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="Y6" SLEW="" name="ddr3_addr[4]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AD4" SLEW="" name="ddr3_addr[5]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AA5" SLEW="" name="ddr3_addr[6]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AF3" SLEW="" name="ddr3_addr[7]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AB5" SLEW="" name="ddr3_addr[8]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AF2" SLEW="" name="ddr3_addr[9]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AF4" SLEW="" name="ddr3_ba[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="Y5" SLEW="" name="ddr3_ba[1]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AD5" SLEW="" name="ddr3_ba[2]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AD6" SLEW="" name="ddr3_cas_n" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="DIFF_SSTL15" PADName="W5" SLEW="" name="ddr3_ck_n[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="DIFF_SSTL15" PADName="W6" SLEW="" name="ddr3_ck_p[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="U2" SLEW="" name="ddr3_cke[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AE5" SLEW="" name="ddr3_cs_n[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AC9" SLEW="" name="ddr3_dm[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="Y7" SLEW="" name="ddr3_dm[1]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AA9" SLEW="" name="ddr3_dq[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="V8" SLEW="" name="ddr3_dq[10]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="Y11" SLEW="" name="ddr3_dq[11]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="Y8" SLEW="" name="ddr3_dq[12]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="V11" SLEW="" name="ddr3_dq[13]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="V7" SLEW="" name="ddr3_dq[14]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="W11" SLEW="" name="ddr3_dq[15]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AD9" SLEW="" name="ddr3_dq[1]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AB9" SLEW="" name="ddr3_dq[2]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AF7" SLEW="" name="ddr3_dq[3]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AA8" SLEW="" name="ddr3_dq[4]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AA7" SLEW="" name="ddr3_dq[5]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AE7" SLEW="" name="ddr3_dq[6]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="AC7" SLEW="" name="ddr3_dq[7]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="V9" SLEW="" name="ddr3_dq[8]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15_T_DCI" PADName="Y10" SLEW="" name="ddr3_dq[9]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="DIFF_SSTL15_T_DCI" PADName="AD8" SLEW="" name="ddr3_dqs_n[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="DIFF_SSTL15_T_DCI" PADName="W9" SLEW="" name="ddr3_dqs_n[1]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="DIFF_SSTL15_T_DCI" PADName="AC8" SLEW="" name="ddr3_dqs_p[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="DIFF_SSTL15_T_DCI" PADName="W10" SLEW="" name="ddr3_dqs_p[1]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="U1" SLEW="" name="ddr3_odt[0]" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AE6" SLEW="" name="ddr3_ras_n" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="LVCMOS15" PADName="AB7" SLEW="" name="ddr3_reset_n" IN_TERM="" />
            <Pin VCCAUX_IO="" IOSTANDARD="SSTL15" PADName="AF5" SLEW="" name="ddr3_we_n" IN_TERM="" />
        </PinSelection>
        <System_Control>
            <Pin PADName="No connect" Bank="Select Bank" name="sys_rst" />
            <Pin PADName="No connect" Bank="Select Bank" name="init_calib_complete" />
            <Pin PADName="No connect" Bank="Select Bank" name="tg_compare_error" />
        </System_Control>
        <TimingParameters>
            <Parameters twtr="7.5" trrd="7.5" trefi="7.8" tfaw="45" trtp="7.5" tcke="5.625" trfc="110" trp="13.5" tras="36" trcd="13.5" />
        </TimingParameters>
        <mrBurstLength name="Burst Length" >8 - Fixed</mrBurstLength>
        <mrBurstType name="Read Burst Type and Length" >Sequential</mrBurstType>
        <mrCasLatency name="CAS Latency" >6</mrCasLatency>
        <mrMode name="Mode" >Normal</mrMode>
        <mrDllReset name="DLL Reset" >No</mrDllReset>
        <mrPdMode name="DLL control for precharge PD" >Slow Exit</mrPdMode>
        <emrDllEnable name="DLL Enable" >Enable</emrDllEnable>
        <emrOutputDriveStrength name="Output Driver Impedance Control" >RZQ/7</emrOutputDriveStrength>
        <emrMirrorSelection name="Address Mirroring" >Disable</emrMirrorSelection>
        <emrCSSelection name="Controller Chip Select Pin" >Enable</emrCSSelection>
        <emrRTT name="RTT (nominal) - On Die Termination (ODT)" >RZQ/4</emrRTT>
        <emrPosted name="Additive Latency (AL)" >0</emrPosted>
        <emrOCD name="Write Leveling Enable" >Disabled</emrOCD>
        <emrDQS name="TDQS enable" >Enabled</emrDQS>
        <emrRDQS name="Qoff" >Output Buffer Enabled</emrRDQS>
        <mr2PartialArraySelfRefresh name="Partial-Array Self Refresh" >Full Array</mr2PartialArraySelfRefresh>
        <mr2CasWriteLatency name="CAS write latency" >5</mr2CasWriteLatency>
        <mr2AutoSelfRefresh name="Auto Self Refresh" >Enabled</mr2AutoSelfRefresh>
        <mr2SelfRefreshTempRange name="High Temparature Self Refresh Rate" >Normal</mr2SelfRefreshTempRange>
        <mr2RTTWR name="RTT_WR - Dynamic On Die Termination (ODT)" >Dynamic ODT off</mr2RTTWR>
        <PortInterface>AXI</PortInterface>
        <AXIParameters>
            <C0_C_RD_WR_ARB_ALGORITHM>RD_PRI_REG</C0_C_RD_WR_ARB_ALGORITHM>
            <C0_S_AXI_ADDR_WIDTH>27</C0_S_AXI_ADDR_WIDTH>
            <C0_S_AXI_DATA_WIDTH>64</C0_S_AXI_DATA_WIDTH>
            <C0_S_AXI_ID_WIDTH>4</C0_S_AXI_ID_WIDTH>
            <C0_S_AXI_SUPPORTS_NARROW_BURST>0</C0_S_AXI_SUPPORTS_NARROW_BURST>
        </AXIParameters>
    </Controller>

</Project> } """

  val migprjname = """{/sakuraxmig.prj}"""
  val modulename = """sakuraxmig"""


  ElaborationArtefacts.add(
  modulename++".vivado.tcl",
   """set migprj """++migprj++"""
   set migprjfile """++migprjname++"""
   set migprjfilepath $ipdir$migprjfile
   set fp [open $migprjfilepath w+]
   puts $fp $migprj
   close $fp
   create_ip -vendor xilinx.com -library ip -name mig_7series -module_name """ ++ modulename ++ """ -dir $ipdir -force
   set_property CONFIG.XML_INPUT_FILE $migprjfilepath [get_ips """ ++ modulename ++ """] """
  )

   
}
//scalastyle:on