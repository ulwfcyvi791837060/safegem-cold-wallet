package com.bankledger.safecold.ui.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.bankledger.safecold.R;
import com.bankledger.safecold.listview.adapter.CommonAdapter;
import com.bankledger.safecold.listview.adapter.ViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * 公用选择组件
 * 
 * @author zm
 *
 */
public class CommonSelectWidget extends CommonTextWidget implements OnClickListener {

	private TYPE selectT = TYPE.TYPE_NONE;
	private CharSequence title;
	private List<Integer> selectIds;
	private ListView listView;
	private CommonAdapter<String> adapter;
	private OnSelectListener listener;
	private CommonDialog dialog;

	public CommonSelectWidget(Context context) {
		super(context);
	}

	public CommonSelectWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void createView() {
		super.createView();
		setOnClickListener(this);
	}

	public void setData(List<String> data) {
		List<Integer> selectIds = new ArrayList<Integer>();
		this.setData(data, selectIds, TYPE.TYPE_NONE);
	}

	public void setData(List<String> data, int defaultSelect) {
		List<Integer> selectIds = new ArrayList<Integer>();
		selectIds.add(defaultSelect);
		this.setData(data, selectIds, TYPE.TYPE_SINGLE);
	}

	public void setData(List<String> data, List<Integer> defaultSelect) {
		this.setData(data, defaultSelect, TYPE.TYPE_MUIL);
	}

	public void setData(List<String> data, TYPE t) {
		List<Integer> selectIds = new ArrayList<Integer>();
		this.setData(data, selectIds, t);
	}

	private void setData(List<String> data, List<Integer> defaultSelect, TYPE t) {
		this.selectIds = defaultSelect;
		this.selectT = t;
		adapter = new CommonAdapter<String>(getContext(), R.layout.common_select_item, data) {
			@Override
			protected void convert(ViewHolder viewHolder, String item, int position) {
				CheckBox common_check = viewHolder.findViewById(R.id.common_check);
				if(getSelectType() == TYPE.TYPE_NONE){
					common_check.setVisibility(View.GONE);
				} else {
					common_check.setVisibility(View.VISIBLE);
				}
				final Integer location = (Integer) position;
				if (selectIds.contains(location)) {
					common_check.setChecked(true);
				} else {
					common_check.setChecked(false);
				}
				TextView common_text = viewHolder.findViewById(R.id.common_text);
				common_text.setText(item);
				viewHolder.getView().setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!selectIds.contains(location)) {
							if (getSelectType() != TYPE.TYPE_MUIL) {
								selectIds.clear();
							}
							selectIds.add(location);
						} else {
							selectIds.remove(location);
						}
						adapter.notifyDataSetChanged();
						if(getSelectType() == TYPE.TYPE_NONE){
							dialog.dismiss();
							setRightText();
						}
					}
				});
			}
		};
		listView = new ListView(getContext());
		listView.setAdapter(adapter);
		setRightText();
	}

	public void setTitle(CharSequence text){
		title = text;
	}

	private void setRightText() {
		if (selectIds != null && selectIds.size() > 0) {
			StringBuilder s = new StringBuilder();
			for (Integer location : selectIds) {
				s.append(adapter.getData().get(location) + ",");
			}
			setRightText(s.substring(0, s.length() - 1));
			if (listener != null) {
				listener.onSelect(selectT, selectIds);
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (listView == null) return;
		if(dialog!=null && dialog.isShowing()) return;
		dialog = new CommonDialog(getContext());
		if (TextUtils.isEmpty(title)) {
			title = getContext().getString(R.string.tip);
		}
		dialog.setTitle(title);
		dialog.setMiddleView(listView);
		dialog.setOnClickConfirmListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setRightText();
			}
		});
		if(getSelectType()== TYPE.TYPE_NONE){
			dialog.setDisplayTopEnable(false);
			dialog.setDisplayBottomEnable(false);
		}
		dialog.show();
	}

	public TYPE getSelectType() {
		return selectT;
	}

	public List<Integer> getSelectIds() {
		return selectIds;
	}

	public enum TYPE {
		TYPE_NONE, TYPE_SINGLE, TYPE_MUIL
	}

	public interface OnSelectListener {
		public void onSelect(TYPE t, List<Integer> selectIds);
	}

	public void setOnSelectListener(OnSelectListener listener) {
		this.listener = listener;
	}

}
