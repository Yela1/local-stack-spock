package com.study.localstackspock

import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import redis.clients.jedis.Jedis
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
class RedisIntegrationTest extends Specification{


    @Shared
    def redis = new GenericContainer<>("redis:5.0.3-alpine")
            .withExposedPorts(6379)

    @Shared
    Jedis jedis

    def setupSpec(){
        redis.start()
        jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))


    }

    def "test string cache"(){
        given:
            jedis.set("test#1", "string")

        when:
            def result = jedis.get("test#1")

        then:
            result == "string"
    }


    def "should delete key"(){
        expect:
            jedis.del("test#1") == 1
    }

    def "should create list"(){
        given:
            jedis.lpush("test#2", "value#1")
            jedis.lpush("test#2", "value#2")

        when:
            def result = jedis.rpop("test#2")

        then:
            result == "value#1"

    }

    def "should create set of strings"(){
        given:
            jedis.sadd("test#3", "value#1")
            jedis.sadd("test#3", "value#2")

        and: 'should be ignored by redis'
            jedis.sadd("test#3", "value#1")

        when:
            def result = jedis.smembers("test#3")

        then:
            result.size() == 2
            result.contains("value#1")
            result.contains("value#2")

    }

    def "test for redis hash"(){
        given:
            jedis.hset("test#4", "first", "value#1")
            jedis.hset("test#4", "second", "value#2")

        when:
            def result = jedis.hget("test#4", "first")

        then:
            result == "value#1"

    }

}
