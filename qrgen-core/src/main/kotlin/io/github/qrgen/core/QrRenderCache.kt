package io.github.qrgen.core

import java.util.LinkedHashMap

class QrRenderCache(private val options: CacheOptions = CacheOptions(enabled = true)) {
    private val storage = object : LinkedHashMap<String, ByteArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > options.maxEntries
        }
    }

    @Synchronized
    fun getOrPut(key: String, producer: () -> ByteArray): ByteArray {
        if (!options.enabled) return producer()
        return storage[key] ?: producer().also { storage[key] = it }
    }
}

fun qrCacheKey(data: String, config: QrStyleConfig, format: String): String {
    return buildString {
        append(format)
        append('|')
        append(data.normalizedConfigKey())
        append('|')
        append(config.hashCode())
    }
}
