package data

import (
	"benchmark_async/types"
	"context"
	"encoding/json"
	"github.com/redis/go-redis/v9"
	"go.uber.org/zap"
)

type RedisProvider struct {
	redisClient *redis.Client
	log         *zap.Logger
}

// SendMessage pushes a message wrapped in MessageWrapper into the Redis list "benchmarkQueue".
func (p *RedisProvider) SendMessage(ctx context.Context, message *types.MessageWrapper) error {

	jsonData, err := json.Marshal(message)
	if err != nil {
		p.log.Error("Error marshalling message", zap.Error(err))
		return err
	}

	return p.redisClient.LPush(ctx, "benchmarkQueue-go", jsonData).Err()

}

// ReceiveMessage retrieves and unmarshals the next message from the Redis "benchmarkQueue" using blocking pop operation.
func (p *RedisProvider) ReceiveMessage(ctx context.Context) (types.MessageWrapper, error) {

	result, err := p.redisClient.BRPop(ctx, 0, "benchmarkQueue-go").Result()
	if err != nil {
		p.log.Error("Error receiving message", zap.Error(err))
		return types.MessageWrapper{}, err
	}

	var message types.MessageWrapper
	if err := json.Unmarshal([]byte(result[1]), &message); err != nil {
		return types.MessageWrapper{}, err
	}

	return message, nil
}

// QueueLength retrieves the length of the Redis list "benchmarkQueue" and returns it as an integer.
func (p *RedisProvider) QueueLength(ctx context.Context) (int64, error) {
	return p.redisClient.LLen(ctx, "benchmarkQueue-go").Result()
}

// NewRedisProvider creates and initializes a RedisProvider with a Redis client.
func NewRedisProvider(log *zap.Logger) *RedisProvider {
	//TODO: Update to use environment variables
	redisClient := redis.NewClient(&redis.Options{
		Addr:     "localhost:6379", // Redis server address
		Password: "",               // No password by default for local Redis
		DB:       0,                // Default DB
	})

	return &RedisProvider{
		redisClient: redisClient,
		log:         log,
	}
}
