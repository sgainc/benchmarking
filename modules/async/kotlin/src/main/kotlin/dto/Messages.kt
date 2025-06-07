package dto

data class MessageWrapper(val messageType: String, val timestamp: Long, val message: String)

data class EmptyMessage(val dataName: String)
data class CreateDataMessage(val dataName: String, val dataSize: Int, val message: String)
data class ReadDataMessage(val dataName: String)
data class UpdateDataMessage(val dataName: String, val original: String, val replace: String)
