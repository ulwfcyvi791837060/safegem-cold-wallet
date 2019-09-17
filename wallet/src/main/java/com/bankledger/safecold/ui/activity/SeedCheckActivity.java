package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Collections;

public class SeedCheckActivity extends ToolbarBaseActivity {

    private ArrayList<String> words = new ArrayList<>(12);
    private ArrayList<String> rWords = new ArrayList<>(12);
    private ArrayList<Integer> tempPositions = new ArrayList<>(12);
    private int[] wordIds = new int[]{R.id.tv_seed01, R.id.tv_seed02, R.id.tv_seed03,
            R.id.tv_seed04, R.id.tv_seed05, R.id.tv_seed06, R.id.tv_seed07, R.id.tv_seed08,
            R.id.tv_seed09, R.id.tv_seed10, R.id.tv_seed11, R.id.tv_seed12};

    private int[] wordRIds = new int[]{R.id.tv_seed001, R.id.tv_seed002, R.id.tv_seed003,
            R.id.tv_seed004, R.id.tv_seed005, R.id.tv_seed006, R.id.tv_seed007, R.id.tv_seed008,
            R.id.tv_seed009, R.id.tv_seed010, R.id.tv_seed011, R.id.tv_seed012};
    private TextView[] tvWords = new TextView[wordIds.length];
    private TextView[] tvRWords = new TextView[wordRIds.length];

    @Override
    protected int setContentLayout() {
        return R.layout.activity_seed_check;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();

        setTitle(R.string.check_seed);
        for (int i = 0; i < wordIds.length; i++) {
            tvWords[i] = findViewById(wordIds[i]);
            final int finalI = i;
            tvWords[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!TextUtils.isEmpty(tvWords[finalI].getText())) {
                        if (finalI == tempPositions.size()-1) {
                            int lastPosition = tempPositions.get(tempPositions.size() - 1);
                            tempPositions.remove(tempPositions.size() - 1);
                            tvRWords[lastPosition].setBackgroundColor(getColor(android.R.color.transparent));
                            tvWords[finalI].setText(null);
                        }
                    }
                }
            });
        }

        for (int i = 0; i < wordRIds.length; i++) {
            tvRWords[i] = findViewById(wordRIds[i]);
            final int finalI = i;
            tvRWords[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!tempPositions.contains(finalI)) {
                        tvRWords[finalI].setBackgroundColor(getColor(R.color.dialog_line));
                        fillWords(finalI);
                    }
                }
            });
        }
    }

    @Override
    public void initData() {
        super.initData();
        Intent intent = getIntent();
        words.addAll(intent.getStringArrayListExtra(Constants.INTENT_KEY1));
        rWords.addAll(intent.getStringArrayListExtra(Constants.INTENT_KEY1));
        Collections.shuffle(rWords);

        for (int i = 0; i < rWords.size(); i++) {
            tvRWords[i].setText(rWords.get(i));
        }

    }

    private void fillWords(int position) {
        tempPositions.add(position);
        tvWords[tempPositions.size() - 1].setText(rWords.get(position));

        if (tempPositions.size() == 12) {
            for (int i = 0; i < 12; i++) {
                if (!rWords.get(tempPositions.get(i)).equals(words.get(i))) {
                    restore();
                    ToastUtil.showToast(R.string.check_seed_error);
                    return;
                }
            }
            Bundle args = new Bundle();
            args.putStringArrayList(Constants.INTENT_KEY1, words);
            go2Activity(CreatePasswordActivity.class, args);
            finish();
        }
    }

    private void restore() {
        tempPositions.clear();
        for (int i = 0; i < 12; i++) {
            tvRWords[i].setBackgroundColor(getColor(android.R.color.transparent));
            tvWords[i].setText(null);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
