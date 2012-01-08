package org.za.hem.ipsec_tools.peer;

import org.za.hem.ipsec_tools.R;

import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.widget.ImageView;

public class StatePreference extends Preference {
	
	private ImageView mIconView;
	private int mState;

	public StatePreference(Context context) {
		super(context);
		mState = 0;
	}
	
	public void setIconLevel(int level) {
		mState = level;
		updateState();
	}
	
	public int getIconLevel() {
		return mState;
	}

	protected void onBindView (View view) {
		mIconView = (ImageView)view.findViewById(R.id.icon);
		updateState();
		super.onBindView(view);
	}
	
	protected void updateState() {
		if (mIconView != null) {
			mIconView.getDrawable().setLevel(mState);
		}
	}
}
