package com.beyond.ordersystem.common.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

//    현재 이러한 설계는 메모리 기반 설계임
    //    SseEmitter는 연결된 사용자 정보(ip, macaddress 정보 등...)를 의미한다.
//    ConcurrentHashMap은 Thread-Safe한 map(동시성 이슈 발생 X)
    private Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();


    //    connect시 emitter를 add하는 로직
    public void addSseEmitter(String email, SseEmitter sseEmitter){
        emitterMap.put(email, sseEmitter);
    }

    //    disconnect시 emiiter를 remove하는 로직
    public void removeEmitter(String email){
        emitterMap.remove(email);
    }

    public SseEmitter getEmitter(String email){
        return emitterMap.get(email);
    }
}
