import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.Instant;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;

public class TweetStatsTest {

	@Test
	public void testEncodeKeys() {
		String consumer_key = "ABCD";
		String consumer_secret = "EFGH";
		String encoded_keys = TweetStats.encodeKeys(consumer_key, consumer_secret);
		assertEquals("QUJDRDpFRkdI",encoded_keys);
	}
	
	@Test
	public void testWriteRequestAndReadResponse() {
        try {
			HttpsURLConnection huc = org.mockito.Mockito.mock(HttpsURLConnection.class);
			InputStream anyInputStream = new ByteArrayInputStream("Hello".getBytes());
			OutputStream outputStream = org.mockito.Mockito.mock(OutputStream.class);
	        PowerMockito.when(huc.getInputStream()).thenReturn(anyInputStream);
	        PowerMockito.when(huc.getOutputStream()).thenReturn(outputStream);
	        TweetStats.writeRequest(huc, "Hello");
	        String result = TweetStats.readResponse(huc);
	        assertEquals("Hello\n",result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
        
	}
	
	@Test
	public void testGetConnection() throws Exception {
		HttpsURLConnection huc = TweetStats.getConnection("ABCD", "https://abc.com");
		assertEquals("GET",huc.getRequestMethod());
		assertEquals(true,huc.getDoInput());
		assertEquals(true,huc.getDoInput());
		assertNotNull(huc);
	}
	
	@Test(expected=ClassCastException.class)
	public void testGetConnectionWithHttpUrl_FailureCase() throws Exception {
		HttpsURLConnection huc = TweetStats.getConnection("ABCD", "http://abc.com");
		assertNotNull(huc);
	}
	
	@Test
	public void testResetRateLimitingWindow_Limit_Not_Reached() throws Exception{
		HttpsURLConnection huc = org.mockito.Mockito.mock(HttpsURLConnection.class);
		JSONObject object = (JSONObject)JSONValue.parse("{\"resources\": { \"search\": { \"\\/search\\/tweets\": { \"limit\": 180, \"remaining\": 180, \"reset\": 1403602426}}}}");
		String json_string = object.toJSONString();
		InputStream anyInputStream = new ByteArrayInputStream((json_string).getBytes());
		PowerMockito.when(huc.getInputStream()).thenReturn(anyInputStream);
		TweetStats.resetRateLimitingWindow(huc, "search", "/search/tweets");
	}
	
}
