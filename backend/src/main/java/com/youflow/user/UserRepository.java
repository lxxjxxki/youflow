package com.youflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    /** findByEmail — AuthService에서 로그인할 때 이메일로 유저 찾기*/
    Optional<User> findByEmail(String email);

    /** 회원가입 시 이미 존재하는 이메일인지 확인 */
    boolean existsByEmail(String email);
}
