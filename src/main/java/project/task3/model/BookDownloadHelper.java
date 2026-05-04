package project.task3.model;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class BookDownloadHelper {
    private static final String ZLIB_URL = "https://z-lib.fm";
    private static final String DOWNLOAD_DIR = "data/bookFiles/";

    public static String getDownloadURL(String title) {
        try {
            String searched = URLEncoder.encode(title, StandardCharsets.UTF_8);
            searched = searched.replaceAll("\\+", "%20");
            Document document = Jsoup.connect("https://zh.z-lib.fm/s/" + searched).get();
            Elements elems = document.body().getElementsByClass("book-item resItemBoxBooks ");
            String downloadURL = "";

            for (Element elem : elems) {
                Element card = elem.getElementsByTag("z-bookcard").first();
                if (card == null) continue;

                Attributes attrs = card.attributes();
                Attribute extension = attrs.attribute("extension");
                if (extension == null || !extension.getValue().equals("pdf")) continue;
                Attribute download = attrs.attribute("download");
                if (download == null) continue;

                downloadURL = "https://zh.z-lib.fm" + download.getValue();

                Element cover = elem.getElementsByClass("image cover").first();
                if (cover == null) continue;
                Attribute coverPath = cover.attribute("src");
                break;
            }

            return downloadURL;
        } catch (Exception e) {
            return "";
        }
    }
    /*

    public static void download(String title) {
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            String fileUrl = getDownloadURL(title);
            Path destPath = Paths.get("data/bookFiles/" + title + ".pdf");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<Path> response = client.send(request,
                    HttpResponse.BodyHandlers.ofFile(destPath));

            if (response.statusCode() == 200) {
                System.out.println("下载成功：" + destPath);
            } else {
                System.err.println("下载失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    */

    public static boolean download(String title) {
        //Launch ChromeDriver
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...");
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            driver.get(ZLIB_URL);
            Thread.sleep(5000); //Cloudflare

            //Search
            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='text']")));
            searchBox.sendKeys(title);
            searchBox.submit();

            //Click search result
            List<WebElement> searchResults = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("div[itemtype='http://schema.org/Book'] h3 a")));

            if (searchResults.isEmpty()) return false;
            searchResults.getFirst().click();

            //Click download button, get temporary link
            WebElement downloadBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[contains(text(), 'Download')]/..")));
            String dynamicDownloadUrl = downloadBtn.getAttribute("href");

            //Download
            if (dynamicDownloadUrl != null && !dynamicDownloadUrl.isEmpty()) {
                String fileName = title.replaceAll("\\s+", "_") + ".pdf";
                downloadFileWithHttpClient(dynamicDownloadUrl, DOWNLOAD_DIR + fileName);
            } else return false;
        } catch (Exception e) {
            return false;
        } finally {
            driver.quit();
        }
        return true;
    }

    private static void downloadFileWithHttpClient(String fileURL, String savePath) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(fileURL));
             InputStream inputStream = response.getEntity().getContent();
             FileOutputStream outputStream = new FileOutputStream(savePath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void main(String[] args) {
        BookDownloadHelper.download("Jane Eyre");
    }
}
