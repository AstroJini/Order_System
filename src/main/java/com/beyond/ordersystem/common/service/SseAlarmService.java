package com.beyond.ordersystem.common.service;

import com.beyond.ordersystem.common.dto.SseMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SseAlarmService implements MessageListener {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final RedisTemplate<String, String> redisTemplate;

    public SseAlarmService(SseEmitterRegistry sseEmitterRegistry, @Qualifier("ssePubSub") RedisTemplate<String, String> redisTemplate) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
    }

    //    특정 사용자에게 message 발송
//    receiver와 sender을 제외한 나머지는 커스텀 해야하는 부분이다.
    public void publishMessage(String receiver, String sender, Long orderingId){
        SseMessageDto dto = SseMessageDto.builder()
                .sender(sender)
                .receiver(receiver)
                .orderingId(orderingId)
                .build();

//        메시지를 json으로 변형하기 위한 아래의 ObjectMapper와 data값 (data값 초기화를 위해 초기값 null로 설정)
        ObjectMapper objectMapper = new ObjectMapper();
        String data = null;
//        예외가 강제되기 때문에 try catch 로 묶음
        try {
            data = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


//        emitter객체를 통해 사용자 정보를 꺼내어 위치를 지정한 후 메시지 전송
        SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(receiver);
//        예외가 강제되기 때문에 try catch 로 묶음
//        emitter객체가 현재 서버에 있으면 직접 알림 발송, 그렇지 않으면 redis에 publish
        if (sseEmitter != null){
            try {
//            eventName은 메세지의 라벨링과 같은 개념임
                sseEmitter.send(SseEmitter.event().name("ordered").data(data));
//        admin유저가 disconnect 되어 있는 동안 쌓인 알림은 db에 저장되었다가 connect됐을 때 db조회해서 보여줌
//        사용자가 로그아웃(또는 새로고침) 후에 다시 화면에 돌아왔을 때 알림 메시지가 남아있으려면 DB에 추가적으로 저장 필요.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            redisTemplate.convertAndSend("order-channel", data);
        }



    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
//        Message : 실질적인 메시지가 담겨있는 객체
//        pattern : 채널명
        String channel_name = new String(pattern);
//        여러개의 채널을 구독하고 있을경우, 채널명으로 분기처리
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SseMessageDto dto = objectMapper.readValue(message.getBody(), SseMessageDto.class);
            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(dto.getReceiver());
            if(sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().name("ordered").data(dto));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
//    @Override
//    public void onMessage(Message message, byte[] pattern) {
////        Message : 실질적인 메시지가 담겨있는 객체
////        Pattern : 채널명
//        String channel_name = new String(pattern);
////        여러개의 채널을 구독하고 있을 경우, 채널명으로 분기처리
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//
//            if (sseEmitter != null){
//                try {
//                    sseEmitter.send(SseEmitter.event().name("ordered").data(dto));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            SseMessageDto dto = objectMapper.readValue(message.getBody(), SseMessageDto.class);
//            System.out.println(channel_name);
//
//
////        emitter객체를 통해 사용자 정보를 꺼내어 위치를 지정한 후 메시지 전송
////        예외가 강제되기 때문에 try catch 로 묶음
////        emitter객체가 현재 서버에 있으면 직접 알림 발송, 그렇지 않으면 redis에 publish
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
