package com.pfc.namibiaroadtrip.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.pfc.namibiaroadtrip.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pfc on 2016/9/6.
 */

public class MyHorizotalScrollView extends HorizontalScrollView implements View.OnTouchListener{

    /**
     * 当前选择的index
     */
    private int selectedIndex;
    public int getSelectedIndex(){
        return this.selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        if ((this.selectedIndex == selectedIndex) || selectedIndex>viewList.size()-1) return;
        View view = viewList.get(this.selectedIndex);
        view.setBackgroundColor(this.unSelectedColor);
        this.selectedIndex = selectedIndex;
        view = viewList.get(this.selectedIndex);
        view.setBackgroundColor(this.selectedColor);
    }

    /**
     * 被选中的颜色
     */
    private int selectedColor = Color.rgb(200,254,254);

    /**
     * 未被选中的颜色
     */
    private int unSelectedColor = Color.argb(0,0,0,0);

    /**
     * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
     */
    private boolean loadOnce;

    /**
     *MyHorizotalScrollView的代理
     */
    public MyHorizotalScrollViewProtocol protocol;

    /**
     *  列的布局
     */
    private   LinearLayout linearlayout_userinfo_personal_column;


    /**
     * 总的宽度
     */
    private static int ColumnWith;

    /**
     * 每张照片的宽度
     */
    public int itemWidth;

    /**
     *  每张照片的高度
     */
    public  int itemHeight;

    /**
     * 照片横向之间的间距
     */
    public int itemSpace;

    /**
     * 照片距上下的边距
     */
    private int itemTBMargin;

    /**
     * 记录上垂直方向的滚动距离。
     */
    private static int lastScrollX = -1;

    /**
     * 记录item的总数
     */
    private int itemCount;


    /**
     * MyScrollView下的直接子布局。
     */
    private static View scrollLayout;

    /**
     * 进行一些关键性的初始化操作，MyHorizotalScrollView，以及得到高度值。并在这里开始加载第一页的图片。
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && !loadOnce) {
            scrollViewHeight = getHeight();
            scrollViewWidth = getWidth();
            int size = (scrollViewWidth - itemSpace) / (itemSpace+itemWidth);
            pageSize = (scrollViewWidth - itemSpace) % (itemSpace+itemWidth)==0 ? size+1 : size+2;
            if (protocol != null){
                itemCount = protocol.countForMHS();
            }
            itemTBMargin = (scrollViewHeight - itemHeight)/2;
            linearlayout_userinfo_personal_column = new LinearLayout(getContext());
            linearlayout_userinfo_personal_column.setLayoutParams(
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                            , ViewGroup.LayoutParams.MATCH_PARENT)
            );
            linearlayout_userinfo_personal_column.setOrientation(LinearLayout.HORIZONTAL);
            this.addView(linearlayout_userinfo_personal_column);
            loadOnce = true;
            //加载下一页图片
            loadMoreData();
            scrollLayout = getChildAt(0);
        }
    }

    /**
     * 记录所有界面上的图片，用以可以随时控制对图片的释放。
     */
    private List<View> viewList = new ArrayList<View>();

    /**
     * MyScrollView布局的高度。
     */
    private static int scrollViewHeight;

    /**
     * MyScrollView布局的宽度。
     */
    private static int scrollViewWidth;

    /**
     * 每页要加载的图片数量
     * 默认为4张
     * @param context
     */
    public int pageSize;

    /**
     * MyScrollView的构造函数。
     * @param context
     * @param attrs
     */
    public MyHorizotalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
    }

    public void reloadData(){
        itemCount = protocol.countForMHS();
        linearlayout_userinfo_personal_column.removeAllViews();
        viewList.clear();
        loadPlaceholderItem();
        loadMoreData();
        selectedIndex = 0;
        scrollLayout = getChildAt(0);
        View view = viewList.get(this.selectedIndex);
        view.setBackgroundColor(this.selectedColor);
    }

    private void loadPlaceholderItem(){
        System.out.println("itemCount:"+itemCount);
        for (int i = 0; i < itemCount; i++) {
            System.out.println("itemCount:"+itemCount);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    itemWidth, scrollViewHeight);
            View itemView = new View(getContext());
            itemView.setTag(R.string.border_left, ColumnWith);
            ColumnWith += itemWidth;//增加总宽

            itemView.setTag(R.string.border_right, ColumnWith);
            if (i < itemCount - 1) {
                params.setMargins(0, 0, itemSpace, 0);
                ColumnWith += itemSpace;
            }
            itemView.setLayoutParams(params);
            linearlayout_userinfo_personal_column.addView(itemView);
        }
    }


    private void replaceItem(int index , View view){
        View placeHolderView = linearlayout_userinfo_personal_column.getChildAt(index);
        view.setTag(R.string.border_left,placeHolderView.getTag(R.string.border_left));
        view.setTag(R.string.border_right,placeHolderView.getTag(R.string.border_right));
        linearlayout_userinfo_personal_column.removeViewAt(index);
        linearlayout_userinfo_personal_column.addView(view,index);
    }

    public void scrollToIndex(int index){
        if (index > viewList.size() - pageSize){
            loadDataToIndex(index);
        }
        myScrollTo(getScrollXFromIndex(index),0);
    }

    /**
     * myScrollTo
     * @param x
     * @param y
     */
    public void myScrollTo(int x, int y) {
        scrollTo(x,y);
        Message message = new Message();
        message.obj = this;
        handler.sendMessageDelayed(message, 5);
    }

    private void loadDataToIndex(int index){
        if (index + pageSize <= viewList.size()) return;
        if ( viewList.size()<itemCount) {
            int startIndex = viewList.size();  //起始位置
            int endIndex = index + pageSize;    //结束位置
            if (startIndex < itemCount) {
                if (endIndex > itemCount) {
                    endIndex = itemCount;
                }
                for (int i = startIndex; i < endIndex; i++) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            itemWidth, scrollViewHeight);

                    View itemView = protocol.getMHSItemForIndex(i);
                    itemView.setTag(R.string.item_index,i);
                    final int viewIndex = i;
                    itemView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            System.out.println("我被点击了e,我是："+ selectedIndex);
                            if (viewIndex == selectedIndex) return;
                            View view = viewList.get(selectedIndex);
                            view.setBackgroundColor(unSelectedColor);
                            protocol.MHSItemDidUnselectedForIndex(selectedIndex);

                            selectedIndex = viewIndex;

                            view = viewList.get(selectedIndex);
                            view.setBackgroundColor(selectedColor);
                            protocol.MHSItemDidSelectedForIndex(selectedIndex);
                        }
                    });
                    itemView.setPadding(0, itemTBMargin,0, itemTBMargin);
                    if (i < itemCount - 1) {
                        params.setMargins(0, 0, itemSpace, 0);
                    }
                    itemView.setLayoutParams(params);
                    replaceItem(i,itemView);
                    viewList.add(itemView);
                }
            }
        }else {
            if (hasOver = false){
                hasOver = true;
                Toast.makeText(getContext(), "图片加载完毕", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * 在Handler中进行图片可见性检查的判断，以及加载更多图片的操作。
     */
    private static Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            MyHorizotalScrollView myScrollView = (MyHorizotalScrollView) msg.obj;
            int scrollX = myScrollView.getScrollX();
            // 如果当前的滚动位置和上次相同，表示已停止滚动
            if (scrollX == lastScrollX) {
                // 当滚动的最右边，并且当前没有正在下载的任务时，开始加载下一页的图片
//                myScrollView.checkVisibility();
            } else {
                lastScrollX = scrollX;
                Message message = new Message();
                message.obj = myScrollView;
                // 5毫秒后再次对滚动位置进行判断
                handler.sendMessageDelayed(message, 100);
            }
            int index = myScrollView.getIndexFromScrollX(scrollX);
            myScrollView.loadDataToIndex(index);
        }
    };

    private int getIndexFromScrollX(int scrollX){
        return (scrollX + itemSpace)/(itemSpace+ itemWidth);
    }

    private int getScrollXFromIndex(int index){
        return index*(itemWidth+itemSpace);
    }


    /**
     * 监听用户的触屏事件，如果用户手指离开屏幕则开始进行滚动检测。
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Message message = new Message();
            message.obj = this;
            handler.sendMessageDelayed(message, 5);
        }
        return false;
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return false;
//    }

    /**
     * 遍历imageViewList中的每张图片，对图片的可见性进行检查，如果图片已经离开屏幕可见范围，则将图片替换成一张空图。
     */
//    public void checkVisibility() {
//        int screenLeft = getScrollX();
//        int screenRight = getScrollX() + scrollViewWidth;
//        for (int i = 0; i < viewList.size(); i++) {
//            ImageView imageView = viewList.get(i);
//            int borderLeft = (Integer) imageView.getTag(R.string.border_left);
//            int borderRight = (Integer) imageView
//                    .getTag(R.string.border_right);
//
//            if ((borderRight > screenLeft && borderRight< screenRight)
//                    || (borderLeft > screenLeft && borderLeft< screenRight)) {
//                System.out.println("出现了+++");
//                String imageUrl = (String) imageView.getTag(R.string.image_url);
//                ErisImageLoader.getInstance().displayImage(imageUrl,imageView);
//
//            } else {
//                System.out.println("消失了+++");
////                imageView.setImageBitmap(null);
//            }
//        }
//    }


    static boolean hasOver = false;

    /**
     * 开始加载下一页的图片，每张图片都会开启一个异步线程去下载。
     */
    public void loadMoreData() {
        loadDataToIndex(viewList.size());
    }

    public interface MyHorizotalScrollViewProtocol{
        public View getMHSItemForIndex(int index);
        public void MHSItemDidSelectedForIndex(int index);
        public void MHSItemDidUnselectedForIndex(int index);
        public int countForMHS();
    }

}
