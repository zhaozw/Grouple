package cs460.grouple.grouple;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * EventAddGroupsActivity allows a user to invite groups to a created event.
 */

public class EventAddGroupsActivity extends ActionBarActivity
{
	private BroadcastReceiver broadcastReceiver;
	private SparseArray<String> added = new SparseArray<String>();    //holds list of name of all group rows to be added
	private Map<Integer, String> allGroups = new HashMap<Integer, String>();   //holds list of all current groups
	private User user;
	private String email = null;
	private String e_id = null;
	private Global GLOBAL;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_event_addgroups);
		load();	
	}
	
	private void load()
	{
		GLOBAL = ((Global) getApplicationContext());
		//grab the email of current users from our GLOBAL class
		email = GLOBAL.getCurrentUser().getEmail();
		
		user = GLOBAL.loadUser(email);
		
		//load our list of current groups.
		allGroups = user.getGroups();
		
		populateGroupCreate();
		initActionBar();
		initKillswitchListener();
	}
	
	@Override
	protected void onDestroy()
	{
		// TODO Auto-generated method stub
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}
	
	private void populateGroupCreate()
	{
		// begin building the interface
		LayoutInflater inflater = getLayoutInflater();
		LinearLayout membersToAdd = (LinearLayout) findViewById(R.id.linearLayoutNested_eventAddGroups);
		
		if(allGroups.size() == 0)
		{
			View row = inflater.inflate(
					R.layout.list_row_eventinvitegroup, null);

			((Button) row.findViewById(R.id.groupNameButtonNoAccess))
					.setText("You don't have any groups to add yet!");
			row.findViewById(R.id.groupNameButtonNoAccess)
					.setVisibility(1);
			membersToAdd.addView(row);
		}
		
		Iterator iterator = allGroups.entrySet().iterator();
		
		//setup for each group
		for(int i=0; i<allGroups.size(); i++)
		{
			
			GridLayout rowView;
			rowView = (GridLayout) inflater.inflate(
					R.layout.list_row_eventinvitegroup, null);

			final Button groupNameButton = (Button) rowView
					.findViewById(R.id.groupNameButtonNoAccess);
			final CheckBox cb = (CheckBox) rowView
					.findViewById(R.id.addToEventBox);
			cb.setId(i);
					
			//listener when clicking checkbox
			cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton view, boolean isChecked)
				{
					String text = groupNameButton.getLayout()
							.getText().toString();
				
					if(cb.isChecked())
					{
						added.put(view.getId(), text);
						
						System.out.println("Added size: "+added.size());
					}
					else
					{
						added.remove(view.getId());
						
						System.out.println("Added size: "+added.size());
					}
				}
			});
			
			Entry thisEntry = (Entry) iterator.next();
			groupNameButton.setText(thisEntry.getValue().toString());
			groupNameButton.setId(i);
			rowView.setId(i);
			membersToAdd.addView(rowView);	
		}
	}
	
	private void initActionBar()
	{
		ActionBar ab = getSupportActionBar();
		ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		ab.setCustomView(R.layout.actionbar);
		ab.setDisplayHomeAsUpEnabled(false);
		TextView actionBarTitle = (TextView) findViewById(R.id.actionbarTitleTextView);
		actionBarTitle.setText("Add Groups");
	}
	
	//onClick for Confirm add groups button
	public void addGroupsButton(View view)
	{		
		/*
		//loop through list of added to add all the groups to the event
		int size = added.size();
		System.out.println("Total count of groups to process: "+size);
		for(int i = 0; i < size; i++) 
		{
			System.out.println("adding group #"+i+"/"+added.size());
			
			//get the groups's g_id by matching indexes from added list with indexes from allGroupslist.
			int key = added.keyAt(i);
			Iterator it1 = allGroups.entrySet().iterator();
			for(int k=0; k<key; k++)
			{
				//skip over iterations until arriving at key
				it1.next();
			}
			Map.Entry pairs = (Map.Entry)it1.next();
			
			//grab the gid of group to add
			String groupsgid = (String) pairs.getKey();
				
			System.out.println("adding group: "+groupsgid);		
			
			//initiate add of group
			new EventAddGroupTask().execute("http://68.59.162.183/"
					+ "android_connect/add_eventmember.php", groupsgid, email, e_id);
		}
		*/			
	}
	
	//aSynch task to add individual group to event.
	private class EventAddGroupTask extends AsyncTask<String,Void,String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			
			int tmpid = Integer.parseInt(urls[1]);
			
			Group group = new Group(tmpid);
			group.fetchMembers();
			
			Map<String, String> allMembers = new HashMap<String, String>();   //holds list of all current groups
			allMembers = group.getUsers();
			Set<String> keys = allMembers.keySet();
			
			System.out.println("Number of members to invite for this group: "+allMembers.size());
			
			//loop through list of allMembers and attempt invite of each one
			Iterator iterator = keys.iterator();
			
			//setup for each group
			while(iterator.hasNext())
			{
				String memberToAdd = iterator.next().toString();
				//initiate add of group member
				new EventAddMemberTask().execute("http://68.59.162.183/"
						+ "android_connect/add_eventmember.php", memberToAdd, email, e_id);
			}
			return "";
		}

		@Override
		protected void onPostExecute(String result)
		{
			
		}		
	}
	
	//aSynch task to add individual group to event.
		private class EventAddMemberTask extends AsyncTask<String,Void,String>
		{
			@Override
			protected String doInBackground(String... urls)
			{
	
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair("email", urls[1]));
					nameValuePairs.add(new BasicNameValuePair("sender", urls[2]));
					nameValuePairs.add(new BasicNameValuePair("e_id", urls[3]));

					//pass url and nameValuePairs off to GLOBAL to do the JSON call.  Code continues at onPostExecute when JSON returns.
					return GLOBAL.readJSONFeed(urls[0], nameValuePairs);
				
			}

			protected void onPostExecute(String result)
			{
				try
				{
					JSONObject jsonObject = new JSONObject(result);

					// group has been successfully added
					if (jsonObject.getString("success").toString().equals("1"))
					{
						//all working correctly, continue to next user or finish.
					} 
					else if (jsonObject.getString("success").toString().equals("0"))
					{	
						//a particular group was unable to be added to database for some reason...
						//Don't tell the user!
					}
				} catch (Exception e)
				{
					Log.d("readJSONFeed", e.getLocalizedMessage());
				}
			}		
		}
	
	//does something
	public void startParentActivity(View view)
	{
		Bundle extras = getIntent().getExtras();

		String className = extras.getString("ParentClassName");
		Intent newIntent = null;
		try
		{
			newIntent = new Intent(this, Class.forName("cs460.grouple.grouple."
					+ className));
			if (extras.getString("ParentEmail") != null)
			{
				newIntent.putExtra("email", extras.getString("ParentEmail"));
			}
			// newIntent.putExtra("email", extras.getString("email"));
			System.out.println("delete");
			// newIntent.putExtra("ParentEmail", extras.getString("email"));
			newIntent.putExtra("ParentClassName", "EventAddGroupsActivity");
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		newIntent.putExtra("up", "true");
		startActivity(newIntent);
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
			intent.putExtra("ParentClassName", "GroupCreateActivity");
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
}
