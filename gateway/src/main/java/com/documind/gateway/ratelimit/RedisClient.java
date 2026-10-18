package com.documind.gateway.ratelimit;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;

final class RedisClient implements AutoCloseable {

	private final io.lettuce.core.RedisClient client;
	private final StatefulRedisConnection<byte[], byte[]> connection;

	private RedisClient(io.lettuce.core.RedisClient client, StatefulRedisConnection<byte[], byte[]> connection) {
		this.client = client;
		this.connection = connection;
	}

	static RedisClient create(String redisUri) {
		io.lettuce.core.RedisClient client = io.lettuce.core.RedisClient.create(RedisURI.create(redisUri));
		StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE);
		return new RedisClient(client, connection);
	}

	StatefulRedisConnection<byte[], byte[]> connection() {
		return connection;
	}

	@Override
	public void close() {
		connection.close();
		client.shutdown();
	}
}
