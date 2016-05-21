package com.example;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.shield.ShieldPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

@SpringBootApplication
public class EsApplication {

	@Bean
	Twitter twitter(@Value("${spring.social.twitter.app-id}") String apiKey,
	                @Value("${spring.social.twitter.app-secret}") String apiSecret) {
		return new TwitterTemplate(apiKey, apiSecret);
	}

	@Bean
	CommandLineRunner search(
			@Value("${TWITTER_SEARCH:#jeeconf}") String query,
			Twitter twitterTemplate, ElasticsearchTemplate elasticsearchTemplate) {
		return args -> {
			SearchResults searchResults = twitterTemplate.searchOperations().search(query);
			searchResults.getTweets().forEach(t -> this.index(elasticsearchTemplate, t));
			this.query(elasticsearchTemplate);
		};
	}


	@Bean
	Client client(ElasticsearchProperties properties,
	              @Value("${spring.data.elasticsearch.properties.shield.user}") String shieldUser) {
		// Build the settings for our client.
		Settings settings = Settings.settingsBuilder()
				.put("transport.ping_schedule", "5s")
				.put("cluster.name", properties.getClusterName())
				.put("action.bulk.compress", false)
				.put("shield.transport.ssl", true)
				.put("request.headers.X-Found-Cluster", "${cluster.name}")
				.put("shield.user",  shieldUser )
				.build();

		// Instantiate a TransportClient and add the cluster to the list of addresses to connect to.
		// Only port 9343 (SSL-encrypted) is currently supported.
		TransportClient client =
				TransportClient.builder()
					.addPlugin(ShieldPlugin.class)
					.settings(settings).build();
		try {
			String clusterName = properties.getClusterNodes();
			URI uri = URI.create(clusterName);
			String host = uri.getHost();
			int port = uri.getPort();
			Arrays.asList(InetAddress.getByName(host))
					.forEach(ia -> {
						client.addTransportAddress(new InetSocketTransportAddress(ia, port));
						System.out.println( ia.toString());
					});


		} catch (UnknownHostException e) {
			System.out.println( e.toString());
		}

		return client;
	}


	void query(ElasticsearchTemplate template) {
	}

	void index(ElasticsearchTemplate template, Tweet tweet) {
		template.createIndex("tweets");

		IndexQuery indexQuery = new IndexQueryBuilder()
				.withId(Long.toString(tweet.getId()))
				.withObject(tweet)
				.build();
		template.index(indexQuery);
	}

	public static void main(String[] args) {
		SpringApplication.run(EsApplication.class, args);
	}
}
