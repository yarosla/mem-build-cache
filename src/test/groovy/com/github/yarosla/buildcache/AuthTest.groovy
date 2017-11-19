package com.github.yarosla.buildcache

import groovy.util.logging.Slf4j
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.Base64Utils
import org.springframework.util.SocketUtils
import spock.lang.Specification
import spock.lang.Stepwise

import java.nio.charset.StandardCharsets

@Stepwise
@Slf4j
class AuthTest extends Specification {

    static int port
    static mainThread = new Thread('serverMain')
    static WebTestClient client
    static String authHeader

    def setupSpec() {
        port = SocketUtils.findAvailableTcpPort()
        mainThread.start {
            Main.main('-v', '-P', Integer.toString(port), '-u', 'usr', '-p', 'pswd')
        }
        client = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:$port/").build()
        authHeader = 'Basic ' + Base64Utils.encodeToString('usr:pswd'.getBytes(StandardCharsets.UTF_8))
    }

    def "stats unauthorized"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.get().uri("/stats").exchange()
        then:
        responseSpec.expectStatus().isUnauthorized()
    }

    def "original stats are zero"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.get().uri('/stats')
                .header('Authorization', authHeader)
                .exchange()
        then:
        responseSpec
                .expectStatus().isOk()
                .expectBody().json('{"currentCount":0,"currentWeight":0,"evictionCount":0,"evictionWeight":0,"hitCount":0,"missCount":0,"requestCount":0,"hottest":[]}')
    }

    def "get non-existent key unauthorized"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.get().uri("/cache/key1").exchange()
        then:
        responseSpec.expectStatus().isUnauthorized()
    }

    def "get non-existent key"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.get().uri("/cache/key1")
                .header('Authorization', authHeader)
                .exchange()
        then:
        responseSpec.expectStatus().isNotFound()
    }

    def "put key2 unauthorized"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.put().uri("/cache/key2")
                .contentType(MediaType.TEXT_PLAIN)
                .syncBody('Hello!')
                .exchange()
        then:
        responseSpec.expectStatus().isUnauthorized()
    }

    def "put key2"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.put().uri("/cache/key2")
                .header('Authorization', authHeader)
                .contentType(MediaType.TEXT_PLAIN)
                .syncBody('Hello!')
                .exchange()
        then:
        responseSpec.expectStatus().isOk()
    }

    def "get key2 unauthorized"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.get().uri("/cache/key2")
                .exchange()
        then:
        responseSpec
                .expectStatus().isUnauthorized()
    }

    def "get key2"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.get().uri("/cache/key2")
                .header('Authorization', authHeader)
                .exchange()
        then:
        responseSpec
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo('Hello!')
    }

    def "final stats"() {
        when:
        WebTestClient.ResponseSpec responseSpec = client.get().uri("/stats")
                .header('Authorization', authHeader)
                .exchange()
        then:
        responseSpec
                .expectStatus().isOk()
                .expectBody().json('{"currentCount":1,"currentWeight":6,"evictionCount":0,"evictionWeight":0,"hitCount":1,"missCount":1,"requestCount":2,"hottest":["/key2"]}')
    }

    void cleanupSpec() {
        log.info 'shutting down server'
        mainThread.interrupt()
    }
}
