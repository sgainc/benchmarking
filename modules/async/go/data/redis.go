package data

import (
	"benchmark_async/types"
	"context"
	"encoding/json"
	"github.com/redis/go-redis/v9"
)

type RedisProvider struct {
	redisClient *redis.Client
}

// SendMessage pushes a message wrapped in MessageWrapper into the Redis list "benchmarkQueue".
func (p *RedisProvider) SendMessage(ctx context.Context, message types.MessageWrapper[types.BaseMessage]) error {
	return p.redisClient.LPush(ctx, "benchmarkQueue", message).Err()
}

// ReceiveMessage retrieves and unmarshals the next message from the Redis "benchmarkQueue" using blocking pop operation.
func (p *RedisProvider) ReceiveMessage(ctx context.Context) (types.MessageWrapper[types.BaseMessage], error) {

	result, err := p.redisClient.BRPop(ctx, 0, "benchmarkQueue").Result()
	if err != nil {
		return types.MessageWrapper[types.BaseMessage]{}, err
	}

	var message types.MessageWrapper[types.BaseMessage]
	if err := json.Unmarshal([]byte(result[1]), &message); err != nil {
		return types.MessageWrapper[types.BaseMessage]{}, err
	}

	return message, nil
}

// QueueLength retrieves the length of the Redis list "benchmarkQueue" and returns it as an integer.
func (p *RedisProvider) QueueLength(ctx context.Context) (int64, error) {
	return p.redisClient.LLen(ctx, "benchmarkQueue").Result()
}

// NewRedisProvider creates and initializes a RedisProvider with a Redis client.
func NewRedisProvider() *RedisProvider {
	//TODO: Update to use environment variables
	redisClient := redis.NewClient(&redis.Options{
		Addr:     "localhost:6379", // Redis server address
		Password: "",               // No password by default for local Redis
		DB:       0,                // Default DB
	})

	return &RedisProvider{
		redisClient: redisClient,
	}
}
