package com.example.EDIP.document.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;  // ✅ ده الصح مش java.net.http.HttpClient

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${ai.api.url}")
    private String aiApiUrl;

    @Bean
    public WebClient webClient() throws Exception {
        SslContext sslContext = SslContextBuilder.forClient()
                .build();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
                .responseTimeout(Duration.ofSeconds(120))
                .secure(spec -> spec
                        .sslContext(sslContext)
                        .handshakeTimeout(Duration.ofSeconds(60))
                )
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(120))
                                .addHandlerLast(new WriteTimeoutHandler(120))
                );

        return WebClient.builder()
                .baseUrl(aiApiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}