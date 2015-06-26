package sra.keyboard;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class Main extends Activity implements OnTouchListener, OnClickListener,
		OnFocusChangeListener {
	private EditText mEt, mEt1; // Edit Text boxes
	private Button mBSpace, mBdone, mBack, mBChange, mNum;
	private RelativeLayout mLayout, mKLayout;
	private boolean isEdit = false, isEdit1 = false;
	private String mUpper = "upper", mLower = "lower";
	private int w, mWindowWidth;
	private String sL[] = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
			"k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w",
			"x", "y", "z", "ç", "à", "é", "è", "û", "î" };
	private String cL[] = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
			"K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
			"X", "Y", "Z", "ç", "à", "é", "è", "û", "î" };
	private String nS[] = { "!", ")", "'", "#", "3", "$", "%", "&", "8", "*",
			"?", "/", "+", "-", "9", "0", "1", "4", "@", "5", "7", "(", "2",
			"\"", "6", "_", "=", "]", "[", "<", ">", "|" };
	private Button mB[] = new Button[32];

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.main);
			// adjusting key regarding window sizes
			setKeys();
			setFrow();
			setSrow();
			setTrow();
			setForow();
			mEt = (EditText) findViewById(R.id.xEt);
			mEt.setOnTouchListener(this);
			mEt.setOnFocusChangeListener(this);
			mEt1 = (EditText) findViewById(R.id.et1);

			mEt1.setOnTouchListener(this);
			mEt1.setOnFocusChangeListener(this);
			mEt.setOnClickListener(this);
			mEt1.setOnClickListener(this);
			mLayout = (RelativeLayout) findViewById(R.id.xK1);
			mKLayout = (RelativeLayout) findViewById(R.id.xKeyBoard);

		} catch (Exception e) {
			Log.w(getClass().getName(), e.toString());
		}

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == mEt) {
			hideDefaultKeyboard();
			enableKeyboard();

		}
		if (v == mEt1) {
			hideDefaultKeyboard();
			enableKeyboard();

		}
		return true;
	}

	@Override
	public void onClick(View v) {

		if (v == mBChange) {

			if (mBChange.getTag().equals(mUpper)) {
				changeSmallLetters();
				changeSmallTags();
			} else if (mBChange.getTag().equals(mLower)) {
				changeCapitalLetters();
				changeCapitalTags();
			}

		} else if (v != mBdone && v != mBack && v != mBChange && v != mNum) {
			addText(v);

		} else if (v == mBdone) {

			disableKeyboard();

		} else if (v == mBack) {
			isBack(v);
		} else if (v == mNum) {
			String nTag = (String) mNum.getTag();
			if (nTag.equals("num")) {
				changeSyNuLetters();
				changeSyNuTags();
				mBChange.setVisibility(Button.INVISIBLE);

			}
			if (nTag.equals("ABC")) {
				changeCapitalLetters();
				changeCapitalTags();
			}

		}

	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (v == mEt && hasFocus == true) {
			isEdit = true;
			isEdit1 = false;

		} else if (v == mEt1 && hasFocus == true) {
			isEdit = false;
			isEdit1 = true;

		}

	}

	private void addText(View v) {
		if (isEdit == true) {
			String b = "";
			b = (String) v.getTag();
			if (b != null) {
				// adding text in Edittext
				mEt.append(b);

			}
		}

		if (isEdit1 == true) {
			String b = "";
			b = (String) v.getTag();
			if (b != null) {
				// adding text in Edittext
				mEt1.append(b);

			}

		}

	}

	private void isBack(View v) {
		if (isEdit == true) {
			CharSequence cc = mEt.getText();
			if (cc != null && cc.length() > 0) {
				{
					mEt.setText("");
					mEt.append(cc.subSequence(0, cc.length() - 1));
				}

			}
		}

		if (isEdit1 == true) {
			CharSequence cc = mEt1.getText();
			if (cc != null && cc.length() > 0) {
				{
					mEt1.setText("");
					mEt1.append(cc.subSequence(0, cc.length() - 1));
				}
			}
		} 
	}
	private void changeSmallLetters() {
		mBChange.setVisibility(Button.VISIBLE);
		for (int i = 0; i < sL.length; i++)
			mB[i].setText(sL[i]);
		mNum.setTag("12#");
	}
	private void changeSmallTags() {
		for (int i = 0; i < sL.length; i++)
			mB[i].setTag(sL[i]);
		mBChange.setTag("lower");
		mNum.setTag("num");
	}
	private void changeCapitalLetters() {
		mBChange.setVisibility(Button.VISIBLE);
		for (int i = 0; i < cL.length; i++)
			mB[i].setText(cL[i]);
		mBChange.setTag("upper");
		mNum.setText("12#");

	}

	private void changeCapitalTags() {
		for (int i = 0; i < cL.length; i++)
			mB[i].setTag(cL[i]);
		mNum.setTag("num");

	}

	private void changeSyNuLetters() {

		for (int i = 0; i < nS.length; i++)
			mB[i].setText(nS[i]);
		mNum.setText("ABC");
	}

	private void changeSyNuTags() {
		for (int i = 0; i < nS.length; i++)
			mB[i].setTag(nS[i]);
		mNum.setTag("ABC");
	}

	// enabling customized keyboard
	private void enableKeyboard() {

		mLayout.setVisibility(RelativeLayout.VISIBLE);
		mKLayout.setVisibility(RelativeLayout.VISIBLE);

	}

	// Disable customized keyboard
	private void disableKeyboard() {
		mLayout.setVisibility(RelativeLayout.INVISIBLE);
		mKLayout.setVisibility(RelativeLayout.INVISIBLE);

	}

	private void hideDefaultKeyboard() {
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

	}

	private void setFrow() {
		w = (mWindowWidth / 13);
		w = w - 15;
		mB[16].setWidth(w);
		mB[22].setWidth(w + 3);
		mB[4].setWidth(w);
		mB[17].setWidth(w);
		mB[19].setWidth(w);
		mB[24].setWidth(w);
		mB[20].setWidth(w);
		mB[8].setWidth(w);
		mB[14].setWidth(w);
		mB[15].setWidth(w);
		mB[16].setHeight(50);
		mB[22].setHeight(50);
		mB[4].setHeight(50);
		mB[17].setHeight(50);
		mB[19].setHeight(50);
		mB[24].setHeight(50);
		mB[20].setHeight(50);
		mB[8].setHeight(50);
		mB[14].setHeight(50);
		mB[15].setHeight(50);

	}

	private void setSrow() {
		w = (mWindowWidth / 10);
		mB[0].setWidth(w);
		mB[18].setWidth(w);
		mB[3].setWidth(w);
		mB[5].setWidth(w);
		mB[6].setWidth(w);
		mB[7].setWidth(w);
		mB[26].setWidth(w);
		mB[9].setWidth(w);
		mB[10].setWidth(w);
		mB[11].setWidth(w);
		mB[26].setWidth(w);

		mB[0].setHeight(50);
		mB[18].setHeight(50);
		mB[3].setHeight(50);
		mB[5].setHeight(50);
		mB[6].setHeight(50);
		mB[7].setHeight(50);
		mB[9].setHeight(50);
		mB[10].setHeight(50);
		mB[11].setHeight(50);
		mB[26].setHeight(50);
	}

	private void setTrow() {
		w = (mWindowWidth / 12);
		mB[25].setWidth(w);
		mB[23].setWidth(w);
		mB[2].setWidth(w);
		mB[21].setWidth(w);
		mB[1].setWidth(w);
		mB[13].setWidth(w);
		mB[12].setWidth(w);
		mB[27].setWidth(w);
		mB[28].setWidth(w);
		mBack.setWidth(w);

		mB[25].setHeight(50);
		mB[23].setHeight(50);
		mB[2].setHeight(50);
		mB[21].setHeight(50);
		mB[1].setHeight(50);
		mB[13].setHeight(50);
		mB[12].setHeight(50);
		mB[27].setHeight(50);
		mB[28].setHeight(50);
		mBack.setHeight(50);

	}

	private void setForow() {
		w = (mWindowWidth / 10);
		mBSpace.setWidth(w * 4);
		mBSpace.setHeight(50);
		mB[29].setWidth(w);
		mB[29].setHeight(50);

		mB[30].setWidth(w);
		mB[30].setHeight(50);

		mB[31].setHeight(50);
		mB[31].setWidth(w);
		mBdone.setWidth(w + (w / 1));
		mBdone.setHeight(50);

	}

	private void setKeys() {
		mWindowWidth = getWindowManager().getDefaultDisplay().getWidth(); // getting
		// window
		// height
		// getting ids from xml files
		mB[0] = (Button) findViewById(R.id.xA);
		mB[1] = (Button) findViewById(R.id.xB);
		mB[2] = (Button) findViewById(R.id.xC);
		mB[3] = (Button) findViewById(R.id.xD);
		mB[4] = (Button) findViewById(R.id.xE);
		mB[5] = (Button) findViewById(R.id.xF);
		mB[6] = (Button) findViewById(R.id.xG);
		mB[7] = (Button) findViewById(R.id.xH);
		mB[8] = (Button) findViewById(R.id.xI);
		mB[9] = (Button) findViewById(R.id.xJ);
		mB[10] = (Button) findViewById(R.id.xK);
		mB[11] = (Button) findViewById(R.id.xL);
		mB[12] = (Button) findViewById(R.id.xM);
		mB[13] = (Button) findViewById(R.id.xN);
		mB[14] = (Button) findViewById(R.id.xO);
		mB[15] = (Button) findViewById(R.id.xP);
		mB[16] = (Button) findViewById(R.id.xQ);
		mB[17] = (Button) findViewById(R.id.xR);
		mB[18] = (Button) findViewById(R.id.xS);
		mB[19] = (Button) findViewById(R.id.xT);
		mB[20] = (Button) findViewById(R.id.xU);
		mB[21] = (Button) findViewById(R.id.xV);
		mB[22] = (Button) findViewById(R.id.xW);
		mB[23] = (Button) findViewById(R.id.xX);
		mB[24] = (Button) findViewById(R.id.xY);
		mB[25] = (Button) findViewById(R.id.xZ);
		mB[26] = (Button) findViewById(R.id.xS1);
		mB[27] = (Button) findViewById(R.id.xS2);
		mB[28] = (Button) findViewById(R.id.xS3);
		mB[29] = (Button) findViewById(R.id.xS4);
		mB[30] = (Button) findViewById(R.id.xS5);
		mB[31] = (Button) findViewById(R.id.xS6);
		mBSpace = (Button) findViewById(R.id.xSpace);
		mBdone = (Button) findViewById(R.id.xDone);
		mBChange = (Button) findViewById(R.id.xChange);
		mBack = (Button) findViewById(R.id.xBack);
		mNum = (Button) findViewById(R.id.xNum);
		for (int i = 0; i < mB.length; i++)
			mB[i].setOnClickListener(this);
		mBSpace.setOnClickListener(this);
		mBdone.setOnClickListener(this);
		mBack.setOnClickListener(this);
		mBChange.setOnClickListener(this);
		mNum.setOnClickListener(this);

	}

}