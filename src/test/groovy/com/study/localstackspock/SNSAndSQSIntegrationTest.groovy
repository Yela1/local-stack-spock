package com.study.localstackspock

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.CreateTopicRequest
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.util.Topics
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import org.json.JSONObject
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared


class SNSAndSQSIntegrationTest extends IntegrationTestContainer{

    @Shared
    def localStackContainer = new LocalStackContainer(localstackImage).withServices(LocalStackContainer.Service.SNS, LocalStackContainer.Service.SQS)

    @Shared
    AmazonSNS sns

    @Shared
    AmazonSQS sqs

    @Shared
    def queueName ="queue"

    @Shared
    String topicArn

    @Shared
    String queueUrl

    def setupSpec(){
        localStackContainer.start()
        //get default credentials provider
        def credentialsProvider = localStackContainer.getDefaultCredentialsProvider()

        //configure sns
        sns =  AmazonSNSClientBuilder.standard()
                .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SNS))
                .withCredentials(credentialsProvider)
                .build()

        //configure sqs
        sqs = AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
                .withCredentials(credentialsProvider)
                .build()

        //create sns topic
        topicArn = sns.createTopic(new CreateTopicRequest("topicName")).getTopicArn()

        //create sqs queue
        queueUrl = sqs.createQueue(new CreateQueueRequest(queueName)).getQueueUrl()

        //subscribe to sns
        Topics.subscribeQueue(sns, sqs, topicArn, queueUrl)
    }


    def "verify that sqs contains message when we sent"(){
        given:
            def message = "someMessage"
            sqs.sendMessage(queueUrl, message)

        when:
            def messages = sqs.receiveMessage(queueUrl).getMessages()

        then:
            messages.size() == 1
            messages.get(0).getBody() == message
        and:
            sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messages[0].getReceiptHandle()))
    }


    def "SQS should poll messages from SNS"(){
        given:
            def expectedMessage = "AnotherMessage"
            sns.publish(new PublishRequest(topicArn as String, expectedMessage).withSubject("Subject"))
            Thread.sleep(2000)
        and:
            def messages = sqs.receiveMessage(queueUrl).getMessages()

        expect:
            messages.size() == 1
            def json = new JSONObject(messages[0].getBody())
            json.get("Message") == expectedMessage
    }


}
