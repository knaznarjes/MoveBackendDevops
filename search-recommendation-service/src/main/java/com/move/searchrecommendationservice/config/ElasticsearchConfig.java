package com.move.searchrecommendationservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PreDestroy;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.connection-timeout}")
    private String connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout}")
    private String socketTimeout;

    private RestClient restClient;

    @Bean
    public ElasticsearchClient elasticsearchClient() throws URISyntaxException {
        URI uri = new URI(elasticsearchUri);
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 9200;
        String scheme = uri.getScheme() != null ? uri.getScheme() : "http";

        restClient = RestClient.builder(new HttpHost(host, port, scheme))
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(parseTimeoutToMs(connectionTimeout))
                                .setSocketTimeout(parseTimeoutToMs(socketTimeout)))
                .build();

        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }

    private int parseTimeoutToMs(String timeout) {
        timeout = timeout.trim().toLowerCase();
        if (timeout.endsWith("ms")) {
            return Integer.parseInt(timeout.replace("ms", ""));
        } else if (timeout.endsWith("s")) {
            return Integer.parseInt(timeout.replace("s", "")) * 1000;
        }
        return Integer.parseInt(timeout);
    }

    @PreDestroy
    public void close() {
        if (restClient != null) {
            try {
                restClient.close();
            } catch (Exception ignored) {
            }
        }
    }
}
