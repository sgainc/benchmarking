package listeners

import (
	"benchmark_async/app"
	"benchmark_async/data"
	"benchmark_async/types"
	"context"
	"github.com/mitchellh/mapstructure"
	"go.uber.org/fx"
	"go.uber.org/zap"
)

// RedisListener listens for messages on a Redis queue and processes them using a message listener mechanism.
type RedisListener struct {
	redisProvider *data.RedisProvider
	log           *zap.Logger
	state         *app.AppState
}

// NewRedisListener creates and initializes a RedisListener and integrates it into the Fx application lifecycle.
func NewRedisListener(
	redisProvider *data.RedisProvider,
	log *zap.Logger,
	state *app.AppState,
	lc fx.Lifecycle) *RedisListener {

	listener := &RedisListener{
		redisProvider: redisProvider,
		log:           log,
		state:         state,
	}

	lc.Append(fx.Hook{
		OnStart: func(ctx context.Context) error {
			go listener.messageListener(ctx)
			return nil
		},
		OnStop: func(ctx context.Context) error {
			return nil
		},
	})

	return listener
}

func (listener *RedisListener) messageListener(ctx context.Context) {

	for {
		message, err := listener.redisProvider.ReceiveMessage(context.Background())
		if err != nil {
			listener.log.Error("Failed to receive message", zap.Error(err))
			continue
		}

		listener.log.Debug("Received message", zap.Any("message", message))
		listener.messageHandler(message)
	}
}

func (listener *RedisListener) messageHandler(message types.MessageWrapper) {

	// Check the message type and cast to the correct type.
	switch message.MessageType {
	case types.CREATE_MESSAGE:
		var createMessage types.CreateDataMessage
		err := mapstructure.Decode(message.Message, &createMessage)
		if err != nil {
			listener.log.Error("Failed to decode message", zap.Error(err))
			return
		}

		//TODO: write datafile to S3
		listener.state.ObjectList.SetIfAbsent(createMessage.DataName, true)
		listener.log.Debug("Created data file", zap.String("dataName", createMessage.DataName))
		listener.state.IncrementEventCount()
	}
}
