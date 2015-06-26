package com.example.hbdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity  implements SpellCheckerSessionListener { 
private static final int NOT_A_LENGTH = -1;
private TextView mMainView;
private SpellCheckerSession mScs;
private EditText editText1;

@Override
public void onCreate(Bundle savedInstanceState) {
   super.onCreate(savedInstanceState);
   setContentView(R.layout.activity_main);
   mMainView = (TextView)findViewById(R.id.main);
   editText1 = (EditText)findViewById(R.id.editText1);
}

@Override
public void onResume() {
   super.onResume();
   final TextServicesManager tsm = (TextServicesManager) getSystemService(
   Context.TEXT_SERVICES_MANAGER_SERVICE);
   mScs = tsm.newSpellCheckerSession(null, null, this, true);         
}

@Override
public void onPause() {
   super.onPause();
   if (mScs != null) {
      mScs.close();
   }
}

public void go(View view){
   Toast.makeText(getApplicationContext(), editText1.getText().toString(),	 
   Toast.LENGTH_SHORT).show();
   mScs.getSuggestions(new TextInfo(editText1.getText().toString()), 3);

}
@Override
public void onGetSuggestions(final SuggestionsInfo[] arg0) {
   final StringBuilder sb = new StringBuilder();

   for (int i = 0; i < arg0.length; ++i) {
      // Returned suggestions are contained in SuggestionsInfo
      final int len = arg0[i].getSuggestionsCount();
      sb.append('\n');
      for (int j = 0; j < len; ++j) {
         sb.append("," + arg0[i].getSuggestionAt(j));
    }
    sb.append(" (" + len + ")");
}
runOnUiThread(new Runnable() {

   public void run() {
      mMainView.append(sb.toString());
   }
});

}
@Override
public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] arg0) {
   // TODO Auto-generated method stub

}}
