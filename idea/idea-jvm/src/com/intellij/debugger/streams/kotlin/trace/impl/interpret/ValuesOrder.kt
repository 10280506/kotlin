package com.intellij.debugger.streams.kotlin.trace.impl.interpret

import com.intellij.debugger.streams.trace.TraceElement
import com.intellij.debugger.streams.trace.TraceInfo
import com.intellij.debugger.streams.wrapper.StreamCall

/**
 * @author Vitaliy.Bibaev
 */
class ValuesOrder(private val call: StreamCall,
                  private val before: Map<Int, TraceElement>,
                  private val after: Map<Int, TraceElement>)
  : TraceInfo {
  override fun getValuesOrderBefore(): Map<Int, TraceElement> = before

  override fun getCall(): StreamCall = call

  override fun getValuesOrderAfter(): Map<Int, TraceElement> = after

  override fun getDirectTrace(): MutableMap<TraceElement, MutableList<TraceElement>>? = null

  override fun getReverseTrace(): MutableMap<TraceElement, MutableList<TraceElement>>? = null
}