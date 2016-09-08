
package com.pfc.namibiaroadtrip;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.query.QueryParameters;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.pfc.namibiaroadtrip.utils.ErisImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by pfc on 2016/9/4.
 */

public class MainActivity extends Activity implements MyHorizotalScrollView.MyHorizotalScrollViewProtocol{

    private MyHorizotalScrollView myHorizotalScrollView;

    private MapView mMapView;
    private FeatureLayer mFeatureLayer;
    private GraphicsLayer mGraphicsLayer;
    private Callout mCallout;
    private Feature mIdentifiedFeature;

    private ViewGroup mCalloutContent;
    private boolean mIsMapLoaded;
    private String mFeatureServiceURL;
    private String mFeatureServiceLayerURL;
    private SpatialReference mapSpatialReference;

    public GeodatabaseFeatureServiceTable mFeatureServiceTable;

    private List<Feature> allFeatures;

    private ProgressDialog progress;
    private ImageView bigImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        allFeatures = new ArrayList<Feature>();

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) findViewById(R.id.map);
        bigImageView = (ImageView)findViewById(R.id.iv_Picture_Show);

        int screenWidth=getWindowManager().getDefaultDisplay().getWidth();

        myHorizotalScrollView = (MyHorizotalScrollView)findViewById(R.id.myHorizotalScrollView);
        myHorizotalScrollView.itemHeight = 50*4;
        myHorizotalScrollView.itemSpace = 20;
        myHorizotalScrollView.pageSize = 4;
        myHorizotalScrollView.protocol = this;
        myHorizotalScrollView.itemWidth = (screenWidth - myHorizotalScrollView.itemSpace * (myHorizotalScrollView.pageSize - 1))/myHorizotalScrollView.pageSize;

        // Get the feature service URL from values->strings.xml
        mFeatureServiceURL = this.getResources().getString(R.string.featureServiceURL);

        // Set the Esri logo to be visible, and enable map to wrap around date line.
        mMapView.setEsriLogoVisible(true);
        mMapView.enableWrapAround(true);

        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            public void onStatusChanged(Object source, STATUS status) {
                // Check to see if map has successfully loaded
                if ((source == mMapView) && (status == STATUS.INITIALIZED)) {
                    // Set the flag to true
                    mIsMapLoaded = true;
                    mapSpatialReference = mMapView.getSpatialReference();
                    // create a GeodatabaseFe   atureServiceTable from a feature service url
                    mFeatureServiceTable = new GeodatabaseFeatureServiceTable(mFeatureServiceURL, 0);
                    mFeatureServiceTable.setSpatialReference(mapSpatialReference);
                    // initialize the GeodatabaseFeatureService and populate it with features from the service

                    mFeatureServiceTable.initialize(new CallbackListener<GeodatabaseFeatureServiceTable.Status>(){
                        @Override
                        public void onCallback(GeodatabaseFeatureServiceTable.Status status) {
                            if (status == GeodatabaseFeatureServiceTable.Status.INITIALIZED) {
                                mFeatureLayer = new FeatureLayer(mFeatureServiceTable);
                                mFeatureLayer.setVisible(true);
                                // emphasize the selected features by increasing the selection halo size and color
                                mFeatureLayer.setSelectionColor(Color.GRAY);
                                mFeatureLayer.setSelectionColorWidth(20);
                                // add feature layer to map
                                mMapView.addLayer(mFeatureLayer);
                                // Add Graphics layer to the MapView
                                mGraphicsLayer = new GraphicsLayer();
                                mMapView.addLayer(mGraphicsLayer);

                                LayoutInflater inflater = getLayoutInflater();
                                mCallout = mMapView.getCallout();
                                // Get the layout for the Callout from
                                // layout->identify_callout_content.xml
                                mCalloutContent = (ViewGroup) inflater.inflate(R.layout.identify_callout_content, null);
                                mCallout.setContent(mCalloutContent);

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


                        }

                    });
                }
            }
        });

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
            Point mapPoint = mMapView.toMapPoint(x, y);
            // Show Callout
            showCallout(mCallout, mIdentifiedFeature, (Point)mIdentifiedFeature.getGeometry());
            int index = allFeatures.indexOf(mIdentifiedFeature);
            for(int i = 0;i<allFeatures.size();i++){
                Feature feature = allFeatures.get(i);
                long id = feature.getId();
                if (id == mIdentifiedFeature.getId()){
                    myHorizotalScrollView.scrollToIndex(i);
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
     * Run the query task on the feature layer and put the result on the map.
     */
    private class QueryFeatureLayer extends AsyncTask<String, Void, FeatureResult> {

        // default constructor
        public QueryFeatureLayer() {
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, "", "Please wait....query task is executing");
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
                results = mFeatureServiceTable.queryFeatures(queryParams,null).get();
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
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.BLACK, 20, SimpleMarkerSymbol.STYLE.CIRCLE);
            // Envelope to focus on the map extent on the results
            TextSymbol txtSymbol = new TextSymbol(12, "1", Color.WHITE);
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
                    txtSymbol.setText(index+"");

                    // convert feature to graphic
                    Graphic graphic = new Graphic(feature.getGeometry(), sms, feature.getAttributes());
                    // merge extent with point
                    Graphic gr = new Graphic(feature.getGeometry(), txtSymbol,feature.getAttributes());
                    extent.merge((Point)graphic.getGeometry());
                    extent.merge((Point)graphic.getGeometry());
                    System.out.println("geo:"+((Feature) element).getId()+"attris:"+((Feature) element).getAttributes());
                    // add it to the layer
                    mGraphicsLayer.addGraphic(graphic);
                    mGraphicsLayer.addGraphic(gr);
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
            mMapView.setExtent(extent, 500);
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
        ErisImageLoader.getInstance().displayImage(feature.getAttributeValue("thumb_url").toString(), imageView);

        return view;
    }

    @Override
    public void MHSItemDidSelectedForIndex(int index) {
        Feature feature = allFeatures.get(index);
        String big_url = feature.getAttributeValue("pic_url").toString();
        ErisImageLoader.getInstance().displayImage(big_url,bigImageView);
    }

    @Override
    public void MHSItemDidUnselectedForIndex(int index) {

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
}
