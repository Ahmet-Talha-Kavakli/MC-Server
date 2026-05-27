package com.reinacraft.core.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class RedisManager {

    private final Plugin plugin;
    private final ConfigurationSection cfg;
    private RedisClient client;
    private StatefulRedisConnection<String, String> conn;
    private StatefulRedisPubSubConnection<String, String> pubSubConn;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<BiConsumer<String, String>>> subscribers = new ConcurrentHashMap<>();

    public RedisManager(Plugin plugin, ConfigurationSection cfg) {
        this.plugin = plugin;
        this.cfg = Objects.requireNonNull(cfg, "redis section missing in config.yml");
    }

    public void start() {
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(cfg.getString("host", "127.0.0.1"))
                .withPort(cfg.getInt("port", 6379))
                .withDatabase(cfg.getInt("database", 0))
                .withTimeout(Duration.ofSeconds(5));
        String password = cfg.getString("password", "");
        if (password != null && !password.isEmpty()) {
            uri.withPassword(password.toCharArray());
        }

        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            this.client = RedisClient.create(uri.build());
            this.conn = client.connect();
            this.pubSubConn = client.connectPubSub();

            pubSubConn.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    CopyOnWriteArrayList<BiConsumer<String, String>> handlers = subscribers.get(channel);
                    if (handlers == null) return;
                    for (BiConsumer<String, String> handler : handlers) {
                        try {
                            handler.accept(channel, message);
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Redis subscriber threw on channel '" + channel + "': " + t.getMessage());
                        }
                    }
                }
            });

            String pong = conn.sync().ping();
            plugin.getLogger().info("Redis connected: ping=" + pong);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    public void stop() {
        if (pubSubConn != null) pubSubConn.close();
        if (conn != null) conn.close();
        if (client != null) client.shutdown();
        subscribers.clear();
    }

    public RedisCommands<String, String> sync() {
        return conn.sync();
    }

    public void publish(String channel, String message) {
        conn.sync().publish(channel, message);
    }

    public void subscribe(String channel, BiConsumer<String, String> handler) {
        boolean firstSubscriberOnChannel = subscribers.compute(channel, (k, list) -> {
            if (list == null) list = new CopyOnWriteArrayList<>();
            list.add(handler);
            return list;
        }).size() == 1;
        if (firstSubscriberOnChannel) {
            RedisPubSubCommands<String, String> ps = pubSubConn.sync();
            ps.subscribe(channel);
        }
    }
}
