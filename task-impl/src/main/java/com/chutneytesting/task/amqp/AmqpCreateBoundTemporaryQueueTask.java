package com.chutneytesting.task.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.chutneytesting.task.spi.FinallyAction;
import com.chutneytesting.task.spi.Task;
import com.chutneytesting.task.spi.TaskExecutionResult;
import com.chutneytesting.task.spi.injectable.FinallyActionRegistry;
import com.chutneytesting.task.spi.injectable.Input;
import com.chutneytesting.task.spi.injectable.Logger;
import com.chutneytesting.task.spi.injectable.Target;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class AmqpCreateBoundTemporaryQueueTask implements Task {

    private final ConnectionFactoryFactory connectionFactoryFactory = new ConnectionFactoryFactory();

    private final Target target;
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;
    private final Logger logger;
    private final FinallyActionRegistry finallyActionRegistry;
    private final ConnectionFactory connectionFactory;

    public AmqpCreateBoundTemporaryQueueTask(Target target,
                                             @Input("exchange-name") String exchangeName,
                                             @Input("routing-key") String routingKey,
                                             @Input("queue-name") String queueName,
                                             Logger logger,
                                             FinallyActionRegistry finallyActionRegistry) {
        this.target = target;
        this.connectionFactory = connectionFactoryFactory.create(target);
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.queueName = queueName;
        this.logger = logger;
        this.finallyActionRegistry = finallyActionRegistry;
    }

    @Override
    public TaskExecutionResult execute() {
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            createQueue(queueName, channel);
            bindQueue(channel, queueName);
            return TaskExecutionResult.ok(Collections.singletonMap("queueName", queueName));
        } catch (TimeoutException | IOException e) {
            logger.error("Unable to establish connection to RabbitMQ: " + e.getMessage());
            return TaskExecutionResult.ko();
        }
    }

    private void bindQueue(Channel channel, String queueName) throws IOException {
        String routingKey = Optional.ofNullable(this.routingKey).orElse(queueName);
        channel.queueBind(queueName, exchangeName, routingKey);
        logger.info("Created AMQP binding " + exchangeName + " (with " + this.routingKey + ") -> " + queueName);

        finallyActionRegistry.registerFinallyAction(FinallyAction.Builder
            .forAction("amqp-unbind-queue")
            .withTarget(target)
            .withInput("queue-name", queueName)
            .withInput("exchange-name", exchangeName)
            .withInput("routing-key", routingKey)
            .build());
        logger.info("Registered unbinding finally action");
    }

    private void createQueue(String queueName, Channel channel) throws IOException {
        channel.queueDeclare(queueName, true, false, false, null);
        logger.info("Created AMQP Queue with name: " + queueName);
        finallyActionRegistry.registerFinallyAction(FinallyAction.Builder
            .forAction("amqp-delete-queue")
            .withTarget(target)
            .withInput("queue-name", queueName)
            .build());
    }
}
