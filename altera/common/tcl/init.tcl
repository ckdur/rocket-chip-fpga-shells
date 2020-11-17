# See LICENSE for license details.

# Include helper functions
source [file join $scriptdir "util.tcl"]

# Create the directory for IPs
file mkdir $ipdir

# Generate IP implementations. Quartus TCL emitted from Chisel Blackboxes
foreach ip_quartus_tcl $ip_quartus_tcls {
  source $ip_quartus_tcl
}

# TODO Generate main QSYS IP
foreach qsys $ip_quartus_qsys {
  if { [catch { exec >@stdout 2>@stderr qsys-script --script=$qsys }] } {
    return -code error "Running qsys-script"
  }
  if { [catch { exec >@stdout 2>@stderr qsys-generate $ipdir/main.qsys --block-symbol-file --output-directory=$ipdir/main --family=$FAMILY --part=$part_fpga }] } {
    return -code error "Running qsys-generate"
  }
  if { [catch { exec >@stdout 2>@stderr qsys-generate $ipdir/main.qsys --synthesis=VERILOG --output-directory=$ipdir/main --family=$FAMILY --part=$part_fpga }] } {
    return -code error "Running qsys-generate"
  }
  set_global_assignment -name QIP_FILE $ipdir/main/synthesis/main.qip
}

# Optional board-specific ip script
set boardiptcl [file join $boarddir tcl ip.tcl]
if {[file exists $boardiptcl]} {
  source $boardiptcl
}

export_assignments
project_close

