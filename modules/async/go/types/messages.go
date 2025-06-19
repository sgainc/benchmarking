package types

type MessageType string

const (
	CREATE_MESSAGE MessageType = "CREATE_MESSAGE"
	READ_MESSAGE   MessageType = "READ_MESSAGE"
	DELETE_MESSAGE MessageType = "DELETE_MESSAGE"
	UPDATE_MESSAGE MessageType = "UPDATE_MESSAGE"
)

type BaseMessage struct {
	DataName string
}

type CreateDataMessage struct {
	BaseMessage
	DataSize int
}

type ReadDataMessage struct {
	BaseMessage
}

type UpdateDataMessage struct {
	BaseMessage
	Original string
	Replace  string
}

type DeleteDataMessage struct {
	BaseMessage
}

type MessageWrapper[T any] struct {
	MessageType MessageType
	Timestamp   int64
	Message     T
}

func NewMessageWrapper[T any](messageType MessageType, timestamp int64, message T) MessageWrapper[T] {
	return MessageWrapper[T]{
		MessageType: messageType,
		Timestamp:   timestamp,
		Message:     message,
	}
}

func NewCreateDataMessage(dataName string, dataSize int) CreateDataMessage {
	return CreateDataMessage{
		BaseMessage: BaseMessage{DataName: dataName},
		DataSize:    dataSize,
	}
}

func NewReadDataMessage(dataName string) ReadDataMessage {
	return ReadDataMessage{
		BaseMessage: BaseMessage{DataName: dataName},
	}
}

func NewUpdateDataMessage(dataName string, original string, replace string) UpdateDataMessage {
	return UpdateDataMessage{
		BaseMessage: BaseMessage{DataName: dataName},
		Original:    original,
		Replace:     replace,
	}
}

func NewDeleteDataMessage(dataName string) DeleteDataMessage {
	return DeleteDataMessage{
		BaseMessage: BaseMessage{DataName: dataName},
	}
}
