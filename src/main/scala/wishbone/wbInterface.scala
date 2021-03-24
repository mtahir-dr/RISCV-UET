/******************************************************************************
 * Filename :    wbInterface.scala
 * Date     :    20-05-2020
 *
 * Description:  Standard Wishbone bus interface (ver. B4) signal definitions.
 *
 * 21-05-2020    All the standard interface signals are defined. In addition,
 *               a Tagged data (tgd) signal for store type is also defined.
 *******************************************************************************/

package wishbone

import chisel3._
import chisel3.util._
import chisel3.testers._
import chisel3.core.SeqUtils
import riscv_uet.Config


class WishboneMasterIO extends Bundle with Config {
/**
 * See wbspec_b4.pdf Chapter 2. Interface Specification.
 * */

  override def cloneType: this.type =
    new WishboneMasterIO().asInstanceOf[this.type]

  val addr_o      = Output(UInt(XLEN.W))
  val data_o      = Output(UInt(XLEN.W)) // DAT_O on master, DAT_I on slave
  val data_i      = Input (UInt(XLEN.W)) // DAT_I on master, DAT_O on slave
  val we_o        = Output(Bool())
  val sel_o       = Output(UInt(WB_SEL_SIZE.W))  // Output(Vec( portSize / granularity, Bool() ))
  val stb_o       = Output(Bool())
  val ack_i       = Input (Bool())
  val cyc_o       = Output(Bool())

  // tag's for other memory signals
  val tgd_sttype_o   = Output(UInt(DATA_SIZE_SIG_LEN.W))
}


class WishboneSlaveIO extends Bundle with Config {
  /**
   * See wbspec_b4.pdf Chapter 2. Interface Specification.
   * */

  override def cloneType: this.type =
    new WishboneSlaveIO().asInstanceOf[this.type]

  val addr_i      = Input(UInt(XLEN.W))
  val data_o      = Output(UInt(XLEN.W)) // DAT_O on master, DAT_I on slave
  val data_i      = Input (UInt(XLEN.W)) // DAT_I on master, DAT_O on slave
  val we_i        = Input(Bool())
  val sel_i       = Input(UInt(WB_SEL_SIZE.W))  // Output(Vec( portSize / granularity, Bool() ))
  val stb_i       = Input(Bool())
  val ack_o       = Output (Bool())
  val cyc_i       = Input(Bool())

  // tag's for other memory signals
  val tgd_sttype_i   = Input(UInt(DATA_SIZE_SIG_LEN.W))
}

/*
class WB_SlaveIO extends WishboneIO{

} */
