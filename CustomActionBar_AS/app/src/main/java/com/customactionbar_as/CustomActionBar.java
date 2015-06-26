package com.customactionbar_as;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by <b>Harshal Benake</b> on 2/5/15.
 */
public class CustomActionBar{

    Context mContext;
    View mView;
    ImageView iv_action_bar_back;
    TextView tv_action_bar_title;

    public CustomActionBar(Context context,View view){
        this.mContext=context;
        this.mView=view;
        initLayout();
    }

    public void initLayout(){
        iv_action_bar_back=(ImageView)mView.findViewById(R.id.iv_action_bar_back);
        tv_action_bar_title=(TextView)mView.findViewById(R.id.tv_action_bar_title);
    }

    /**
     * set action bar Title
     * @param title
     */
    public void setTitle(String title){
        tv_action_bar_title.setText(title);
    }

    /**
     * set action bar TitleColor
     * @param titleColor
     */
    public void setTitleColor(int titleColor){
        tv_action_bar_title.setTextColor(titleColor);
    }

    /**
     * set action bar Gravity
     * @param gravity
     */
    public void setGravity(int gravity){
        tv_action_bar_title.setGravity(gravity);
    }

    public void setBackButtonOnClickListner(View.OnClickListener onClickListner){
        iv_action_bar_back.setOnClickListener(onClickListner);
    }
}
