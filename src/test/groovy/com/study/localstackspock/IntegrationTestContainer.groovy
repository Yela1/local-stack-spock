package com.study.localstackspock

import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
class IntegrationTestContainer extends Specification{

    @Shared
    def dockerImageName = "localstack/localstack:0.14.2"
    @Shared
    def localstackImage = DockerImageName.parse(dockerImageName)


}
