package com.lose10.auth.security;

import com.lose10.auth.RefreshToken;
import com.lose10.auth.RefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Split out from {@link RefreshTokenService} specifically so family
 * revocation runs in its own committed transaction ({@code REQUIRES_NEW}),
 * invoked through a real Spring proxy rather than a same-class
 * self-invocation. Without this, calling {@code revokeFamily} from inside
 * {@code rotate()} and then throwing {@code TOKEN_REUSE_DETECTED} from that
 * same {@code @Transactional} method would roll the revocation back along
 * with everything else — Spring rolls back the whole transaction on any
 * unchecked exception, silently undoing the exact security action the
 * reuse-detection path exists to perform. Caught via live end-to-end
 * testing, not by inspection.
 */
@Component
class RefreshTokenFamilyRevoker {

    private final RefreshTokenRepository repository;

    RefreshTokenFamilyRevoker(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void revokeFamily(UUID familyId) {
        List<RefreshToken> tokens = repository.findByFamilyId(familyId);
        for (RefreshToken t : tokens) {
            if (!t.isRevoked()) {
                t.revoke(null);
            }
        }
    }
}
