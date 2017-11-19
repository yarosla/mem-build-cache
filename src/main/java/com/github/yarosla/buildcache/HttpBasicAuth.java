package com.github.yarosla.buildcache;

import org.springframework.http.HttpStatus;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

class HttpBasicAuth {

    private String expectedAuthorization;

    HttpBasicAuth(String username, String password) {
        expectedAuthorization = "Basic " + Base64Utils.encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        List<String> authorization = request.headers().header("Authorization");
        if (authorization.contains(expectedAuthorization)) {
            return next.handle(request);
        } else {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"Build Cache\"")
                    .build();
        }
    }

}
