package com.xq.fasterdialog.base;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xq.fasterdialog.FasterDialogInterface;
import com.xq.fasterdialog.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class BaseDialog<T extends BaseDialog> extends Dialog {

    protected static int STYLE_DEFAULT = R.style.BaseDialog;

    //上下文
    protected Context context;

    //根布局
    protected View rootView;

    //自定义属性
    protected int layoutId;
    protected int gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
    protected int width = WindowManager.LayoutParams.WRAP_CONTENT;
    protected int height = WindowManager.LayoutParams.WRAP_CONTENT;
    protected int maxWidth;
    protected int maxHeight;
    protected int x;
    protected int y;
    protected int animatStyle;
    protected int autoDismissTime;
    protected DialogImageLoder dialogImageLoder;
    protected Object tag;

    public BaseDialog(@NonNull Context context) {
        this(context, STYLE_DEFAULT);
    }

    public BaseDialog(@NonNull Context context, int themeResId) {
        super(context,themeResId);
        this.context = context;
        init();
    }

    @Deprecated
    protected BaseDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        this(context);
    }

    public static void setDefaultStyle(int style){
        STYLE_DEFAULT = style;
    }

    //重写此方法完成初始化工作
    protected void init() {

    }

    //AutoDismiss进度改变时的回调
    protected void onAutoDismissProgressChanged(int progress){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        if (rootView == null)
            rootView = getLayoutInflater().inflate(layoutId,null);
        window.setContentView(rootView);

        //设置弹窗位置
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.x= x;
        lp.y= y;
        window.setGravity(gravity);

        if (animatStyle != 0)
            window.setWindowAnimations(animatStyle);
    }

    @Override
    protected void onStart() {
        super.onStart();

        goneAllEmptyLayout();

        measure();
    }

    //如果一个布局中的所有子控件被隐藏，那么直接隐藏该布局
    private void goneAllEmptyLayout(){
        List<ViewGroup> list = getAllSomeView(rootView,ViewGroup.class);
        for (ViewGroup viewGroup : list)
        {
            boolean isGone =true;
            for (int i = 0; i < viewGroup.getChildCount(); i++)
            {
                if (viewGroup.getChildAt(i).getVisibility() == View.VISIBLE)
                    break;
                if (i == viewGroup.getChildCount()-1 && isGone)
                    viewGroup.setVisibility(View.GONE);
            }
        }
    }

    protected List getAllSomeView(View container,Class someView) {
        List allchildren = new ArrayList<>();
        if (container instanceof ViewGroup)
        {
            ViewGroup vp = (ViewGroup) container;
            for (int i = 0; i < vp.getChildCount(); i++)
            {
                View viewchild = vp.getChildAt(i);
                if (someView.isAssignableFrom(viewchild.getClass()))
                    allchildren.add(viewchild);
                //再次 调用本身（递归）
                allchildren.addAll(getAllSomeView(viewchild,someView));
            }
        }
        return allchildren;
    }

    //当Dialog需要动态调整宽高的时候，请调用此方法
    protected void measure() {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        rootView.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
        if (maxHeight > 0 && rootView.getMeasuredHeight() > maxHeight)
            lp.height = maxHeight;
        else
            lp.height = height;
        if (maxWidth > 0 && rootView.getMeasuredWidth() > maxWidth)
            lp.width = maxWidth;
        else
            lp.width = width;
    }



    //以下重写Dialog方法
    @Override
    public void show() {
        if (((Activity)context).isFinishing())
            return;

        super.show();

        if (autoDismissTime > 0)
            autoDismiss();
    }

    @Override
    public void dismiss() {
        if (((Activity)context).isFinishing())
            return;

        if (autoDismissTime > 0 && task != null)
            task.cancel(true);

        super.dismiss();
    }

    @Override
    public <T_VIEW extends View> T_VIEW findViewById(int id) {
        return rootView.findViewById(id);
    }

    protected AsyncTask task;
    protected void autoDismiss() {
        task = new AsyncTask<Object,Float,Void>(){

            @Override
            protected Void doInBackground(Object... objects) {
                int a = autoDismissTime/100;
                for (int i=a;i<autoDismissTime;i=i+a)
                {
                    publishProgress(i/(float)autoDismissTime);
                    try {
                        Thread.sleep(a);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if(isCancelled())
                    return;
                dismiss();
            }

            @Override
            protected void onProgressUpdate(Float... values) {
                if(isCancelled())
                    return;
                if (values[0]<=1)
                    onAutoDismissProgressChanged((int) (values[0]*100));
            }
        };
        task.execute();
    }

    //所有set
    public T setWidth(int width) {
        this.width = width;
        return (T) this;
    }

    public T setHeight(int height) {
        this.height = height;
        return (T) this;
    }

    public T setWidthPercent(float percent) {
        this.width = (int) (percent * ScreenUtils.getScreenW(context));
        return (T) this;
    }

    public T setHeightPercent(float percent) {
        this.height = (int) (percent * ScreenUtils.getScreenH(context));
        return (T) this;
    }

    public T setWidthWrap() {
        this.width = WindowManager.LayoutParams.WRAP_CONTENT;
        return (T) this;
    }

    public T setHeightWrap() {
        this.height = WindowManager.LayoutParams.WRAP_CONTENT;
        return (T) this;
    }

    public T setWidthMatch() {
        this.width = WindowManager.LayoutParams.MATCH_PARENT;
        return (T) this;
    }

    public T setHeightMatch() {
        this.height = WindowManager.LayoutParams.MATCH_PARENT;
        return (T) this;
    }

    public T setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return (T) this;
    }

    public T setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return (T) this;
    }

    public T setMaxWidthPercent(float percent) {
        this.maxWidth = (int) (percent * ScreenUtils.getScreenH(context));
        return (T) this;
    }

    public T setMaxHeightPercent(float percent) {
        this.maxHeight = (int) (percent * ScreenUtils.getScreenH(context));
        return (T) this;
    }

    public T setX(int x) {
        this.x = x;
        return (T) this;
    }

    public T setY(int y) {
        this.y = y;
        return (T) this;
    }

    public T setPopupFromBottom(){
        setWidthMatch();
        setAnimatStyle(R.style.Animation_Bottom);
        this.gravity = Gravity.BOTTOM;
        return (T) this;
    }

    public T setPopupFromTop(){
        setWidthMatch();
        setAnimatStyle(R.style.Animation_Top);
        this.gravity = Gravity.TOP;
        return (T) this;
    }

    public T setCustomView(int layoutId){
        this.layoutId = layoutId;
        return (T) this;
    }

    public T setDialogImageLoder(DialogImageLoder dialogImageLoder) {
        this.dialogImageLoder = dialogImageLoder;
        return (T) this;
    }

    public T setTag(Object tag) {
        this.tag = tag;
        return (T) this;
    }

    public T setAnimatStyle(int animatStyle) {
        this.animatStyle = animatStyle;
        return (T) this;
    }

    public T setAutoDismissTime(int autoDismissTime) {
        this.autoDismissTime = autoDismissTime;
        return (T) this;
    }

    public T setCancele(boolean cancel) {
        super.setCancelable(cancel);
        return (T) this;
    }

    public T setCancelOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
        return (T) this;
    }

    public T setCancelMsg(@Nullable Message msg) {
        super.setCancelMessage(msg);
        return (T) this;
    }

    public T setDismissMsg(@Nullable Message msg) {
        super.setDismissMessage(msg);
        return (T) this;
    }

    public T setCancelListener(@Nullable final OnDialogCancleListener listener) {
        super.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.onCancle(BaseDialog.this);
            }
        });
        return (T) this;
    }

    private List<OnDialogDismissListener> list_dismissListener = new LinkedList<>();
    public T addDismissListener(@Nullable OnDialogDismissListener listener) {
        list_dismissListener.add(listener);
        super.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                for (OnDialogDismissListener l : list_dismissListener)
                    l.onDismiss(BaseDialog.this);
            }
        });
        return (T) this;
    }

    private List<OnDialogShowListener> list_showListener = new LinkedList<>();
    public T addShowListener(@Nullable OnDialogShowListener listener) {
        list_showListener.add(listener);
        super.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                for(OnDialogShowListener l : list_showListener)
                    l.onShow(BaseDialog.this);
            }
        });
        return (T) this;
    }



    //所有get
    //如果在设置layoutId后没有show出来就调用此方法，那么getRootView将返回null
    public View getCustomView() {
        return rootView;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getAutoDismissTime() {
        return autoDismissTime;
    }

    public Object getTag() {
        return tag;
    }



    //便捷控件设置方法
    protected void setTextToView(TextView view, CharSequence text,int visibilityIfNot){
        if (view == null)
            return;

        if (TextUtils.isEmpty(text))
            view.setVisibility(visibilityIfNot);
        else
        {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }

    protected void setImageResourceToView(ImageView view, int id,int visibilityIfNot){
        if (view == null)
            return;

        if (id == 0)
            view.setVisibility(visibilityIfNot);
        else
        {
            view.setImageResource(id);
            view.setVisibility(View.VISIBLE);
        }
    }

    protected void setImageUrlToView(final ImageView view, final String url,int visibilityIfNot){
        if (view == null)
            return;

        if (TextUtils.isEmpty(url))
            view.setVisibility(visibilityIfNot);
        else
        {
            if (dialogImageLoder == null)
                FasterDialogInterface.getImageLoaderd().loadImage(context,view,url);
            else
                dialogImageLoder.loadImage(context,view,url);
            view.setVisibility(View.VISIBLE);
        }
    }

    protected void bindDialogClickListenerWithView(View view, final OnDialogClickListener listener, final boolean isDismiss){
        if (view == null)
            return;

        if (listener != null)
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(BaseDialog.this);
                    if (isDismiss)
                        dismiss();
                }
            });
    }



    //以下方法不建议使用了
    @Override
    @Deprecated
    public void setContentView(int layoutResID) throws IllegalAccessError {
        throw new IllegalAccessError(
                "setContentView() is not supported");
    }

    @Override
    @Deprecated
    public void setContentView(View view) throws IllegalAccessError {
        throw new IllegalAccessError(
                "setContentView() is not supported");
    }

    @Override
    @Deprecated
    public void setContentView(View view, @Nullable ViewGroup.LayoutParams params)
            throws IllegalAccessError {
        throw new IllegalAccessError(
                "setContentView() is not supported");
    }

    @Deprecated
    @Override
    public void setTitle(int titleId) {
        throw new IllegalAccessError(
                "setTitle() is not supported");
    }

    @Deprecated
    @Override
    public void setTitle(@Nullable CharSequence title) {
        throw new IllegalAccessError(
                "setTitle() is not supported");
    }

    @Deprecated
    @Override
    public void setCancelable(boolean flag) {
        throw new IllegalAccessError(
                "setCancelable() is not supported");
    }

    @Deprecated
    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        throw new IllegalAccessError(
                "setCanceledOnTouchOutside() is not supported");
    }

    @Deprecated
    @Override
    public void setCancelMessage(@Nullable Message msg) {
        throw new IllegalAccessError(
                "setCancelMessage() is not supported");
    }

    @Deprecated
    @Override
    public void setDismissMessage(@Nullable Message msg) {
        throw new IllegalAccessError(
                "setDismissMessage() is not supported");
    }

    @Deprecated
    @Override
    public void setOnCancelListener(@Nullable OnCancelListener listener) {
        throw new IllegalAccessError(
                "setOnCancelListener() is not supported");
    }

    @Deprecated
    @Override
    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        throw new IllegalAccessError(
                "setOnDismissListener() is not supported");
    }

    @Deprecated
    @Override
    public void setOnShowListener(@Nullable OnShowListener listener) {
        throw new IllegalAccessError(
                "setOnShowListener() is not supported");
    }

    @Deprecated
    @Override
    public void setOnKeyListener(@Nullable OnKeyListener onKeyListener) {
        throw new IllegalAccessError(
                "setOnKeyListener() is not supported");
    }

    //内部工具类或者监听
    public static interface OnDialogClickListener {
        public void onClick(BaseDialog dialog);
    }

    public static interface OnDialogShowListener {
        public void onShow(BaseDialog dialog);
    }

    public static interface OnDialogDismissListener {
        public void onDismiss(BaseDialog dialog);
    }

    public static interface OnDialogCancleListener {
        public void onCancle(BaseDialog dialog);
    }

    protected static class ScreenUtils {

        public static int dip2px(Context c, float dpValue) {
            final float scale = c.getResources().getDisplayMetrics().density;
            return (int) (dpValue * scale + 0.5f);
        }

        public static int dip2sp(Context c, float dpValue) {
            return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, c.getResources().getDisplayMetrics()));
        }

        public static int px2dip(Context c, float pxValue) {
            final float scale = c.getResources().getDisplayMetrics().density;
            return (int) (pxValue / scale + 0.5f);
        }

        public static int px2sp(Context c, float pxValue) {
            float fontScale = c.getResources().getDisplayMetrics().scaledDensity;
            return (int) (pxValue / fontScale + 0.5f);
        }

        public static int sp2px(Context c, float spValue) {
            float fontScale = c.getResources().getDisplayMetrics().scaledDensity;
            return (int) (spValue * fontScale + 0.5f);
        }

        public static int sp2dip(Context c, float spValue) {
            return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, c.getResources().getDisplayMetrics()));
        }

        public static int getScreenW(Context c) {
            return c.getResources().getDisplayMetrics().widthPixels;
        }

        public static int getScreenH(Context c) {
            return c.getResources().getDisplayMetrics().heightPixels;
        }
    }
}
