package app

import (
	"github.com/orcaman/concurrent-map/v2"
)

type AppState struct {
	ObjectList cmap.ConcurrentMap[string, bool]
}

func NewAppState() *AppState {
	objectList := cmap.New[bool]()
	return &AppState{objectList}
}
