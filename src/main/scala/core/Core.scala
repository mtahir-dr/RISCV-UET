/*********************************************************************
* Filename :    Core.scala
* Date     :    28-03-2020
* 
* Description:  Based on riscv mini. Modified cache memory interface
*               to simple Inst/Data memory interfaces
*
* 13-04-2020    The HostIO interface is temporarily disabled. 
*********************************************************************/

package riscv_uet

import chisel3._
import chisel3.util.Valid

/* MT
class HostIO extends Bundle with Config {
  val fromhost = Flipped(Valid(UInt(XLEN.W)))
  val tohost   = Output(UInt(XLEN.W))
} */

class DebugIO extends Bundle with Config {
  val inst      = Output(UInt(XLEN.W))
  val regWaddr  = Output(UInt(5.W))
  val regWdata  = Output(UInt(XLEN.W))
  val pc        = Output(UInt(XLEN.W))
}

class IrqIO extends Bundle {
  val uartIrq  = Input(Bool())
 // val timerIrq  = Input(Bool())
}

class CoreIO extends Bundle {
// MT  val host  = new HostIO
  val debug = new DebugIO
  val irq   = new IrqIO
  val imem  = Flipped((new InstMemIO))
  val dmem  = Flipped((new DataMemIO))
}

class Core extends Module {
  val io = IO(new CoreIO)
  val dpath = Module(new Datapath) 
  val ctrl  = Module(new Control)

// MT  io.host <> dpath.io.host
  io.debug <> dpath.io.debug
  io.irq <> dpath.io.irq
  dpath.io.imem <> io.imem 
  dpath.io.dmem <> io.dmem 
  dpath.io.ctrl <> ctrl.io
}

object Core_generate extends App {
  chisel3.Driver.execute(args, () => new Core)
} 

