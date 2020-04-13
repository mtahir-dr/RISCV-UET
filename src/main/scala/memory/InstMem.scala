/*********************************************************************
* Filename :    InstMem.scala
* Date     :    28-03-2020
* 
* Description:  Test data memory, 1024 Bytes, 32 Words, implemented 
*               using 'mem'. The memory size can be changed in 
*               Configurations.scala.
*
* 13-04-2020    Supports word (32-bit) sized and word aligned reads.
*
*********************************************************************/


package riscv_uet

import chisel3._
import chisel3.util.Cat
import chisel3.util.experimental.loadMemoryFromFile
import scala.io.Source
import java.io.{File, PrintWriter}


class InstMemIO extends Bundle with Config {
  val addr = Input(UInt(WLEN.W))
  val inst = Output(UInt(WLEN.W))
}

class InstMem(initFile: String) extends Module with Config {
  val io = IO(new InstMemIO)

  // INST_MEM_LEN Byte and INST_MEM_LEN / 4 Words
  val imem = Mem(INST_MEM_LEN, UInt(WLEN.W))

    // loadMemoryFromFile(imem , "src/test/resources/fib.txt")
   loadMemoryFromFile(imem , initFile)

  // io.inst := Cat(imem (io.addr), imem (io.addr + 1.U), imem (io.addr + 2.U), imem (io.addr + 3.U))
   
   io.inst := imem (io.addr / 4.U)  // 0x00000013.U 
   
}

/*
object InstMem extends App {
  chisel3.Driver.execute(args, () => new InstMem)
}
*/