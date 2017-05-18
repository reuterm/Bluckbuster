package com.retuerm.android.blockbuster;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.retuerm.android.blockbuster.data.FavouriteMoviesContract.FavouriteMoviesEntry;
import com.retuerm.android.blockbuster.utility.TrailerListTaskLoader;
import com.retuerm.android.blockbuster.utility.MovieItem;
import com.retuerm.android.blockbuster.utility.MovieTrailer;
import com.retuerm.android.blockbuster.utility.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity implements MovieTrailerListAdapter.TrailerAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<ArrayList<MovieTrailer>> {

    @BindView(R.id.iv_movie_poster) ImageView mPosterDisplay;
    @BindView(R.id.tv_movie_title) TextView mTitleDisplay;
    @BindView(R.id.tv_release_year) TextView mReleaseYearDisplay;
    @BindView(R.id.tv_plot_synopsis) TextView mPlotDisplay;
    @BindView(R.id.tv_rating) TextView mAverageRatingDisplay;
    @BindView(R.id.iv_favourite_bt) ImageButton mFavButton;
    @BindView(R.id.trailer_list) RecyclerView mRecyclerView;

    private MovieTrailerListAdapter mTrailerAdapter;
    private Bundle queryBundle;
    private MovieItem mMovie;

    private static final int TRAILER_LOADER_ID = 43;

    public static final String MOVIE_ID_EXTRA = "movie_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ButterKnife.bind(this);

        Intent intent = getIntent();

        mTrailerAdapter = new MovieTrailerListAdapter(this);
        mRecyclerView.setAdapter(mTrailerAdapter);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        if(intent.hasExtra(MainActivity.PASS)) {
            mMovie = intent.getParcelableExtra(MainActivity.PASS);
            Uri posterUri = Uri.parse(mMovie.getPosterURL());

            queryBundle = new Bundle();
            queryBundle.putInt(MOVIE_ID_EXTRA, mMovie.getId());
            getSupportLoaderManager().initLoader(TRAILER_LOADER_ID, queryBundle, this);

            // Nicely format release date string
            DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
            SimpleDateFormat format = new SimpleDateFormat("yyyy");
            String release;
            try {
                release = format.format(format.parse(mMovie.getReleaseDate()));
            } catch (ParseException e) {
                e.printStackTrace();
                release = mMovie.getReleaseDate();
            }


            Picasso.with(this).load(posterUri).into(mPosterDisplay);
            mTitleDisplay.setText(mMovie.getTitle());
            mReleaseYearDisplay.setText(release);
            mPlotDisplay.setText(mMovie.getPlotSynopsis());
            mAverageRatingDisplay.setText(getString(R.string.average_rating_format, mMovie.getVoteAverage()));

            String movieId = Integer.toString(mMovie.getId());
            Uri queryUri = FavouriteMoviesEntry.CONTENT_URI.buildUpon()
                    .appendPath(movieId).build();
            Cursor cursor = getContentResolver().query(
                    queryUri, null, null, null, null
            );
            if (cursor.getCount() > 0) {
                mMovie.setFavourite(true);
                mFavButton.setImageResource(R.drawable.ic_favorite_24dp);
            }
        }


    }

    public void favButtonClicked (View view) {
        if (mMovie.isFavourite()) {
            mMovie.setFavourite(false);
            String movieId = Integer.toString(mMovie.getId());
            Uri deleteUri = FavouriteMoviesEntry.CONTENT_URI.buildUpon()
                    .appendPath(movieId).build();
            int result = getContentResolver().delete(deleteUri, null, null);
            if(result > 0) {
                mFavButton.setImageResource(R.drawable.ic_favorite_border_24dp);
            }
        } else {
            mMovie.setFavourite(true);
            ContentValues cv = new ContentValues();
            cv.put(FavouriteMoviesEntry.COLUMN_MOVIE_ID, mMovie.getId());
            cv.put(FavouriteMoviesEntry.COLUMN_TITLE, mMovie.getTitle());
            cv.put(FavouriteMoviesEntry.COLUMN_RELEASE_DATE, mMovie.getReleaseDate());
            cv.put(FavouriteMoviesEntry.COLUMN_PLOT_SYNOPSIS, mMovie.getPlotSynopsis());
            cv.put(FavouriteMoviesEntry.COLUMN_AVERAGE_RATING, mMovie.getVoteAverage());
            cv.put(FavouriteMoviesEntry.COLUMN_POSTER_PATH, mMovie.getPosterPath());
            Uri result = getContentResolver().insert(
                    FavouriteMoviesEntry.CONTENT_URI, cv);
            if(result != null) {
                mFavButton.setImageResource(R.drawable.ic_favorite_24dp);
            }
        }
    }

    @Override
    public Loader<ArrayList<MovieTrailer>> onCreateLoader(int id, Bundle args) {
        return new TrailerListTaskLoader(this, args);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<MovieTrailer>> loader, ArrayList<MovieTrailer> data) {
        if(data != null) {
            MovieTrailer[] trailerList = new MovieTrailer[data.size()];
            trailerList = data.toArray(trailerList);
            mTrailerAdapter.setTrailerList(trailerList);
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<MovieTrailer>> loader) {

    }

    @Override
    public void onClick(MovieTrailer trailer) {
        String url = NetworkUtils.buildYoutubeURL(trailer.getKey());
        Intent watchTrailerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(watchTrailerIntent);
    }
}
