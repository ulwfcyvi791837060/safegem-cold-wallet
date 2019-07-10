package com.bankledger.safecold.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.R;
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.EditLengthInputFilter;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/8/10 09:23
 */
public class DialogUtil {

    public static void showEditDialog(Context context, int titleResource, int editHintResource, String defaultStr, int maxLength, OnClickListener listener) {
        showEditDialog(context, context.getString(titleResource), context.getString(editHintResource), defaultStr, -1, maxLength, listener);
    }

    public static void showEditDialog(Context context, int titleResource, int editHintResource, int maxLength, OnClickListener listener) {
        showEditDialog(context, context.getString(titleResource), context.getString(editHintResource), "", -1, maxLength, listener);
    }

    public static void showEditPasswordDialog(Context context, int titleResource, int editHintResource, OnClickListener listener) {
        showEditDialog(context, context.getString(titleResource), context.getString(editHintResource), "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, 32, listener);
    }

    private static void showEditDialog(Context context, String title, String editHint, String defaultStr, int inputType, int maxLength, final OnClickListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edittext, null);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        final CommonEditWidget cewAlias = view.findViewById(R.id.cew_content);
        cewAlias.setText(defaultStr);
        cewAlias.getEditText().setSelection(cewAlias.getText().length());
        if (maxLength > 0) {
            cewAlias.getEditText().setFilters(new InputFilter[]{new EditLengthInputFilter(maxLength)});
        }
        if (inputType > 0) {
            cewAlias.setInputType(inputType);
        }
        cewAlias.setHint(editHint);
        new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null)
                            listener.onClick(dialog, which, cewAlias.getText());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null)
                            listener.onClick(dialog, which, cewAlias.getText());
                    }
                })
                .show();
    }

    public interface OnClickListener {
        void onClick(DialogInterface dialog, int which, String content);
    }

    public static void showImageDialog(Context context, int resource, String content) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_qr_code, null, false);
        ImageView ivQrCode = view.findViewById(R.id.iv_qr_code);
        TextView tvContent = view.findViewById(R.id.tv_content);

        tvContent.setText(content);
        ivQrCode.setImageResource(resource);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .show();
        view.findViewById(R.id.iv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public static void showImageDialog(Context context, Bitmap bitmap, String content) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_qr_code, null, false);
        ImageView ivQrCode = view.findViewById(R.id.iv_qr_code);
        TextView tvContent = view.findViewById(R.id.tv_content);

        tvContent.setText(content);
        ivQrCode.setImageBitmap(bitmap);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .show();
        view.findViewById(R.id.iv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public static void showImageDialog(Context context, Bitmap bitmap, final String content, final OnClickListener listener, final OnClickListener colseListener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_qr_code, null, false);
        ImageView ivQrCode = view.findViewById(R.id.iv_qr_code);
        TextView tvContent = view.findViewById(R.id.tv_content);
        tvContent.setText(content);
        ivQrCode.setImageBitmap(bitmap);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null)
                            listener.onClick(dialog, which, content);
                    }
                }).show();
        view.findViewById(R.id.iv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (colseListener != null)
                    colseListener.onClick(dialog, 0, content);
            }
        });
    }

    public static void showTextDialog(Context context, int titleResource, int contentResource, OnClickListener listener) {
        showTextDialog(context, context.getString(titleResource), context.getString(contentResource), context.getString(R.string.confirm), false, listener);
    }

    public static void showTextDialogWithCancelButton(Context context, int titleResource, int contentResource, OnClickListener listener) {
        showTextDialog(context, context.getString(titleResource), context.getString(contentResource), null, true, listener);
    }

    public static void showTextDialogWithCancelButton(Context context, int titleResource, int contentResource, int subTextResource, OnClickListener listener) {
        showTextDialog(context, context.getString(titleResource), context.getString(contentResource), context.getString(subTextResource), true, listener);
    }

    public static void showTextDialog(Context context, String title, final String content, boolean showCancelButton, final OnClickListener listener) {
        showTextDialog(context, title, content, null, showCancelButton, listener);
    }

    public static void showTextDialog(Context context, String title, final String content, String subText, boolean showCancelButton, final OnClickListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_text, null, false);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        TextView tvContent = view.findViewById(R.id.tv_content);
        tvContent.setText(content);
        String submit = TextUtils.isEmpty(subText) ? context.getString(R.string.confirm) : subText;
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null)
                            listener.onClick(dialog, which, content);
                    }
                });
        if (showCancelButton) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (listener != null)
                        listener.onClick(dialog, which, content);
                }
            });
        }
        builder.show();
    }

    public static void showTextDialog(Context context, String title, final String content, final OnClickListener sureListener, final OnClickListener cancelListener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_text, null, false);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        TextView tvContent = view.findViewById(R.id.tv_content);
        tvContent.setText(content);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (sureListener != null)
                            sureListener.onClick(dialog, which, content);
                    }
                });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (cancelListener != null)
                    cancelListener.onClick(dialog, which, content);
            }
        });
        builder.show();
    }

    public static void showProtocolUpdateDilog(Context context, boolean isSelf) {
        showTextDialog(context, R.string.tip, isSelf ? R.string.hint_cold_wallet_update : R.string.hint_hot_wallet_update, null);
    }

    public static void showGuide(Context context, int title, Bitmap qrCode, int hint, final View.OnClickListener onNextClickListener) {
        showGuide(context, title, qrCode, hint, null, onNextClickListener);
    }

    public static void showGuide(Context context, int title, Bitmap qrCode, int hint, final View.OnClickListener onLastClickListener, final View.OnClickListener onNextClickListener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_guide, null, false);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        ImageView ivQrCode = view.findViewById(R.id.iv_qr_code);
        ivQrCode.setImageBitmap(qrCode);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        tvHint.setText(hint);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .show();
        View btLast = view.findViewById(R.id.bt_last);
        if (onLastClickListener == null) {
            btLast.setVisibility(View.GONE);
        } else {
            btLast.setVisibility(View.VISIBLE);
            btLast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onLastClickListener.onClick(v);
                    dialog.dismiss();
                }
            });
        }

        view.findViewById(R.id.bt_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextClickListener.onClick(v);
                dialog.dismiss();
            }
        });
    }
}
