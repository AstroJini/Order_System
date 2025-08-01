package com.beyond.ordersystem.common.config;

import com.beyond.ordersystem.common.service.SseAlarmService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;


//    Qualifier : 같은 Bean 객체가 여러개 있을 경우 Bean객체를 구분하기 위한 어노테이션이다
//    위와 같은 어노테이션이 필요한 이유는 configuration.setDatabase(0);의 연결된 db가 여러개 있을 수 있을 경우 구분자가 없으면 구분을 못하기 때문이다.
    @Bean
    @Qualifier("rtInventory")
    public RedisConnectionFactory redisConnectionFactory(){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(0);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("stockInventory")
    public RedisConnectionFactory stockConnectionFactory(){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(1);
        return new LettuceConnectionFactory(configuration);
    }
//    redis pub/sub을 위한 redis 연결객체 생성
    @Bean
    @Qualifier("ssePubSub")
    public RedisConnectionFactory sseFactory(){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
//        redis pub/sub기능은 db에 값을 저장하는 기능이 아니므로 특정db에 의존적이지 않음.
        return new LettuceConnectionFactory(configuration);
    }


    @Bean
//    RedisTemplate이 여러개 있을 경우에도 구분자가 필요하기 때문에 아래와 같은 @Qualifier("rtInventory")어노테이션이 필요하다.
    @Qualifier("rtInventory")
//    매개변수에 @Qualifier를 사용하여 싱글톤 객체(의존성)를 주입받을 수 있다
//    Bean들끼리 서로 의존성을 주입받을 때 메서드 파라미터로도 주입 가능
//    모든 Template 중에 redisTemplate라는 메서드명이 반드시 1개는 있어야함
    public RedisTemplate<String, String> redisTemplate(@Qualifier("rtInventory") RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
//        아래의 코드는 매개변수에 들어간 객체를 redisTemplate에 다음 싱글톤 객체를 연결하겠다는 의미이다.
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }


    @Bean
    @Qualifier("stockInventory")
    public RedisTemplate<String, String> stockTemplate(@Qualifier("stockInventory") RedisConnectionFactory stockConnectionFactory){
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(stockConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("ssePubSub")
    public RedisTemplate<String, String> sseRedisTemplate(@Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

//    redis 리스너 객체
    @Bean
    @Qualifier("ssePubSub")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory,
            MessageListenerAdapter messageListenerAdaptor){

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory());
        container.addMessageListener(messageListenerAdaptor, new PatternTopic("order-channel"));
//        만약에 여러 채널을 구독해야하는 경우 여러개의 PatternTopic을 add하거나 별도의 Bean객체 생성.
        return container;
    }

//    redis의 channel에서 수신된 메시지를 처리하는 빈객체
    @Bean
    public MessageListenerAdapter messageListenerAdapter(SseAlarmService sseAlarmService){
//        채널로부터 수신되는 message처리를 SseAlarmService의 OnMessage메서드로 설정
//        즉, 메시지가 수신되면 onMessage메서드가 호춣
        return new MessageListenerAdapter(sseAlarmService, "onMessage");
    }

}
