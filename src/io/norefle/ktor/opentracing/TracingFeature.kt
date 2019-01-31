package io.norefle.ktor.io.norefle.ktor.opentracing

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.request.header
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.header
import io.ktor.util.AttributeKey
import java.util.Stack
import java.util.concurrent.ConcurrentHashMap

data class TraceId(val id: String)
data class SpanId(val id: String)

class TracingFeature(configuration: Configuration) {
    val openTraces: ConcurrentHashMap<TraceId, Stack<SpanId>> = ConcurrentHashMap()

    class Configuration {
        // opentracing-api implementation
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, TracingFeature> {
        override val key = AttributeKey<TracingFeature>("TracingFeature")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): TracingFeature {
            val instanceConfig = Configuration().apply(configure)
            val feature = TracingFeature(instanceConfig)

            pipeline.intercept(ApplicationCallPipeline.Setup) {
                // Get or generate TraceId
                /// TODO: check if sampled is set then instrument else do nothing
                val id = this.call.request.header("X-B3-TraceId")
                this.application.log.debug("Intercepting setup with trace id = $id")
                // Before sending response to client set TraceId and the last open span id
                this.call.response.pipeline.intercept(ApplicationSendPipeline.After) {
                    this.application.log.debug("Intercepting after with trace id = $id")
                    // Set headers for client
                    this.call.response.header("X-B3-TraceId", id ?: "xxx-xxx-xxxxxxx")
                    this.call.response.header("X-B3-SpanId", "123-123-1234567")
                    // TODO: Drop trace from openTraces here
                }
            }

            return feature
        }
    }
}