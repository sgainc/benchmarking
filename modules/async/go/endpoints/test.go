package endpoints

import (
	"go.uber.org/zap"
	"net/http"
)

// TestEndpoint is an http.Handler indicates if the system is up
type TestEndpoint struct{}

// Pattern returns the URL path for the TestEndpoint, which determines the route it serves.
func (*TestEndpoint) Pattern() string {
	return "/test/isup"
}

// TestEndpointFactory builds a new EchoHandler.
func TestEndpointFactory() *TestEndpoint {
	return &TestEndpoint{}
}

// ServeHTTP handles an HTTP request to the /echo endpoint.
func (*TestEndpoint) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("Hello World!"))
}

// AppStateEndpoint is an HTTP handler that returns some app state data.
type AppStateEndpoint struct {
	log *zap.Logger
}

// AppStateEndpointFactory builds a new AppStateEndpoint.
func AppStateEndpointFactory(log *zap.Logger) *AppStateEndpoint {
	return &AppStateEndpoint{log: log}
}

func (*AppStateEndpoint) Pattern() string {
	return "/test/appstate"
}

// ServeHTTP handles an HTTP request to the /echo endpoint.
func (*AppStateEndpoint) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("App State!"))
}
