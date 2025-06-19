package main

import (
	"benchmark_async/app"
	"benchmark_async/data"
	"benchmark_async/endpoints/test"
	"benchmark_async/tasks"
	"github.com/go-co-op/gocron/v2"
	"go.uber.org/fx"
	"go.uber.org/fx/fxevent"
	"go.uber.org/zap"
	"net/http"
)

func main() {

	fx.New(
		fx.WithLogger(func(log *zap.Logger) fxevent.Logger {
			return &fxevent.ZapLogger{Logger: log}
		}),

		fx.Provide(
			app.NewHTTPServer,
			data.NewRedisProvider,
			fx.Annotate(app.NewScheduler, fx.ParamTags(`group:"tasks"`)),
			fx.Annotate(app.NewServeMux, fx.ParamTags(`group:"routes"`)),
			app.AsRoute(testEndpoints.NewAppStateEndpoint),
			app.AsRoute(testEndpoints.NewIsUpEndpoint),
			app.AsScheduledTask(tasks.NewInstrumentationTask),
			zap.NewProduction,
		),

		fx.Invoke(func(*http.Server) {}),
		fx.Invoke(func(*gocron.Scheduler) {}),
	).Run()
}
