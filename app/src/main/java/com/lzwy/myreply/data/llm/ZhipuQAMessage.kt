package com.lzwy.myreply.data.llm

import com.google.gson.annotations.SerializedName

typealias MultimediaType = MutableList<MutableMap<String, Any>>

@JvmInline
value class ContentType(val value: String) {
    companion object {
        val empty = ContentType("")
        val video = ContentType("video_url")
        val image = ContentType("image_url")
    }
}

@JvmInline
value class Role(val value: String) {
    companion object {
        val user = Role("user")
        val system = Role("system")
        val assistant = Role("assistant")
    }
}

@JvmInline
value class Query(val value: String) {
    companion object {
        fun create(query: String) = Query(query)
    }
}

@JvmInline
value class Content(val value: String) {
    companion object {
        fun create(text: String) = Content(text)
    }
}

@JvmInline
value class FileInfo(val value: String) {
    companion object {
        val empty = FileInfo("")
        fun imageBase64(image64: String) = FileInfo(image64)
        fun imageUrl(imageUrl: String) = FileInfo(imageUrl)
        fun videoUrl(videoUrl: String) = FileInfo(videoUrl)
        fun FileInfo?.isValid(): Boolean {
            if (this == null) {
                return false
            }
            return value.isNotEmpty()
        }
    }
}

open class LlmMessage()

open class ZhipuQAMessage<T>(
    @SerializedName("role")
    var role: String?,
    @SerializedName("query")
    var query: String?,
    @SerializedName("tools_call")
    var toolsCall: String? = null,
    @SerializedName("content")
    var content: T? = null
): LlmMessage() {

    fun removeImageFromContent() {
    }

    override fun toString(): String {
        return "ZhipuQAMessage(role=$role, query=$query, toolsCall=$toolsCall, content=$content)"
    }

    companion object {
        private fun <T> instantiate(
            role: Role,
            query: Query? = null,
            toolsCall: String? = null,
            contentInstantiate: (ZhipuQAMessage<T>) -> Unit
        ): ZhipuQAMessage<T> {
            return ZhipuQAMessage<T>(
                query = query?.value.orEmpty(),
                role = role.value,
                toolsCall = toolsCall
            ).also {
                contentInstantiate(it)
            }
        }

        fun plainMessage(
            role: Role,
            content: Content,
            query: Query? = null,
            toolsCall: String? = null
        ): ZhipuQAMessage<String> {
            return instantiate(
                role = role,
                query = query,
                toolsCall = toolsCall
            ) {
                it.content = content.value
            }
        }

        fun multimediaMessage(
            role: Role,
            contentType: ContentType,
            fileInfo: FileInfo,
            query: Query? = null,
            toolsCall: String? = null,
        ): ZhipuQAMessage<MultimediaType> {
            return instantiate(
                role = role,
                query = query,
                toolsCall = toolsCall
            ) {
                it.content = mutableListOf<MutableMap<String, Any>>().apply {
                    add(
                        mutableMapOf(
                            "type" to "text",
                            "text" to it.query.orEmpty()
                        )
                    )
                    add(
                        mutableMapOf(
                            "type" to contentType.value,
                            contentType.value to mapOf("url" to fileInfo.value)
                        )
                    )
                }
            }
        }
    }
}