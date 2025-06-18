package tasks

import (
	"github.com/go-co-op/gocron/v2"
	"log/slog"
	"time"
)

func InitiateTasks(logger *slog.Logger) {

	s, _ := gocron.NewScheduler()
	defer func() { _ = s.Shutdown() }()

	_, _ = s.NewJob(
		gocron.DurationJob(
			time.Second*5,
		),
		gocron.NewTask(
			func() {},
		),
	)
}
