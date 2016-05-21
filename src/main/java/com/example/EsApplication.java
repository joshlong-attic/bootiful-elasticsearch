package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

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
