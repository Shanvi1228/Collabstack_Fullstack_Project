package com.collabstack.editor.dto.websocket;

import java.util.List;

public record PresenceListMessage(
        String type,   // always "PRESENCE_LIST"
        List<PresenceUser> users
) {
    public record PresenceUser(String userId, String username) {}
}
