package com.crawler4j.project.youtube;

import com.github.kiulian.downloader.OnYoutubeDownloadListener;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.quality.AudioQuality;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeCrawler extends WebCrawler {
    //  youtube Id  [视频唯一值]
    //  video Url   [视频链接]
    //  time    [视频时常]
    //  views   [视频浏览量]
    //  title   [视频标题]
    //  cover   [视频封面图链接]
    //  updateAt    [视频上传日期]
    //  createAr    [视频抓取日期]

    // 记录下载数
    private static int count = 0;

    // 记录最大下载数
    private static int maxCount = 40;

    // 设置爬取文件的本地存储路径
    private static File storageFolder;

    // 设置爬取文件的域名
    private static String[] crawlDomains;

    // 配置存储路径和过滤的域名地址
    public static void configure(String[] domain, String storageFolderName) {
        crawlDomains = domain;

        storageFolder = new File(storageFolderName); // 实例化
        if (!storageFolder.exists()) { // 假如文件不存在
            storageFolder.mkdirs(); // 我们创建一个
        }
    }

    // 过滤指定条件执行到visit()方法中
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();   // 得到小写的url

        for (String crawlDomain : crawlDomains) {   //  过滤指定的域名地址
            if (href.startsWith(crawlDomain)) {
                return true;
            }
        }
        return false;
    }

    // 过滤指定条件后的具体操作流程
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL(); //  当前url链接

        if (page.getParseData() instanceof HtmlParseData) {   //  判断当前是否是真正的网页
            System.out.println("createAr ：" + new Date().toString());
            Launcher launcher = new Launcher();
            //第一个参数是本地谷歌浏览器的可执行地址
            try (SessionFactory factory = launcher.launch();
                 Session session = factory.create()) {
                //这个参数是你想要爬取的网址
                session.navigate(url);
                //等待加载完毕
                session.waitDocumentReady();
                session.wait(1000);
                //获得爬取的数据
                String content = (String) session.getContent();
                //使用Jsoup转换成可以解析的Document
                Document document = Jsoup.parse(content);
                Elements div = document.getElementsByClass("yt-simple-endpoint style-scope ytd-video-renderer");
                for (Element element : div) {
                    String href = "https://www.youtube.com" + element.attr("href").toString();
                    getVideoId2(href);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            getVideoId2(url);
        }
    }


    private void getVideoId2(String url)  {
            //  获取上传日期
        Launcher launcher = new Launcher();
        //第一个参数是本地谷歌浏览器的可执行地址
        try (SessionFactory factory = launcher.launch();
             Session session = factory.create()) {
            //这个参数是你想要爬取的网址
            session.navigate(url);
            //等待加载完毕
            session.waitDocumentReady();
            session.wait(1000);
            //获得爬取的数据
            String content = (String) session.getContent();
            //使用Jsoup转换成可以解析的Document
            Document doc = Jsoup.parse(content);
            Element scriptTag = doc.getElementById("scriptTag");
            if (scriptTag != null && !scriptTag.equals("")) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(scriptTag.html(), JsonObject.class);
                String uploadDate = jsonObject.get("uploadDate").getAsString();
                System.out.println("updateAt : " + uploadDate);
            }else{
                System.out.println("url:"+url);
            }

            try {
                downloadVideo(getVideoId(url));    //  下载视频 并获取信息
            } catch (IOException e) {
                e.printStackTrace();
            } catch (YoutubeException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //  获取视频的Id
    private static String getVideoId(String url) {
        String pattern = "https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*";

        Pattern compiledPattern = Pattern.compile(pattern,
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    //  下载视频并获取信息
    public static void downloadVideo(String videoId) throws IOException, YoutubeException {
        if (count > maxCount) {
            YoutubeController.controller.shutdown();
        } else {
            // 视频+1 记录下载数量
            count++;
            // init downloader
            YoutubeDownloader downloader = new YoutubeDownloader();

            YoutubeVideo video = downloader.getVideo(videoId);

            // video details
            VideoDetails details = video.details();
            System.out.println("youtube Id ：" + videoId);
            System.out.println("time ：" + details.lengthSeconds());
            System.out.println("views ：" + details.viewCount());
            System.out.println("title ：" + details.title());


            details.thumbnails().forEach(image -> System.out.println("cover: " + image));
            // filtering formats
            List<AudioFormat> videoFormats = video.findAudioWithQuality(AudioQuality.low);
            videoFormats.forEach(it -> {
                System.out.println(it.audioQuality() + " : " + it.url());
            });

            Format formatByItag = video.findFormatByItag(136);
            if (formatByItag != null) {
                System.out.println(formatByItag.url());
            }
            System.out.println("video Url ：" + videoFormats.get(0));
            video.downloadAsync(videoFormats.get(0), storageFolder, new OnYoutubeDownloadListener() {
                @Override
                public void onDownloading(int progress) {
                    System.out.printf("Downloaded %d%%\n", progress);
                }

                @Override
                public void onFinished(File file) {
                    System.out.println("Finished file: " + file);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("Error: " + throwable.getLocalizedMessage());
                }
            });
        }
    }
}
