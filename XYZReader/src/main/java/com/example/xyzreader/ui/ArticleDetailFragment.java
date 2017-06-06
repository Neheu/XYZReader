package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    //    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    //  private ColorDrawable mStatusBarColorDrawable;
    private FloatingActionButton shareButton;
    private ImageView mPhotoView;
    private int mScrollY;
    // private int mStatusBarFullOpacityBottom;
    private Toolbar mToolbar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private CollapsingToolbarLayout mCollapsingToolbar;
    private AppBarLayout appBarLayout;
    int barColor;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        setHasOptionsMenu(true);

    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

            mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
            shareButton = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
            mToolbar = (Toolbar) mRootView.findViewById(R.id.detail_toolbar);

            appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.app_bar);

            mCollapsingToolbar =
                    (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);
            mCollapsingToolbar.setTitleEnabled(true);

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setText("Some sample text")
                            .getIntent(), getString(R.string.action_share)));
                }
            });
        }

        bindViews();
        return mRootView;
    }

/*
    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
//        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }
*/


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

    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        /*TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
//        bylineView.setMovementMethod(new LinkMovementMethod());*/
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);



        ViewTreeObserver viewTreeObserver = shareButton.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        shareButton.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        shareButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    // not sure the above is equivalent, but that's beside the point for this example...
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) shareButton.getLayoutParams();
                    params.setMargins(0, 0, 16, -shareButton.getHeight() / 2); // (int left, int top, int right, int bottom)
                    shareButton.setLayoutParams(params);
                }
            });
        }
//        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {


            final String title = mCursor.getString(ArticleLoader.Query.TITLE);
            mCollapsingToolbar.setTitle(title);
           /* String author = Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)).toString();
            //            bylineView.setText(author);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(author);

            */
            final String body = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)).toString();
//            String photo = mCursor.getString(ArticleLoader.Query.PHOTO_URL);


//            titleView.setText(title);
//            bylineView.setText(author);
            bodyView.setText(body);




            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
//            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
           /* Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }
*///            bodyView.setText(Html.fromHtml("<align= 'justify'" + mCursor.getString(ArticleLoader.Query.BODY)
//                    .replaceAll("(\r\n|\n)", "<br />")));
           /* if (mToolbar != null) {

                mToolbar.setNavigationIcon(R.drawable.ic_back);
                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().finish();
                    }
                });
            }
                       mCollapsingToolbar.setTitle(title);

            */


            Glide.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .override(400,200)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            Bitmap bitmap = ((GlideBitmapDrawable) resource.getCurrent()).getBitmap();
                            if (bitmap != null) {
                                Palette palete = Palette.generate(bitmap, 12);
                                barColor = palete.getDarkVibrantColor(palete.getDarkMutedColor(0xFF333333));
                                int scrimColor = palete.getVibrantColor(palete.getMutedColor(0xFF888888));

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActivity() != null && getActivity().getWindow() != null) {
                                    getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                    getActivity().getWindow().setStatusBarColor(barColor);
                                }                                shareButton.setBackgroundTintList(ColorStateList.valueOf(scrimColor));
                                mCollapsingToolbar.setContentScrimColor(barColor);
                            }
                            return false;
                        }
                    })


                    .into(mPhotoView);
        } else {
            mRootView.setVisibility(View.GONE);
//            titleView.setText("N/A");
//            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        if (mToolbar != null) {

            mToolbar.setNavigationIcon(R.drawable.ic_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
        }


        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
//        bindViews();
    }


}
