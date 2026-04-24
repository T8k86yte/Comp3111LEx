package project.task3.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class DeepSeekResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    // getters 和 setters（必须）
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    // 内部类 Choice
    public static class Choice {
        private int index;
        private Message message;
        private String finish_reason;
        // getters/setters...
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public String getFinish_reason() { return finish_reason; }
        public void setFinish_reason(String finish_reason) { this.finish_reason = finish_reason; }
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