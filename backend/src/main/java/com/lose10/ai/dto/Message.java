package com.lose10.ai.dto;

import java.util.List;

public record Message(Role role, List<ContentBlock> content) {

    public enum Role {
        USER, ASSISTANT
    }

    public static Message user(String text) {
        return new Message(Role.USER, List.of(new ContentBlock.TextBlock(text)));
    }

    public static Message assistant(List<ContentBlock> content) {
        return new Message(Role.ASSISTANT, content);
    }

    public static Message toolResults(List<ContentBlock.ToolResultBlock> results) {
        return new Message(Role.USER, List.copyOf(results));
    }
}
