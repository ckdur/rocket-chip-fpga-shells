package sifive.fpgashells.shell.altera

import chisel3._
import chisel3.experimental.{Analog, attach}
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters
import sifive.fpgashells.ip.altera._
import sifive.fpgashells.shell._

abstract class GPIOAlteraPlacedOverlay(name: String, di: GPIODesignInput, si: GPIOShellInput)
  extends GPIOPlacedOverlay(name, di, si)
{
  def shell: AlteraGenericShell

  shell { InModuleBody {
    tlgpioSink.bundle.pins.zipWithIndex.foreach{ case (tlpin, idx) => {
      val m = Module(new ALT_IOBUF)
      m.io.i := tlpin.o.oval
      m.io.oe := tlpin.o.oe
      tlpin.i.ival := m.io.o
      // m.fromBase(tlpin.toBasePin()) // This doesn't work
      attach(m.io.io, io.gpio(idx))
    } }
  } }
}

case class GPIODirectAlteraDesignInput(width: Int)(implicit val p: Parameters)
case class GPIODirectAlteraOverlayOutput(io: ModuleValue[Vec[Analog]])
trait GPIODirectAlteraShellPlacer[Shell] extends ShellPlacer[GPIODirectAlteraDesignInput, GPIOShellInput, GPIODirectAlteraOverlayOutput]

abstract class GPIODirectAlteraPlacedOverlay(val name: String, val designInput: GPIODirectAlteraDesignInput, val shellInput: GPIOShellInput)
  extends IOPlacedOverlay[ShellGPIOPortIO, GPIODirectAlteraDesignInput, GPIOShellInput, GPIODirectAlteraOverlayOutput]
{
  implicit val p = designInput.p

  def ioFactory = new ShellGPIOPortIO(designInput.width)

  val gpio = shell { InModuleBody { io.gpio /*Wire(Vec(designInput.width, Analog(1.W)))*/ } }

  def overlayOutput = GPIODirectAlteraOverlayOutput(gpio)

  def shell: AlteraGenericShell

  /*shell { InModuleBody {
    gpio.zipWithIndex.foreach{ case (pin, idx) =>
      attach(pin, io.gpio(idx))
    }
  } }*/
}
