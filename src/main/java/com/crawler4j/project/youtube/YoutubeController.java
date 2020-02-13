package com.crawler4j.project.youtube;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class YoutubeController {

    private final static String rootFolder = "c:/inmobi-work/crawler4j-project/crawl";    //  定义爬虫数据存储路径
    private final static String storageFolder = rootFolder + "/data4"; //  定义数据本地存储文件路径
    private final static String[] crawlDomains = {"https://www.youtube.com/results?search_query=tiktok+girl&sp=CAMSBAgEEAE%253D"};  //  定义爬虫种子页面url链接
    private final static int numberOfCrawlers = 7;  //  定义爬虫线程数量
    private final static String USER_AGENT_NAME = "Mozilla/<version> (<system-information>) <platform> (<platform-details>) <extensions>"; //  定义爬虫机器人user-agent名称
    public static CrawlController controller = null;

    public static void main(String[] args) throws Exception {    //  执行main方法
        CrawlConfig config = new CrawlConfig(); //  实例化爬虫配置
        config.setCrawlStorageFolder(rootFolder);   //  设置爬虫文件存储路径
        config.setIncludeBinaryContentInCrawling(true); //  设置允许爬取二进制文件

        PageFetcher pageFetcher = new PageFetcher(config);  //  实例化页面获取器

        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();    //  实例化爬虫机器人配置 比如可以设置 user-agent
        robotstxtConfig.setUserAgentName(USER_AGENT_NAME);
        robotstxtConfig.setEnabled(false);

        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);    //  实例化爬虫机器人对目标服务器的配置，每个网站都有一个robots.txt文件 规定了该网站哪些页面可以爬，哪些页面禁止爬，该类是对robots.txt规范的实现

        controller = new CrawlController(config, pageFetcher, robotstxtServer); //  实例化爬虫控制器

        for (String domain : crawlDomains) {    //  配置爬虫种子页面，就是规定的从哪里开始爬，可以配置多个种子页面
            controller.addSeed(domain);
        }

        YoutubeCrawler.configure(crawlDomains, storageFolder);   //  配置爬虫域名，以及本地存储位置

        controller.start(YoutubeCrawler.class, numberOfCrawlers);   //  启动爬虫
    }
}
