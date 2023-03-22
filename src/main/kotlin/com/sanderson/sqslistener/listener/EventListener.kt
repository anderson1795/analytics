package com.sanderson.sqslistener.listener

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EventListener(@Autowired val eventProcessor: EventProcessor) {
    @SqsListener(
        value = ["\${analytics.sqs.queue-url}"]
//        Ignore deletion policy for now
//        deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS
    )
    fun listen(message: String) {
        eventProcessor.processMessage(message)
    }
}