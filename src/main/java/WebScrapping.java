import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;

import java.awt.*;
import java.util.*;

public class WebScrapping extends JPanel {

    public static final int BUTTON_SIZE = 100;

    public WebScrapping(int x, int y, int windowWidth, int windowHeight) {
        this.setDoubleBuffered(true);
        this.setBounds(x,y, windowWidth, windowHeight);
        this.setLayout(null);


        JTextField userIgnoreList = new JTextField();
        userIgnoreList.setText("add ignore words with , between them (no spaces)");
        userIgnoreList.setBounds(100,10,300,30);
        userIgnoreList.setVisible(true);
        userIgnoreList.setBackground(Color.orange);
        this.add(userIgnoreList);

        JLabel ignore = new JLabel();
        ignore.setVisible(true);
        ignore.setText("ignore: ");
        ignore.setBounds(0,10,100,30);
        this.add(ignore);


        JButton start = new JButton("start");
        start.setVisible(true);
        start.setBounds(windowWidth/2-BUTTON_SIZE+40, windowHeight/2-BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        this.add(start);

        Map<String, Integer> merged = new HashMap<>();
        LinkedList<JLabel> labels = new LinkedList<>();

        start.addActionListener((event) -> {
            start.setVisible(false);
            userIgnoreList.setVisible(false);
            ignore.setVisible(false);
            String[] ignoreList = userIgnoreList.getText().split(",");

            new Thread(() -> {
                while (true) {
                    scrape(merged, ignoreList, labels);
                    this.removeAll();
                    while (!labels.isEmpty()) {
                        this.add(labels.getFirst());
                        repaint();
                        labels.removeFirst();
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                }
            }).start();

        });
    }

    public void scrape(Map<String, Integer> merged, String[] ignoreList, LinkedList<JLabel> labels) {

        Map<String, Integer> makoMap = new HashMap<>();
        Map<String, Integer> ynetMap = new HashMap<>();
        Map<String, Integer> sport5Map = new HashMap<>();
        Map<String, Integer> temp = new HashMap<>();
        Map<String, Integer> trends = new HashMap<>();
        Map<String, Integer> mergeStepOne = new HashMap<>();

        LinkedList<Thread> news1Threads = new LinkedList<>();
        LinkedList<Thread> news2Threads = new LinkedList<>();
        LinkedList<Thread> sportThreads = new LinkedList<>();


        if (!merged.isEmpty()) {
            copyMap(merged,temp);
        }

        merged.clear();
        mergeStepOne.clear();
        labels.clear();

        try {
            String makoUrl = "https://www.mako.co.il/";
            Document mako = Jsoup.connect(makoUrl).get();
            Elements main = mako.getElementsByClass("mako_main_portlet_group_container_td side_bar_width");
            Elements articles = main.get(0).getElementsByClass("element");

            ArrayList<String> articlesLinks = new ArrayList<>();
            for (Element article : articles) {
                String link = article.child(0).child(0).attr("href");
                if (!link.substring(0, 5).equals("https")) {
                    link = makoUrl + link;
                    articlesLinks.add(link);

                }
            }

            for (String link : articlesLinks) {

                Thread news1 = new Thread(() -> {
                    try {
                        Document article = Jsoup.connect(link).get();
                        Elements articleBody = article.getElementsByClass("article-body");

                        if (articleBody.size() != 0) {
                            Elements sentences = articleBody.get(0).getElementsByTag("p");
                            String[] words = sentences.text().split(" ");
                            putInMap(words,makoMap,ignoreList);
                        }

                    } catch (Exception e) {

                    }
                });
                news1Threads.addFirst(news1);
                news1.start();
                try {
                    Thread.sleep(50);
                } catch (Exception exception) {

                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            Document ynet = Jsoup.connect("https://www.ynet.co.il/home/0,7340,L-8,00.html").get();
            Elements slotListClass = ynet.getElementsByClass("slotList");
            for (Element slotList : slotListClass) {
                Elements lookForLinks = slotList.getElementsByTag("a");
                for (Element lookForLink : lookForLinks) {
                    Thread news2 = new Thread(() -> {
                        String link = lookForLink.attr("href");
                        try {
                            Document article = Jsoup.connect(link).get();
                            Elements content = article.getElementsByAttributeValue("data-text", "true");
                            String[] words = content.text().split(" ");
                            putInMap(words, ynetMap,ignoreList);

                        } catch (Exception e) {

                        }
                    });
                    news2Threads.addFirst(news2);
                    news2.start();
                    try {
                        Thread.sleep(50);
                    } catch (Exception exception) {

                    }


                }
            }
        } catch (Exception e) {

        }


        try {
            Document sport5 = Jsoup.connect("https://www.sport5.co.il/").get();
            Elements articlesHeadLines = sport5.getElementsByClass("abstract");
            for (Element articleHead : articlesHeadLines) {
                String link = articleHead.child(0).attr("href");

                Thread sport = new Thread(() -> {
                    try {
                        Document article = Jsoup.connect(link).get();
                        Element content = article.getElementById("content");
                        Elements articleText = content.getElementsByTag("p");
                        String[] words = articleText.text().split(" ");

                        putInMap(words, sport5Map,ignoreList);
                    } catch (Exception e) {

                    }
                });
                sportThreads.addFirst(sport);
                sport.start();
                try {
                    Thread.sleep(50);
                } catch (Exception exception) {

                }


            }
        } catch (Exception e) {

        }


        wait(news1Threads);
        wait(news2Threads);
        wait(sportThreads);

        mergeMaps(makoMap,sport5Map,mergeStepOne);
        mergeMaps(sport5Map,mergeStepOne,merged);

        Map<String, Integer> top10Mako = get10Big(makoMap);
        Map<String, Integer> top10Ynet = get10Big(ynetMap);
        Map<String, Integer> top10Sport5 = get10Big(sport5Map);
        Map<String, Integer> top10OfAll = get10Big(merged);

        if (!temp.isEmpty()) {
            int count = 0;
            for (String key : merged.keySet()) {
                if (temp.get(key)!=null) {
                    count = merged.get(key) - temp.get(key);
                } else {
                    count = merged.get(key);
                }
                if (count>0) {
                    trends.put(key, count);
                }
                System.out.println("good "+ key+" "+ count);
            }
            if (!trends.isEmpty()) {
                Map<String, Integer> top10Trends = get10Big(trends);
                printToGUI(top10Trends, 400, labels, "top trends:");
            }
//            printToConsole(top10Trends, "trends:");
        }


        printToGUI(top10OfAll, 0, labels, "top 10:");
        printToGUI(top10Mako, 100, labels, "top 10 (mako):");
        printToGUI(top10Ynet, 200, labels, "top 10 (ynet):");
        printToGUI(top10Sport5, 300, labels, "top 10 (sport5)");
        
//        printToConsole(top10OfAll,"top 10:");
//        printToConsole(top10Mako, "top 10 (mako)");
//        printToConsole(top10Sport5, "top 10 (sport 5)");
//        printToConsole(top10Ynet, "top 10 (ynet)");


        System.out.println("done");


        

    }
//    private boolean haKeys(Map<String, Integer> map) {
//        boolean hasKeys = false;
//        for (String key: map.keySet()) {
//            if (!key.equals("")) {
//                if (map.get(key)!= null) {
//                    hasKeys = true;
//                }
//            }
//        }
//        return hasKeys;
//    }

    public void printToConsole(Map<String, Integer> map, String headLine) {
        System.out.println(headLine);
        while (!map.isEmpty()) {
            String key = maxKeyMap(map);
            System.out.println(key + ": " + map.get(key) + " times");
            map.remove(key);
        }
        System.out.println();

    }

    public static final int LABELS_HEIGHT = 30;
    public static final int LABELS_WIDTH = 100;

    public void printToGUI(Map<String, Integer> map, int x, LinkedList<JLabel> labels, String headLine) {
        JLabel headLineLabel = new JLabel(headLine);
        headLineLabel.setBounds(x, 0, LABELS_WIDTH, LABELS_HEIGHT);
        headLineLabel.setVisible(true);
        labels.addFirst(headLineLabel);
        System.out.println(headLine);

        int height = LABELS_HEIGHT;
        while (!map.isEmpty()) {
            String key = maxKeyMap(map);
            if(map.get(key)!=null) {
                JLabel label = new JLabel(key + ": " + map.get(key) + " times");
                System.out.println(label.getText());
                label.setVisible(true);
                label.setBounds(x, height, LABELS_WIDTH, LABELS_HEIGHT);
                this.add(label);
                labels.addFirst(label);
                height += 30;
                map.remove(key);
            }
        }
        System.out.println();
    }

    public Map<String, Integer> get10Big(Map<String, Integer> map) {
        Map<String, Integer> get10 = new HashMap<>();
        for (int i=0; i<10; i++) {
            String key = maxKeyMap(map);
            get10.put(key,map.get(key));
            map.remove(key);
        }
        for (String key: get10.keySet()) {
            map.put(key, get10.get(key));
        }
        return get10;
    }
    public String maxKeyMap(Map<String, Integer> map) {
        int max = 0;
        String maxKey = "";
        for (String key: map.keySet()) {
            int current = map.get(key);
            if (current>max) {
                max = current;
                maxKey = key;
            }
        }
        return maxKey;
    }

    public void wait(LinkedList<Thread> threadLinkedList) {
        while (!threadLinkedList.isEmpty()) {
            try {
                threadLinkedList.getFirst().join();
            } catch (Exception e) {

            }
            threadLinkedList.removeFirst();
        }
    }

    public void putInMap(String[] words, Map<String,Integer> map, String[] ignoreList) {
        for (String word: words) {
            if (!exist(word,ignoreList)) {
                Integer count = map.get(word);
                if (count == null) {
                    count = 0;
                }
                count++;
                map.put(word, count);
            }
        }
    }
    public boolean exist(String word, String [] words) {
        boolean exist = false;
        for (String word1: words) {
            if (word1.equals(word)) {
                exist = true;
                break;
            }
        }
        return exist;
    }

    public void mergeMaps(Map<String, Integer> map1, Map<String, Integer> map2, Map<String,Integer> result) {

        copyMap(map2,result);

        for (String key: map1.keySet()) {
            result.merge(key,map1.get(key),(v1,v2)->(v1+v2));
        }

    }

    public void copyMap(Map<String , Integer> map, Map<String, Integer> result) {

        for (String key : map.keySet()) {
            result.put(key, map.get(key));
        }

    }
}
