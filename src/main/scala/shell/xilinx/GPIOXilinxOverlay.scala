package sifive.fpgashells.shell.xilinx

import chisel3._
import chisel3.experimental.{Analog, attach}
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.shell._

abstract class GPIOXilinxPlacedOverlay(name: String, di: GPIODesignInput, si: GPIOShellInput)
  extends GPIOPlacedOverlay(name, di, si)
{
  def shell: XilinxShell

  shell { InModuleBody {
      tlgpioSink.bundle.pins.zipWithIndex.foreach{ case (tlpin, idx) => {
        UIntToAnalog(tlpin.o.oval, io.gpio(idx), tlpin.o.oe)
        tlpin.i.ival := AnalogToUInt(io.gpio(idx))
      } }
  } }
}

case class GPIODirectXilinxDesignInput(width: Int)(implicit val p: Parameters)
case class GPIODirectXilinxOverlayOutput(io: ModuleValue[Vec[Analog]])
trait GPIODirectXilinxShellPlacer[Shell] extends ShellPlacer[GPIODirectXilinxDesignInput, GPIOShellInput, GPIODirectXilinxOverlayOutput]

abstract class GPIODirectXilinxPlacedOverlay(val name: String, val designInput: GPIODirectXilinxDesignInput, val shellInput: GPIOShellInput)
  extends IOPlacedOverlay[ShellGPIOPortIO, GPIODirectXilinxDesignInput, GPIOShellInput, GPIODirectXilinxOverlayOutput]
{
  implicit val p = designInput.p

  def ioFactory = new ShellGPIOPortIO(designInput.width)

  val gpio = shell { InModuleBody { io.gpio /*Wire(Vec(designInput.width, Analog(1.W)))*/ } }

  def overlayOutput = GPIODirectXilinxOverlayOutput(gpio)

  def shell: XilinxShell

  /*shell { InModuleBody {
    gpio.zipWithIndex.foreach{ case (pin, idx) =>
      attach(pin, io.gpio(idx))
    }
  } }*/
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
