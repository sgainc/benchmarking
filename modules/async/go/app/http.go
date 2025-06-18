package main

import (
	"benchmark_async/endpoints"
	"context"
	"fmt"
	"go.uber.org/fx"
	"net"
	"net/http"
)

// NewHTTPServer builds an HTTP server that will begin serving requests
// when the Fx application starts.
func NewHTTPServer(lc fx.Lifecycle, mux *http.ServeMux) *http.Server {

	srv := &http.Server{Addr: ":8080", Handler: mux}
	lc.Append(fx.Hook{
		OnStart: func(ctx context.Context) error {
			ln, err := net.Listen("tcp", srv.Addr)
			if err != nil {
				return err
			}
			fmt.Println("Starting HTTP server at", srv.Addr)
			go srv.Serve(ln)
			return nil
		},
		OnStop: func(ctx context.Context) error {
			return srv.Shutdown(ctx)
		},
	})
	return srv
}

// NewServeMux builds a ServeMux that will route requests
// to the given EchoHandler.
func NewServeMux(echo *endpoints.TestEndpoint) *http.ServeMux {
	mux := http.NewServeMux()
	mux.Handle("/test/isup", echo)
	return mux
}
