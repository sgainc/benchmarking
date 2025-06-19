package types

type MessageType string

const (
	CREATE_MESSAGE MessageType = "CREATE_MESSAGE"
	READ_MESSAGE   MessageType = "READ_MESSAGE"
	DELETE_MESSAGE MessageType = "DELETE_MESSAGE"
	UPDATE_MESSAGE MessageType = "UPDATE_MESSAGE"
)

type ReadDataMessage struct {
	DataName string
}

type CreateDataMessage struct {
	DataName string
	DataSize int
}

type UpdateDataMessage struct {
	DataName string
	Original string
	Replace  string
}

type DeleteDataMessage struct {
	DataName string
}

type MessageWrapper struct {
	MessageType MessageType
	Timestamp   int64
	Message     any
}

func NewMessageWrapper(messageType MessageType, timestamp int64, message any) MessageWrapper {
	return MessageWrapper{
		MessageType: messageType,
		Timestamp:   timestamp,
		Message:     message,
	}
}

func NewCreateDataMessage(dataName string, dataSize int) CreateDataMessage {
	return CreateDataMessage{
		DataName: dataName,
		DataSize: dataSize,
	}
}

func NewReadDataMessage(dataName string) ReadDataMessage {
	return ReadDataMessage{
		DataName: dataName,
	}
}

func NewUpdateDataMessage(dataName string, original string, replace string) UpdateDataMessage {
	return UpdateDataMessage{
		DataName: dataName,
		Original: original,
		Replace:  replace,
	}
}

func NewDeleteDataMessage(dataName string) DeleteDataMessage {
	return DeleteDataMessage{
		DataName: dataName,
	}
}
