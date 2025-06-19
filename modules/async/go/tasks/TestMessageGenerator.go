package tasks

import (
	"benchmark_async/app"
	"benchmark_async/data"
	"context"
	"github.com/go-co-op/gocron/v2"
	"go.uber.org/zap"
	"time"
)

func NewMessageGeneratorTask(redis *data.RedisProvider, log *zap.Logger) app.ScheduledTask {
	return &MessageGeneratorTask{
		redis:   redis,
		log:     log,
		context: context.Background(),
	}
}

type MessageGeneratorTask struct {
	redis   *data.RedisProvider
	log     *zap.Logger
	context context.Context
}

func (*MessageGeneratorTask) Interval() time.Duration {
	return time.Millisecond * 500
}

func (h *MessageGeneratorTask) Task() gocron.Task {
	return gocron.NewTask(h.messageGeneratorTask)
}

func (h *MessageGeneratorTask) messageGeneratorTask() {

	queueLen, err := h.redis.QueueLength(h.context)

	if err != nil {
		h.log.Error("Error getting queue length", zap.Error(err))
		return
	}

	if queueLen < 1000 {
		newMessageCount := 1000 - queueLen
		h.log.Info("Adding ", zap.Int64("count", newMessageCount))
	}
}
