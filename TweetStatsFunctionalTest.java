import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.junit.Test;

public class TweetStatsFunctionalTest {

	private static final String OAUTH_TOKEN = "AAAAAAAAAAAAAAAAAAAAADnbkQAAAAAAFzzgVw2l%2F%2FQNm1C%2Bpno7XKXk%2FI0%3DT7YstVGsZGYbbIUqDlXJi6xmzBaI9HZHyMQd2Dn2BKBK3F7yMo";
	
	private static final String SCREEN_NAME_LESS_FOLLOWERS = "BKTesters"; //Only 6 followers. 
	
	private static final String SCREEN_NAME_MORE_FOLLOWERS = "Qoo10SG"; //3582 at the time of testing. 
	@Test
	public void testFetchFolloweIds_LessThan10Followers() throws Exception {
		String endPointUrl = "https://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name="+SCREEN_NAME_LESS_FOLLOWERS+"&count=5000";
		JSONArray followerIds = TweetStats.fetchFollowerIds(endPointUrl, OAUTH_TOKEN);
		assertEquals(6,followerIds.size());
	}
	
	@Test
	public void testFetchFolloweIds_MoreThan1000Followers() throws Exception {
		String endPointUrl = "https://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name="+SCREEN_NAME_MORE_FOLLOWERS+"&count=5000";
		JSONArray followerIds = TweetStats.fetchFollowerIds(endPointUrl, OAUTH_TOKEN);
		assertTrue(followerIds.size()<=3582);
	}
	
	@Test
	public void testGetScreenNameFromIds() throws Exception {
		String endPointUrl = "https://api.twitter.com/1.1/users/lookup.json?user_id=";
		String user_ids = "1263968610,1223437416,99485350,109636071,16508569,474672660";
		List<String> screenName = TweetStats.getScreenNameFromIds(endPointUrl, OAUTH_TOKEN, user_ids);
		assertNotNull(screenName);
		assertEquals(6, screenName.size());
	}
	
	@Test
	public void fetchTweetStats() throws Exception {
		Map<String, Map<String, Integer>> tweetStats = TweetStats.fetchTweetStats(SCREEN_NAME_LESS_FOLLOWERS, OAUTH_TOKEN);
		assertNotNull(tweetStats);
		assertTrue(tweetStats.size()>0);
	}
	
	@Test
	public void getRequestBearerToken() throws Exception {
		String endPointUrl = "https://api.twitter.com/oauth2/token";
		String bearer = TweetStats.requestBearerToken(endPointUrl);
		assertEquals(OAUTH_TOKEN, bearer);
	}

}
