package sifive.fpgashells.shell.lattice

import chisel3._
import chisel3.experimental.{Analog, attach}
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters
import sifive.fpgashells.ip.lattice._
import sifive.fpgashells.shell._

abstract class GPIOLatticePlacedOverlay
(name: String, di: GPIODesignInput, si: GPIOShellInput)
  extends GPIOPlacedOverlay(name, di, si)
{
  def shell: LatticeShell

  shell { InModuleBody {
    tlgpioSink.bundle.pins.zipWithIndex.foreach{ case (tlpin, idx) => {
      val m = Module(new BB)
      m.io.I := tlpin.o.oval
      m.io.T := !tlpin.o.oe
      tlpin.i.ival := m.io.O
      // m.fromBase(tlpin.toBasePin()) // This doesn't work
      attach(m.io.B, io.gpio(idx))
    } }
  } }
}

case class GPIODirectLatticeDesignInput(width: Int)(implicit val p: Parameters)
case class GPIODirectLatticeOverlayOutput(io: ModuleValue[Vec[Analog]])
trait GPIODirectLatticeShellPlacer[Shell] extends ShellPlacer[GPIODirectLatticeDesignInput, GPIOShellInput, GPIODirectLatticeOverlayOutput]

abstract class GPIODirectLatticePlacedOverlay(val name: String, val designInput: GPIODirectLatticeDesignInput, val shellInput: GPIOShellInput)
  extends IOPlacedOverlay[ShellGPIOPortIO, GPIODirectLatticeDesignInput, GPIOShellInput, GPIODirectLatticeOverlayOutput]
{
  implicit val p = designInput.p

  def ioFactory = new ShellGPIOPortIO(designInput.width)

  val gpio = shell { InModuleBody { io.gpio /*Wire(Vec(designInput.width, Analog(1.W)))*/ } }

  def overlayOutput = GPIODirectLatticeOverlayOutput(gpio)

  def shell: LatticeShell

  /*shell { InModuleBody {
    gpio.zipWithIndex.foreach{ case (pin, idx) =>
      attach(pin, io.gpio(idx))
    }
  } }*/
}
