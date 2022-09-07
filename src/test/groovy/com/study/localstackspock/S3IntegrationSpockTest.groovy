package com.study.localstackspock

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.GetObjectRequest
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared

class S3IntegrationSpockTest extends IntegrationTestContainer{

    @Shared
    def localStackContainer = new LocalStackContainer(localstackImage).withServices(LocalStackContainer.Service.S3)

    @Shared
    AmazonS3 amazonS3

    @Shared
    def bucketName = "bucket"

    @Shared
    def anotherBucketName = "bucket2"


    def setupSpec(){
        localStackContainer.start()

        amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.S3))
                .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                .build()

    }

    def "should create new bucket"(){
        given:
            amazonS3.createBucket(bucketName)

        when:
            def result = amazonS3.listBuckets()

        then:
            result.size() == 1
            result[0].getName() == bucketName
    }

    def "should upload file"(){
        given:
            def resource = getClass().getClassLoader().getResource("test.json")
            amazonS3.putObject(bucketName, "test", new File(resource.toURI()))

        when:
            def result = amazonS3.getObject(bucketName, "test")

        then:
            result != null
            result.getKey() == "test"

    }

    def "should download file"(){
        given:
            def resource = getClass().getClassLoader().getResource("testFile.json")

        when:
            def result = amazonS3.getObject(new GetObjectRequest(bucketName,"test"), new File(resource.toURI()))

        then:
            result != null


    }


    def "should copy object from one bucket to another"(){
        given:
            amazonS3.createBucket(anotherBucketName)
        and:
            amazonS3.copyObject(bucketName, "test", anotherBucketName, "test2")

        when:
            def result = amazonS3.getObject(anotherBucketName, "test2")

        then:
            result != null
            result.getKey() == "test2"

    }

    def "should delete object from bucket"(){
        given:
            amazonS3.deleteObject(anotherBucketName, "test2")

        when:
            amazonS3.getObject(anotherBucketName, "test2")

        then:
            thrown(AmazonS3Exception)
    }

    def "should delete s3 bucket"(){
        given:
            amazonS3.deleteObject(bucketName, "test")
            amazonS3.deleteBucket(bucketName)
            amazonS3.deleteBucket(anotherBucketName)

        when:
            def result = amazonS3.listBuckets()

        then:
            result.size() == 0
    }

}
