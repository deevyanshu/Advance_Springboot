package com.deevyanshu.orderservice.configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

@Configuration
public class WebClientConfig {

    @Value("${external.service.base-url:http://localhost:8082}")
    private String baseUrl;

    @Value("${external.service.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${external.service.response-timeout-ms:10000}")
    private int responseTimeoutMs;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        TcpClient tcpClient = TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .doOnConnected(connection ->
                connection.addHandlerLast(new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS))
                          .addHandlerLast(new WriteTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS))
            );

        HttpClient httpClient = HttpClient.from(tcpClient)
            .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return builder.clientConnector(connector)
                .baseUrl(baseUrl)
                .build();
    }
}


