package com.study.localstackspock

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared

class DynamoDBIntegrationTest extends IntegrationTestContainer{

    @Shared
    LocalStackContainer localStackContainer = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)

    @Shared
    AmazonDynamoDB amazonDynamoDB

    @Shared
    private DynamoDB dynamoDB

    @Shared
    def tableName = "Test"


    def setupSpec(){

        createConnection()
        createDynamoDBTableAndPopulateWithData()
    }


    def "table with table name #tableName should exist"(){
        expect:
            dynamoDB.getTable(tableName).describe().getTableName() == "Test"

    }

    def "table should contain data"(){
        when:
            def item = dynamoDB.getTable(tableName).getItem("year", 1985, "title", "Superman")

        then:
            item != null
            item.get("year") == 1985
            item.get("title") == "Superman"
    }


    def createConnection(){
        amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.DYNAMODB))
                    .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                    .build()

        dynamoDB = new DynamoDB(amazonDynamoDB)

    }


    def createDynamoDBTableAndPopulateWithData(){
        def hashKey = new KeySchemaElement("year", KeyType.HASH)
        def rangeKey = new KeySchemaElement("title", KeyType.RANGE)
        def yearAttribute = new AttributeDefinition("year", ScalarAttributeType.N)
        def titleAttribute = new AttributeDefinition("title", ScalarAttributeType.S)
        def provisioning = new ProvisionedThroughput(10L, 10L)
        Table table = dynamoDB.createTable(tableName, [hashKey, rangeKey], [yearAttribute, titleAttribute], provisioning)
        table.waitForActive()


        def attributeValues = new HashMap<String, AttributeValue>()
        attributeValues.put("year", new AttributeValue().withN("1985"))
        attributeValues.put("title", new AttributeValue().withS("Superman"))

        def putItemRequest = new PutItemRequest()
                .withTableName(tableName)
                .withItem(attributeValues)
        amazonDynamoDB.putItem(putItemRequest)

    }
}
