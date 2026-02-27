package sifive.fpgashells.devices.xilinx.xilinxvcu108mig

import freechips.rocketchip.diplomacy.{AddressRange, LazyModule, LazyModuleImp}
import freechips.rocketchip.subsystem.{BaseSubsystem, MBUS}
import freechips.rocketchip.tilelink.TLWidthWidget
import org.chipsalliance.cde.config._

case object MemoryXilinxDDRKey extends Field[XilinxVCU108MIGParams]

trait HasMemoryXilinxVCU108MIG { this: BaseSubsystem =>
  val module: HasMemoryXilinxVCU108MIGModuleImp

  val xilinxvcu108mig = LazyModule(new XilinxVCU108MIG(p(MemoryXilinxDDRKey)))

  private val mbus = locateTLBusWrapper(MBUS)
  mbus.coupleTo("xilinxvcu108mig") { xilinxvcu108mig.node := TLWidthWidget(mbus.beatBytes) := _ }
}

trait HasMemoryXilinxVCU108MIGBundle {
  val xilinxvcu108mig: XilinxVCU108MIGIO
  def connectXilinxVCU108MIGToPads(pads: XilinxVCU108MIGPads) {
    pads <> xilinxvcu108mig
  }
}

trait HasMemoryXilinxVCU108MIGModuleImp extends LazyModuleImp
  with HasMemoryXilinxVCU108MIGBundle {
  val outer: HasMemoryXilinxVCU108MIG
  val ranges = AddressRange.fromSets(p(MemoryXilinxDDRKey).address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val depth = ranges.head.size
  val xilinxvcu108mig = IO(new XilinxVCU108MIGIO(depth))

  xilinxvcu108mig <> outer.xilinxvcu108mig.module.io.port
}

/*
   Copyright 2016 SiFive, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
