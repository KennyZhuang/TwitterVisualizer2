package com.example.kenny.twittertest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import twitter4j.*;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.impl.client.DefaultHttpClient;


public class MainActivity extends Activity implements OnClickListener {
	
	/* Shared preference keys */
	private static final String PREF_NAME = "sample_twitter_pref";
	private static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	private static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	private static final String PREF_KEY_TWITTER_LOGIN = "is_twitter_loggedin";
	private static final String PREF_USER_NAME = "twitter_user_name";

	/* Any number for uniquely distinguish your request */
	public static final int WEBVIEW_REQUEST_CODE = 101;

	private ProgressDialog pDialog;

	private static Twitter twitter;
	private static RequestToken requestToken;
	
	private static SharedPreferences mSharedPreferences;

	private EditText mShareEditText;
	private EditText keyWordEditText;
	private TextView resultEditText;
	private TextView userName;
	private View loginLayout;
	private View shareLayout;
	private View searchLayout;

	private int[] opCount = {1, 1, 1};
	private String consumerKey = null;
	private String consumerSecret = null;
	private String accessToken = null;
	private String accessTokenSecret = null;
	private String callbackUrl = null;
	private String oAuthVerifier = null;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* initializing twitter parameters from string.xml */
		initTwitterConfigs();

		/* Enabling strict mode */
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		/* Setting activity layout file */
		setContentView(R.layout.activity_main);

		loginLayout = (RelativeLayout) findViewById(R.id.login_layout);
		shareLayout = (LinearLayout) findViewById(R.id.share_layout);
		searchLayout = (LinearLayout) findViewById(R.id.search_layout);
		mShareEditText = (EditText) findViewById(R.id.share_text);
		keyWordEditText = (EditText) findViewById(R.id.searchTerm);
		resultEditText = (TextView) findViewById(R.id.result_text);
		resultEditText.setMovementMethod(new ScrollingMovementMethod());
		userName = (TextView) findViewById(R.id.user_name);
		
		/* register button click listeners */
		findViewById(R.id.btn_login).setOnClickListener(this);
		findViewById(R.id.btn_share).setOnClickListener(this);
		findViewById(R.id.btn_search).setOnClickListener(this);
		findViewById(R.id.btn_viewmap).setOnClickListener(this);
		findViewById(R.id.btn_wordcloud).setOnClickListener(this);

		/* Check if required twitter keys are set */
		if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) {
			Toast.makeText(this, "Twitter key and secret not configured",
					Toast.LENGTH_SHORT).show();
			return;
		}

		/* Initialize application preferences */
		mSharedPreferences = getSharedPreferences(PREF_NAME, 0);
		/* clear user login info */
		//mSharedPreferences.edit().remove(PREF_KEY_TWITTER_LOGIN).commit();
		//mSharedPreferences.edit().clear().commit();
		boolean isLoggedIn = mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
		//boolean isLoggedIn = false;
		/*  if already logged in, then hide login layout and show share layout */
		if (isLoggedIn) {
			loginLayout.setVisibility(View.GONE);
			shareLayout.setVisibility(View.GONE);
			searchLayout.setVisibility(View.VISIBLE);

			String username = mSharedPreferences.getString(PREF_USER_NAME, "");
			//userName.setText(getResources ().getString(R.string.hello)
					//+ username);

		} else {
			loginLayout.setVisibility(View.VISIBLE);
			shareLayout.setVisibility(View.GONE);

			Uri uri = getIntent().getData();
			
			if (uri != null && uri.toString().startsWith(callbackUrl)) {
			
				String verifier = uri.getQueryParameter(oAuthVerifier);

				try {
					
					/* Getting oAuth authentication token */
					AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

					/* Getting user id form access token */
					long userID = accessToken.getUserId();
					final User user = twitter.showUser(userID);
					final String username = user.getName();

					/* save updated token */
					saveTwitterInfo(accessToken);

					loginLayout.setVisibility(View.GONE);
					shareLayout.setVisibility(View.GONE);
					searchLayout.setVisibility(View.VISIBLE);
					//userName.setText(getString(R.string.hello) + username);
					
				} catch (Exception e) {
					Log.e("Failed to login!!", e.getMessage());
				}
			}

		}
	}

	
	/**
	 * Saving user information, after user is authenticated for the first time.
	 * You don't need to show user to login, until user has a valid access toen
	 */
	private void saveTwitterInfo(AccessToken accessToken) {
		
		long userID = accessToken.getUserId();
		
		User user;
		try {
			user = twitter.showUser(userID);
		
			String username = user.getName();

			/* Storing oAuth tokens to shared preferences */
			Editor e = mSharedPreferences.edit();
			e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
			e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
			e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
			e.putString(PREF_USER_NAME, username);
			e.commit();

		} catch (TwitterException e1) {
			e1.printStackTrace();
		}
	}

	/* Reading twitter essential configuration parameters from strings.xml */
	private void initTwitterConfigs() {
		consumerKey = getString(R.string.twitter_consumer_key);
		consumerSecret = getString(R.string.twitter_consumer_secret);
		accessToken = getString(R.string.twitter_access_token);
		accessTokenSecret = getString(R.string.twitter_token_secret);
		callbackUrl = getString(R.string.twitter_callback);
		oAuthVerifier = getString(R.string.twitter_oauth_verifier);
	}

	
	private void loginToTwitter() {
		boolean isLoggedIn = mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
		//isLoggedIn = false;
		if (!isLoggedIn) {
			final ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(consumerKey);
			builder.setOAuthConsumerSecret(consumerSecret);

			final Configuration configuration = builder.build();
			final TwitterFactory factory = new TwitterFactory(configuration);
			twitter = factory.getInstance();

			try {
				requestToken = twitter.getOAuthRequestToken(callbackUrl);

				/**
				 *  Loading twitter login page on webview for authorization 
				 *  Once authorized, results are received at onActivityResult
				 *  */
				final Intent intent = new Intent(this, WebViewActivity.class);
				intent.putExtra(WebViewActivity.EXTRA_URL, requestToken.getAuthenticationURL());
				startActivityForResult(intent, WEBVIEW_REQUEST_CODE);
				
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		} else {

			loginLayout.setVisibility(View.GONE);
			searchLayout.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_OK) {
			String verifier = data.getExtras().getString(oAuthVerifier);
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

				long userID = accessToken.getUserId();
				final User user = twitter.showUser(userID);
				String username = user.getName();
				
				saveTwitterInfo(accessToken);

				loginLayout.setVisibility(View.GONE);
				searchLayout.setVisibility(View.VISIBLE);
				userName.setText(MainActivity.this.getResources().getString(
						R.string.hello) + username);

			} catch (Exception e) {
				Log.e("Twitter Login Failed", e.getMessage());
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:
			loginToTwitter();
			break;
		case R.id.btn_share:
			final String status = mShareEditText.getText().toString();
			
			if (status.trim().length() > 0) {
				new updateTwitterStatus().execute(status);
			} else {
				Toast.makeText(this, "Message is empty!!", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.btn_search:
			resultEditText.setText("");
			String keyword = keyWordEditText.getText().toString();
			searchTweet(keyword);
			//new searchTweets().execute(keyword);
			break;
		case R.id.btn_viewmap:
			Intent intent = new Intent(MainActivity.this, TweetMapActivity.class);
			MainActivity.this.startActivity(intent);
			break;

		case R.id.btn_wordcloud:
			Intent intent2 = new Intent(MainActivity.this, WordcloudActivity.class);
			intent2.putExtra("pos", opCount[0]);
			intent2.putExtra("neu", opCount[1]);
			intent2.putExtra("neg", opCount[2]);
			MainActivity.this.startActivity(intent2);
			//break;
			//
		}
	}
	public class getData extends AsyncTask<String, String, String> {

		HttpURLConnection urlConnection;

		@Override
		protected String doInBackground(String... args) {

			StringBuilder result = new StringBuilder();

			try {
				URL url = new URL("https://api.github.com/users/dmnugent80/repos");
				urlConnection = (HttpURLConnection) url.openConnection();
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				String line;
				while ((line = reader.readLine()) != null) {
					result.append(line);
				}

			}catch( Exception e) {
				e.printStackTrace();
			}
			finally {
				urlConnection.disconnect();
			}


			return result.toString();
		}

		@Override
		protected void onPostExecute(String result) {

			//Do something with the JSON string

		}

	}
	public class searchTweets extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... args) {
			String keyword = args[0];
			try {
				//resultEditText.setText("");
				//String keyword = keyWordEditText.getText().toString();
				searchTweet(keyword);

			} catch( Exception e) {
			e.printStackTrace();
			}
			return null;
		}

		//@Override
		protected void onPostExecute(String result) {

			//Do something with the JSON string

		}

	}
	public String getOpinion(String tweet_text) {
		resultEditText.append("sending: "+tweet_text+"\n");
		String res = "";
		try {
			URL url = new URL("http://text-processing.com/api/sentiment/");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			//Send request
			Uri.Builder builder = new Uri.Builder().appendQueryParameter("text", tweet_text);
			String query = builder.build().getEncodedQuery();

			OutputStream os = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(query);
			writer.flush();
			writer.close();
			os.close();
			conn.connect();
			InputStream in = new BufferedInputStream(conn.getInputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = reader.readLine()) != null) {
				res += line;
			}
			JSONObject opinion = new JSONObject(res);
			res = opinion.getString("label");
			if (res.contains("pos"))
				opCount[0]++;
			if (res.contains("neu"))
				opCount[1]++;
			if (res.contains("neg"))
				opCount[2]++;
			//resultEditText.append();
		} catch (Exception ex) {
			resultEditText.append("Connection failed\n");
		}
		return res;
	}
	public void searchTweet(String keyword) {
		if (keyword.length() == 0) {
			resultEditText.append("Error: Keyword should not be empty !\n");
			return;
		}
		resultEditText.append("Start searching keyword: " + keyword + "\n");
		opCount[0] = 1;
		opCount[1] = 1;
		opCount[2] = 1;
		try {
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(consumerKey);
			builder.setOAuthConsumerSecret(consumerSecret);

			// Access Token
			String access_token = accessToken;
			// Access Token Secret
			String access_token_secret = accessTokenSecret;
			AccessToken accessToken = new AccessToken(access_token, access_token_secret);
			Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
			Query query = new Query(keyword);
			query.setCount(100);
			query.setLang("en");
			GeoLocation la = new GeoLocation(34.05, -118.25);
			query.geoCode(la, 50.0, Query.MILES.toString());
			QueryResult result;
			Map<String, double[]> data = new HashMap<>();


				result = twitter.search(query);
				List<Status> tweets = result.getTweets();
				int i = 0;
				resultEditText.append("Found "+tweets.size()+" tweets\n");
				//resultEditText.append("Found "+tweets.size()+" tweets\n");
				for (Status tweet : tweets) {
					//resultEditText.append("enter loop\n");
					//resultEditText.append("@" + tweet.getUser().getScreenName() + ":" + tweet.getText() + "\n");
					GeoLocation geo = tweet.getGeoLocation();
					//resultEditText.append(i+"\n");

					if(geo != null) {
						double[] latlong = new double[2];
						latlong[0] = geo.getLatitude();
						latlong[1] = geo.getLongitude();
						resultEditText.append(latlong.toString() + "\n");
						String opinion = getOpinion(tweet.getText());
						resultEditText.append(opinion + "\n");
						data.put(opinion + "" + i, latlong);
						i++;
					}
				}
				//resultEditText.append("End loop\n");
			//} while ((query = result.nextQuery()) != null);
			// Write data to json file
			try {
				JSONObject json = new JSONObject(data);
				String content = json.toString();
				resultEditText.append(content+"\n");
				FileOutputStream fos = openFileOutput("tweetmap.json", Context.MODE_PRIVATE);
				fos.write(content.getBytes());
				fos.close();
				String path=getApplicationContext().getFilesDir().getAbsolutePath();
				resultEditText.append(path+"\n");
				//FileWriter fw = new FileWriter("/storage/emulated/0/tweetmap.json");
				//BufferedWriter bw = new BufferedWriter(fw);
				//bw.write(content);
				//bw.close();
			}
			catch (Exception e) {
				resultEditText.append("Can not write file !\n");
			}

			resultEditText.append("Search successfully completed !\n");

		} catch (TwitterException te) {
			te.printStackTrace();
			resultEditText.append("Failed to search tweets:\n" + te.getMessage() + "\n");
		}
	}


	class updateTwitterStatus extends AsyncTask<String, String, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Posting to twitter...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		protected Void doInBackground(String... args) {

			String status = args[0];
			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(consumerKey);
				builder.setOAuthConsumerSecret(consumerSecret);

				// Access Token
				String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
				// Access Token Secret
				String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");

				AccessToken accessToken = new AccessToken(access_token, access_token_secret);
				Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

				// Update status
				StatusUpdate statusUpdate = new StatusUpdate(status);
				InputStream is = getResources().openRawResource(R.drawable.lakeside_view);
				statusUpdate.setMedia("test.jpg", is);

				twitter4j.Status response = twitter.updateStatus(statusUpdate);

				Log.d("Status", response.getText());

			} catch (TwitterException e) {
				Log.d("Failed to post!", e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			
			/* Dismiss the progress dialog after sharing */
			pDialog.dismiss();
			
			Toast.makeText(MainActivity.this, "Posted to Twitter!", Toast.LENGTH_SHORT).show();

			// Clearing EditText field
			mShareEditText.setText("");
		}

	}
}