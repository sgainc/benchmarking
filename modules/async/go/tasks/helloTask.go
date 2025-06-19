package tasks

import (
	"benchmark_async/app"
	"github.com/go-co-op/gocron/v2"
	"time"
)

func NewHelloTask() app.ScheduledTask {
	return &HelloTask{}
}

type HelloTask struct{}

func (*HelloTask) Interval() time.Duration {
	return time.Second
}

func (h *HelloTask) Task() gocron.Task {
	return gocron.NewTask(testTask)
}

func testTask() {
	println("test")
}
