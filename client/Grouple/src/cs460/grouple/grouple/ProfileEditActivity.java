package cs460.grouple.grouple;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * EditProfileActivity allows user to make changes to his/her profile.
 */
public class ProfileEditActivity extends ActionBarActivity implements
		View.OnClickListener
{
	// Set up fields. Most are just for the camera.
	private Button b;
	private ImageView iv;
	private final static int CAMERA_DATA = 0;
	private Bitmap bmp;
	private Intent i;
	private User user;
	private BroadcastReceiver broadcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Set the activity layout to activity_edit_profile.
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_profile);
		
		load();		
	}

	private void load()
	{
		Global global = ((Global) getApplicationContext());
		// Resetting error text view
		TextView errorTextView = (TextView) findViewById(R.id.errorTextViewEPA);
		errorTextView.setVisibility(1);

		Bundle extras = getIntent().getExtras();
		user = global.loadUser(global.getCurrentUser().getEmail());
	
		
		if (user != null)
			getProfile();

		initActionBar();
		initKillswitchListener();
	}
	
	private void initActionBar()
	{
		// Set up the action bar.
		ActionBar ab = getSupportActionBar();
		ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		ab.setCustomView(R.layout.actionbar);
		ab.setDisplayHomeAsUpEnabled(false);
		//ImageButton upButton = (ImageButton) findViewById(R.id.actionbarUpButton);		
		TextView actionbarTitle = (TextView) findViewById(R.id.actionbarTitleTextView);
		//ImageButton upButton = (ImageButton) findViewById(R.id.actionbarUpButton);

		actionbarTitle.setText(user.getFirstName() + "'s Profile");
	}
	
	@Override
	protected void onDestroy()
	{
		// TODO Auto-generated method stub
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		finish();
	}

	
	/*
	 * Get profile executes get_profile.php. It uses the current users email
	 * address to retrieve the users name, age, and bio.
	 */
	private void getProfile()
	{
		// Find the text views.
		TextView nameTextView = (TextView) findViewById(R.id.nameEditTextEPA);
		TextView ageTextView = (TextView) findViewById(R.id.ageEditTextEPA);
		TextView locationTextView = (TextView) findViewById(R.id.locationEditTextEPA);
		TextView bioTextView = (TextView) findViewById(R.id.bioEditTextEPA);
		if (iv == null)
		{
			Log.d("scott", "7th");
			iv = (ImageView) findViewById(R.id.profileImageGPA);
		}
		// Add the info to the textviews for editing.
		nameTextView.setText(user.getName());
		ageTextView.setText(Integer.toString(user.getAge()));
		bioTextView.setText(user.getAbout());
		locationTextView.setText(user.getLocation());
		iv.setImageBitmap(user.getImage());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation_actions, menu);
		// Set up the edit button and image view
		b = (Button) findViewById(R.id.editProfilePhotoButton);
		b.setOnClickListener(this);
		if (iv == null)
		{
			iv = (ImageView) findViewById(R.id.profileImageGPA);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_logout)
		{
			Intent login = new Intent(this, LoginActivity.class);
			startActivity(login);
			Intent intent = new Intent("CLOSE_ALL");
			this.sendBroadcast(intent);
			return true;
		}
		if (id == R.id.action_home)
		{
			Intent intent = new Intent(this, HomeActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	// Button Listener for submit changes. It the profile in the database.
	// This executes the
	public void submitButton(View view) throws InterruptedException, ExecutionException, TimeoutException
	{
		// error checking

		// bio no more than
		TextView bioTextView = (TextView) findViewById(R.id.bioEditTextEPA);
		String bio = bioTextView.getText().toString();
		if (bio.length() > 100)
		{
			TextView errorTextView = (TextView) findViewById(R.id.errorTextViewEPA);
			errorTextView.setText("Bio is too many characters.");
			errorTextView.setVisibility(0);
		} else
		{
			AsyncTask<String, Void, String> task = new setProfileTask()
			.execute("http://68.59.162.183/android_connect/update_profile.php");
	        
			
			task.get(10000, TimeUnit.MILLISECONDS);
			

			
		}

	}

	/*
	 * Set profile executes update_profile.php. It uses the current users email
	 * address to update the users name, age, and bio.
	 */
	private class setProfileTask extends AsyncTask<String, Void, String>
	{

		@Override
		protected String doInBackground(String... urls)
		{

			return readJSONFeed(urls[0]);
		}

		// Grab the data from the textviews and push it to the database.
		public String readJSONFeed(String URL)
		{

			StringBuilder stringBuilder = new StringBuilder();
			HttpClient httpClient = new DefaultHttpClient();
			// kaboom
			HttpPost httpPost = new HttpPost(URL);
			try
			{
				String email = user.getEmail();
				TextView nameTextView = (TextView) findViewById(R.id.nameEditTextEPA);
				TextView ageTextView = (TextView) findViewById(R.id.ageEditTextEPA);
				TextView bioTextView = (TextView) findViewById(R.id.bioEditTextEPA);
				TextView locationTextView = (TextView) findViewById(R.id.locationEditTextEPA);

				String name = nameTextView.getText().toString();
				// Split name by space because sleep.
				String[] splitted = name.split("\\s+");
				String firstName = splitted[0];
				String lastName = splitted[1];

				String age = ageTextView.getText().toString();

				String bio = bioTextView.getText().toString();

				String location = locationTextView.getText().toString();

				MultipartEntityBuilder builder = MultipartEntityBuilder
						.create();

				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

				byte[] data;

				// process photo if set and add it to builder
				if (bmp != null)
				{
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					bmp.compress(CompressFormat.JPEG, 100, bos);
					data = bos.toByteArray();
					ByteArrayBody bab = new ByteArrayBody(data, ".jpg");
					builder.addPart("profilepic", bab);
					data = null;
					bab = null;
					bos.close();
				}

				// add remaining fields to builder, then execute
				builder.addTextBody("first", firstName, ContentType.TEXT_PLAIN);
				builder.addTextBody("last", lastName, ContentType.TEXT_PLAIN);
				builder.addTextBody("age", age, ContentType.TEXT_PLAIN);
				builder.addTextBody("bio", bio, ContentType.TEXT_PLAIN);
				builder.addTextBody("location", location,
						ContentType.TEXT_PLAIN);
				builder.addTextBody("email", email, ContentType.TEXT_PLAIN); 

				httpPost.setEntity(builder.build());

				HttpResponse response = httpClient.execute(httpPost);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200)
				{
					HttpEntity entity = response.getEntity();
					InputStream inputStream = entity.getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(inputStream));
					String line;
					while ((line = reader.readLine()) != null)
					{
						stringBuilder.append(line);
					}
					// cleanup
					inputStream.close();
					reader.close();
					bmp = null;
					builder = null;
				} else
				{
					Log.d("JSON", "Failed to download file");
				}
			} catch (Exception e)
			{
				Log.d("readJSONFeed", e.getLocalizedMessage());
			}
			return stringBuilder.toString();
		}

		// need to do more error checking.
		@Override
		protected void onPostExecute(String result)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(result);
				if (jsonObject.getString("success").toString().equals("1"))
				{
					// Success
					//refresh?
					Global global = ((Global) getApplicationContext());
					global.loadUser(user.getEmail());
					System.out.println("Success");
					 
					finish();
				} else
				{
					// Fail
					System.out.println("Fail");
				}
			} catch (Exception e)
			{
				Log.d("ReadatherJSONFeedTask", e.getLocalizedMessage());
			}
		}
	}

	
	public void startUserProfileActivity()
	{
		Intent intent = new Intent(this, ProfileActivity.class);
		intent.putExtra("up", "true");
		intent.putExtra("email", user.getEmail());

		startActivity(intent);
	}
	@Override
	public void onClick(View v) 
	{
		// TODO Auto-generated method stub
		switch (v.getId()) 
		{
		case R.id.editProfilePhotoButton:
			final CharSequence[] items = {"Take Photo", "Choose from Gallery",
					"Cancel" };

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose your profile picture:");
			builder.setItems(items, new DialogInterface.OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int item) 
				{
					if (items[item].equals("Take Photo")) 
					{
						i = new Intent(
								android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(i, 1);
					}
					else if (items[item].equals("Choose from Gallery")) 
					{
						Intent intent = new Intent(
								Intent.ACTION_PICK,
								android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						intent.setType("image/*");
						startActivityForResult(
								Intent.createChooser(intent, "Select Photo"), 2);
					} 
					else if (items[item].equals("Cancel")) 
					{
						dialog.dismiss();
					}
				}
			});
			builder.show();
			
			break;
		}
	}

	@Override
	protected void onActivityResult(int reqCode, int resCode, Intent data)
	{
		super.onActivityResult(reqCode, resCode, data);
		if (resCode == RESULT_OK) 
		{
			if (reqCode == 1) 
			{
				Bundle extras = data.getExtras();
				bmp = (Bitmap) extras.get("data");
				iv.setImageBitmap(bmp);
			} else if (reqCode == 2) 
			{
				Uri selectedImageUri = data.getData();

				String tempPath = selectedImageUri.getPath();
				bmp = BitmapFactory.decodeFile(tempPath);
				iv.setImageBitmap(bmp);
			}
		}
	}


	private void initKillswitchListener()
	{
		// START KILL SWITCH LISTENER
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("CLOSE_ALL");
		broadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				// close activity
				if (intent.getAction().equals("CLOSE_ALL"))
				{
					Log.d("app666", "we killin the login it");
					// System.exit(1);
					finish();
				}

			}
		};
		registerReceiver(broadcastReceiver, intentFilter);
		// End Kill switch listener
	}
}
