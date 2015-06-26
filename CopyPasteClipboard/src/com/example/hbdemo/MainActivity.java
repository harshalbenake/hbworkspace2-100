package com.example.hbdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity {

   private ClipboardManager myClipboard;
   private ClipData myClip;
   private EditText copyField,pasteField;
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      myClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
      copyField = (EditText)findViewById(R.id.editText1);
      pasteField = (EditText)findViewById(R.id.editText2);

   }

   @SuppressLint("NewApi")
   public void copy(View view){
      String text = copyField.getText().toString();
      myClip = ClipData.newPlainText("text", text);
      myClipboard.setPrimaryClip(myClip);
      Toast.makeText(getApplicationContext(), "Text Copied", 
      Toast.LENGTH_SHORT).show();
   }

   @SuppressLint("NewApi")
   public void paste(View view){
      ClipData abc = myClipboard.getPrimaryClip();
      ClipData.Item item = abc.getItemAt(0);
      String text = item.getText().toString();
      pasteField.setText(text);
      Toast.makeText(getApplicationContext(), "Text Pasted", 
      Toast.LENGTH_SHORT).show();
   }

  
}