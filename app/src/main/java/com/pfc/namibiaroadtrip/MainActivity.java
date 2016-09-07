
package com.pfc.namibiaroadtrip;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
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
import com.esri.core.tasks.query.QueryTask;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by pfc on 2016/9/4.
 */

public class MainActivity extends Activity {

    private MyHorizotalScrollView myHorizotalScrollView;

    private MapView mMapView;
    private ArcGISFeatureLayer mFeatureLayer;
    private GraphicsLayer mGraphicsLayer;
    private Callout mCallout;
    private Graphic mIdentifiedGraphic;

    private ViewGroup mCalloutContent;
    private boolean mIsMapLoaded;
    private String mFeatureServiceURL;
    private String mFeatureServiceLayerURL;
    private SpatialReference mapSpatialReference;

    public GeodatabaseFeatureServiceTable mFeatureServiceTable;

    private FeatureResult allFeatures;

    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int width=getWindowManager().getDefaultDisplay().getWidth();


        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) findViewById(R.id.map);
        myHorizotalScrollView = (MyHorizotalScrollView)findViewById(R.id.myHorizotalScrollView);

        // Get the feature service URL from values->strings.xml
        mFeatureServiceLayerURL = this.getResources().getString(R.string.featureServiceLayerURL);
        mFeatureServiceURL = this.getResources().getString(R.string.featureServiceURL);
        ArcGISFeatureLayer.Options options = new ArcGISFeatureLayer.Options();
        options.outFields = new String[]{"objectid","name","description","icon_color","pic_url","thumb_url"};
        options.mode = ArcGISFeatureLayer.MODE.ONDEMAND;
        // Add Feature layer to the MapView
        mFeatureLayer = new ArcGISFeatureLayer(mFeatureServiceLayerURL, options);
        mMapView.addLayer(mFeatureLayer);
        // Add Graphics layer to the MapView
        mGraphicsLayer = new GraphicsLayer();
        mMapView.addLayer(mGraphicsLayer);

        // Set the Esri logo to be visible, and enable map to wrap around date line.
        mMapView.setEsriLogoVisible(true);
        mMapView.enableWrapAround(true);

        LayoutInflater inflater = getLayoutInflater();
        mCallout = mMapView.getCallout();
        // Get the layout for the Callout from
        // layout->identify_callout_content.xml
        mCalloutContent = (ViewGroup) inflater.inflate(R.layout.identify_callout_content, null);
        mCallout.setContent(mCalloutContent);

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
                                // create a FeatureLayer from teh initialized GeodatabaseFeatureServiceTable
                                System.out.println("1234567890+++++++++++");
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
        if (mIdentifiedGraphic != null) {
            Point mapPoint = mMapView.toMapPoint(x, y);
            // Show Callout
            showCallout(mCallout, mIdentifiedGraphic, mapPoint);
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

        if (mapPoint != null) {

            for (Layer layer : mMapView.getLayers()) {
                if (layer == null)
                    continue;

                if (layer instanceof ArcGISFeatureLayer) {
                    ArcGISFeatureLayer fLayer = (ArcGISFeatureLayer) layer;
                    // Get the Graphic at location x,y
                    mIdentifiedGraphic = getFeature(fLayer, x, y);
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
    private Graphic getFeature(ArcGISFeatureLayer fLayer, float x, float y) {

        // Get the graphics near the Point.
        int[] ids = fLayer.getGraphicIDs(x, y, 10, 1);
        if (ids == null || ids.length == 0) {
            return null;
        }
        return fLayer.getGraphic(ids[0]);
    }

    /**
     * Shows the Attribute values for the Graphic in the Callout
     *
     * @param calloutView a callout to show
     * @param graphic selected graphic
     * @param mapPoint point to show callout
     */
    private void showCallout(Callout calloutView, Graphic graphic, Point mapPoint) {

        // Get the values of attributes for the Graphic
        String name = (String) graphic.getAttributeValue("name");

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

//            String whereClause = "objectid >'" + "1" + "'";
//
//            // Define a new query and set parameters
//            QueryParameters mParams = new QueryParameters();
//            mParams.setWhere(whereClause);
//            mParams.setReturnGeometry(true);
//
//            // Define the new instance of QueryTask
//            QueryTask queryTask = new QueryTask(mFeatureServiceLayerURL);
//            FeatureResult results;
//
//            try {
//                // run the querytask
//                results = queryTask.execute(mParams);
//                return results;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

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
            allFeatures = results;


            // iterate through results
            int index = 0;
            final String[] imageUrls = new String[(int) results.featureCount()];
            for (Object element : results) {
                // if object is feature cast to feature
                if (element instanceof Feature){
                    Feature feature = (Feature) element;
                    imageUrls[index] = feature.getAttributeValue("thumb_url").toString();
                    index++;
                    txtSymbol.setText(index+"");

                    // convert feature to graphic
                    Graphic graphic = new Graphic(feature.getGeometry(), sms, feature.getAttributes());
                    // merge extent with point
                    Graphic gr = new Graphic(feature.getGeometry(), txtSymbol,feature.getAttributes());
                    extent.merge((Point)graphic.getGeometry());
                    extent.merge((Point)graphic.getGeometry());
                    System.out.println("geo:"+((Feature) element).getGeometry()+"attris:"+((Feature) element).getAttributes());
                    // add it to the layer
                    mGraphicsLayer.addGraphic(graphic);
                    mGraphicsLayer.addGraphic(gr);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myHorizotalScrollView.setImageUrls(imageUrls);
                }
            });

            // Set the map extent to the envelope containing the result graphics
            mMapView.setExtent(extent, 100);
            // Disable the progress dialog
            progress.dismiss();

        }
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
