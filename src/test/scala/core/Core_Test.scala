/******************************************************************
* Filename:      Core_Test.scala
* Date:          04-04-2020
* Author:        M Tahir
*
* Description:   This is a simple test based on Chisel3 iotesters  
*                using FIRRTL compiler.
*
* Issues:        
*                 
******************************************************************/

package riscv_uet

import chisel3._
import chisel3.util
import chisel3.UInt
import chisel3.iotesters
import chisel3.iotesters.{Driver, PeekPokeTester}
import scala.io.Source
import org.scalatest.{FlatSpec, Matchers}
import java.io.{File, PrintWriter}

import CONSTANTS._
import ALUOP._
// import utils.ArgParser


class TestCore(c: ProcessorTile) // traceFile: String, genTrace: Boolean
      extends PeekPokeTester(c) {
  //val endFlag = BigInt("deadc0de", 16)

 /*   do{
        step(1)
      } while (peek(c.io.pc) == 0x204) */
      
      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")
      
      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")

      step(1)
      println(f" pc: 0x${peek(c.io.debug.pc)}%x")
      println(f" inst: 0x${peek(c.io.debug.inst)}%x")

      for (i <- 0 until 32) {
        step(1)
      }
}


object Core_Main extends App {
  var initFile = "src/test/resources/test3.txt"
  var traceFile = ""
  var genTrace = false

 /* val manager = ArgParser(args, (o, v) => {
    o match {
      case Some("--init-file") | Some("-if") => initFile = v; true
      case Some("--trace-file") | Some("-tf") => traceFile = v; true
      case Some("--gen-trace") | Some("-gt") => genTrace = v != "0"; true
      case _ => false
    }
  }) */
  
 /* Driver.execute(args, () => new ProcessorTile) {
    (c) => new TestCore(c)
  } 
  
   Driver.execute(() => new ProcessorTile(initFile), manager) ) {
     c => new CoreUnitTester(c, traceFile, genTrace)
  } */

  iotesters.Driver.execute(Array("--generate-vcd-output", "on", "--backend-name", "treadle"), () => new ProcessorTile(initFile)) {
    c => new TestCore(c)
  } 

}
