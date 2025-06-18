package endpoints

import (
	"net/http"
)

// TestEndpoint is an http.Handler indicates if the system is up
type TestEndpoint struct{}

// NewTestEndpoint builds a new EchoHandler.
func NewTestEndpoint() *TestEndpoint {
	return &TestEndpoint{}
}

// ServeHTTP handles an HTTP request to the /echo endpoint.
func (*TestEndpoint) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("Hello World!"))
}
