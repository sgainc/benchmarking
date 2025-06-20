package listeners

import (
	"benchmark_async/data"
	"context"
	"go.uber.org/fx"
	"go.uber.org/zap"
)

// RedisListener listens for messages on a Redis queue and processes them using a message listener mechanism.
type RedisListener struct {
	redisProvider *data.RedisProvider
	log           *zap.Logger
}

// NewRedisListener creates and initializes a RedisListener and integrates it into the Fx application lifecycle.
func NewRedisListener(redisProvider *data.RedisProvider, log *zap.Logger, lc fx.Lifecycle) *RedisListener {

	listener := &RedisListener{
		redisProvider: redisProvider,
		log:           log,
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

		listener.log.Info("Received message", zap.Any("message", message))
	}
}
