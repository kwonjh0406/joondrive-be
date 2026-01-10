package kwonjh0406.joondrive.auth.repository;

import kwonjh0406.joondrive.auth.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, String> {
}
