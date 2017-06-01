package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.MainActivity;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.example.xyzreader.data.UpdaterService.BROADCAST_ACTION_STATE_CHANGE;

/**
 * Created by Neha on 20-05-2017.
 */

public class ArticalListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
// ------------------------------ FIELDS ------------------------------
private static final String TAG = MainActivity.class.toString();

    BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                boolean isRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI(isRefreshing);
            }
        }
    };

    private Adapter mAdapter;
    private boolean mIsRefreshing;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newAllArticlesInstance(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }

        // now set the items in the adapter

        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artical_list,container,false);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mRefreshingReceiver, new IntentFilter(BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mRefreshingReceiver);
        super.onStop();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        //INit swiperefresh layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.primaryColor);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        getActivity().getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        if (savedInstanceState == null) {
            refresh();
        }

    }


    private void refresh() {
        if (!mIsRefreshing) {
            updateRefreshingUI(true);
            getActivity().startService(new Intent(getActivity(), UpdaterService.class));
        }
    }

    private void updateRefreshingUI(boolean isRefreshing) {
        mIsRefreshing = isRefreshing;
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.list_items, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
//                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        View viewStart = vh.cardView;
//
//                        ActivityOptionsCompat options =
//
//                                ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
//                                        view,   // Starting view
//                                        getString(R.string.transition_string)    // The String
//                                );
//                        //Start the Intent
//                        ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
//                    }
//                    else {
                        startActivity(intent);
//                    }

                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
//            holder.thumbnailView.setImageUrl(
//                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
//                    ImageLoaderHelper.getInstance(getActivity()).getImageLoader());
//            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            Picasso.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .config(Bitmap.Config.RGB_565)
                    .into(holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public CardView cardView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            cardView = (CardView) view.findViewById(R.id.card_view);
        }
    }
}