package kwonjh0406.joondrive.drive.service;

import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.auth.repository.UserRepository;
import kwonjh0406.joondrive.common.exception.CustomException;
import kwonjh0406.joondrive.common.exception.ErrorCode;
import kwonjh0406.joondrive.drive.dto.DriveInfoResponse;
import kwonjh0406.joondrive.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriveService {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    public DriveInfoResponse getDriveInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long usedStorage = getUsedStorage(userId);

        return new DriveInfoResponse(
                user.getEmail(),
                usedStorage,
                user.getStorageLimit());
    }

    public long getUsedStorage(Long userId) {
        return fileRepository.calculateUsedStorage(userId);
    }

    public long getStorageLimit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return getStorageLimit(user);
    }

    private long getStorageLimit(User user) {
        return (long) user.getStorageLimit() * 1024L * 1024L * 1024L;
    }
}
