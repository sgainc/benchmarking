package app

import (
	"github.com/orcaman/concurrent-map/v2"
	"sync/atomic"
	"time"
)

type AppState struct {
	ObjectList          cmap.ConcurrentMap[string, bool]
	eventCount          int64
	eventCountTimeStart int64
}

func NewAppState() *AppState {

	objectList := cmap.New[bool]()
	return &AppState{objectList, int64(0), time.Now().UnixMilli()}
}

func (state *AppState) IncrementEventCount() {

	atomic.AddInt64(&state.eventCount, 1)
}

func (state *AppState) GetRateAndReset() float64 {

	timeNow := time.Now().UnixMilli()
	count := atomic.SwapInt64(&state.eventCount, 0)
	timeThen := atomic.SwapInt64(&state.eventCountTimeStart, timeNow)

	return float64(count) / float64(timeThen)
}
