package cs460.grouple.grouple;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

/*
 * GroupCreateActivity allows a user to create a new group.
 */

public class EventCreateActivity extends ActionBarActivity
{
	private BroadcastReceiver broadcastReceiver;
	private SparseArray<String> added = new SparseArray<String>();    //holds list of name of all friend rows to be added
	private SparseArray<Boolean> role = new SparseArray<Boolean>();   //holds list of role of all friend rows to be added
	private Map<String, String> allFriends = new HashMap<String, String>();   //holds list of all current friends
	private User user;
	private String email = null;
	private String e_id = null;
	private String startDate = "";
	private String endDate = "";
	private String minimum = "";
	private String maximum = "";
	private String category = "";
	private String location = "";
	private EditText categoryEditText;
	private EditText startDateEditText;
	private EditText endDateEditText;
	private AlertDialog categoryDialog;
	
	
	private DatePicker datePicker;
	private Calendar calendar;
	private TextView dateView;
	private int year, month, day, hour, minute;
	private Global GLOBAL;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		calendar = Calendar.getInstance();
		year = calendar.get(Calendar.YEAR);
	    month = calendar.get(Calendar.MONTH);
	    day = calendar.get(Calendar.DAY_OF_MONTH);
	    hour = calendar.get(Calendar.HOUR_OF_DAY);
	    minute = calendar.get(Calendar.MINUTE);
		setContentView(R.layout.activity_event_create);
		categoryEditText = (EditText) findViewById(R.id.category);
		startDateEditText = (EditText) findViewById(R.id.startTimeButton);
		endDateEditText = (EditText) findViewById(R.id.endTimeButton);
		load();	
		
		
	}
	
	private void load()
	{
		GLOBAL = ((Global) getApplicationContext());
		//grab the email of current users from our GLOBAL class
		user =  GLOBAL.getCurrentUser();
		email = user.getEmail();

		initActionBar();
		initKillswitchListener();
	}
		
	private void initActionBar()
	{
		ActionBar ab = getSupportActionBar();
		ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		ab.setCustomView(R.layout.actionbar);
		ab.setDisplayHomeAsUpEnabled(false);
		TextView actionBarTitle = (TextView) findViewById(R.id.actionbarTitleTextView);
		actionBarTitle.setText("Create Event");
	}
	
	@Override
	protected void onDestroy()
	{
		// TODO Auto-generated method stub
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	//onClick for category button
	public void selectCategoryButton(View view)
	{
		System.out.println("clicked on category");
		
		// Strings to Show In Dialog with Radio Buttons
		final CharSequence[] items = {"Food ","Sports ","Party ","Work ","School"};
		            
		// Creating and Building the Dialog 
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select your category");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int item) {
		                   	                
	    switch(item)
		{
			case 0:
		    categoryEditText.setText(items[0]);
		    break;
		    case 1:
		    categoryEditText.setText(items[1]);
		    break;
		    case 2:
		    categoryEditText.setText(items[2]);
		    break;
		    case 3:
		    categoryEditText.setText(items[3]);           
		    break;
		    case 4:
		    categoryEditText.setText(items[4]);
		    break;
		}  
	    categoryDialog.cancel();
		}
		});
		categoryDialog = builder.create();
		categoryDialog.show();
	}
	
	//onClick for start date button
	public void selectStartDateButton(View view)
	{
		System.out.println("clicked on startdate");
		new DatePickerDialog(this, myStartDateListener, year, month, day).show();
		
	}
	
	//onClick for end date button
	public void selectEndDateButton(View view)
	{
		System.out.println("clicked on enddate");
		new DatePickerDialog(this, myEndDateListener, year, month, day).show();
	}
	
	//onClick for Confirm create event button
	public void createEventButton(View view)
	{		
		//Checking user inputs on event name, category, start date, end date, and min_part
		EditText eventNameEditText = (EditText) findViewById(R.id.eventName);
		String eventname = eventNameEditText.getText().toString();
		startDate = startDateEditText.getText().toString();
		endDate = endDateEditText.getText().toString();
		startDate.concat(":00");
		endDate.concat(":00");
		System.out.println("startdate to be used is: "+startDate);
		category = categoryEditText.getText().toString();
		EditText minimumEditText = (EditText) findViewById(R.id.minPartButton);
		minimum = minimumEditText.getText().toString();
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    	Date start = null;
    	Date end = null;
		try {
			start = sdf.parse(startDate);
			end = sdf.parse(endDate);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println(sdf.format(start));
    	System.out.println(sdf.format(end));

    	if(start.compareTo(end)>0){
    		System.out.println("Start is after End");
    	}else if(start.compareTo(end)<0){
    		System.out.println("start is before end");
    	}else if(start.compareTo(end)==0){
    		System.out.println("start is equal to end");
    	}else{
    		System.out.println("How to get here?");
    	}
  
		
		//if empty group name, display error box
		if(eventname.compareTo("") == 0)
		{
			new AlertDialog.Builder(this)
			.setMessage("Please give your event a name before creating.")
			.setCancelable(true)
			.setNegativeButton("Ok", null).show();
		}
		//if empty start or end date
		else if(startDate.compareTo("") == 0 || endDate.compareTo("") == 0)
		{
			new AlertDialog.Builder(this)
			.setMessage("Please specify a Start Date and End Date before creating.")
			.setCancelable(true)
			.setNegativeButton("Ok", null).show();
		}
		//if endDate is prior to startDate
		else if(start.compareTo(end) >=0)
		{
			new AlertDialog.Builder(this)
			.setMessage("Your Start Date must come before your End Date.")
			.setCancelable(true)
			.setNegativeButton("Ok", null).show();
		}
		//if empty category
		else if(category.compareTo("") == 0)
		{
			new AlertDialog.Builder(this)
			.setMessage("Please select a Category before creating.")
			.setCancelable(true)
			.setNegativeButton("Ok", null).show();
		}
		//if empty minimum
		else if(minimum.compareTo("") == 0)
		{
			new AlertDialog.Builder(this)
			.setMessage("Please specify a Minimum size before creating.")
			.setCancelable(true)
			.setNegativeButton("Ok", null).show();
		}
		//otherwise, display confirmation box to proceed
		else
		{
			new AlertDialog.Builder(this)
			.setMessage("Are you sure you want to create this event?")
			.setCancelable(true)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int id)
				{
					//initiate creation of event
					new CreateEventTask().execute("http://68.59.162.183/"
					+ "android_connect/create_event.php");
				}
			}).setNegativeButton("Invite Groups to Event", new DialogInterface.OnClickListener()
		{
		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			//take user to eventaddmembersactivity page. (pass e_id as extra so invites can be sent to correct group id)
			Intent intent = new Intent(EventCreateActivity.this, EventAddGroupsActivity.class);
			intent.putExtra("e_id", e_id);
			GLOBAL.loadUser(GLOBAL.getCurrentUser().getEmail());
			startActivity(intent);
			finish();
		}
		}).show();
		}
	}
	
	//aSynch class to create event
	private class CreateEventTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			EditText eventNameEditText = (EditText) findViewById(R.id.eventName);
			EditText eventBioEditText = (EditText) findViewById(R.id.eventBio);

			//grab group name and bio from textviews
			String eventname = eventNameEditText.getText().toString();
			String eventbio = eventBioEditText.getText().toString();	
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("e_name", eventname));
			nameValuePairs.add(new BasicNameValuePair("about", eventbio));
			nameValuePairs.add(new BasicNameValuePair("creator", email));
			nameValuePairs.add(new BasicNameValuePair("start_date", startDate));
			nameValuePairs.add(new BasicNameValuePair("end_date", endDate));
			nameValuePairs.add(new BasicNameValuePair("category", category));
			nameValuePairs.add(new BasicNameValuePair("min_part", minimum));
			nameValuePairs.add(new BasicNameValuePair("max_part", maximum));
			nameValuePairs.add(new BasicNameValuePair("mustbringlist", ""));
			nameValuePairs.add(new BasicNameValuePair("location", location));

			//pass url and nameValuePairs off to GLOBAL to do the JSON call.  Code continues at onPostExecute when JSON returns.
			return GLOBAL.readJSONFeed(urls[0], nameValuePairs);
		}

		@Override
		protected void onPostExecute(String result)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(result);

				// event has been successfully created
				if (jsonObject.getString("success").toString().equals("1"))
				{
					//now we can grab the newly created e_id returned from the server
					//Note: g_id is the only unique identifier of a group and therefore must be used for any future calls concerning that group.
					e_id = jsonObject.getString("e_id").toString();
					
					System.out.println("e_id of newly created group is: "+e_id);
					
					//display confirmation box
					AlertDialog dialog = new AlertDialog.Builder(EventCreateActivity.this)
					.setMessage("Nice work, you've successfully created an event!")
					.setCancelable(true)
					.setPositiveButton("View your Event Profile", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int id)
						{
							//add code here to take user to newly created event profile page.  (pass e_id as extra so correct group profile can be loaded)
							finish();
						}
					}).setNegativeButton("Invite Groups to Event", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							//add code here to take user to eventaddmembersactivity page.  (pass e_id as extra so invites can be sent to correct group id)
							finish();
						}
					}).show();
					
					dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			            @Override
			            public void onCancel(DialogInterface dialog) {
			            	finish();			                
			            }
			        });
				
				} 
				//Create event failed for some reasons.
				else if (jsonObject.getString("success").toString().equals("0"))
				{	
					//display error box
					new AlertDialog.Builder(EventCreateActivity.this)
					.setMessage("Unable to create event! Please choose an option:")
					.setCancelable(true)
					.setPositiveButton("Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int id)
						{
							//initiate creation of event AGAIN
							new CreateEventTask().execute("http://68.59.162.183/"
							+ "android_connect/create_event.php");
						}
					}).setNegativeButton("Cancel", null).show();
				}
			} catch (Exception e)
			{
				Log.d("readJSONFeed", e.getLocalizedMessage());
			}
		}
	}
		


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation_actions, menu);
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
			GLOBAL.destroySession();
			startActivity(login);
			Intent intent = new Intent("CLOSE_ALL");
			this.sendBroadcast(intent);
			return true;
		}
		if (id == R.id.action_home)
		{
			Intent intent = new Intent(this, HomeActivity.class);
			intent.putExtra("up", "false");
			intent.putExtra("ParentClassName", "EventCreateActivity");
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	public void initKillswitchListener()
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
	
	private DatePickerDialog.OnDateSetListener myStartDateListener
	   = new DatePickerDialog.OnDateSetListener() 
	 {

	   @Override
	   public void onDateSet(DatePicker view, int year, int month, int day) 
	   {
		   if (view.isShown()) 
		   {
			   int tmpMonth = month+1;
			   startDateEditText.setText(year+"-"+tmpMonth+"-"+day);
			   new TimePickerDialog(EventCreateActivity.this, myStartTimeListener, hour, minute, false).show();
		   }
	   }
	};
	
	private TimePickerDialog.OnTimeSetListener myStartTimeListener
	   = new TimePickerDialog.OnTimeSetListener() 
	{

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) 
		{
			if (view.isShown()) 
			{
				startDateEditText.append(" "+hourOfDay+":"+minute);
			}
		}
	};
	
	private DatePickerDialog.OnDateSetListener myEndDateListener
	   = new DatePickerDialog.OnDateSetListener() 
	 {

	   @Override
	   public void onDateSet(DatePicker view, int year, int month, int day) 
	   {
		   if (view.isShown()) 
		   {
			   int tmpMonth = month+1;
			   endDateEditText.setText(year+"-"+tmpMonth+"-"+day);
			   new TimePickerDialog(EventCreateActivity.this, myEndTimeListener, hour, minute, false).show();
		   }
	   }
	};
	
	private TimePickerDialog.OnTimeSetListener myEndTimeListener
	   = new TimePickerDialog.OnTimeSetListener() 
	{

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) 
		{
			if (view.isShown()) 
			{
				endDateEditText.append(" "+hourOfDay+":"+minute);
			}
		}
	};
	
	
}
