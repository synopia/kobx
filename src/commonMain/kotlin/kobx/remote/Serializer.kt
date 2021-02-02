package kobx.remote

import kobx.types.ListChangeType
import kobx.types.ListDidChange
import kobx.types.ValueDidChange
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

class EntityCreatedSerializer(val em: EntityManager): KSerializer<EntityCreated> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EC") {
        element<Int>("i")
        element<String>("t")
        element<Int>("aid")
    }

    override fun serialize(encoder: Encoder, value: EntityCreated) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.entity.id)
            encodeStringElement(descriptor, 1, value.entity.type)
            encodeIntElement(descriptor, 2, value.entity.attributes.firstOrNull()?.id ?: -1)
        }
    }

    override fun deserialize(decoder: Decoder): EntityCreated {
        decoder.decodeStructure(descriptor) {
            var id: Int = -1
            var type: String = ""
            var aid = -1
            while (true) {
                when(val i=decodeElementIndex(descriptor)) {
                    0->id = decodeIntElement(descriptor, 0)
                    1->type = decodeStringElement(descriptor, 1)
                    2->aid = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE->break
                    else->throw IllegalStateException()
                }
            }
            em.nextAttributeId(aid)
            val entity = EntityFactory.create(em, id, type)
            return EntityCreated(entity)
        }
    }
}
class ListDidChangeSerializer(val em: EntityManager): KSerializer<ListDidChange<*>> {
    @ExperimentalSerializationApi
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LDC") {
        element<Int>("ai")
        element<ListChangeType>("t")
        element<Int>("i")
        element("ns", String.serializer().descriptor)
        element("ni", Int.serializer().descriptor)
        element("nf", Float.serializer().descriptor)

        element("os", String.serializer().descriptor)
        element("oi", Int.serializer().descriptor)
        element("of", Float.serializer().descriptor)

        element("ls", listSerialDescriptor<String>())
        element("li", listSerialDescriptor<Int>())
        element("lf", listSerialDescriptor<Float>())

        element("rs", listSerialDescriptor<String>())
        element("ri", listSerialDescriptor<Int>())
        element("rf", listSerialDescriptor<Float>())

    }


    @ExperimentalSerializationApi
    override fun deserialize(decoder: Decoder): ListDidChange<*> {
        decoder.decodeStructure(descriptor) {
            var id: Int = -1
            var changeType: ListChangeType? = null
            var index: Int = -1
            var ns: String? = null
            var ni: Int? = null
            var nf: Float? = null
            var os: String? = null
            var oi: Int? = null
            var of: Float? = null
            var ls: List<String>? = null
            var li: List<Int>? = null
            var lf: List<Float>? = null
            var rs: List<String>? = null
            var ri: List<Int>? = null
            var rf: List<Float>? = null
            while (true) {
                when(val i=decodeElementIndex(descriptor)) {
                    0->id = decodeIntElement(descriptor, 0)
                    1->changeType = decodeSerializableElement(descriptor, 1, ListChangeType.serializer())
                    2->index = decodeIntElement(descriptor, 2)
                    3->ns = decodeStringElement(descriptor, 3)
                    4->ni = decodeIntElement(descriptor, 4)
                    5->nf = decodeFloatElement(descriptor, 5)
                    6->os = decodeStringElement(descriptor, 6)
                    7->oi = decodeIntElement(descriptor, 7)
                    8->of = decodeFloatElement(descriptor, 8)
                    9->ls = decodeSerializableElement<List<String>>(descriptor, 9, kotlinx.serialization.serializer())
                    10->li = decodeSerializableElement<List<Int>>(descriptor, 10, kotlinx.serialization.serializer())
                    11->lf = decodeSerializableElement<List<Float>>(descriptor, 11, kotlinx.serialization.serializer())
                    12->rs = decodeSerializableElement<List<String>>(descriptor, 12, kotlinx.serialization.serializer())
                    13->ri = decodeSerializableElement<List<Int>>(descriptor, 13, kotlinx.serialization.serializer())
                    14->rf = decodeSerializableElement<List<Float>>(descriptor, 14, kotlinx.serialization.serializer())
                    CompositeDecoder.DECODE_DONE->break
                    else->throw IllegalStateException()
                }
            }
            return ListDidChange(em.getList(id), changeType!!, index, ns ?: ni ?: nf, os ?: oi ?: of, ls ?:li ?:lf, rs ?: ri ?: rf)
        }
    }

    @ExperimentalSerializationApi
    override fun serialize(encoder: Encoder, value: ListDidChange<*>) {
        encoder.encodeStructure(descriptor) {
            val attr = em.getAttribute<Any>(value.obj)!!
            encodeIntElement(descriptor, 0, attr.id)
            encodeSerializableElement(descriptor, 1, ListChangeType.serializer(), value.changeType)
            encodeIntElement(descriptor, 2, value.index)

            encodePrimitive(descriptor, 3, value.newValue)
            encodePrimitive(descriptor, 6, value.oldValue)

            encodePrimitiveList(descriptor, 9, value.added)
            encodePrimitiveList(descriptor, 12, value.removed)
        }
    }
}

fun CompositeEncoder.encodePrimitive(descriptor: SerialDescriptor, index: Int, v: Any?) {
    when(v) {
        null->{}
        is String->encodeStringElement(descriptor, index, v)
        is Int->encodeIntElement(descriptor, index+1, v)
        is Float->encodeFloatElement(descriptor, index+2, v)
        else->throw IllegalStateException()
    }
}

fun CompositeEncoder.encodePrimitiveList(descriptor: SerialDescriptor, index: Int, list: List<*>?) {
    if( list!=null && list.isNotEmpty() ) {
        when (list[0]) {
            is String -> encodeSerializableElement(descriptor, index, kotlinx.serialization.serializer(), list as List<String>)
            is Int -> encodeSerializableElement(descriptor, index + 1, kotlinx.serialization.serializer(), list as List<Int>)
            is Float -> encodeSerializableElement(descriptor, index + 2, kotlinx.serialization.serializer(), list as List<Float>)
            else -> throw IllegalStateException()
        }
    }
}

class ValueDidChangeSerializer(val em: EntityManager): KSerializer<ValueDidChange<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VDC") {
        element<Int>("ai")
        element("ns", String.serializer().descriptor)
        element("ni", Int.serializer().descriptor)
        element("nf", Float.serializer().descriptor)
        element("os", String.serializer().descriptor)
        element("oi", Int.serializer().descriptor)
        element("of", Float.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: ValueDidChange<*>) {
        encoder.encodeStructure(descriptor) {
            val attr = em.getAttribute<Any>(value.obj)!!
            encodeIntElement(descriptor, 0, attr.id)

            encodePrimitive(descriptor, 1, value.newValue!!)
            encodePrimitive(descriptor, 4, value.oldValue)
        }
    }



    override fun deserialize(decoder: Decoder): ValueDidChange<*> {
        decoder.decodeStructure(descriptor) {
            var index: Int=-1
            var ns: String? = null
            var os: String? = null
            var ni: Int? = null
            var oi: Int? = null
            var nf: Float? = null
            var of: Float? = null
            while (true) {
                when(val i=decodeElementIndex(descriptor)) {
                    0->index = decodeIntElement(descriptor, 0)
                    1->ns = decodeStringElement(descriptor, 1)
                    2->ni = decodeIntElement(descriptor, 2)
                    3->nf = decodeFloatElement(descriptor, 3)
                    4->os = decodeStringElement(descriptor, 4)
                    5->oi = decodeIntElement(descriptor, 5)
                    6->of = decodeFloatElement(descriptor, 6)
                    CompositeDecoder.DECODE_DONE->break
                    else->throw IllegalStateException()
                }
            }
            val old = os ?: oi ?: of
            val new = ns ?: ni ?: nf
            return ValueDidChange(em.getValue(index), new!!, old)
        }
    }
}