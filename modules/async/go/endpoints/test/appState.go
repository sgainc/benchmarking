package testEndpoints

import (
	"go.uber.org/zap"
	"net/http"
)

// AppStateEndpoint is an HTTP handler that returns some app state data.
type AppStateEndpoint struct {
	log *zap.Logger
}

// NewAppStateEndpoint builds a new AppStateEndpoint.
func NewAppStateEndpoint(log *zap.Logger) *AppStateEndpoint {
	return &AppStateEndpoint{log: log}
}

func (*AppStateEndpoint) Pattern() string {
	return "/test/appstate"
}

// ServeHTTP handles an HTTP request to the /echo endpoint.
func (endpoint *AppStateEndpoint) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("App State!"))
	endpoint.log.Info("App State!")
}
