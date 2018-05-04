package com.tblf.monitors

import java.io.File
import java.util.concurrent.TimeUnit

import com.tblf.reporters.ChronicleQueueReporter
import org.powerapi.core.LinuxHelper
import org.powerapi.core.target.Process
import org.powerapi.module.cpu.simple.ProcFSCpuSimpleModule
import org.powerapi.{PowerMeter, PowerMonitoring}

import scala.concurrent.duration.FiniteDuration

class BitWattsMonitor(reportDirectory: File) extends AbstractMonitor(reportDirectory) {

  val linuxHelper = new LinuxHelper
  val powerModule = ProcFSCpuSimpleModule()
  val powerMeter = PowerMeter.loadModule(powerModule)
  val queue = new ChronicleQueueReporter(reportDirectory)
  var monitoring: PowerMonitoring = _

  override def startMonitor(pid: Int): Unit = {

    monitoring = powerMeter.monitor(FiniteDuration(50, TimeUnit.MILLISECONDS))(Process(pid)) to queue
  }

  override def endMonitor(pid: Int): Unit = {
    monitoring.cancel
  }
}