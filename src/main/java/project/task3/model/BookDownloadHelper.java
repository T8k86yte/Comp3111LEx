package project.task3.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.*;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BookDownloadHelper {
    private static final String DOWNLOAD_DIR = System.getenv("user.dir") + "/data/bookFiles";

    public static void crawl(String bookTitle, ProgressBar bar, Label prompt) {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        Platform.runLater(() -> prompt.setText("Preparing for download..."));

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", DOWNLOAD_DIR);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_setting_values.automatic_downloads", 1);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Random random = new Random();

        try {
            String mirrorUrl = "https://1lib.sk/";
            driver.get(mirrorUrl);
            Thread.sleep(3000 + random.nextInt(3000));

            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("q")));

            WebElement searchBox = driver.findElement(By.name("q"));
            searchBox.clear();
            searchBox.sendKeys(bookTitle);
            Thread.sleep(500 + random.nextInt(1000));
            searchBox.submit();
            Thread.sleep(5000 + random.nextInt(3000));

            WebElement resultContainer = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchResultBox")));
            List<WebElement> allBookCards = resultContainer.findElements(By.cssSelector("z-bookcard"));
            WebElement targetBookCard = null;
            for (WebElement card : allBookCards) {
                String ext = card.getAttribute("extension");
                if (ext != null && ext.trim().equalsIgnoreCase("pdf")) {
                    targetBookCard = card;
                    break;
                }
            }
            if (targetBookCard == null) {
                throw new RuntimeException("No books with PDF files found.");
            }

            String bookRelativeUrl = targetBookCard.getAttribute("href");
            if (bookRelativeUrl == null) {
                throw new NoSuchElementException("no href attribute");
            }
            String fullBookUrl = mirrorUrl.replaceAll("/$", "") + bookRelativeUrl;
            driver.get(fullBookUrl);
            Thread.sleep(3000 + random.nextInt(2000));

            WebElement pdfDownloadLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[@class='btn btn-default addDownloadedBook']//span[@class='book-property__extension' and text()='pdf']/ancestor::a")
            ));
            WebElement pdfLinkElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[contains(@class, 'addDownloadedBook') and .//span[@class='book-property__extension' and text()='pdf']]")
            ));
            String DownloadUrl = pdfLinkElement.getAttribute("href");
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", pdfDownloadLink);
            Thread.sleep(1000);

            Set<Cookie> cookies = driver.manage().getCookies();
            StringBuilder cookieHeader = new StringBuilder();
            for (Cookie ck : cookies) {
                cookieHeader.append(ck.getName()).append("=").append(ck.getValue()).append("; ");
            }
            pdfDownloadLink.click();

            Platform.runLater(() -> prompt.setText("Download started."));
            DownloadMonitor monitor = new DownloadMonitor(DownloadUrl, new File(DOWNLOAD_DIR), bar, cookieHeader.toString());
            monitor.call();
            Platform.runLater(() -> prompt.setText("Download complete, the file is at " + DOWNLOAD_DIR));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
