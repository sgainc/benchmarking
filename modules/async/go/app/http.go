package main

import (
	"context"
	"fmt"
	"go.uber.org/fx"
	"net"
	"net/http"
)

// HttpRoute is an http.Handler that knows the mux pattern
// under which it will be registered.
type HttpRoute interface {
	http.Handler

	// Pattern reports the path at which this is registered.
	Pattern() string
}

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
func NewServeMux(routes []HttpRoute) *http.ServeMux {

	mux := http.NewServeMux()

	// Iterate our list and add each
	for _, route := range routes {
		mux.Handle(route.Pattern(), route)
	}

	return mux

}

// AsRoute annotates the given constructor to state that
// it provides a route to the "routes" group.
func AsRoute(f any) any {

	return fx.Annotate(f, fx.As(new(HttpRoute)), fx.ResultTags(`group:"routes"`))

}
