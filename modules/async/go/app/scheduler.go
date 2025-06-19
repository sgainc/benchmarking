package app

import (
	"context"
	"github.com/go-co-op/gocron/v2"
	"go.uber.org/fx"
	"time"
)

// ScheduledTask defines an interface for scheduling tasks with specific intervals and associated gocron.Task implementations.
// Interval specifies the duration between task executions.
// Task returns the gocron.Task to be executed.
type ScheduledTask interface {
	Interval() time.Duration
	Task() gocron.Task
}

// NewScheduler creates and initializes a gocron.Scheduler with provided tasks and
// integrates it into the Fx application lifecycle.
func NewScheduler(tasks []ScheduledTask, lc fx.Lifecycle) *gocron.Scheduler {

	scheduler, err := gocron.NewScheduler()
	if err != nil {
		return nil
	}

	lc.Append(fx.Hook{
		OnStart: func(ctx context.Context) error {
			for _, task := range tasks {
				scheduler.NewJob(gocron.DurationJob(task.Interval()), task.Task())
			}
			scheduler.Start()
			return nil
		},
		OnStop: func(ctx context.Context) error {
			return scheduler.Shutdown()
		},
	})

	return &scheduler
}

// AsScheduledTask annotates a constructor to provide its output as a ScheduledTask to the "tasks" group.
func AsScheduledTask(f any) any {

	return fx.Annotate(f, fx.As(new(ScheduledTask)), fx.ResultTags(`group:"tasks"`))

}
