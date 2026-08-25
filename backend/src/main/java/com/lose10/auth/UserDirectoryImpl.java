package com.lose10.auth;

import com.lose10.auth.dto.UserSummary;
import com.lose10.platform.error.ApiException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class UserDirectoryImpl implements UserDirectory {

    private final UserRepository userRepository;

    UserDirectoryImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserSummary getSummary(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return new UserSummary(user.getId(), user.getEmail());
    }

    @Override
    public boolean isEmailVerified(UUID userId) {
        return userRepository.findById(userId).map(User::isEmailVerified).orElse(false);
    }
}
