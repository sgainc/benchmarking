package main

import (
	"benchmark_async/endpoints"
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
			NewHTTPServer,
			fx.Annotate(NewServeMux, fx.ParamTags(`group:"routes"`)),
			AsRoute(endpoints.AppStateEndpointFactory),
			AsRoute(endpoints.TestEndpointFactory),
			zap.NewProduction,
		),

		fx.Invoke(func(*http.Server) {}),
	).Run()
}
