package com.beyond.ordersystem.member.controller;

import com.beyond.ordersystem.common.auth.JwtTokenProvider;
import com.beyond.ordersystem.common.dto.CommonDto;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.*;
import com.beyond.ordersystem.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<?> save(@RequestBody @Valid MemberCreateDto memberCreateDto){
        Long id = memberService.save(memberCreateDto);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원가입 완료")
                        .build()
                , HttpStatus.CREATED);
    }
    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody LoginReqDto dto){
        Member member = memberService.doLogin(dto);
//        at 토큰 생성
        String accessToken = jwtTokenProvider.createAtToken(member);
//        rt 토큰 생성
        String refreshToken =jwtTokenProvider.createRtToken(member);
        LoginResDto loginResDto = LoginResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();


        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(loginResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("로그인 성공")
                        .build()
                ,HttpStatus.OK);
    }


//    rt를 통한 at 갱신 요청
    @PostMapping("/refresh-at")
    public ResponseEntity<?> generateNewAt(@RequestBody RefreshTokenDto refreshTokenDto){
//        rt검증 로직
//        토큰 그 자체를 검증해야함 at검증과 같음 + DB에 있는 rt값과 비교
//        member를 at 생성시에 보내줘야함. 그래서
        Member member = jwtTokenProvider.validateRt(refreshTokenDto.getRefreshToken());
//        at신규 생성 로직
        String accessToken = jwtTokenProvider.createAtToken(member);
        LoginResDto loginResDto = LoginResDto.builder()
                .accessToken(accessToken)
                .build();


        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(loginResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("at 재발급 성공")
                        .build()
                ,HttpStatus.OK);    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll(){
        List<MemberResDto> memberResDtoList = memberService.findAll();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(memberResDtoList)
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원목록 조회 완료")
                        .build(),HttpStatus.OK);
    }
    @GetMapping("/myinfo")
    public ResponseEntity<?> myinfo(){
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(memberService.myInfo())
                        .status_code(HttpStatus.OK.value())
                        .status_message("내 정보 조회 완료")
                        .build(),HttpStatus.OK);
    }
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(){
        memberService.delete();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result("OK")
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원탈퇴 완료")
                        .build(), HttpStatus.OK);
    }
}

