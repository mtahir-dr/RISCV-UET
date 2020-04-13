/*********************************************************************
* Filename :    ProcessorTile.scala
* Date     :    28-03-2020
* 
* Description:  Based on riscv mini. Modified cache memory interface
*               to simple Inst/Data memory interfaces
*
* 13-04-2020    The DebugIO interface is being used for testing.
*     
*          
*               An object code file 'fib.txt' is used to load program
*               to code memory for execution during testing. 
*********************************************************************/


package riscv_uet

import chisel3._
import chisel3.util._


class TileIO extends Bundle {
  val debug  = new DebugIO
}

trait TileBase extends core.BaseModule {
  def io: TileIO
  def clock: Clock
  def reset: core.Reset
}

class ProcessorTile(initFile: String) extends Module with TileBase {
  val io     = IO(new TileIO)
  val core   = Module(new Core)
  val imem = Module(new InstMem(initFile))
  val dmem = Module(new DataMem)
 
  io.debug  <> core.io.debug  
  core.io.imem <> imem.io
  core.io.dmem <> dmem.io
}

object ProcessorTile_generate extends App {
var initFile = "src/test/resources/fib.txt"

  chisel3.Driver.execute(args, () => new ProcessorTile(initFile))
} 

