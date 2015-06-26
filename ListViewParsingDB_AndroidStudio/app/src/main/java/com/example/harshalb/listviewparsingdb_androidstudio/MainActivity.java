package com.example.harshalb.listviewparsingdb_androidstudio;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends Activity {
    ListView listView;
    private String response;
    private String Name;
    private ArrayAdapter<String> listAdapter;
    ArrayList<String> arrayList=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        listView = (ListView) findViewById(R.id.listView);
        arrayList = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        Async_GetData async_getData = new Async_GetData();
        async_getData.execute("");
    }


public class Async_GetData extends AsyncTask<String,String,String>
{
    ArrayList<String> arrayList=null;
    @Override
    protected String doInBackground(String... params) {
        try {
            response=getData();
            JSONObject jsonObject=new JSONObject(response);
            JSONArray jsonArray=jsonObject.getJSONArray("contacts");
            JSONObject jsonObject1 = null;
            System.out.println(jsonArray);
            for (int i = 0; i <jsonArray.length(); i++) {
                jsonObject1 = jsonArray.getJSONObject(i);
                String Id= jsonObject1.getString("id");
                Name= jsonObject1.getString("name");
                JSONObject jsonObject2=jsonObject1.getJSONObject("phone");
                String Office=jsonObject2.getString("mobile");
                System.out.println("Id: "+Id);
                System.out.println("Name: "+Name);
                System.out.println("Office: "+Office);
                listAdapter.add(Name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        listView.setAdapter(listAdapter);
    }
}

    public String getData(){
        HttpResponse httpResponse;
        try {
            String url="http://api.androidhive.info/contacts/";
            HttpClient httpClient=new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity=httpResponse.getEntity();
            String response= EntityUtils.toString(httpEntity);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }
}
