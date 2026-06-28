package com.lumen.essentials.teleport;

import java.util.UUID;

/**
 * A pending teleport request. {@code here == false} is a normal /tpa (the requester
 * teleports to the target on accept); {@code here == true} is /tpahere (the target
 * teleports to the requester on accept).
 */
public final class TeleportRequest {

    private final UUID requester;
    private final String requesterName;
    private final boolean here;
    private final long expiresAt;

    public TeleportRequest(UUID requester, String requesterName, boolean here, long expiresAt) {
        this.requester = requester;
        this.requesterName = requesterName;
        this.here = here;
        this.expiresAt = expiresAt;
    }

    public UUID requester() {
        return requester;
    }

    public String requesterName() {
        return requesterName;
    }

    public boolean here() {
        return here;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
