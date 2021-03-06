package net.theunifyproject.lethalskillzz.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.android.volley.DefaultRetryPolicy;
import net.android.volley.Request;
import net.android.volley.Response;
import net.android.volley.VolleyError;
import net.android.volley.toolbox.StringRequest;
import net.theunifyproject.lethalskillzz.R;
import net.theunifyproject.lethalskillzz.adapter.GridAdapter;
import net.theunifyproject.lethalskillzz.app.AppConfig;
import net.theunifyproject.lethalskillzz.app.AppController;
import net.theunifyproject.lethalskillzz.app.ConnectionDetector;
import net.theunifyproject.lethalskillzz.app.PrefManager;
import net.theunifyproject.lethalskillzz.model.GridItem;
import net.theunifyproject.lethalskillzz.util.Logout;

/**
 * Created by Ibrahim on 20/12/2015.
 */
public class SearchStoreFragment extends Fragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener{

    private static final String TAG = SearchStoreFragment.class.getSimpleName();
    private Logout mLogout;

    private RecyclerView rView;
    private PrefManager pref;
    private List<GridItem> gridItems;
    private GridAdapter gridAdapter;
    //private HashMap<String, String> profile;
    private boolean isRefreshing = false;
    private SearchView mSearchView;
    private String searchQuery;

    private ConnectionDetector cd;

    public SearchStoreFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_search_store, container, false);

        // creating connection detector class instance
        cd = new ConnectionDetector(getActivity());

        mLogout = new Logout(getActivity());

        pref = new PrefManager(getActivity());
        //profile = pref.getUserDetails();
        gridItems = new ArrayList<GridItem>();

        LinearLayoutManager lLayout = new LinearLayoutManager(getActivity());
        rView = (RecyclerView) rootView.findViewById(R.id.search_store_recycler_view);
        rView.setLayoutManager(lLayout);
        rView.setHasFixedSize(true);

        mSearchView = (SearchView) rootView.findViewById(R.id.search_store_editor);
        setupSearchView();

        setGridAdapter();


        return rootView;
    }


    private void setupSearchView() {
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setSubmitButtonEnabled(false);
        //mSearchView.setQueryHint("Search Here");
        mSearchView.setIconified(false);

        TextView textView = (TextView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        textView.setTextColor(Color.WHITE);

        ImageView searchCloseIcon = (ImageView)mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchCloseIcon.setImageResource(R.drawable.ic_cross);

       /* ImageView searchIcon = (ImageView)mSearchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        searchIcon.setImageResource(R.drawable.ic_toolbar_search); */

    }


    private void setGridAdapter() {
        gridAdapter = new GridAdapter(getActivity(), rView, gridItems);
        rView.setAdapter(gridAdapter);

        gridAdapter.setOnLoadMoreListener(new GridAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (!isRefreshing) {
                    //add progress item
                    gridItems.add(null);
                    gridAdapter.notifyItemInserted(gridItems.size() - 1);

                    if (cd.isConnectingToInternet()) {
                        loadShopSearch(searchQuery, String.valueOf(gridAdapter.getItemCount() - 1));
                    } else {

                    }
                }
            }
        });

    }



    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
        } else {
            searchQuery = newText;
            if (cd.isConnectingToInternet()) {
                isRefreshing = true;
                loadShopSearch(searchQuery, String.valueOf(0));
            }else {

            }

        }
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        return false;
    }




    public boolean onClose() {

        getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                .remove(this).commit();
        return false;
    }


    private void loadShopSearch(final String searchQuery, final String search_pos) {

        // making fresh volley request and getting json
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_LOAD_STORE_SEARCH, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Response: " + response.toString());

                try {
                    JSONObject responseObj = new JSONObject(response);

                    // Parsing json object response
                    // response will be a json object

                    //Checking if current session is active
                    if(!responseObj.getBoolean("isSession"))
                        mLogout.logout();

                    boolean error = responseObj.getBoolean("error");
                    String message = responseObj.getString("message");

                    // checking for error
                    if (!error) {

                        // parsing the feed data
                        JSONObject feedObj = responseObj.getJSONObject("data");
                        parseJsonShopping(feedObj);

                    }else {

                    }

                } catch (JSONException e) {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    if (!isRefreshing) {
                        gridItems.remove(gridItems.size() - 1);
                        gridAdapter.notifyItemRemoved(gridItems.size());
                        gridAdapter.setLoaded();
                    }else isRefreshing = false;
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error: " + error.getMessage());

                if (!isRefreshing) {
                    gridItems.remove(gridItems.size() - 1);
                    gridAdapter.notifyItemRemoved(gridItems.size());
                    gridAdapter.setLoaded();
                }else isRefreshing = false;
            }
        }) {

            /**
             * Passing feed parameters to our server
             *
             * @return
             */
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", pref.getUsername());
                params.put("sessionId", pref.getSessionId());
                params.put("searchQuery", searchQuery);
                params.put("search_pos", search_pos);

                Log.e(TAG, "Posting params: " + params.toString());

                return params;
            }

        };

        //Setting request policy to max timeout
        strReq.setRetryPolicy(new DefaultRetryPolicy(0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        // Adding request to volley request queue
        AppController.getInstance().addToRequestQueue(strReq);
    }


    /**
     * Parsing json response and passing the data to search shop view grid adapter
     */
    private void parseJsonShopping(JSONObject response) {
        try {
            JSONArray shopArray = response.getJSONArray("shopping");


            if (isRefreshing) {
                gridItems.clear();
                isRefreshing = false;
            } else {
                gridItems.remove(gridItems.size() - 1);
                gridAdapter.notifyItemRemoved(gridItems.size());
                gridAdapter.setLoaded();
            }


            for (int i = 0; i < shopArray.length(); i++) {
                JSONObject shopObj = (JSONObject) shopArray.get(i);

                GridItem item = new GridItem();
                item.setId(shopObj.getInt("id"));
                item.setType(shopObj.getInt("type"));
                item.setPrice(shopObj.getString("price"));
                item.setTitle(shopObj.getString("title"));
                item.setImage(shopObj.getString("image"));

                gridItems.add(item);
            }

            // notify data changes to feed adapater
            gridAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }





}
