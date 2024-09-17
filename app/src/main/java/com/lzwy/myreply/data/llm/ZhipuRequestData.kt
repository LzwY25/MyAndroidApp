package com.lzwy.myreply.data.llm

import com.google.gson.annotations.SerializedName
import java.io.File

class ZhipuApiMediaRequestData {
    @SerializedName("model")
    var model: String = ""
    @SerializedName("messages")
    var messages: ArrayList<ZhipuQAMessage<*>> = ArrayList()
    @SerializedName("tools")
    var tools: ArrayList<Tools> = ArrayList()
    @SerializedName("stream")
    var stream: Boolean = false
}

class ZhipuApiRequestData {
    @SerializedName("model")
    var model: String = ""
    @SerializedName("messages")
    var messages: ArrayList<ZhipuQAMessage<*>> = ArrayList()
    @SerializedName("tools")
    var tools: ArrayList<Tools> = ArrayList()
    @SerializedName("stream")
    var stream: Boolean = false
}

class ZhipuApiAssistantRequestData {
    @SerializedName("assistant_id")
    var assistant_id: String = "" //"assistant-1"
    @SerializedName("conversation_id")
    var conversation_id: String? = null
    @SerializedName("model")
    var model: String = ""
    @SerializedName("messages")
    var messages: ArrayList<ZhipuQAMessage<*>> = ArrayList()
    @SerializedName("tools")
    var tools: ArrayList<Tools> = ArrayList()
    @SerializedName("stream")
    var stream: Boolean = false
}

class ZhipuApiFileData {
    @SerializedName("model")
    var model: String = ""
    @SerializedName("file")
    var file: File? = null
    @SerializedName("purpose")
    var purpose: String = "file-extract"
}

data class Tools(
    @SerializedName("type")
    var type: String,
    @SerializedName("web_search")
    var web_search: WebSearch
)

data class WebSearch(
    @SerializedName("enable")
    var enable: Boolean,
    @SerializedName("search_result")
    var search_result: Boolean,
    @SerializedName("search_prompt")
    var search_prompt: String
)
