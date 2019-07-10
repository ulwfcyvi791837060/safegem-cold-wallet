package com.bankledger.safecold.ui.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bankledger.safecold.R;

/**
 * 公用对话框
 * 
 * @author zm
 *
 */
public class CommonDialogFragment extends DialogFragment {

	private View rootView;
	private TextView common_tv_title;
	private TextView common_tv_context;
	private ImageView common_img_close;
	private Button common_btn_cancel;
	private Button common_btn_confirm;
	private OnActivityListener onActivityListener;
	private View.OnClickListener onConfirmListener, onCancelListener, onCloseListener;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		rootView =  inflater.inflate(R.layout.common_dialog, container);
		initView();
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(onActivityListener != null){
			onActivityListener.onActivityCreated(savedInstanceState);
		}
	}

	private void initView() {
		common_tv_title = (TextView) findViewById(R.id.common_tv_title);
		common_tv_context = (TextView) findViewById(R.id.common_tv_content);
		common_img_close = (ImageView) findViewById(R.id.common_img_close);
		common_img_close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onCloseListener != null) {
					onCloseListener.onClick(v);
				}
				removeMiddleView();
				removeBottomView();
				dismiss();
			}
		});
		common_btn_cancel = (Button) findViewById(R.id.common_btn_cancel);
		common_btn_cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onCancelListener != null) {
					onCancelListener.onClick(v);
				}
				removeMiddleView();
				removeBottomView();
				dismiss();
			}
		});
		common_btn_confirm = (Button) findViewById(R.id.common_btn_confirm);
		common_btn_confirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onConfirmListener != null) {
					onConfirmListener.onClick(v);
				}
				removeMiddleView();
				removeBottomView();
				dismiss();
			}
		});

	}

	public void setTitle(CharSequence text) {
		common_tv_title.setText(text);
	}

	public void setContentText(CharSequence text) {
		common_tv_context.setText(text);
	}

	public void setOnClickCancelListener(CharSequence text, View.OnClickListener listener) {
		this.setOnClickCancelListener(listener);
		common_btn_cancel.setText(text);
	}

	public void setOnClickCancelListener(View.OnClickListener listener) {
		findViewById(R.id.common_ll_bottom).setVisibility(View.VISIBLE);
		common_btn_cancel.setVisibility(View.VISIBLE);
		if(common_btn_confirm.getVisibility() == View.VISIBLE){
			common_btn_cancel.setBackgroundResource(R.drawable.dialog_bottom_left_selector);
			common_btn_confirm.setBackgroundResource(R.drawable.dialog_bottom_right_selector);
		}else{
			common_btn_cancel.setBackgroundResource(R.drawable.dialog_bottom_selector);
		}
		this.onCancelListener = listener;
	}

	public void setOnClickConfirmListener(CharSequence text, View.OnClickListener listener) {
		this.setOnClickConfirmListener(listener);
		common_btn_confirm.setText(text);
	}

	public void setOnClickConfirmListener(View.OnClickListener listener) {
		findViewById(R.id.common_ll_bottom).setVisibility(View.VISIBLE);
		common_btn_confirm.setVisibility(View.VISIBLE);
		if(common_btn_cancel.getVisibility() == View.VISIBLE){
			common_btn_cancel.setBackgroundResource(R.drawable.dialog_bottom_left_selector);
			common_btn_confirm.setBackgroundResource(R.drawable.dialog_bottom_right_selector);
		}else{
			common_btn_confirm.setBackgroundResource(R.drawable.dialog_bottom_selector);
		}
		this.onConfirmListener = listener;
	}

	public void setOnClickCloseListener(View.OnClickListener listener) {
		this.onCloseListener = listener;
	}

	public void setMiddleView(View v) {
		setView(R.id.common_ll_middle, v);
	}

	public void setMiddleView(int layoutResId) {
		setView(R.id.common_ll_middle, layoutResId);
	}

	public void setBootomView(View v) {
		findViewById(R.id.common_ll_bottom).setVisibility(View.VISIBLE);
		setBottomBgRes(R.drawable.dialog_bottom_selector);
		setView(R.id.common_ll_bottom, v);
	}

	public void setBootomView(int layoutResId) {
		findViewById(R.id.common_ll_bottom).setVisibility(View.VISIBLE);
		setBottomBgRes(R.drawable.dialog_bottom_selector);
		setView(R.id.common_ll_bottom, layoutResId);
	}

	private void setView(int rootId, View v) {
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		LinearLayout ll = (LinearLayout) findViewById(rootId);
		ll.setVisibility(View.VISIBLE);
		ll.removeAllViews();
		ll.addView(v, lp);
	}

	private void setView(int rootId, int layoutId) {
		LinearLayout ll = (LinearLayout) findViewById(rootId);
		ll.setVisibility(View.VISIBLE);
		ll.removeAllViews();
		getActivity().getLayoutInflater().inflate(layoutId, ll);
	}

	private void removeMiddleView() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.common_ll_middle);
		ll.removeAllViews();
	}

	private void removeBottomView() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.common_ll_bottom);
		ll.removeAllViews();
	}

	public void setDisplayTopEnable(boolean enable){
		if(enable){
			findViewById(R.id.common_rl_top).setVisibility(View.VISIBLE);
		}else{
			findViewById(R.id.common_rl_top).setVisibility(View.GONE);
		}
	}

	public void setDisplayMiddleEnable(boolean enable){
		if(enable){
			findViewById(R.id.common_ll_middle).setVisibility(View.VISIBLE);
		}else{
			findViewById(R.id.common_ll_middle).setVisibility(View.GONE);
		}
	}

	public void setDisplayBottomEnable(boolean enable){
		if(enable){
			findViewById(R.id.common_ll_bottom).setVisibility(View.VISIBLE);
		}else{
			findViewById(R.id.common_ll_bottom).setVisibility(View.GONE);
		}
	}

	public void setTopBgRes(int resId){
		findViewById(R.id.common_rl_top).setBackgroundResource(resId);
	}

	public void setMiddleBgRes(int resId){
		findViewById(R.id.common_ll_middle).setBackgroundResource(resId);
	}

	public void setBottomBgRes(int resId){
		findViewById(R.id.common_ll_bottom).setBackgroundResource(resId);
	}

	public View findViewById(int id){
		return rootView.findViewById(id);
	}

	public void show(FragmentManager manager) {
		try {
			show(manager, "dialog");
		} catch (Exception e) {
		}
	}

	@Override
	public void dismiss() {
		try {
			super.dismiss();
		} catch (Exception e) {
		}
	}

	public interface OnActivityListener{
		void onActivityCreated(Bundle savedInstanceState);
		void onDestroyView();
	}

	public void setOnActivityListener(OnActivityListener listener){
		this.onActivityListener = listener;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if(onActivityListener != null){
			onActivityListener.onDestroyView();
		}
	}
}
