package li.barter.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import li.barter.R;
import li.barter.fragments.AbstractBarterLiFragment;
import li.barter.fragments.BooksPagerFragment;
import li.barter.http.IBlRequestContract;
import li.barter.http.ResponseInfo;
import li.barter.utils.AppConstants;

/**
 * Activity for paging between the search results
 * <p/>
 * Created by vinay.shenoy on 11/07/14.
 */
public class SearchBookPagerActivity extends AbstractDrawerActivity {

    /** The position at which the books paging should start. */
    private int mStartingBookPosition;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        initDrawer(R.id.drawer_layout, R.id.frame_nav_drawer);
        mStartingBookPosition = getIntent().getIntExtra(AppConstants.Keys.BOOK_POSITION, 0);
        if (savedInstanceState == null) {
            loadBooksPagerFragment();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Loads the books pager fragment */
    private void loadBooksPagerFragment() {

        final Bundle showBookArgs = new Bundle(1);
        showBookArgs.putInt(AppConstants.Keys.BOOK_POSITION, mStartingBookPosition);
        loadFragment(R.id.frame_content,
                     (AbstractBarterLiFragment) Fragment.instantiate(
                             this,
                             BooksPagerFragment.class.getName(),
                             showBookArgs
                     ),

                     AppConstants.FragmentTags.BOOKS_PAGER, false, null
        );

    }

    @Override
    protected boolean isDrawerActionBarToggleEnabled() {
        return false;
    }

    @Override
    protected String getAnalyticsScreenName() {
        return null;
    }

    @Override
    protected Object getTaskTag() {
        return hashCode();
    }

    @Override
    public void onSuccess(final int requestId, final IBlRequestContract request,
                          final ResponseInfo response) {

    }

    @Override
    public void onBadRequestError(final int requestId, final IBlRequestContract request, final
    int errorCode, final String errorMessage, final Bundle errorResponseBundle) {

    }
}