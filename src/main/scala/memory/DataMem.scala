/*********************************************************************
* Filename :    DataMem.scala
* Date     :    28-03-2020
* 
* Description:  Test data memory, 1024 Bytes, 32 Words, implemented 
*               using synchronous read memory'SyncReadMem'. The memory
 *              size can be changed in Configurations.scala.
*
* 13-04-2020    Supports variable length as well as unaligned
*               load and store
*********************************************************************/


package riscv_uet

import chisel3._
import chisel3.util._
import Control._
import chisel3.util.experimental.loadMemoryFromFile

import CONSTANTS._

class DataMemIO extends Bundle with Config {
  val addr        = Input(UInt(WLEN.W))
  val wdata       = Input(UInt(WLEN.W))
  val rd_en       = Input(UInt(MEM_READ_SIG_LEN.W))
  val wr_en       = Input(UInt(MEM_WRITE_SIG_LEN.W))
  val st_type     = Input(UInt(DATA_SIZE_SIG_LEN.W))
  val ld_type     = Input(UInt(LOAD_TYPE_SIG_LEN.W))
  val rdata       = Output(UInt(WLEN.W))
}

class DataMem extends Module with Config {
  val io = IO(new DataMemIO)

  // DATA_CAHCE_LEM Byte and DATA_CAHCE_LEM / 4 Words
  val dmem = SyncReadMem(DATA_MEM_LEN, UInt(BLEN.W))
 // loadMemoryFromFile(dmem, "resources/datamem.txt")


  val addr = io.addr
  val read_data = Wire(UInt(XLEN.W)) 
      read_data := 0.U      
  
    when (io.wr_en.toBool()) {
      when (io.st_type === 1.U) {
        dmem (addr) := io.wdata(7,0)
        dmem (addr + 1.U) := io.wdata(15,8)
        dmem (addr + 2.U) := io.wdata(23,16)
        dmem (addr + 3.U) := io.wdata(31,24) 
      }.elsewhen (io.st_type === 2.U) {
        dmem (addr) := io.wdata(7,0)
        dmem (addr + 1.U) := io.wdata(15,8)
      }.elsewhen (io.st_type === 3.U) {
        dmem (addr) := io.wdata(7,0)
      }
    } 

    read_data := Cat(dmem(addr + 3.U), dmem(addr + 2.U), dmem(addr + 1.U), dmem(addr))
     
    io.rdata := Mux(io.rd_en.toBool(), read_data, 0.U)
}
   

/*
object DataMem extends App {
  chisel3.Driver.execute(args, () => new DataMem)
} */
