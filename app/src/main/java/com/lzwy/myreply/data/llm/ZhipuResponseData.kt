package com.lzwy.myreply.data.llm

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class ZhipuResponseData(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("created")
    val created: Long = -1,
    @SerializedName("model")
    val model: String = "",
    @SerializedName("choices")
    val choices: List<Choices> = ArrayList(),
    @SerializedName("web_search")
    var webSearch: ArrayList<WebSearchContent>? = ArrayList(),
    @SerializedName("status")
    val status: String?,
    @SerializedName("assistant_id")
    val assistantId: String = "",
    @SerializedName("conversation_id")
    val conversationId: String?,
    @SerializedName("content")
    var content: String = ""
)

@Keep
class ZhipuResponseFileData {
    @SerializedName("id")
    var id: String = ""

    @SerializedName("object")
    var obj: String = ""

    @SerializedName("bytes")
    var bytes: Int = 0

    @SerializedName("filename")
    var filename: String = ""

    @SerializedName("purpose")
    var purpose: String = ""

    @SerializedName("created_at")
    var createdAt: Long = 0

    override fun toString(): String {
        return "ZhipuResponse(id='$id',filename='$filename', created_at='$createdAt')"
    }
}

@Keep
class ZhipuResponseContentData {
    @SerializedName("content")
    var content: String = ""

    @SerializedName("file_type")
    var fileType: String = ""

    @SerializedName("filename")
    var fileName: String = ""

    @SerializedName("title")
    var title: String = ""

    @SerializedName("type")
    var type: String = ""

    @SerializedName("fileID")
    var fileId: String = ""   // self add

    @SerializedName("requestID")
    var requestId: String = ""   // self add
    override fun toString(): String {
        return "ZhipuResponseContentData(id='$content')"
    }
}


@Keep
class ZhipuResponseFileList {
    @SerializedName("object")
    var obj: String = ""

    @SerializedName("data")
    var data: ArrayList<ZhipuResponseFileData> = ArrayList()
}

@Keep
data class Choices(
    @SerializedName("index")
    var index: Int,
    @SerializedName("finish_reason")
    var finishReason: String?,
    @SerializedName("delta", alternate = ["message"])
    var delta: Delta
)

@Keep
data class Delta(
    @SerializedName("role")
    var role: String,
    @SerializedName("content")
    var content: String,
    @SerializedName("tool_calls")
    val toolCalls: List<ToolCall>?,
    @SerializedName("metadata")
    val metadata: Metadata?,
)

@Keep
data class ToolCall(
    @SerializedName("function")
    val function: Function?,
    @SerializedName("index")
    val index: Int,
    @SerializedName("type")
    val type: String
)

@Keep
data class Function(
    @SerializedName("arguments")
    val arguments: String?,
    @SerializedName("name")
    val name: String,
    @SerializedName("outputs")
    val outputs: List<Output>?
)

@Keep
data class Output(
    @SerializedName("content")
    val content: String
)

@Keep
data class Metadata(
    val data: String?
)

@Keep
data class WebSearchContent(
    @SerializedName("refer")
    var refer: String = "",
    @SerializedName("title")
    var title: String = "",
    @SerializedName("link")
    var link: String = "",
    @SerializedName("content")
    var content: String = "",
    @SerializedName("media")
    var media: String = "",
    @SerializedName("icon")
    var icon: String = ""
) {
    override fun toString(): String {
        return "WebSearchContent(refer='$refer', " +
                "title='$title', " +
                "link='$link', " +
                "content='$content', " +
                "media='$media', " +
                "icon='$icon')"
    }
}