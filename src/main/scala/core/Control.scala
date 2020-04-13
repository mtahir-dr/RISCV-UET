/*********************************************************************
* Filename :    Control.scala
* Date     :    28-03-2020
* 
* Description:  Based on riscv mini. 
*********************************************************************/

package riscv_uet

import chisel3._
import chisel3.util.ListLookup

import Instructions._
import ALUOP._
import CSR._
import CONSTANTS._


object Control {

 
  val default =
    //                                                            kill                        wb_en  illegal?
    //            pc_sel  A_sel   B_sel  imm_sel   alu_op   br_type |  st_type ld_type wb_sel  | csr_cmd |
    //              |       |       |     |          |          |   |     |       |       |    |  |      |
             List(PC_4,   A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, Y)
  val map = Array(
    LUI   -> List(PC_4  , A_PC,   B_IMM, IMM_U, ALU_COPY_B, BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    AUIPC -> List(PC_4  , A_PC,   B_IMM, IMM_U, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    JAL   -> List(PC_ALU, A_PC,   B_IMM, IMM_J, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_XXX, WB_PC4, Y, CSR.Z, N),
    JALR  -> List(PC_ALU, A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_XXX, WB_PC4, Y, CSR.Z, N),
    BEQ   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_EQ , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    BNE   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_NE , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    BLT   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_LT , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    BGE   -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_GE , N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    BLTU  -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_LTU, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    BGEU  -> List(PC_4  , A_PC,   B_IMM, IMM_B, ALU_ADD   , BR_GEU, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    LB    -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LB , WB_MEM, Y, CSR.Z, N),
    LH    -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LH , WB_MEM, Y, CSR.Z, N),
    LW    -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LW , WB_MEM, Y, CSR.Z, N),
    LBU   -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LBU, WB_MEM, Y, CSR.Z, N),
    LHU   -> List(PC_0  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, Y, ST_XXX, LD_LHU, WB_MEM, Y, CSR.Z, N),
    SB    -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SB , LD_XXX, WB_ALU, N, CSR.Z, N),
    SH    -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SH , LD_XXX, WB_ALU, N, CSR.Z, N),
    SW    -> List(PC_4  , A_RS1,  B_IMM, IMM_S, ALU_ADD   , BR_XXX, N, ST_SW , LD_XXX, WB_ALU, N, CSR.Z, N),
    ADDI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SLTI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLT   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SLTIU -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLTU  , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    XORI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_XOR   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    ORI   -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_OR    , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    ANDI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_AND   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SLLI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SLL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SRLI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SRL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SRAI  -> List(PC_4  , A_RS1,  B_IMM, IMM_I, ALU_SRA   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    ADD   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_ADD   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SUB   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SUB   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SLL   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SLT   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLT   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SLTU  -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SLTU  , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    XOR   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_XOR   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SRL   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SRL   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    SRA   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_SRA   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    OR    -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_OR    , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    AND   -> List(PC_4  , A_RS1,  B_RS2, IMM_X, ALU_AND   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.Z, N),
    FENCE -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    FENCEI-> List(PC_0  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N),
    CSRRW -> List(PC_0  , A_RS1,  B_XXX, IMM_X, ALU_COPY_A, BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.W, N),
    CSRRS -> List(PC_0  , A_RS1,  B_XXX, IMM_X, ALU_COPY_A, BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.S, N),
    CSRRC -> List(PC_0  , A_RS1,  B_XXX, IMM_X, ALU_COPY_A, BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.C, N),
    CSRRWI-> List(PC_0  , A_XXX,  B_XXX, IMM_Z, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.W, N),
    CSRRSI-> List(PC_0  , A_XXX,  B_XXX, IMM_Z, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.S, N),
    CSRRCI-> List(PC_0  , A_XXX,  B_XXX, IMM_Z, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, Y, CSR.C, N),
    ECALL -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N),
    EBREAK-> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N),
    ERET  -> List(PC_EPC, A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, Y, ST_XXX, LD_XXX, WB_CSR, N, CSR.P, N),
    WFI   -> List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.Z, N))
}

class ControlSignals extends Bundle with Config {
  val inst      = Input(UInt(XLEN.W))
  val pc_sel    = Output(UInt(2.W))
  val inst_kill = Output(Bool())
  val A_sel     = Output(UInt(1.W))
  val B_sel     = Output(UInt(1.W))
  val imm_sel   = Output(UInt(3.W))
  val alu_op    = Output(UInt(5.W))
  val br_type   = Output(UInt(3.W))
  val st_type   = Output(UInt(2.W))
  val ld_type   = Output(UInt(3.W))
  val wb_sel    = Output(UInt(2.W))
  val wb_en     = Output(Bool())
  val csr_cmd   = Output(UInt(3.W))
  val illegal   = Output(Bool())
}

class Control extends Module {
  val io = IO(new ControlSignals)
  val ctrlSignals = ListLookup(io.inst, Control.default, Control.map)

  // Control signals for Fetch
  io.pc_sel    := ctrlSignals(0)
  io.inst_kill := ctrlSignals(6).toBool 

  // Control signals for Execute
  io.A_sel   := ctrlSignals(1)
  io.B_sel   := ctrlSignals(2)
  io.imm_sel := ctrlSignals(3)
  io.alu_op  := ctrlSignals(4)
  io.br_type := ctrlSignals(5)
  io.st_type := ctrlSignals(7)

  // Control signals for Write Back
  io.ld_type := ctrlSignals(8)
  io.wb_sel  := ctrlSignals(9)
  io.wb_en   := ctrlSignals(10).toBool
  io.csr_cmd := ctrlSignals(11)
  io.illegal := ctrlSignals(12)
}

