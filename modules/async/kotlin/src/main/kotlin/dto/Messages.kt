package dto

enum class MessageType
{
    CREATE_MESSAGE, READ_MESSAGE, UPDATE_MESSAGE, DELETE_MESSAGE
}

/**
 * The MessageWrapper contains metadata that can be use to deserialize the data
 * contained in the message field. It also contains a timestamp so we can calculate
 * latency.
 * @param <T> The type of the message contained in the message field.
 * @property messageType The type of message.
 * @property timestamp The timestamp of the message in epoch milliseconds.
 * @property message The actual message.
 */
class MessageWrapper<T>(val messageType: MessageType, val timestamp: Long, val message: T)

/**
 * Base class that all messages inherit from. The dataName field is used to identify
 * the key used to store the data in the datastore.
 * @param dataName The unique identifier for the data in the datastore.
 */
open class BaseMessage(open val dataName: String)


/**
 * Message class used for creating new data entries in the datastore.
 *
 * @property dataName The unique identifier for the data in the datastore.
 * @property dataSize The size of the data being created.
 */
class CreateDataMessage(override val dataName: String, val dataSize: Int): BaseMessage(dataName)

/**
 * Message class used for reading data from the datastore.
 *
 * @property dataName The unique identifier for the data in the datastore.
 */
class ReadDataMessage(override val dataName: String): BaseMessage(dataName)

/**
 * Message class used for updating data in the datastore.
 *
 * @property dataName The unique identifier for the data in the datastore.
 * @property original The original data string to search for.
 * @property replace The replacement value of the data.
 */
class UpdateDataMessage(override val dataName: String, val original: String, val replace: String): BaseMessage(dataName)

/**
 * Message class used for deleting data from the datastore.
 *
 * @property dataName The unique identifier for the data in the datastore.
 */
class DeleteDataMessage(override val dataName: String): BaseMessage(dataName)

