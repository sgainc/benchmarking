package testEndpoints

import (
	"go.uber.org/zap"
	"net/http"
)

// IsUpEndpoint is an http.Handler indicates if the system is up
type IsUpEndpoint struct {
	log *zap.Logger
}

// Pattern returns the URL path for the TestEndpoint, which determines the route it serves.
func (*IsUpEndpoint) Pattern() string {
	return "/test/isup"
}

// NewIsUpEndpoint builds a new EchoHandler.
func NewIsUpEndpoint(log *zap.Logger) *IsUpEndpoint {
	return &IsUpEndpoint{log: log}
}

// ServeHTTP handles an HTTP request to the /echo endpoint.
func (endpoint *IsUpEndpoint) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("Hello World!"))
	endpoint.log.Info("Hello World!")
}
