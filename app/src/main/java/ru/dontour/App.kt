package ru.dontour

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import ru.ok.tracer.CoreTracerConfiguration
import ru.ok.tracer.HasTracerConfiguration
import ru.ok.tracer.TracerConfiguration
import ru.ok.tracer.crash.report.CrashFreeConfiguration
import ru.ok.tracer.crash.report.CrashReportConfiguration
import ru.ok.tracer.disk.usage.DiskUsageConfiguration
import ru.ok.tracer.heap.dumps.HeapDumpConfiguration
import ru.ok.tracer.profiler.sampling.SamplingProfilerConfiguration
import ru.ok.tracer.profiler.systrace.SystraceProfilerConfiguration

class App : Application(), HasTracerConfiguration {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("0d179806-09da-46e6-ac02-2bff3bc930e8")

    }
    override val tracerConfiguration: List<TracerConfiguration>
        get() = listOf(
            CoreTracerConfiguration.build {
                // опции ядра трейсера
            },
            CrashReportConfiguration.build {
                // опции сборщика крэшей
            },
            CrashFreeConfiguration.build {
                // опции подсчета crash free
            },
            HeapDumpConfiguration.build {
                // опции сборщика хипдампов при ООМ
            },
            DiskUsageConfiguration.build {
                // опции анализатора дискового пространства
            },
            SystraceProfilerConfiguration.build {
                // опции systrace-профайлера в продакшене
            },
            SamplingProfilerConfiguration.build {
                // опции семплирующего профайлера
            },
        )
}
