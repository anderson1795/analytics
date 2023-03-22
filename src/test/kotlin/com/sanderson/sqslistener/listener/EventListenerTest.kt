package com.sanderson.sqslistener.listener

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Spy
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Testcontainers
@SpringBootTest
//@SqsTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [EventListenerTest.IntegrationTestLocalstackConfiguration::class])
//@ComponentScan(
//    basePackages = ["com.sanderson.sqslistener.listener"]
//)
class EventListenerTest {
    @Spy
    lateinit var eventProcessor: EventProcessor

    companion object {
        private lateinit var sqsClient: SqsClient
        private lateinit var queueUrl: String
        private const val queueName = "my-queue"
        var localstackImage = DockerImageName.parse("localstack/localstack:1.4.0")

        @Container
        val localstack: LocalStackContainer = LocalStackContainer(localstackImage)
            .withServices(
                SQS
            );

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            sqsClient = buildSqsClient()
            queueUrl = setupAnalyticsQueue()

        }

        private fun buildSqsClient(): SqsClient {
            return SqsClient.builder().endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.region))
                .build()
        }

        private fun setupAnalyticsQueue(): String =
            sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl()!!

    }


    @Test
    fun itListensForMessages() {
        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody("yo").build())
        verify(eventProcessor, timeout(5000)).processMessage("yo")
    }

    @SpringBootConfiguration
    internal class IntegrationTestLocalstackConfiguration :
        ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.cloud.aws.sqs.endpoint=" + localstack.getEndpointOverride(LocalStackContainer.Service.SQS),
                "analytics.sqs.queue-url=" + queueName,
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
