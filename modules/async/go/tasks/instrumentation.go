package tasks

import (
	"benchmark_async/app"
	"benchmark_async/data"
	"context"
	"github.com/go-co-op/gocron/v2"
	"go.uber.org/zap"
	"time"
)

// NewInstrumentationTask creates a new InstrumentationTask, initializing RedisProvider, logger, and context settings.
func NewInstrumentationTask(redis *data.RedisProvider, log *zap.Logger) app.ScheduledTask {
	return &InstrumentationTask{
		redis:   redis,
		log:     log,
		context: context.Background(),
	}
}

// InstrumentationTask provides functionality to monitor Redis queue length and log its state at scheduled intervals.
type InstrumentationTask struct {
	redis   *data.RedisProvider
	log     *zap.Logger
	context context.Context
}

// Interval returns the default time duration for scheduling instrumentation tasks, which is set to 5 seconds.
func (*InstrumentationTask) Interval() time.Duration {
	return time.Second * 5
}

// Task creates and returns a new gocron.Task for executing the instrumentation task at scheduled intervals.
func (h *InstrumentationTask) Task() gocron.Task {
	return gocron.NewTask(h.instrumentationTask)
}

// instrumentationTask retrieves the current length of the Redis queue and logs it. Logs errors if the operation fails.
func (h *InstrumentationTask) instrumentationTask() {

	queueLen, err := h.redis.QueueLength(h.context)

	if err != nil {
		h.log.Error("Error getting queue length", zap.Error(err))
		return
	}

	h.log.Info("Current queue length", zap.Int64("length", queueLen))
}
