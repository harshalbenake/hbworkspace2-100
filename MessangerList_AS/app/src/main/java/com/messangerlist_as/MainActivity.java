package com.messangerlist_as;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends Activity {
    private MyCustomAdapter mAdapter;
    private EditText editText;
    private ListView listView;
    private ArrayList<DataPojo> mData = new ArrayList<DataPojo>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText=(EditText)findViewById(R.id.editText);
        Button button1=(Button)findViewById(R.id.button1);
        Button button2=(Button)findViewById(R.id.button2);
        listView=(ListView)findViewById(R.id.listView);

        mAdapter = new MyCustomAdapter(mData);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strEditTextValue=editText.getText().toString();
                if(!TextUtils.isEmpty(strEditTextValue)) {
                    DataPojo dataPojo=new DataPojo();
                    dataPojo.strValue=strEditTextValue;
                    dataPojo.flag=true;
                    mData.add(dataPojo);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strEditTextValue=editText.getText().toString();
                if(!TextUtils.isEmpty(strEditTextValue)) {
                    DataPojo dataPojo=new DataPojo();
                    dataPojo.strValue=strEditTextValue;
                    dataPojo.flag=false;
                    mData.add(dataPojo);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        listView.setAdapter(mAdapter);
    }

    private class MyCustomAdapter extends BaseAdapter {

        private static final int ITEM_TYPE_ONE = 1;
        private static final int ITEM_TYPE_TWO = 2;
        private static final int ITEM_TYPE_MAX_COUNT = ITEM_TYPE_ONE+ITEM_TYPE_TWO+1;

        private ArrayList<DataPojo> mData;
        private LayoutInflater mInflater;

        public MyCustomAdapter(ArrayList<DataPojo> data) {
            this.mData=data;
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getItemViewType(int position) {
            int type;
            DataPojo dataPojo=mData.get(position);
            if(dataPojo.flag){
                type=ITEM_TYPE_ONE;
            }
            else {
                type=ITEM_TYPE_TWO;
            }
            return type;
        }

        @Override
        public int getViewTypeCount() {
            return ITEM_TYPE_MAX_COUNT;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                holder = new ViewHolder();
                switch (type) {
                    case ITEM_TYPE_ONE:
                        convertView = mInflater.inflate(R.layout.item_view_one,  null);
                        holder.textView1 = (TextView)convertView.findViewById(R.id.textView1);
                        holder.imageView=(ImageView)convertView.findViewById(R.id.imageView1);
                        break;
                    case ITEM_TYPE_TWO:
                        convertView = mInflater.inflate(R.layout.item_view_two, null);
                        holder.textView1 = (TextView)convertView.findViewById(R.id.textView1);
                        holder.imageView=(ImageView)convertView.findViewById(R.id.imageView1);
                        break;

                }
                convertView.setTag(holder);
            }

                holder = (ViewHolder)convertView.getTag();

            DataPojo dataPojo=mData.get(position);
            holder.textView1.setText(dataPojo.strValue);
            return convertView;
        }
    }

    public static class ViewHolder {
        public TextView textView1;
        public ImageView imageView;
    }

    public class DataPojo{
        public String strValue;
        public boolean flag;
    }
}
