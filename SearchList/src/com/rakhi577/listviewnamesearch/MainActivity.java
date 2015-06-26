package com.rakhi577.listviewnamesearch;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {

	private EditText et;
	private ListView lv;
	private String[] listview_names = {"aaa","bbb","ccc","ddd",
									   "eee","fff","ggg",
									   "hhh","abc"};
	
	private ArrayList<String> array_sort;
	int textlength=0;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		et 	= 	(EditText) findViewById(R.id.EditText01);
		lv	=	(ListView) findViewById(android.R.id.list);
		
		array_sort=new ArrayList<String> (Arrays.asList(listview_names));
		setListAdapter(new bsAdapter(this));


		et.addTextChangedListener(new TextWatcher()
		{
			public void afterTextChanged(Editable s)
			{
                  // Abstract Method of TextWatcher Interface.
			}
			public void beforeTextChanged(CharSequence s,
					int start, int count, int after)
			{
				// Abstract Method of TextWatcher Interface.
			}
			public void onTextChanged(CharSequence s,
					int start, int before, int count)
			{
				textlength = et.getText().length();
				array_sort.clear();
				for (int i = 0; i < listview_names.length; i++)
				{
					if (textlength <= listview_names[i].length())
					{
						/***
						 * If you want to highlight the countries which start with 
						 * entered letters then choose this block. 
						 * And comment the below If condition Block
						 */
						/*if(et.getText().toString().equalsIgnoreCase(
								(String)
								listview_names[i].subSequence(0,
										textlength)))
						{
							array_sort.add(listview_names[i]);
							image_sort.add(listview_images[i]);
						}*/
						
						/***
						 * If you choose the below block then it will act like a 
						 * Like operator in the Mysql
						 */
						
						if(listview_names[i].toLowerCase().contains(
								et.getText().toString().toLowerCase().trim()))
						{
							array_sort.add(listview_names[i]);
						}
                      }
				}
				AppendList(array_sort);
				}
			});
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0,
			                    View arg1, int position, long arg3)
			{
			   Toast.makeText(getApplicationContext(), array_sort.get(position),
					   Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public void AppendList(ArrayList<String> str)
	{
		setListAdapter(new bsAdapter(this));
	}
	
	public class bsAdapter extends BaseAdapter
    {
        Activity cntx;
        public bsAdapter(Activity context)
        {
            // TODO Auto-generated constructor stub
            this.cntx=context;

        }

        public int getCount()
        {
            // TODO Auto-generated method stub
            return array_sort.size();
        }

        public Object getItem(int position)
        {
            // TODO Auto-generated method stub
            return array_sort.get(position);
        }

        public long getItemId(int position)
        {
            // TODO Auto-generated method stub
            return array_sort.size();
        }

        public View getView(final int position, View convertView, ViewGroup parent)
        {
            View row=null;
            
            LayoutInflater inflater=cntx.getLayoutInflater();
            row=inflater.inflate(R.layout.search_list_item, null);
            
            TextView   tv	=	(TextView)	row.findViewById(R.id.title);
            
            tv.setText(array_sort.get(position));
            
        return row;
        }
    }
}