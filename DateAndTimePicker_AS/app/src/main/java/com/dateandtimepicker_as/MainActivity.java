package com.dateandtimepicker_as;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;


public class MainActivity extends Activity{

    private int mHours,mMins,mSecs;
    private TextView mtv_date,mtv_time;
    private int mYear;
    private int mMonth;
    private int mDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
    }

    public void initLayout(){
        mtv_date=(TextView)findViewById(R.id.tv_date);
        mtv_time=(TextView)findViewById(R.id.tv_time);
        Button btn_date_picker=(Button)findViewById(R.id.btn_date_picker);
        Button btn_time_picker=(Button)findViewById(R.id.btn_time_picker);

        btn_date_picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                mYear = calendar.get(Calendar.YEAR);
                mMonth = calendar.get(Calendar.MONTH);
                mDay = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                mtv_date.setText(dayOfMonth + "-"+ (monthOfYear + 1) + "-" + year);
                                Toast.makeText(getBaseContext(), dayOfMonth + "-" + (monthOfYear + 1) + "-" + year, Toast.LENGTH_SHORT).show();
                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        btn_time_picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                mHours = calendar.get(Calendar.HOUR);
                mMins = calendar.get(Calendar.MINUTE);
                mSecs = calendar.get(Calendar.SECOND);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                mtv_time.setText(hourOfDay + ":" + minute);
                                Toast.makeText(getBaseContext(),hourOfDay + ":" + minute+view,Toast.LENGTH_SHORT).show();
                            }
                        }, mHours, mMins, false);
                timePickerDialog.show();
            }
        });

    }

}
