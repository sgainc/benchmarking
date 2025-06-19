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

func NewMessageGeneratorTask(redis *data.RedisProvider, log *zap.Logger, state *app.AppState) app.ScheduledTask {
	return &MessageGeneratorTask{
		redis:   redis,
		log:     log,
		state:   state,
		context: context.Background(),
	}
}

type MessageGeneratorTask struct {
	redis   *data.RedisProvider
	log     *zap.Logger
	state   *app.AppState
	context context.Context
}

func (*MessageGeneratorTask) Interval() time.Duration {
	return time.Millisecond * 500
}

func (h *MessageGeneratorTask) Task() gocron.Task {
	return gocron.NewTask(h.messageGeneratorTask)
}

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
