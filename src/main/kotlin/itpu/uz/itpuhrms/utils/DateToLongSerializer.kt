package itpu.uz.itpuhrms.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.util.*

class DateToLongSerializer : JsonSerializer<Date?>() {
    override fun serialize(value: Date?, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(
            value?.time ?: return gen.writeNull())
    }
}