package main

import (
	"log/slog"
	"os"
)

func InitLogger() *slog.Logger {

	opts := slog.HandlerOptions{
		Level:     slog.LevelInfo,
		AddSource: true,
	}

	jsonHandler := slog.NewJSONHandler(os.Stderr, &opts)
	logger := slog.New(jsonHandler)

	return logger
}
