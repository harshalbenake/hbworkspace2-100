package com.customspinner_as;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends Activity {
    ArrayList arrayListSpinner;
    String[] spinnerItemValues = {"Sound", "Vibrate"};
    private CustomSpinnerAdapter customSpinnerAdapter;
    private Spinner spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setPrompt("Select Auto - Send Notification Type");
        setAdapter();
    }

    /**
     * adapter for Custom Spinner
     */
    public void setAdapter() {
        arrayListSpinner = new ArrayList<String>();
        arrayListSpinner.addAll(Arrays.asList(spinnerItemValues));
        customSpinnerAdapter = new CustomSpinnerAdapter(getBaseContext(), R.layout.spinner_row,arrayListSpinner);
        customSpinnerAdapter.setDropDownViewResource(R.layout.createaccount_dropdown);
        spinner.setAdapter(customSpinnerAdapter);
    }

    /**
     * @author <B>harshalb</B>
     *         This is holder class custom spinner.
     */
    class ViewHoader {
        TextView tv_custom_spinner_text;
        TextView spinner_default_text;
    }

    /**
     * @author <B>harshalb</B>
     *         This is adapter class used for SecurityQuestions spinner.
     */
    public class CustomSpinnerAdapter extends ArrayAdapter<String> {

        private final ArrayList mArrayListData;
        Context context;
        LayoutInflater inflater;

        public CustomSpinnerAdapter(Context context, int textViewResourceId, ArrayList arrayListData) {
            super(context, textViewResourceId, arrayListData);
            this.context = context;
            this.mArrayListData = arrayListData;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (position == 0)
                return new View(getApplicationContext());
            else
                return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public String getItem(int position) {
            return super.getItem(position);
        }

        @Override
        public int getCount() {
            return mArrayListData.size() + 1;
        }

        @Override
        public int getPosition(String item) {
            return super.getPosition(item);
        }

        public View getCustomView(int position, View view, ViewGroup parent) {
            ViewHoader viewHoader;
            viewHoader = new ViewHoader();
            if (position == 0) {
                view = inflater.inflate(R.layout.spinner_row_default_selected, parent, false);
                viewHoader.spinner_default_text = (TextView) view.findViewById(R.id.spinner_default_text);
                viewHoader.spinner_default_text.setText(Html.fromHtml("Auto - Send Notification Type"));
                return view;

            } else {
                view = inflater.inflate(R.layout.spinner_row, parent, false);
                viewHoader.tv_custom_spinner_text = (TextView) view.findViewById(R.id.tv_custom_spinner_text);
                view.setTag(viewHoader);
                viewHoader.tv_custom_spinner_text.setText(mArrayListData.get(position-1)+"");
            }
            return view;
        }
    }

}