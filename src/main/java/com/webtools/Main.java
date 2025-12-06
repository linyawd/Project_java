package com.webtools;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String url = "https://wikipedia.org/wiki/Java"; 

        try {
            System.out.println("Починаємо скрапінг: " + url);

            PageScraper scraper = new PageScraper();
            scraper.scrape(url);

            System.out.println("\nЗнайдено унікальних посилань: " + scraper.getLinks().size());
            System.out.println("Знайдено унікальних зображень: " + scraper.getImages().size());

            System.out.println("\nПерші 10 посилань:");
            scraper.getLinks().stream().limit(10).forEach(System.out::println);

            FileFetcher fetcher = new FileFetcher();
            fetcher.downloadAll(
                    scraper.getImages(),
                    Paths.get("downloads/images")
            );

            fetcher.downloadAll(
                scraper.getLinks(),
                Paths.get("downloads/files")
            );

            
            fetcher.shutdown(); 

            System.out.println("\nУсе готово!");
            BandwidthMonitor.getInstance().showReport(); 

        } catch (Exception e) {
            System.err.println("Сталася помилка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
