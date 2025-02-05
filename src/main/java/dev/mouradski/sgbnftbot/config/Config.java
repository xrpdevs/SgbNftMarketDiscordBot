package dev.mouradski.sgbnftbot.config;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

@Configuration
public class Config {

    @Value("${web3.songbird.provider}")
    private String wssSongbirdProviderUrl;

    @Value("${web3.flare.provider}")
    private String wssFlareProviderUrl;

    @Value("${discord.token}")
    private String discordToken;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();


        converter.setSupportedMediaTypes(
                Arrays.asList(new MediaType[]{MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM}));

        restTemplate.setMessageConverters(Arrays.asList(converter, new FormHttpMessageConverter()));
        return restTemplate;
    }


    @Bean("songbirdWeb3")
    public Web3j songbirdWeb3() throws ConnectException, URISyntaxException {
        WebSocketClient webSocketClient = new WebSocketClient(new URI(wssSongbirdProviderUrl));

        WebSocketService webSocketService = new WebSocketService(webSocketClient, false);

        webSocketService.connect();

        return Web3j.build(webSocketService);
    }


    @Bean("flareWeb3")
    public Web3j flaredWeb3() throws ConnectException, URISyntaxException {
        WebSocketClient webSocketClient = new WebSocketClient(new URI(wssFlareProviderUrl));

        WebSocketService webSocketService = new WebSocketService(webSocketClient, false);

        webSocketService.connect();

        return Web3j.build(webSocketService);
    }

    @Bean
    @ConditionalOnProperty(value = "app.production")
    public DiscordApi discordApi() {
        return new DiscordApiBuilder().setToken(discordToken).setAllNonPrivilegedIntents()
                .login().join();
    }
}
