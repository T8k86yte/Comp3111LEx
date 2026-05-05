package project.task3.model;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import org.openqa.selenium.Cookie;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DownloadMonitor extends Task<Void> {
    private final String downloadUrl;
    private final File downloadDir;
    private final ProgressBar progressBar;
    private final String cookieHeader;

    private long totalBytes = -1;

    public DownloadMonitor(String downloadUrl, File downloadDir, ProgressBar progressBar, String cookieHeader) {
        this.downloadUrl = downloadUrl;
        this.downloadDir = downloadDir;
        this.progressBar = progressBar;
        this.cookieHeader = cookieHeader;
    }

    @Override
    protected Void call() throws Exception {
        totalBytes = getContentLength(downloadUrl);
        if (totalBytes <= 0) {
            Platform.runLater(() -> progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS));
        }

        File crFile = null;
        while (crFile == null && !isCancelled()) {
            crFile = findLatestCrdownloadFile();
            if (crFile == null) {
                TimeUnit.MILLISECONDS.sleep(500);
            }
        }

        if (crFile == null) return null;

        long lastSize = -1;
        while (!isCancelled()) {
            if (!crFile.exists()) {
                Platform.runLater(() -> progressBar.setProgress(1.0));
                break;
            }
            else {
                long currentSize = crFile.length();
                if (currentSize != lastSize) {
                    lastSize = currentSize;
                    if (totalBytes > 0) {
                        double progress = (double) currentSize / totalBytes;
                        Platform.runLater(() -> progressBar.setProgress(progress));
                    } else {
                        Platform.runLater(() -> progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS));
                    }
                }
            }
            TimeUnit.MILLISECONDS.sleep(300);
        }
        return null;
    }

    private long getContentLength(String urlString) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestProperty("Cookie", cookieHeader);
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            return conn.getContentLengthLong();
        } catch (Exception e) {
            return -1;
        }
    }

    private File findLatestCrdownloadFile() {
        File[] files = downloadDir.listFiles((dir, name) -> name.endsWith(".crdownload"));
        if (files == null || files.length == 0) return null;
        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) latest = f;
        }
        return latest;
    }
}