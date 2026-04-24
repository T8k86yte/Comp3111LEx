package project.task3.model;

import lombok.Data;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class DeepSeekRequest {
    public DeepSeekRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    @Getter
    @Setter
    public String model;
    @Getter
    @Setter
    public List<Message> messages;

    @Data
    public static class Message {
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        public String role;
        public String content;
    }
}
