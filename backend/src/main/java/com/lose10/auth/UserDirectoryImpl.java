package com.lose10.auth;

import com.lose10.auth.dto.AdminUserListItem;
import com.lose10.auth.dto.UserDetail;
import com.lose10.auth.dto.UserSummary;
import com.lose10.platform.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<AdminUserListItem> search(String query, Pageable pageable) {
        return userRepository.search(query, pageable)
                .map(u -> new AdminUserListItem(u.getId(), u.getEmail(), u.getPhoneE164(), u.getStatus().name(), u.getCreatedAt()));
    }

    @Override
    public UserDetail getDetail(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return new UserDetail(user.getId(), user.getEmail(), user.getPhoneE164(), user.getStatus().name(), user.isEmailVerified(), user.getCreatedAt());
    }
}
