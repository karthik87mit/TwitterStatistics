import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class TweetStats {
    
    
	/**
	 * Fetches the follower ids of a given user. Used as helper method inside fetchTweetStats.
	 * @param endPointUrl - Twitter endpoint to get follower ids
	 * @param bearerToken - OAuth Token
	 * @return JSONArray of follower IDs
	 * @throws IOException
	 */
    public static JSONArray fetchFollowerIds(String endPointUrl, String bearerToken) throws IOException {
        HttpsURLConnection connection = null;
        
        JSONArray ids = null;
        
        try {
            connection = getConnection(bearerToken, endPointUrl);
            
            JSONObject obj = (JSONObject)JSONValue.parse(readResponse(connection));
            
            if (obj != null) {
                
                ids = (JSONArray) obj.get("ids");
                
                return ids;
            }
            return new JSONArray();
        }
        catch (MalformedURLException e) {
            throw new IOException("Invalid endpoint URL specified.", e);
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Main method to return the tweet statistics for all following users in the past 7 days.
     * @param twitterHandle - To fetch the statistics
     * @param bearerToken - OAuth token
     * @return Map<String, Map<String, Integer>>
     *         Map of Username/ScreenName with Tweets in the last 7 days as value
     *         Eg:  flywindy : Date:19/2/2016
 	 *						   Number of tweets:1
     *         
     * @throws IOException
     */
    public static Map<String, Map<String, Integer>> fetchTweetStats(String twitterHandle, String bearerToken) throws IOException {
        
        HttpsURLConnection search_connection =null, lookup_rate_limit_connection =null, search_rate_limit_connection =null;
        
        Map<String, Map<String, Integer>> statsMap = null;
        
        Map<String, Integer> dateMap =null;
        
        Date date = new Date();
        
        String modifiedDate= new SimpleDateFormat("yyyy-MM-dd").format(date);
        
        String searchAPIUrl = null;
        
        List<String> screenName = null;
        
        String user_id_csv_100 = new String(); //Send user_id as comma separate string of 100 to maximize the throughput.
        
        int count_for_lookup_api = 0,limit_for_lookup_api = 0;
        
        int limit_for_search_api = 0, idle_followers = 0;
        
        DateFormat format = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
        
        boolean addToMapFlag = false;
        
        JSONObject search_obj = null;
        
        try {
            statsMap = new HashMap<String, Map<String, Integer>>();
            // Fetch the follower ids for the handle
        	JSONArray user_ids = fetchFollowerIds("https://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name="+twitterHandle+"&count=5000",bearerToken);
        	System.out.println("Follower Count:" + user_ids.size());
        	System.out.println("**************************************************************************\n");
        	Iterator<Long> idsIterator=user_ids.iterator();
            
            while(idsIterator.hasNext()){
                
                long user_id = (Long) idsIterator.next();
                user_id_csv_100 += user_id + "," ; // Get the screen name for the user_ids. Using CSV of 100 ids to efficiently retrieve screen name without hitting rate limits.
                count_for_lookup_api++;
                if (count_for_lookup_api == 100 || !(idsIterator.hasNext())) {
                    if (user_id_csv_100.endsWith(",")) {
                        user_id_csv_100 = user_id_csv_100.substring(0, user_id_csv_100.length() - 1);
                    }
                    if (limit_for_lookup_api <= 60){ // 60 requests per 15 mins is the rate limit of app authentication for this API.
                        screenName = getScreenNameFromIds("https://api.twitter.com/1.1/users/lookup.json?user_id=",bearerToken, user_id_csv_100);
                        limit_for_lookup_api++;
                    }
                    else{ // Reset the rate limit window by sleeping for the time remaining in the window.
                        lookup_rate_limit_connection = getConnection(bearerToken, "https://api.twitter.com/1.1/application/rate_limit_status.json?resources=users");
                        resetRateLimitingWindow(lookup_rate_limit_connection,"users", "/users/lookup");
                        screenName = getScreenNameFromIds("https://api.twitter.com/1.1/users/lookup.json?user_id=",bearerToken, user_id_csv_100);
                        limit_for_lookup_api=1;
                    }
                    for (int i=0; i < screenName.size(); i++){
                        addToMapFlag = false;
                        dateMap = new HashMap<String,Integer>();
                        searchAPIUrl = "https://api.twitter.com/1.1/search/tweets.json?q=from%3A"+screenName.get(i)+"&until=" + modifiedDate;
                        search_connection = getConnection(bearerToken, searchAPIUrl);
                        if (limit_for_search_api <= 450){ // 450 requests per 15 mins is the rate limit of app authentication for this API.
                            search_obj = (JSONObject)JSONValue.parse(readResponse(search_connection));
                            limit_for_search_api ++;
                        }
                        else{ // Reset the rate limit window by sleeping for the time remaining in the window.
                            search_rate_limit_connection = getConnection(bearerToken, "https://api.twitter.com/1.1/application/rate_limit_status.json?resources=search");
                            resetRateLimitingWindow(search_rate_limit_connection,"search", "/search/tweets");
                            search_obj = (JSONObject)JSONValue.parse(readResponse(search_connection));
                            limit_for_search_api=1;
                        }
                        if (search_obj != null) {
                        	// Constructing the Map to be returned.
                            JSONArray tweets = (JSONArray) search_obj.get("statuses");
                            for (int j =0; j< tweets.size(); j++){
                                String created_date_str = (String) ((JSONObject)tweets.get(j)).get("created_at");
                                Date created_date = format.parse(created_date_str);
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(created_date);
                                String formatedDate = cal.get(Calendar.DATE) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" +         cal.get(Calendar.YEAR);
                                
                                if (dateMap.size() >0 && dateMap.get(formatedDate)!=null){
                                    int counter = dateMap.get(formatedDate) + 1;
                                    dateMap.put(formatedDate,counter);
                                }
                                else
                                    dateMap.put(formatedDate,1);
                                addToMapFlag = true;
                            }
                            if (!addToMapFlag){
                            	idle_followers++;
                            	System.out.println("Follower:"+screenName.get(i)+" has 0 tweets in the past 7 days\n");
                            }
                            else{
                                statsMap.put(screenName.get(i), dateMap);
                            }
                        }
                    }
                    count_for_lookup_api = 0;
                    user_id_csv_100 = new String();
                }
            }
            System.out.println("**************************************************************************\n");
            System.out.println("Idle followers in the past 7 days (with 0 tweets):"+idle_followers+"\n");
            printTweetStatistics(statsMap);
        }
        catch (MalformedURLException e) {
            throw new IOException("Invalid endpoint URL specified.", e);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            if (search_connection != null) {
                search_connection.disconnect();
            }
            else if (search_rate_limit_connection != null) {
                search_rate_limit_connection.disconnect();
            }
            else if (lookup_rate_limit_connection != null) {
                lookup_rate_limit_connection.disconnect();
            }
        }
        return statsMap;
    }
    
    /**
     * Helper method to reset the rate limit window by hitting the rate limit status end point.
     * @param connection HttpsURLConnection
     * @param apiRoot Root of the API. Eg: "users", "search"
     * @param apiEndPoint Endpoint of the API. Eg: "/users/lookup". "/search/tweets"
     * @throws InterruptedException
     */
    public static void resetRateLimitingWindow(HttpsURLConnection connection,String apiRoot,String apiEndPoint)
    throws InterruptedException {
        JSONObject object = (JSONObject)JSONValue.parse(readResponse(connection));
        JSONObject user_lookup = (JSONObject) ((JSONObject) ((JSONObject) object.get("resources")).get(apiRoot)).get(apiEndPoint);
        long reset = (Long) user_lookup.get("reset");
        Date expiry = new Date(reset * 1000);
        long reset_time_in_mills = expiry.getTime() - new Date().getTime();
        if (reset_time_in_mills > 0)
            Thread.sleep(reset_time_in_mills);
    }
    
    /**
     * Methods to extract screen name given the user_ids. Used inside the fetchTweetStats methods to get the screen name given the follower IDs.
     * @param endPointUrl Twitter REST API for users/lookup
     * @param bearerToken OAuth token
     * @param user_ids List of follower_ids as CSV String.
     * @return List<String> of screen names.
     * @throws IOException
     */
    public static List<String> getScreenNameFromIds(String endPointUrl, String bearerToken, String user_ids) throws IOException {
        HttpsURLConnection connection = null;
        
        List<String> screenName = new ArrayList<String>();
        
        String screenNameUrl = endPointUrl + user_ids;
        
        try {
            connection = getConnection(bearerToken, screenNameUrl);
            
            String response = readResponse(connection);
            
            JSONArray obj = (JSONArray)JSONValue.parse(response);
            
            if (obj != null && obj.size() >0) {
                
                for (int i=0; i< obj.size(); i++) {
                    screenName.add((String) ((JSONObject)obj.get(i)).get("screen_name"));
                }
                
                return screenName;
            }
            return new ArrayList<String>();
        }
        catch (MalformedURLException e) {
            throw new IOException("Invalid endpoint URL specified.", e);
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Helper method to set all the request params given a twitter endPoint.
     * @param bearerToken OAuth token
     * @param endPointUrl Endpoint to set the connection params
     * @return HttpsURLConnection
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    public static HttpsURLConnection getConnection(String bearerToken, String endPointUrl)
    throws MalformedURLException, IOException, ProtocolException {
        HttpsURLConnection connection;
        URL url = new URL(endPointUrl);
        connection = (HttpsURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Host", "api.twitter.com");
        connection.setRequestProperty("User-Agent", "Your Program Name");
        connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        connection.setUseCaches(false);
        return connection;
    }
    
    
    
    /**
     * Encodes the app key and app secret using Base64 encoding to create the basic authorization key
     * @param consumerKey
     * @param consumerSecret
     * @return String - Encoded keys as String
     */
    public static String encodeKeys(String appKey, String appSecret) {
        try {
            String encodedConsumerKey = URLEncoder.encode(appKey, "UTF-8");
            String encodedConsumerSecret = URLEncoder.encode(appSecret, "UTF-8");
            
            String fullKey = encodedConsumerKey + ":" + encodedConsumerSecret;
            byte[] encodedBytes = Base64.encodeBase64(fullKey.getBytes());
            return new String(encodedBytes);
        }
        catch (UnsupportedEncodingException e) {
            return new String();
        }
    }
    
     
    /**
     * Returns the OAuth token as a string that is used to access other end points.
     * This is APP OAuth.
     * @param endPointUrl Twitter REST API for oauth2/token
     * @return OAuth2 token as String
     * @throws IOException
     */
    public static String requestBearerToken(String endPointUrl) throws IOException {
        HttpsURLConnection connection = null;
        String encodedCredentials = encodeKeys("JIBww1s6PyKnNvIoJwzHBrd3U","44d487TISiFh0wGfXuvbkWC7Fkm1VA0g9NeTy0t9XGuq5qa096");
        
        try {
            URL url = new URL(endPointUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Host", "api.twitter.com");
            connection.setRequestProperty("User-Agent", "Your Program Name");
            connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            connection.setRequestProperty("Content-Length", "29");
            connection.setUseCaches(false);
            
            writeRequest(connection, "grant_type=client_credentials");
            
            // Parse the JSON response into a JSON mapped object to fetch fields from.
            JSONObject obj = (JSONObject)JSONValue.parse(readResponse(connection));
            
            if (obj != null) {
                String tokenType = (String)obj.get("token_type");
                String token = (String)obj.get("access_token");
                
                return ((tokenType.equals("bearer")) && (token != null)) ? token : "";
            }
            return new String();
        }
        catch (MalformedURLException e) {
            throw new IOException("Invalid endpoint URL specified.", e);
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
     
   
    /**
     * Writes a request to a connection
     * @param connection HttpsURLConnection
     * @param textBody param to write in the request.
     * @return
     */
    public static boolean writeRequest(HttpsURLConnection connection, String textBody) {
        try {
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            wr.write(textBody);
            wr.flush();
            wr.close();
            
            return true;
        }
        catch (IOException e) { return false; }
    }
    
   
    /**
     * Reads a response for a given connection and returns it as a string.
     * @param connection HttpsURLConnection
     * @return
     */
    public static String readResponse(HttpsURLConnection connection) {
        try {
            StringBuilder str = new StringBuilder();
            
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            while((line = br.readLine()) != null) {
                str.append(line+ System.getProperty("line.separator"));
            }
            return str.toString();
        }
        catch (IOException e) { return new String(); }
    }
    
    /**
     * Main method to run the application for testing purpose.
     * @param args
     */
    public static void main (String args[]){
        try {            
            String bearer = requestBearerToken("https://api.twitter.com/oauth2/token");
            Map<String, Map<String, Integer>> tweet_stats = fetchTweetStats("praveenatMIT",bearer);
            printTweetStatistics(tweet_stats);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to print the Statistics in a readable manner.
     * @param tweet_stats
     */
	private static void printTweetStatistics(Map<String, Map<String, Integer>> tweet_stats) {
		Iterator tweetIterator = tweet_stats.entrySet().iterator();
		System.out.println("Active followers in the last 7 days (with atleast 1 tweet):"+tweet_stats.size()+"\n");  
		System.out.println("**************************************************************************\n");
		while (tweetIterator.hasNext()) {
		    Map.Entry tweetPair = (Map.Entry)tweetIterator.next();
		    System.out.println("Statistics for User:"+tweetPair.getKey());
		    System.out.println("-------------------------------------");
		    Iterator dateIterator = ((HashMap) tweetPair.getValue()).entrySet().iterator();
		    while (dateIterator.hasNext()){
		        Map.Entry date_tweet_Pair = (Map.Entry)dateIterator.next();
		        System.out.println("Date:"+date_tweet_Pair.getKey()+"\n Number of tweets:"+date_tweet_Pair.getValue()+"\n");
		    }
		}
	}
    
}
