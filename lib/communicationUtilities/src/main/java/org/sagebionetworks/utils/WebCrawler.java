package org.sagebionetworks.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Workflow activities relevant to crawling data sources to look for new data.
 * 
 * @author deflaux
 * 
 */
public class WebCrawler {

	private static final Logger log = LogManager.getLogger(WebCrawler.class.getName());

	private List<SimpleObserver<String>> observers = new ArrayList<SimpleObserver<String>>();

	/**
	 * Perform a breadth-first crawl on the start url with which we are
	 * interested
	 * 
	 * http://jsoup.org/cookbook/extracting-data/working-with-urls
	 * 
	 * @param startUrl
	 * @param descend
	 * 
	 * @throws Exception
	 */
	public void doCrawl(String startUrl, Boolean descend) throws Exception {

		LinkedBlockingQueue<String> urlsToCrawl = new LinkedBlockingQueue<String>();
		urlsToCrawl.add(startUrl);

		//
		// Find all terminal urls
		//
		while (!urlsToCrawl.isEmpty()) {
			String url = urlsToCrawl.take();
			log.debug("WebCrawler url: " + url);

			String page = null;
			try {
				page = HttpClientHelper.getContent(DefaultHttpClientSingleton.getInstance(), url);
			} catch (Exception e) {
				log.error("Failed to crawl " + url, e);
				continue;
			}

			Document doc = Jsoup.parse(page, url);
			Elements links = doc.select("a[href]"); // a with href

			for (Element link : links) {
				// only proceed down directories that are in this tree and
				// deeper than this url
				if (link.attr("href").endsWith("/")
						&& (link.attr("abs:href").startsWith(url))
						&& (!url.equals(link.attr("abs:href")))) {
					notifyObservers(link.attr("abs:href"));
					if (descend) {
						urlsToCrawl.add(link.attr("abs:href"));
					}
				} else {
					if (link.attr("abs:href").startsWith(url)) {
						log.debug("Found a terminating url: "
								+ link.attr("abs:href"));
						notifyObservers(link.attr("abs:href"));
					}
				}
			}
		}
	}

	/**
	 * @param observer
	 */
	public void addObserver(SimpleObserver<String> observer) {
		observers.add(observer);
	}

	private void notifyObservers(String url) throws Exception {
		for (SimpleObserver<String> observer : observers) {
			observer.update(url);
		}
	}
	
	/**
	 * Static crawl method appropriate for workflow activities
	 * 
	 * @param observer
	 * @param startUrl
	 * @param descend
	 * @throws Exception 
	 */
	public static void doCrawl(SimpleObserver<String> observer, String startUrl, Boolean descend) throws Exception {
		WebCrawler crawler = new WebCrawler();
		crawler.addObserver(observer);
		crawler.doCrawl(startUrl, descend);
	}
	
}
