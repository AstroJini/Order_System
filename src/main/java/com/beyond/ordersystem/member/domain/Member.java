package com.beyond.ordersystem.member.domain;

import com.beyond.ordersystem.common.domain.BaseTimeEntity;
import com.beyond.ordersystem.member.dto.MemberResDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
// jpql을 제외하고 모든 조회 쿼리에 where del_yn = "N"을 붙이는 효과
// 만약 내가 탈퇴한 회원들을 조회하고 싶은 경우에는 jpql로 쿼리를 직접 작성하여야 한다.
@Where(clause = "del_yn='N'")
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length=50, unique = true, nullable = false)
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;
    @Builder.Default
    private String delYn = "N";

    public void deleteMember(String delYn){
        this.delYn = delYn;
    }
}
