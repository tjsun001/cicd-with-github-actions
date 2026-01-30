package com.thurman.kafka;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.kafka.startup.producer",
        name = "enabled",
        havingValue = "true"
)
public class KafkaOneShotStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KafkaOneShotStartupRunner.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationContext ctx;
    private final Environment env;

    public KafkaOneShotStartupRunner(
            KafkaTemplate<String, String> kafkaTemplate,
            ApplicationContext ctx,
            Environment env
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.ctx = ctx;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String topic = env.getProperty("APP_TOPIC");
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("APP_TOPIC is required (env var).");
        }

        boolean exitAfterSend = env.getProperty("app.exit-after-send", Boolean.class, false);

        String key = UUID.randomUUID().toString();
        String payload = """
        {"type":"startup-smoke","ts":"%s","msg":"hello from startup producer"}
        """.formatted(Instant.now().toString()).trim();

        log.info("Startup producer enabled. Sending 1 message to topic='{}' key='{}'", topic, key);

        // Wait for broker ack so logs clearly show success/failure
        kafkaTemplate.send(topic, key, payload).get();

        log.info("Message sent successfully.");

        if (exitAfterSend) {
            log.info("Exit-after-send is true. Shutting down cleanly with exit code 0.");
            SpringApplication.exit(ctx, () -> 0);
        } else {
            log.info("Exit-after-send is false. App will keep running.");
        }
    }
}

