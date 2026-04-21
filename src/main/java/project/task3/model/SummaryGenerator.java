package project.task3.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import dev.langchain4j.model.openai.*;
import dev.langchain4j.model.chat.*;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;

public class SummaryGenerator {
    public String generate(String filePath) {
        String key = System.getenv("DEEPSEEK_APIKEY");

        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey("sk-8cce4cd2a2434f83a64c696aa2308ea7")
                .modelName("deepseek-reasoner")
                .build();

        Path pdfFilePath = Paths.get(filePath);
        Document pdfDocument = FileSystemDocumentLoader.loadDocument(pdfFilePath, new ApachePdfBoxDocumentParser());
        pdfDocument.text();


        return model.chat("Hello!");
    }

    public static void main(String[] s) {
        SummaryGenerator g = new SummaryGenerator();
        System.out.println(g.generate("C:\\Users\\jeff_\\Downloads\\四麻全书.pdf"));
    }
}
