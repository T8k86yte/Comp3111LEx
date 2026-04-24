package project.task3.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class DeepSeekResponse {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String object;
    @Getter
    @Setter
    private long created;
    @Getter
    @Setter
    private String model;
    @Getter
    @Setter
    private List<Choice> choices;
    @Getter
    @Setter
    private Usage usage;

    public static class Choice {
        @Getter
        @Setter
        private int index;
        @Getter
        @Setter
        private Message message;
        @Getter
        @Setter
        private String finish_reason;
    }

    public static class Message {
        @Getter
        @Setter
        private String role;
        @Getter
        @Setter
        private String content;
    }

    public static class Usage {
        @Getter
        @Setter
        private int prompt_tokens;
        @Getter
        @Setter
        private int completion_tokens;
        @Getter
        @Setter
        private int total_tokens;
    }
}