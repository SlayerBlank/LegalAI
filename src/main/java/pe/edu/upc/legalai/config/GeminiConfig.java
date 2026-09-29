package pe.edu.upc.legalai.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class GeminiConfig {
    @Bean
    public WebClient geminiWebClient(@Value("${gemini.base-url}") String baseUrl,
                                     @Value("${gemini.connect-timeout}") Duration connectTimeout,
                                     @Value("${gemini.request-timeout}") Duration requestTimeout) {
        if (connectTimeout.toMillis() < 1 || requestTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("Los timeouts de Gemini deben ser positivos");
        }
        HttpClient client = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()))
                .responseTimeout(requestTimeout)
                .disableRetry(true);
        return WebClient.builder().baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(client)).build();
    }
}
