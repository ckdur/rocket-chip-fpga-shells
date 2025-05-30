package sifive.fpgashells.shell

import chisel3._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._
import sifive.fpgashells.ip.altera.Rational
import sifive.fpgashells.shell._


class AlteraSDC(val sdc_name: String) extends SDC(sdc_name)
{

  override def addGroup(clocks: => Seq[String] = Nil, pins: => Seq[IOPin] = Nil) {
    def thunk = {
      val clocksList = clocks
      val (pinsList, portsList) = pins.map(_.name).partition(_.contains("/"))
      val sep = " \\\n      "
      val clocksStr = (" [get_clocks {" +: clocksList).mkString(sep) + " \\\n    }]"
      val pinsStr   = (" [get_clocks -of_objects [get_pins {"  +: pinsList ).mkString(sep) + " \\\n    }]]"
      val portsStr  = (" [get_clocks -of_objects [get_ports {" +: portsList).mkString(sep) + " \\\n    }]]"
      val str = s"  -group [list${if (clocksList.isEmpty) "" else clocksStr}${if (pinsList.isEmpty) "" else pinsStr}${if (portsList.isEmpty) "" else portsStr}]"  
      if (clocksList.isEmpty && pinsList.isEmpty && portsList.isEmpty) "" else str
    }
    addRawGroup(thunk)
  }

  override def addClock(name: => String, pin: => IOPin, freqMHz: => Double, jitterNs: => Double = 0.5) {
    addRawClock(s"create_clock -name ${name} -period ${1000/freqMHz} ${pin.sdcPin}")
  }
  private def flatten(x: Seq[() => String], sep: String = "\n") = x.map(_()).filter(_ != "").reverse.mkString(sep)

  // TODO: Hack, figure out how to add sdc directives more elegantly
  def addSDCDirective(command: => String) { addRawClock(command)}
}

class AlteraGenericSDC(name: String) extends SDC(name) {
  // A version of the SDC but for Quartus, as SDC quartus does not support some stuff
  override def addClock(name: => String, pin: => IOPin, freqMHz: => Double, jitterNs: => Double = 0.5) {
    addRawClock(s"create_clock -name ${name} -period ${1000 / freqMHz} ${pin.sdcPin}")
    // addRawClock(s"set_input_jitter ${name} ${jitterNs}") // Not supported
  }

  override def addGroup(clocks: => Seq[String] = Nil, pins: => Seq[IOPin] = Nil): Unit = {
    // Unimplemented
  }

  def addGeneratedClock(name: => String, pinin: => IOPin, opath: String, ratio: Rational, phaseDeg: => Double = 0.0): Unit = {
    // TODO: This can be addDerivedClock() but if supported by multiply
    addRawClock(s"create_generated_clock -add -name \"${name}\" -source ${pinin.sdcPin} -multiply_by ${ratio.num} -divide_by ${ratio.den} -phase ${phaseDeg} ${opath}")
  }

  def addGroupOnlyNames(clocks: => Seq[String] = Nil, pins: => Seq[IOPin] = Nil) {
    def thunk = {
      val clocksList = clocks
      val (pinsList, portsList) = pins.map(_.name.replace('/', '|')).partition(_.contains("|"))
      val sep = " \\\n      "
      val clocksStr = (" [get_clocks " +: clocksList).mkString(sep) + " \\\n    ]"
      val pinsStr   = (" [get_clocks -of_objects [get_pins {"  +: pinsList ).mkString(sep) + " \\\n    }]]"
      val portsStr  = (" [get_clocks -of_objects [get_ports {" +: portsList).mkString(sep) + " \\\n    }]]"
      val str = s"  -group [list${if (clocksList.isEmpty) "" else clocksStr}${if (pinsList.isEmpty) "" else pinsStr}${if (portsList.isEmpty) "" else portsStr}]"
      if (clocksList.isEmpty && pinsList.isEmpty && portsList.isEmpty) "" else str
    }
    addRawGroup(thunk)
  }
}