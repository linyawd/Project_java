package com.webtools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileFetcher {
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public void downloadAll(Set<String> urls, Path downloadDir) throws IOException, InterruptedException {
        Files.createDirectories(downloadDir);

        
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (String url : urls) {
            if (isDownloadable(url)) {
                executor.submit(() -> downloadFile(url, downloadDir));
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Завантаження завершено в папку: " + downloadDir.toAbsolutePath());
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
    }

    private boolean isDownloadable(String url) {
        String lower = url.toLowerCase();
        return lower.matches(".*\\.(jpe?g|png|gif|webp|mp4|webm|pdf|zip|rar|mp3|wav|ogg)$") ||
                lower.contains("/download/") || lower.contains("file=");
    }

    private String getDirectImageUrl(String wikiUrl) throws IOException, InterruptedException {
        
        if (wikiUrl.contains("upload.wikimedia.org")) {
            return wikiUrl;
        }

        
        if (wikiUrl.contains("/wiki/File:") || wikiUrl.contains("/wiki/Image:")) {
            Document doc = Jsoup.connect(wikiUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            Element link = doc.selectFirst("a.internal[href*='.jpg'], a.internal[href*='.png'], a.internal[href*='.svg'], a.internal[href*='.gif'], a.internal[href*='.webp']");
            if (link != null) {
                return "https:" + link.attr("href"); 
            }
        }

        
        return null;
    }

    private void downloadFile(String urlStr, Path dir) {
        try {
            
            String directUrl = getDirectImageUrl(urlStr);
            if (directUrl == null) {
                System.err.println("Не вдалося отримати пряме посилання: " + urlStr);
                return;
            }

           
            String fileName = directUrl.substring(directUrl.lastIndexOf('/') + 1);
            if (fileName.isEmpty() || fileName.contains("?")) {
                
                fileName = urlStr.substring(urlStr.lastIndexOf('/') + 1);
                fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_"); 
                if (!fileName.contains(".")) fileName += guessExtension(directUrl);
            }

            Path target = dir.resolve(fileName).normalize();

            if (Files.exists(target)) {
                System.out.println("Вже є: " + fileName);
                return;
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(directUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            long size = response.headers().firstValue("content-length")
                    .map(Long::parseLong).orElse(0L);

            Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Завантажено: " + fileName + " (" + formatBytes(size) + ")");
            BandwidthMonitor.getInstance().addDownloaded(size);

        } catch (Exception e) {
            System.err.println("Помилка завантаження " + urlStr + ": " + e.getMessage());
        }
    }

    private String guessExtension(String url) {
        if (url.contains(".jpg") || url.contains(".jpeg")) return ".jpg";
        if (url.contains(".png")) return ".png";
        if (url.contains(".gif")) return ".gif";
        if (url.contains(".mp4")) return ".mp4";
        return ".bin";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024*1024) return String.format("%.1f KB", bytes/1024.0);
        if (bytes < 1024*1024*1024) return String.format("%.1f MB", bytes/(1024.0*1024));
        return String.format("%.1f GB", bytes/(1024.0*1024*1024));
    }
}