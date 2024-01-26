# See LICENSE for license details.

# Include helper functions
source [file join $scriptdir "util.tcl"]

# Create the directory for IPs
file mkdir $ipdir

# Generate IP implementations. Quartus TCL emitted from Chisel Blackboxes
foreach ip_quartus_tcl $ip_quartus_tcls {
  source $ip_quartus_tcl
}

# Add the SDC files
foreach file_sdc $ip_quartus_sdc {
  set_global_assignment -name SDC_FILE $file_sdc
}

foreach qsys_tcl $ip_quartus_qsys_tcls {
  set fname [file tail $qsys_tcl]
  set fnamespl [split $fname .]
  set ipname [lindex $fnamespl end-2]
  if { [catch { exec >@stdout 2>@stderr qsys-script --script=$qsys_tcl }] } {
    return -code error "Running qsys-script"
  }
  exec mv $ipname.qsys $ipdir/$ipname.qsys
  if { [catch { exec >@stdout 2>@stderr qsys-generate $ipdir/$ipname.qsys --block-symbol-file --output-directory=$ipdir/$ipname --family=$FAMILY --part=$part_fpga }] } {
    return -code error "Running qsys-generate"
  }
  if { [catch { exec >@stdout 2>@stderr qsys-generate $ipdir/$ipname.qsys --synthesis=VERILOG --output-directory=$ipdir/$ipname --family=$FAMILY --part=$part_fpga }] } {
    return -code error "Running qsys-generate"
  }
  set_global_assignment -name QIP_FILE $ipdir/$ipname/synthesis/$ipname.qip
}

foreach qsys $ip_quartus_qsys {
  set fname [file tail $qsys]
  set fnamespl [split $fname .]
  set ipname [lindex $fnamespl end-1]
  if { [catch { exec >@stdout 2>@stderr qsys-generate $qsys --block-symbol-file --output-directory=$ipdir/$ipname --family=$FAMILY --part=$part_fpga }] } {
    return -code error "Running qsys-generate"
  }
  if { [catch { exec >@stdout 2>@stderr qsys-generate $qsys --synthesis=VERILOG --output-directory=$ipdir/$ipname --family=$FAMILY --part=$part_fpga }] } {
    return -code error "Running qsys-generate"
  }
  set_global_assignment -name QIP_FILE $ipdir/$ipname/synthesis/$ipname.qip
}

# Optional board-specific ip script
set boardiptcl [file join $boarddir tcl ip.tcl]
if {[file exists $boardiptcl]} {
  source $boardiptcl
}

export_assignments
project_close

