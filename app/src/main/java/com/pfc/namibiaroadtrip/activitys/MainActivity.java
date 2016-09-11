
package com.pfc.namibiaroadtrip.activitys;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.ags.FeatureServiceInfo;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusCallback;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusInfo;
import com.esri.core.tasks.geodatabase.GeodatabaseSyncTask;
import com.esri.core.tasks.query.QueryParameters;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.pfc.namibiaroadtrip.utils.FileCache;
import com.pfc.namibiaroadtrip.views.MyHorizotalScrollView;
import com.pfc.namibiaroadtrip.R;
import com.pfc.namibiaroadtrip.utils.EsriImageLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by pfc on 2016/9/4.
 */

public class MainActivity extends Activity implements MyHorizotalScrollView.MyHorizotalScrollViewProtocol {
//create offline feature datas
    static GeodatabaseSyncTask gdbSyncTask;
    private static File demoDataFile;
    private static String offlineDataSDCardDirName;
    private static String filename;
    static String localGdbFilePath;
    protected static String OFFLINE_FILE_EXTENSION = ".geodatabase";
    private MyHorizotalScrollView myHorizotalScrollView;
    private static Context mContext;

    private int selectedGraphicSize = 30;
    private int unselectedGraphicSize = 20;
    private int selectedGraphicTextSize = 18;
    private int unselectedGraphicTextSize = 12;

    private Envelope homeExtent;
    private int homePadding = 500;


    private static MapView mMapView;
    private static FeatureLayer mFeatureLayer;
    private static GraphicsLayer mGraphicsLayer;
    private Callout mCallout;
    private Feature mIdentifiedFeature;

    private ViewGroup mCalloutContent;
    private boolean mIsMapLoaded;
    private String mFeatureServiceURL;
    private SpatialReference mapSpatialReference;

    public static GeodatabaseFeatureServiceTable mFeatureServiceTable;
    public static GeodatabaseFeatureTable mFeatureTable;

    private List<Feature> allFeatures;
    private List<Integer> allLGraphicUids;

    private static ProgressDialog progress;
    private TextView titeDes;
    private ImageView bigImageView;
    private TextView bigImageTV;
    private Button bigImageShowTVBtn;
    private Button bigImageLeftArrowBtn;
    private Button bigImageRightArrowBtn;
    private Button downloadBtn;
    private Button clearCacheBtn;

    private Button zoomInBtn;
    private Button zoomOutBtn;
    private Button zoomHomeBtn;

    private boolean isBigImageTextShow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivity.setContext(this);

        demoDataFile = Environment.getExternalStorageDirectory();
        offlineDataSDCardDirName = this.getResources().getString(
                R.string.config_data_sdcard_offline_dir);
        filename = this.getResources().getString(
                R.string.config_geodatabase_name);
        downloadBtn = (Button) findViewById(R.id.download_btn);
        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLocalDownload()){//下载
                    downloadData();
                }else {
                    Toast.makeText(mContext,"数据已经下载到本地！",Toast.LENGTH_SHORT).show();
                }
            }
        });
        clearCacheBtn = (Button)findViewById(R.id.clearCache_btn);
        clearCacheBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageLoader.getInstance().clearDiskCache();

                String dir = demoDataFile.getAbsolutePath() + File.separator + offlineDataSDCardDirName;
                FileCache.deleteDirectory(dir);
                Toast.makeText(mContext,"缓存清除成功！",Toast.LENGTH_SHORT).show();
            }
        });


        progress = new ProgressDialog(MainActivity.this);

        allFeatures = new ArrayList<Feature>();
        allLGraphicUids = new ArrayList<Integer>();

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) findViewById(R.id.map);
        titeDes = (TextView)findViewById(R.id.title_des);
        bigImageView = (ImageView)findViewById(R.id.iv_Picture_Show);
        bigImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow();
            }
        });
        bigImageTV = (TextView)findViewById(R.id.bigImage_tv);
        bigImageLeftArrowBtn = (Button)findViewById(R.id.leftArrow_btn);
        bigImageLeftArrowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MHSItemDidUnselectedForIndex(myHorizotalScrollView.getSelectedIndex());
                int index = myHorizotalScrollView.getSelectedIndex() - 1;
                myHorizotalScrollView.scrollToIndex(index);
                myHorizotalScrollView.setSelectedIndex(index);
                MHSItemDidSelectedForIndex(index);
//                bigImageRightArrowBtn
            }
        });
        bigImageRightArrowBtn = (Button)findViewById(R.id.rightArrow_btn);
        bigImageRightArrowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MHSItemDidUnselectedForIndex(myHorizotalScrollView.getSelectedIndex());
                int index = myHorizotalScrollView.getSelectedIndex() + 1;
                myHorizotalScrollView.scrollToIndex(index);
                myHorizotalScrollView.setSelectedIndex(index);
                MHSItemDidSelectedForIndex(index);
            }
        });
        bigImageShowTVBtn = (Button)findViewById(R.id.show_text_btn);
        bigImageShowTVBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBigImageTextShow){
                    hideBigImageText();
                }else {
                    showBigImageText();
                }
            }
        });

        zoomHomeBtn = (Button)findViewById(R.id.homeBtn);
        zoomHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapView.setExtent(homeExtent,homePadding);
            }
        });

        zoomInBtn = (Button)findViewById(R.id.zoomInBtn);
        zoomInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapView.zoomin(true);
            }
        });
        zoomOutBtn = (Button)findViewById(R.id.zoomOutBtn);
        zoomOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapView.zoomout(true);
            }
        });

        int screenWidth=getWindowManager().getDefaultDisplay().getWidth();

        myHorizotalScrollView = (MyHorizotalScrollView)findViewById(R.id.myHorizotalScrollView);
        myHorizotalScrollView.itemHeight = getResources().getDimensionPixelSize(R.dimen.MHS_item_height);
        myHorizotalScrollView.itemSpace = getResources().getDimensionPixelSize(R.dimen.MHS_item_space);
        myHorizotalScrollView.protocol = this;
        int pageSize = 4;
        myHorizotalScrollView.itemWidth = (screenWidth - myHorizotalScrollView.itemSpace * (pageSize - 1))/pageSize;

        // Get the feature service URL from values->strings.xml
        mFeatureServiceURL = this.getResources().getString(R.string.featureServiceURL);

        // Set the Esri logo to be visible, and enable map to wrap around date line.
        mMapView.setEsriLogoVisible(true);
        mMapView.enableWrapAround(true);

        mMapView.setOnSingleTapListener(new OnSingleTapListener() {

            @Override
            public void onSingleTap(float x, float y) {

                if (mIsMapLoaded) {
                    // If map is initialized and Single tap is registered on screen
                    // identify the location selected
                    System.out.println("地图被点击了");
                    identifyLocation(x, y);
                }
            }
        });

        LayoutInflater inflater = getLayoutInflater();
        mCallout = mMapView.getCallout();
        // Get the layout for the Callout from
        // layout->identify_callout_content.xml
        mCalloutContent = (ViewGroup) inflater.inflate(R.layout.identify_callout_content, null);
        mCallout.setContent(mCalloutContent);

        progress.setTitle("Please wait....data is loading");
        progress.show();
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            public void onStatusChanged(Object source, STATUS status) {
                // Check to see if map has successfully loaded
                if ((source == mMapView) && (status == STATUS.INITIALIZED)) {
                    showProgressBar(MainActivity.this,"Map Loaded");
                    // Set the flag to true
                    mIsMapLoaded = true;
                    mapSpatialReference = mMapView.getSpatialReference();

                    //先判断本地数据库是否存在
//                    String featureLayerPath = createGeodatabaseFilePath();
//                    Geodatabase localGdb = null;
//                    try {
//                        localGdb = new Geodatabase(featureLayerPath);
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//
//                    // Geodatabase contains GdbFeatureTables representing attribute data
//                    // and/or spatial data. If GdbFeatureTable has geometry add it to
//                    // the MapView as a Feature Layer
//                    if (localGdb != null) {//本地存在
//                        System.out.println("本地数据库存在 ");
//                        for (GeodatabaseFeatureTable gdbFeatureTable : localGdb
//                                .getGeodatabaseTables()) {
//                            if (gdbFeatureTable.hasGeometry()){
//                                mFeatureTable = gdbFeatureTable;
//                                mFeatureLayer = new FeatureLayer(mFeatureTable);
//                                mFeatureLayer.setVisible(true);
//                                mMapView.addLayer(mFeatureLayer);
//                                mMapView.addLayer(mGraphicsLayer);
//                                break;
//                            }
//                        }
//                        return;
//                    }else {
//                        System.out.println("本地数据库不存在 ");
//                    }

                    // create a GeodatabaseFe   atureServiceTable from a feature service url
                    mFeatureServiceTable = new GeodatabaseFeatureServiceTable(mFeatureServiceURL, 0);
                    mFeatureServiceTable.setSpatialReference(mapSpatialReference);
                    // initialize the GeodatabaseFeatureService and populate it with features from the service
                    showProgressBar(MainActivity.this,"Feature is loading...");
                    mFeatureServiceTable.initialize(new CallbackListener<GeodatabaseFeatureServiceTable.Status>(){
                        @Override
                        public void onCallback(GeodatabaseFeatureServiceTable.Status status) {
                            if (status == GeodatabaseFeatureServiceTable.Status.INITIALIZED) {
                                showProgressBar(MainActivity.this,"Feature loaded");
                                mFeatureLayer = new FeatureLayer(mFeatureServiceTable);
                                mFeatureLayer.setVisible(true);
                                // emphasize the selected features by increasing the selection halo size and color
                                // add feature layer to map
                                mMapView.addLayer(mFeatureLayer);
                                // Add Graphics layer to the MapView
                                mGraphicsLayer = new GraphicsLayer();
                                mMapView.addLayer(mGraphicsLayer);

                                // create a FeatureLayer from teh initialized GeodatabaseFeatureServiceTable
                                runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                new QueryFeatureLayer().execute();
                                            }
                                        }
                                );

                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            progress.dismiss();
                            Toast.makeText(mContext,"the feature was loaded failed! please check your internet!",Toast.LENGTH_SHORT);

                        }

                    });
                }else if (status == STATUS.INITIALIZATION_FAILED || status == STATUS.LAYER_LOADING_FAILED){
                    progress.dismiss();
                    Toast.makeText(mContext,"地图加载失败，请检查您的网络",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void hideBigImageText(){
        bigImageShowTVBtn.setEnabled(false);
        bigImageShowTVBtn.setText(R.string.up_arrow);
        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, bigImageTV.getHeight());
        animation.setFillAfter(true);
        animation.setDuration(500);
        bigImageTV.startAnimation(animation);
        animation = new TranslateAnimation(0, 0, 0, bigImageTV.getHeight());
        animation.setFillAfter(true);
        animation.setDuration(500);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bigImageShowTVBtn.setEnabled(true);
                bigImageShowTVBtn.clearAnimation();

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bigImageShowTVBtn.getLayoutParams();
                params.bottomMargin = params.bottomMargin - bigImageTV.getHeight();
                bigImageShowTVBtn.setLayoutParams(params);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bigImageShowTVBtn.startAnimation(animation);
        isBigImageTextShow = false;
    }

    private void showBigImageText(){
        bigImageShowTVBtn.setEnabled(false);
        bigImageShowTVBtn.setText(R.string.down_arrow);
        TranslateAnimation animation = new TranslateAnimation(0, 0, bigImageTV.getHeight(), 0);
        animation.setFillAfter(true);
        animation.setDuration(500);
        bigImageTV.startAnimation(animation);
        animation = new TranslateAnimation(0, 0, 0, -bigImageTV.getHeight());
        animation.setFillAfter(true);
        animation.setDuration(500);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bigImageShowTVBtn.setEnabled(true);
                bigImageShowTVBtn.clearAnimation();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bigImageShowTVBtn.getLayoutParams();
                params.bottomMargin = params.bottomMargin + bigImageTV.getHeight();
                bigImageShowTVBtn.setLayoutParams(params);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bigImageShowTVBtn.startAnimation(animation);

        isBigImageTextShow = true;
    }
    private void showPopupWindow() {
        System.out.println("showPopupWindow:");
        // 一个自定义的布局，作为显示的内容
        View contentView = LayoutInflater.from(this).inflate(
                R.layout.pop_window, null);

        final PopupWindow popupWindow = new PopupWindow(contentView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        ImageView imageView = (ImageView) contentView.findViewById(R.id.popup_image);
        EsriImageLoader.getInstance().displayImage(bigImageView.getTag(R.string.bigImageurl).toString(),imageView,true);

        TextView textView = (TextView)contentView.findViewById(R.id.popup_name_tv);
        textView.setText(bigImageTV.getText());

        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);


        popupWindow.setTouchInterceptor(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });

        // 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
        // 我觉得这里是API的一个bug
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        // 设置好参数之后再show
        popupWindow.showAsDropDown(titeDes);

    }

    /**
     * Takes in the screen location of the point to identify the feature on map.
     *
     * @param x
     *          x co-ordinate of point
     * @param y
     *          y co-ordinate of point
     */
    private void identifyLocation(float x, float y) {

        // Hide the callout, if the callout from previous tap is still showing
        // on map
        if (mCallout.isShowing()) {
            mCallout.hide();
        }

        // Find out if the user tapped on a feature
        searchForFeature(x, y);

        // If the user tapped on a feature, then display information regarding
        // the feature in the callout
        if (mIdentifiedFeature != null) {
            for(int i = 0;i<allFeatures.size();i++){
                Feature feature = allFeatures.get(i);
                long id = feature.getId();
                if (id == mIdentifiedFeature.getId()){
                    MHSItemDidUnselectedForIndex(myHorizotalScrollView.getSelectedIndex());
                    myHorizotalScrollView.scrollToIndex(i);
                    myHorizotalScrollView.setSelectedIndex(i);
                    MHSItemDidSelectedForIndex(i);
                    return;
                }
            }
        }
    }

    /**
     * Sets the value of mIdentifiedGraphic to the Graphic present on the
     * location of screen tap
     *
     * @param x
     *          x co-ordinate of point
     * @param y
     *          y co-ordinate of point
     */
    private void searchForFeature(float x, float y) {

        Point mapPoint = mMapView.toMapPoint(x, y);
        System.out.println("地图被点击了++++");
        if (mapPoint != null) {
            System.out.println("找到地图的点了");
            for (Layer layer : mMapView.getLayers()) {
                if (layer == null)
                    continue;

                if (layer instanceof FeatureLayer) {
                    FeatureLayer fLayer = (FeatureLayer) layer;
                    // Get the Graphic at location x,y
                    mIdentifiedFeature =  getFeature(fLayer, x, y);
                }
            }
        }
    }

    /**
     * Returns the Graphic present the location of screen tap
     *
     * @param fLayer
     *          ArcGISFeatureLayer to get graphics ids
     * @param x
     *          x co-ordinate of point
     * @param y
     *          y co-ordinate of point
     * @return Graphic at location x,y
     */
    private Feature getFeature(FeatureLayer fLayer, float x, float y) {

        // Get the graphics near the Point.
        long[] ids = fLayer.getFeatureIDs(x, y, 30, 1);
        if (ids == null || ids.length == 0) {
            return null;
        }
        return fLayer.getFeature(ids[0]);
    }

    /**
     * Shows the Attribute values for the Graphic in the Callout
     *
     * @param calloutView a callout to show
     * @param feature selected graphic
     * @param mapPoint point to show callout
     */
    private void showCallout(Callout calloutView, Feature feature, Point mapPoint) {

        // Get the values of attributes for the Graphic
        String name = (String) feature.getAttributeValue("name");

        // Set callout properties
        calloutView.setCoordinates(mapPoint);

        // Compose the string to display the results
        StringBuilder cityCountryName = new StringBuilder();
        cityCountryName.append(name);

        TextView calloutTextLine1 = (TextView) findViewById(R.id.tv_name);
        calloutTextLine1.setText(cityCountryName);

        calloutView.setContent(mCalloutContent);
        calloutView.show();
    }

    /**
     * Create the GeodatabaseTask from the feature service URL w/o credentials.
     */
    private void downloadData() {
        // create a dialog to update user on progress
        progress.setTitle("Create local runtime geodatabase");
        progress.show();
        // create the GeodatabaseTask

        gdbSyncTask = new GeodatabaseSyncTask("http://services.arcgis.com/P3ePLMYs2RVChkJx/arcgis/rest/services/Wildfire/FeatureServer", null);
        gdbSyncTask
                .fetchFeatureServiceInfo(new CallbackListener<FeatureServiceInfo>() {

                    @Override
                    public void onError(Throwable arg0) {
                    }

                    @Override
                    public void onCallback(FeatureServiceInfo fsInfo) {
                        if (fsInfo.isSyncEnabled()) {
                            createGeodatabase(fsInfo);
                        }
                    }
                });

    }

    /**
     * Set up parameters to pass the the {@link #()} method. A
     * {@link CallbackListener} is used for the response.
     */
    private static void createGeodatabase(FeatureServiceInfo featureServerInfo) {
        // set up the parameters to generate a geodatabase
        GenerateGeodatabaseParameters params = new GenerateGeodatabaseParameters(
                featureServerInfo, mMapView.getExtent(),
                mMapView.getSpatialReference());

        // a callback which fires when the task has completed or failed.
        CallbackListener<String> gdbResponseCallback = new CallbackListener<String>() {
            @Override
            public void onError(final Throwable e) {
                progress.dismiss();
            }

            @Override
            public void onCallback(String path) {
                progress.dismiss();
                // update map with local feature layer from geodatabase
                updateFeatureLayer(path);
            }
        };

        // a callback which updates when the status of the task changes
        GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {
            @Override
            public void statusUpdated(final GeodatabaseStatusInfo status) {
                // get current status
                String progress = status.getStatus().toString();
                // get activity context
                Context context = MainActivity.getContext();
                // create activity from context
                MainActivity activity = (MainActivity) context;
                // update progress bar on main thread
                showProgressBar(activity, progress);

            }
        };

        // create the fully qualified path for geodatabase file
        localGdbFilePath = createGeodatabaseFilePath();

        // get geodatabase based on params
        submitTask(params, localGdbFilePath, statusCallback,
                gdbResponseCallback);
    }

    /*
	 * Create the geodatabase file location and name structure
	 */
    static String createGeodatabaseFilePath() {
        return demoDataFile.getAbsolutePath() + File.separator + offlineDataSDCardDirName + File.separator + filename + OFFLINE_FILE_EXTENSION;
    }

    /**
     * Request database, poll server to get status, and download the file
     */
    private static void submitTask(GenerateGeodatabaseParameters params,
                                   String file, GeodatabaseStatusCallback statusCallback,
                                   CallbackListener<String> gdbResponseCallback) {
        // submit task
        gdbSyncTask.generateGeodatabase(params, file, false, statusCallback,
                gdbResponseCallback);
    }

    /**
     * Add feature layer from local geodatabase to map
     *
     * @param featureLayerPath
     */
    private static void updateFeatureLayer(String featureLayerPath) {
        // create a new geodatabase
        Geodatabase localGdb = null;
        try {
            localGdb = new Geodatabase(featureLayerPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Geodatabase contains GdbFeatureTables representing attribute data
        // and/or spatial data. If GdbFeatureTable has geometry add it to
        // the MapView as a Feature Layer
//        if (localGdb != null) {
//            for (GeodatabaseFeatureTable gdbFeatureTable : localGdb
//                    .getGeodatabaseTables()) {
//                if (gdbFeatureTable.hasGeometry()){
//                    mMapView.removeLayer(mGraphicsLayer);
//                    mMapView.removeLayer(mFeatureLayer);
//
//                    mFeatureTable = gdbFeatureTable;
//                    mFeatureLayer = new FeatureLayer(mFeatureTable);
//                    mMapView.addLayer(mFeatureLayer);
//                    mMapView.addLayer(mGraphicsLayer);
//                }
//            }
//        }
    }

    private static void showProgressBar(final MainActivity activity, final String message){
        activity.runOnUiThread(new Runnable(){

            @Override
            public void run() {
                progress.setMessage(message);
            }

        });
    }

    public static Context getContext(){
        return mContext;
    }

    public static void setContext(Context context){
        mContext = context;
    }

    private boolean isLocalDownload(){
        Geodatabase localGdb = null;
        try {
            localGdb = new Geodatabase(createGeodatabaseFilePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Geodatabase contains GdbFeatureTables representing attribute data
        // and/or spatial data. If GdbFeatureTable has geometry add it to
        // the MapView as a Feature Layer
        return localGdb!=null;
    }

    /**
     * Run the query task on the feature layer and put the result on the map.
     */
    private class QueryFeatureLayer extends AsyncTask<String, Void, FeatureResult> {

        // default constructor
        public QueryFeatureLayer() {
        }

        @Override
        protected void onPreExecute() {
            showProgressBar(MainActivity.this,"Query task is executing");
        }

        @Override
        protected FeatureResult doInBackground(String... params) {

            String whereClause;
            System.out.println("params:+++"+params.toString());
            if (params.length>0 && !"".equals(params[0])){
                whereClause = "objectid like'" + params[0] + "'";
            }else {//查找所有
                whereClause = "objectid >'" + "1" + "'";
            }
            QueryParameters queryParams = new QueryParameters();
            queryParams.setWhere(whereClause);
            // Execute the query and create a callback for dealing with the results of the query
            FeatureResult results = null;
            try {
                if (mFeatureTable!=null){
                    results = mFeatureTable.queryFeatures(queryParams,null).get();
                }else {
                    results = mFeatureServiceTable.queryFeatures(queryParams,null).get();
                }
                return results;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(FeatureResult results) {

            // Remove the result from previously run query task
            mGraphicsLayer.removeAll();

            // Define a new marker symbol for the result graphics
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.BLACK, unselectedGraphicSize, SimpleMarkerSymbol.STYLE.CIRCLE);
            // Envelope to focus on the map extent on the results
            TextSymbol txtSymbol = new TextSymbol(unselectedGraphicTextSize, "1", Color.WHITE);
            txtSymbol.setHorizontalAlignment(TextSymbol.HorizontalAlignment.CENTER);
            txtSymbol.setVerticalAlignment(TextSymbol.VerticalAlignment.MIDDLE);

            Envelope extent = new Envelope();

            if (results==null){

                Toast.makeText(MainActivity.this,"空的",Toast.LENGTH_SHORT).show();
            }


            // iterate through results
            int index = 0;
            final String[] imageUrls = new String[(int) results.featureCount()];
            for (Object element : results) {
                // if object is feature cast to feature
                if (element instanceof Feature){
                    Feature feature = (Feature) element;
                    allFeatures.add(feature);
                    imageUrls[index] = feature.getAttributeValue("thumb_url").toString();
                    index++;
                    if (index==1){
                        txtSymbol.setSize(selectedGraphicTextSize);
                        sms.setSize(selectedGraphicSize);
                    }else {
                        txtSymbol.setSize(unselectedGraphicTextSize);
                        sms.setSize(unselectedGraphicSize);
                    }
                    txtSymbol.setText(index+"");

                    // convert feature to graphic
                    Graphic graphic = new Graphic(feature.getGeometry(), sms, feature.getAttributes());
                    // merge extent with point
                    Graphic gr = new Graphic(feature.getGeometry(), txtSymbol,feature.getAttributes());
                    extent.merge((Point)graphic.getGeometry());
                    System.out.println("geo:"+((Feature) element).getId()+"attris:"+((Feature) element).getAttributes());
                    // add it to the layer
                    allLGraphicUids.add(mGraphicsLayer.addGraphic(graphic));
                    allLGraphicUids.add(mGraphicsLayer.addGraphic(gr));
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myHorizotalScrollView.reloadData();
                    MHSItemDidSelectedForIndex(0);
                }
            });

            // Set the map extent to the envelope containing the result graphics
            homeExtent = extent;
            mMapView.setExtent(extent,homePadding);
            // Disable the progress dialog
            progress.dismiss();
        }
    }

    /**
     * 实现每一个item
     * @param index
     * @return
     */
    @Override
    public View getMHSItemForIndex(int index) {
        System.out.println("+++++++这就是代理啊");

        Feature feature = allFeatures.get(index);
        System.out.println("代理的feature"+feature.getAttributes());

        LayoutInflater inflater = getLayoutInflater();
        View view = (View) inflater.inflate(R.layout.horizotal_scroll_item, null);

        TextView nameTV = (TextView) view.findViewById(R.id.MHS_item_name_tv);
        nameTV.setText(feature.getAttributeValue("name").toString());

        TextView numberTV = (TextView) view.findViewById(R.id.MHS_item_number_tv);
        numberTV.setText((index+1)+"");

        ImageView imageView = (ImageView) view.findViewById(R.id.MHS_item_im);
        EsriImageLoader.getInstance().displayImage(feature.getAttributeValue("thumb_url").toString(), imageView,false);

        return view;
    }

    @Override
    public void MHSItemDidSelectedForIndex(int index) {
        Feature feature = allFeatures.get(index);
        //大图的修改
        String big_url = feature.getAttributeValue("pic_url").toString();
        String name = feature.getAttributeValue("name").toString();
        bigImageView.setTag(R.string.bigImageurl,big_url);
        EsriImageLoader.getInstance().displayImage(big_url,bigImageView,true);
        bigImageTV.setText(name);
        //地图的修改
        mMapView.zoomToResolution((Point)feature.getGeometry(),100);
        showCallout(mCallout, feature, (Point)feature.getGeometry());

        //graphics
        SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.BLACK, selectedGraphicSize, SimpleMarkerSymbol.STYLE.CIRCLE);
        // Envelope to focus on the map extent on the results
        TextSymbol txtSymbol = new TextSymbol(selectedGraphicTextSize, ""+(index+1), Color.WHITE);
        txtSymbol.setHorizontalAlignment(TextSymbol.HorizontalAlignment.CENTER);
        txtSymbol.setVerticalAlignment(TextSymbol.VerticalAlignment.MIDDLE);

        int bgGraphicUid = allLGraphicUids.get(2*index);
        int txtGraphicUid = allLGraphicUids.get(2*index+1);
        mGraphicsLayer.removeGraphic(bgGraphicUid);
        mGraphicsLayer.removeGraphic(txtGraphicUid);
        Graphic graphic = new Graphic(feature.getGeometry(), sms, feature.getAttributes());
        // merge extent with point
        Graphic gr = new Graphic(feature.getGeometry(), txtSymbol,feature.getAttributes());
        allLGraphicUids.set(2*index,mGraphicsLayer.addGraphic(graphic));
        allLGraphicUids.set(2*index+1,mGraphicsLayer.addGraphic(gr));

        if (index == allFeatures.size()-1){
            bigImageRightArrowBtn.setVisibility(View.INVISIBLE);
        }else if (index == 0){
            bigImageLeftArrowBtn.setVisibility(View.INVISIBLE);
        }else {
            bigImageLeftArrowBtn.setVisibility(View.VISIBLE);
            bigImageRightArrowBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void MHSItemDidUnselectedForIndex(int index) {
        Feature feature = allFeatures.get(index);
        //graphics
        SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.BLACK, unselectedGraphicSize, SimpleMarkerSymbol.STYLE.CIRCLE);
        // Envelope to focus on the map extent on the results
        TextSymbol txtSymbol = new TextSymbol(unselectedGraphicTextSize, ""+(index+1), Color.WHITE);
        txtSymbol.setHorizontalAlignment(TextSymbol.HorizontalAlignment.CENTER);
        txtSymbol.setVerticalAlignment(TextSymbol.VerticalAlignment.MIDDLE);

        int bgGraphicUid = allLGraphicUids.get(2*index);
        int txtGraphicUid = allLGraphicUids.get(2*index+1);
        mGraphicsLayer.removeGraphic(bgGraphicUid);
        mGraphicsLayer.removeGraphic(txtGraphicUid);
        Graphic graphic = new Graphic(feature.getGeometry(), sms, feature.getAttributes());
        // merge extent with point
        Graphic gr = new Graphic(feature.getGeometry(), txtSymbol,feature.getAttributes());
        allLGraphicUids.set(2*index,mGraphicsLayer.addGraphic(graphic));
        allLGraphicUids.set(2*index+1,mGraphicsLayer.addGraphic(gr));
    }

    @Override
    public int countForMHS() {
        if (allFeatures != null && allFeatures.size()>0)
            return allFeatures.size();
        return 0;
    }

    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    protected void onResume() {
        super.onResume();
        mMapView.unpause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageLoader.getInstance().clearMemoryCache();
    }

    public int dpToPx(Resources res, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }
}
