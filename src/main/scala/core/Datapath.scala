/*********************************************************************
* Filename :    Core.scala
* Date     :    28-03-2020
* 
* Description:  Based on riscv mini. Modified cache memory interface
*               to simple Inst/Data memory interfaces
*
* 13-04-2020    The HostIO interface is temporarily disabled. 
*               The store operation is completed in 2 cycles.
*               The load operation loads data in a temporary register
*               'ld_data' at the end of cycle 2 and is loaded to
*               register file at the start of cycle 4. There is one 
*               cycle pipeline stall for load instructions by default. 
*********************************************************************/


package riscv_uet

import chisel3._
import chisel3.util._

import CONSTANTS._

object Const {
  val PC_START = 0x4
  val PC_EVEC  = 0x500
}

class DebugIO extends Bundle with Config {
  val inst      = Output(UInt(XLEN.W))
  val regWaddr  = Output(UInt(5.W))
  val regWdata  = Output(UInt(XLEN.W))
  val pc        = Output(UInt(XLEN.W))
}

class DatapathIO extends Bundle {
 // MT val host  = new HostIO
  val debug = new DebugIO
  val imem  = Flipped(new InstMemIO)
  val dmem  = Flipped(new DataMemIO)
  val ctrl  = Flipped(new ControlSignals)
}

class Datapath extends Module with Config {
  val io      = IO(new DatapathIO)
  val csr     = Module(new CSR)
  val regFile = Module(new RegFile) 
  val alu     = Module(new ALU) 
  val immGen  = Module(new ImmGen) 
  val brCond  = Module(new Branch) 

  import Control._

  /***** Fetch / Execute Registers *****/
  val fe_inst = RegInit(Instructions.NOP)
  val fe_pc   = Reg(UInt())

  /***** Execute / Write Back Registers *****/
  val ew_inst = RegInit(Instructions.NOP) 
  val ew_pc   = Reg(UInt())
  val ew_alu  = Reg(UInt())
  val csr_in  = Reg(UInt())
  val ld_data = Reg(UInt(XLEN.W)) 

  /****** Control signals *****/
  val st_type  = Reg(io.ctrl.st_type.cloneType)
  val ld_type  = Reg(io.ctrl.ld_type.cloneType)
  val wb_sel   = Reg(io.ctrl.wb_sel.cloneType)
  val wb_en    = Reg(Bool())
  val csr_cmd  = Reg(io.ctrl.csr_cmd.cloneType)
  val illegal  = Reg(Bool())
  val pc_check = Reg(Bool())
 
  /****** Fetch *****/
  val started = RegNext(reset.toBool) // MT false.B 
  val stall = false.B    // MT !io.icache.resp.valid || !io.dcache.resp.valid
  val pc   = RegInit(Const.PC_START.U(XLEN.W) - 4.U(XLEN.W))
  val npc  = Mux(stall, pc, Mux(csr.io.expt, csr.io.evec,
             Mux(io.ctrl.pc_sel === PC_EPC,  csr.io.epc,
             Mux(io.ctrl.pc_sel === PC_ALU || brCond.io.taken, alu.io.sum >> 1.U << 1.U,
             Mux(io.ctrl.pc_sel === PC_0, pc, pc + 4.U)))))
  val inst = Mux(started || io.ctrl.inst_kill || brCond.io.taken || csr.io.expt, Instructions.NOP, io.imem.inst)   // io.icache.resp.bits.data  
  pc                      := npc 
  io.imem.addr := npc  // io.icache.req.bits.addr := npc // MT Changed npc to pc to synchronize the instruction fetch from instruction memory
 
/* MT io.icache.req.bits.data := 0.U
  io.icache.req.bits.mask := 0.U
  io.icache.req.valid     := !stall
  io.icache.abort         := false.B */
 
  // Pipelining
  when (!stall) {
    fe_pc   := pc
    fe_inst := inst
  }

  /****** Execute *****/
  // Decode
  io.ctrl.inst  := fe_inst

  // regFile read
  val rd_addr  = fe_inst(11, 7)
  val rs1_addr = fe_inst(19, 15)
  val rs2_addr = fe_inst(24, 20)
  regFile.io.raddr1 := rs1_addr
  regFile.io.raddr2 := rs2_addr

  // gen immdeates
  immGen.io.inst := fe_inst
  immGen.io.sel  := io.ctrl.imm_sel

  // bypass
  val wb_rd_addr = ew_inst(11, 7)
  val rs1hazard = wb_en && rs1_addr.orR && (rs1_addr === wb_rd_addr)
  val rs2hazard = wb_en && rs2_addr.orR && (rs2_addr === wb_rd_addr)
  val rs1 = Mux(wb_sel === WB_ALU && rs1hazard, ew_alu, regFile.io.rdata1) 
  val rs2 = Mux(wb_sel === WB_ALU && rs2hazard, ew_alu, regFile.io.rdata2)
 
  // ALU operations
  alu.io.in_A := Mux(io.ctrl.A_sel === A_RS1, rs1, fe_pc)
  alu.io.in_B := Mux(io.ctrl.B_sel === B_RS2, rs2, immGen.io.out)
  alu.io.alu_Op := io.ctrl.alu_op

  // Branch condition calc
  brCond.io.rs1 := rs1 
  brCond.io.rs2 := rs2
  brCond.io.br_type := io.ctrl.br_type

  // D$ access
  val daddr   = Mux(stall, ew_alu, alu.io.sum) // MT >> 2.U << 2.U (seems it was done for word allignment)
 //MT val woffset = alu.io.sum(1) << 4.U | alu.io.sum(0) << 3.U
  io.dmem.addr     := daddr 
  
  io.dmem.wr_en    := !stall && io.ctrl.st_type.orR 
  io.dmem.st_type  := Mux(stall, st_type, io.ctrl.st_type)
  io.dmem.wdata    := MuxLookup(Mux(stall, st_type, io.ctrl.st_type), 
                                0.U , Seq(    // MT -- have used default value of 0.U should be reconsidered
    ST_SW -> rs2 ,
    ST_SH -> rs2(15, 0) ,
    ST_SB -> rs2(7, 0) )).asUInt
 
 
  // Pipelining
  when(reset.toBool || !stall && csr.io.expt) {
    st_type   := 0.U
    ld_type   := 0.U
    wb_en     := false.B
    csr_cmd   := 0.U
    illegal   := false.B
    pc_check  := false.B
  }.elsewhen(!stall && !csr.io.expt) {
    ew_pc     := fe_pc
    ew_inst   := fe_inst
    ew_alu    := alu.io.out
    csr_in    := Mux(io.ctrl.imm_sel === IMM_Z, immGen.io.out, rs1)
    st_type   := io.ctrl.st_type
    ld_type   := io.ctrl.ld_type
    wb_sel    := io.ctrl.wb_sel
    wb_en     := io.ctrl.wb_en
    csr_cmd   := io.ctrl.csr_cmd
    illegal   := io.ctrl.illegal
    pc_check  := io.ctrl.pc_sel === PC_ALU
  }

  // Load
  io.dmem.rd_en     := !stall && io.ctrl.ld_type.orR
  io.dmem.ld_type  := Mux(stall, ld_type, io.ctrl.ld_type)

//  val loffset = ew_alu(1) << 4.U | ew_alu(0) << 3.U
  ld_data  := io.dmem.rdata 
  val load    = MuxLookup(ld_type, io.dmem.rdata.zext, Seq(
    LD_LW  -> ld_data.zext,
    LD_LH  -> ld_data(15, 0).asSInt, LD_LB  -> ld_data(7, 0).asSInt,
    LD_LHU -> ld_data(15, 0).zext,   LD_LBU -> ld_data(7, 0).zext) )

    
  // CSR access
  csr.io.stall    := stall
  csr.io.in       := csr_in
  csr.io.cmd      := csr_cmd
  csr.io.inst     := ew_inst
  csr.io.pc       := ew_pc
  csr.io.addr     := ew_alu
  csr.io.illegal  := illegal
  csr.io.pc_check := pc_check
  csr.io.ld_type  := ld_type
  csr.io.st_type  := st_type
 // MT io.host <> csr.io.host 

  // Regfile Write
  val regWrite = MuxLookup(wb_sel, ew_alu.zext, Seq(
    WB_MEM -> load,
    WB_PC4 -> (ew_pc + 4.U).zext,
    WB_CSR -> csr.io.out.zext) ).asUInt 

  regFile.io.wen   := wb_en && !stall && !csr.io.expt 
  regFile.io.waddr := wb_rd_addr
  regFile.io.wdata := regWrite

  // Abort store when there's an excpetion
//MT  io.dcache.abort := csr.io.expt

/* MT  if (p(Trace)) {
    printf("PC: %x, INST: %x, REG[%d] <- %x\n", ew_pc, ew_inst,
      Mux(regFile.io.wen, wb_rd_addr, 0.U),
      Mux(regFile.io.wen, regFile.io.wdata, 0.U))
  } */

 // debug signals
  io.debug.inst     := fe_inst
  io.debug.regWaddr := wb_rd_addr
  io.debug.regWdata := regWrite
  io.debug.pc       := fe_pc

}

object Datapath_generate extends App {
  chisel3.Driver.execute(args, () => new Datapath )
} 


