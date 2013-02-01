package com.sjz.cl.smspopup;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity implements SlipButton.OnChangedListener {
	private SlipButton button1;
	private SlipButton button2;
	private TextView text1;
	private TextView text2;
	private SharedPreferences mPreferences;
	private SharedPreferences.Editor edit;
	private boolean bService,bScreenon;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mPreferences = this.getSharedPreferences("popup", MODE_PRIVATE);
		bService = mPreferences.getBoolean("enable_service", true);
		bScreenon = mPreferences.getBoolean("enable_screenon", false);
		edit = mPreferences.edit();
		
		button1 = (SlipButton) findViewById(R.id.slipButton1);
		button1.SetOnChangedListener("enable_service", this);
		button2 = (SlipButton) findViewById(R.id.slipButton2);
		button2.SetOnChangedListener("enable_screenon", this);

		button1.setChecked(bService);
		button2.setChecked(bScreenon);
		
		text1 = (TextView) findViewById(R.id.tv_enable_service);
		text2 = (TextView) findViewById(R.id.tv_enable_screenon);
		text1.setText(getResources().getString(R.string.popup_enable_service));
		text2.setText(getResources().getString(R.string.popup_disable_screenon));
		
	}

	@Override
	public void OnChanged(String strName, boolean CheckState) {
		if (CheckState) {
			if ("enable_service".equals(strName)) {
				//第一按钮开
				text1.setText(getResources().getString(R.string.popup_enable_service));
				edit.putBoolean("enable_service", true);
				edit.commit();
			}else if("enable_screenon".equals(strName)){
				//第二按钮开
				text2.setText(getResources().getString(R.string.popup_enable_screenon));
				edit.putBoolean("enable_screenon", true);
				edit.commit();
			}
		} else {
			if ("enable_service".equals(strName)) {
				//第一按钮关
				text1.setText(getResources().getString(R.string.popup_disable_service));
				edit.putBoolean("enable_service", false);
				edit.commit();
			}else if("enable_screenon".equals(strName)){
				//第二按钮关
				text2.setText(getResources().getString(R.string.popup_disable_screenon));
				edit.putBoolean("enable_screenon", false);
				edit.commit();
			}
		}

	}

}
