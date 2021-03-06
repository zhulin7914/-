package org.zhulin.mq1.common.rabbitMQ.receiver;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.zhulin.mq1.common.rabbitMQ.config.RabbitConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * 比如一个生产者，多个消费者，可以写多个消费者，并且他们的分发是负载均衡的。
 */
@Slf4j
@Component
@RabbitListener(queues = RabbitConfig.QUEUE_A)//监听的队列A
public class MsgReceiverOne {
 
    @RabbitHandler
    public void process(String content) {
    	log.info("消费者one：接收处理队列A当中的消息： " + content);
    }
 
}
