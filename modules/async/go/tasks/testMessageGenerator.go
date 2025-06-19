package tasks

import (
	"benchmark_async/app"
	"benchmark_async/data"
	"benchmark_async/types"
	"context"
	"fmt"
	"github.com/go-co-op/gocron/v2"
	"github.com/google/uuid"
	"go.uber.org/zap"
	"math/rand"
	"time"
)

// NewMessageGeneratorTask creates and initializes a MessageGeneratorTask with provided RedisProvider, logger, and AppState.
func NewMessageGeneratorTask(redis *data.RedisProvider, log *zap.Logger, state *app.AppState) app.ScheduledTask {
	return &MessageGeneratorTask{
		redis:   redis,
		log:     log,
		state:   state,
		context: context.Background(),
	}
}

// MessageGeneratorTask represents a scheduled task for managing message generation and transmission to a Redis queue.
// It interacts with a Redis provider, application state, and a logger to generate, queue, and log messages accordingly.
// The task ensures that the queue maintains a minimum number of messages by generating additional messages as needed.
// This task executes at a defined interval and facilitates CRUD operations via generated message types.
type MessageGeneratorTask struct {
	redis   *data.RedisProvider
	log     *zap.Logger
	state   *app.AppState
	context context.Context
}

// Interval returns the execution interval for the MessageGeneratorTask as a time.Duration, set to 500 milliseconds.
func (*MessageGeneratorTask) Interval() time.Duration {
	return time.Millisecond * 500
}

// Task creates and returns a gocron.Task instance that wraps the message generation task logic for scheduling purposes.
func (h *MessageGeneratorTask) Task() gocron.Task {
	return gocron.NewTask(h.messageGeneratorTask)
}

// messageGeneratorTask ensures the Redis queue maintains at least 1000 messages by generating and enqueuing additional ones.
// It calculates the deficit, generates new messages using generateTestMessage, and pushes them to the queue using redis.SendMessage.
// Logs errors during queue operations and generates informational logs for message generation events.
func (h *MessageGeneratorTask) messageGeneratorTask() {

	ctx := context.Background()
	queueLen, err := h.redis.QueueLength(h.context)

	if err != nil {
		h.log.Error("Error getting queue length", zap.Error(err))
		return
	}

	if queueLen < 1000 {
		newMessageCount := 1000 - queueLen
		h.log.Info("Adding messages", zap.Int64("count", newMessageCount))

		for range newMessageCount {
			msg := h.generateTestMessage()

			err = h.redis.SendMessage(ctx, msg)
			if err != nil {
				h.log.Error("Error sending message", zap.Error(err))
			}
		}
	}
}

// generateTestMessage generates and returns a new message of type MessageWrapper based on the
// internal state and logic rules.
func (h *MessageGeneratorTask) generateTestMessage() *types.MessageWrapper {

	if h.state.ObjectList.Count() < 100 {

		dataName := uuid.New().String()

		return &types.MessageWrapper{
			MessageType: types.CREATE_MESSAGE,
			Timestamp:   time.Now().UnixMilli(),
			Message: &types.CreateDataMessage{
				DataName: dataName,
				DataSize: rand.Intn(9000) + 1000,
			},
		}
	}

	if h.state.ObjectList.Count() > 1000 {

		randomIndex := rand.Intn(h.state.ObjectList.Count())
		randomKey := h.state.ObjectList.Keys()[randomIndex]

		return &types.MessageWrapper{
			MessageType: types.DELETE_MESSAGE,
			Timestamp:   time.Now().UnixMilli(),
			Message: &types.DeleteDataMessage{
				DataName: randomKey,
			},
		}
	}

	randomNum := rand.Intn(100)
	switch {
	case randomNum <= 50:

		randomIndex := rand.Intn(h.state.ObjectList.Count())
		randomKey := h.state.ObjectList.Keys()[randomIndex]

		return &types.MessageWrapper{
			MessageType: types.READ_MESSAGE,
			Timestamp:   time.Now().UnixMilli(),
			Message: &types.ReadDataMessage{
				DataName: randomKey,
			},
		}

	case randomNum <= 75:

		randomIndex := rand.Intn(h.state.ObjectList.Count())
		randomKey := h.state.ObjectList.Keys()[randomIndex]
		randomOriginal := fmt.Sprintf("%02x", rand.Intn(256))
		randomReplace := fmt.Sprintf("%02x", rand.Intn(256))

		return &types.MessageWrapper{
			MessageType: types.UPDATE_MESSAGE,
			Timestamp:   time.Now().UnixMilli(),
			Message: &types.UpdateDataMessage{
				DataName: randomKey,
				Original: randomOriginal,
				Replace:  randomReplace,
			},
		}

	case randomNum <= 86:

		dataName := uuid.New().String()

		return &types.MessageWrapper{
			MessageType: types.CREATE_MESSAGE,
			Timestamp:   time.Now().UnixMilli(),
			Message: &types.CreateDataMessage{
				DataName: dataName,
				DataSize: rand.Intn(9000) + 1000,
			},
		}

	default:

		randomIndex := rand.Intn(h.state.ObjectList.Count())
		randomKey := h.state.ObjectList.Keys()[randomIndex]

		return &types.MessageWrapper{
			MessageType: types.DELETE_MESSAGE,
			Timestamp:   time.Now().UnixMilli(),
			Message: &types.DeleteDataMessage{
				DataName: randomKey,
			},
		}
	}
}
