package com.webtools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PageScraper {
    private final Set<String> links = ConcurrentHashMap.newKeySet();
    private final Set<String> images = ConcurrentHashMap.newKeySet();
    private final TreeMap<String, String> indexedLinks = new TreeMap<>();
    private final TreeMap<String, String> indexedImages = new TreeMap<>();

    public void scrape(String url) throws IOException {
        System.out.println("Скрапимо: " + url);
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();

        
        Elements linkElements = doc.select("a[href]");
        for (Element link : linkElements) {
            String absUrl = link.absUrl("href");
            if (!absUrl.isEmpty() && !absUrl.startsWith("javascript:")) {
                if (links.add(absUrl)) {
                    indexedLinks.put(normalizeForIndex(absUrl), absUrl);
                }
            }
        }

        
        Elements imgElements = doc.select("img[src]");
        for (Element img : imgElements) {
            String absUrl = img.absUrl("src");
            if (!absUrl.isEmpty() && images.add(absUrl)) {
                indexedImages.put(normalizeForIndex(absUrl), absUrl);
            }
        }

        System.out.println("Знайдено унікальних посилань: " + links.size());
        System.out.println("Знайдено унікальних зображень: " + images.size());
    }

    private String normalizeForIndex(String url) {
        return url.toLowerCase().replaceAll("^https?://(www\\.)?", "");
    }

    public Set<String> getLinks() { return Collections.unmodifiableSet(links); }
    public Set<String> getImages() { return Collections.unmodifiableSet(images); }
    public NavigableMap<String, String> getIndexedLinks() { return Collections.unmodifiableNavigableMap(indexedLinks); }
    public NavigableMap<String, String> getIndexedImages() { return Collections.unmodifiableNavigableMap(indexedImages); }
}