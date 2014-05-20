/*******************************************************************************
 * Copyright 2014, barter.li
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package li.barter.fragments;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request.Method;
import com.squareup.picasso.Picasso;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import li.barter.R;
import li.barter.chat.ChatService;
import li.barter.data.DBInterface;
import li.barter.data.DBInterface.AsyncDbQueryCallback;
import li.barter.data.DatabaseColumns;
import li.barter.data.SQLConstants;
import li.barter.data.SQLiteLoader;
import li.barter.data.TableMyBooks;
import li.barter.data.TableSearchBooks;
import li.barter.data.ViewUsersWithLocations;
import li.barter.http.HttpConstants;
import li.barter.http.HttpConstants.ApiEndpoints;
import li.barter.http.HttpConstants.RequestId;
import li.barter.http.BlRequest;
import li.barter.http.IBlRequestContract;
import li.barter.http.ResponseInfo;
import li.barter.utils.AppConstants;
import li.barter.utils.AppConstants.BarterType;
import li.barter.utils.AppConstants.FragmentTags;
import li.barter.utils.AppConstants.Keys;
import li.barter.utils.AppConstants.Loaders;
import li.barter.utils.AppConstants.QueryTokens;
import li.barter.utils.AppConstants.UserInfo;
import li.barter.utils.Logger;
import li.barter.utils.SharedPreferenceHelper;

@FragmentTransition(enterAnimation = R.anim.slide_in_from_right, exitAnimation = R.anim.zoom_out, popEnterAnimation = R.anim.zoom_in, popExitAnimation = R.anim.slide_out_to_right)
public class BookDetailFragment extends AbstractBarterLiFragment implements
AsyncDbQueryCallback,  LoaderCallbacks<Cursor>,OnClickListener {

	private static final String TAG = "ShowSingleBookFragment";

	private TextView            mIsbnTextView;
	private TextView            mTitleTextView;
	private TextView            mAuthorTextView;
	private TextView            mDescriptionTextView;
	private TextView            mSuggestedPriceLabelTextView;
	private TextView            mSuggestedPriceTextView;
	private ImageView           mBookImageView;
	private TextView            mPublicationDateTextView;
	private TextView			mOwnerLabel;
	private TextView			mBarterTypes;
	
//	private CheckBox            mBarterCheckBox;
//	private CheckBox            mReadCheckBox;
//	private CheckBox            mSellCheckBox;
//	private CheckBox            mWishlistCheckBox;
//	private CheckBox            mGiveAwayCheckBox;
//	private CheckBox            mKeepPrivateCheckBox;
//	private CheckBox[]          mBarterTypeCheckBoxes;
	
	private String              mBookId;
	private String              mUserId;
	private String              mImageUrl;
	private String              mId;
	private boolean             mOwnedByUser;
	private boolean             mCameFromOtherProfile;
	private ImageView			mOwnerImageView;
	private ImageView			mChatLinkImageView;

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		init(container, savedInstanceState);
		setHasOptionsMenu(true);
		setActionBarTitle(R.string.Book_Detail_fragment_title);
		final View view = inflater
				.inflate(R.layout.fragment_book_detail, container, false);
		initViews(view);

		getActivity().getWindow()
		.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
				| WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		final Bundle extras = getArguments();

		if (extras != null) {
			mBookId = extras.getString(Keys.BOOK_ID);
			mUserId = extras.getString(Keys.USER_ID);
			mId = extras.getString(Keys.ID);
			mCameFromOtherProfile = extras.getBoolean(Keys.OTHER_PROFILE_FLAG);
			if ((mUserId != null) && mUserId.equals(UserInfo.INSTANCE.getId())) {
				mOwnedByUser = true;
			} else {
				mOwnedByUser = false;
			}
		}

		updateViewForUser();
		loadBookDetails();
		getUserDetails(mUserId);
		loadUserDetails();
		setActionBarDrawerToggleEnabled(false);
		return view;
	}

	/**
	 * Checks whether the book belongs to the current user or not, and updates
	 * the UI accordingly
	 */
	 private void updateViewForUser() {

		if (mOwnedByUser) {
			mChatLinkImageView.setEnabled(false);
			mChatLinkImageView.setVisibility(View.GONE);
			mOwnerLabel.setVisibility(View.GONE);
			mOwnerImageView.setVisibility(View.GONE);

		}

		if (mCameFromOtherProfile) {
			mOwnerImageView.setVisibility(View.GONE);
			mOwnerLabel.setVisibility(View.GONE);
		}
	 }

	 private void loadBookDetails() {

		 if (mOwnedByUser) {
			 //Reached here either by creating a new book, OR by tapping a book item in My Profile
			 DBInterface.queryAsync(QueryTokens.LOAD_BOOK_DETAIL_CURRENT_USER, null, false, TableMyBooks.NAME, null, DatabaseColumns.BOOK_ID
					 + SQLConstants.EQUALS_ARG, new String[] {
					 mBookId
			 }, null, null, null, null, this);
		 } else {
			 //Reached here by tapping a book item in Books Around Me screen
			 DBInterface.queryAsync(QueryTokens.LOAD_BOOK_DETAIL_OTHER_USER, null, false, TableSearchBooks.NAME, null, DatabaseColumns.BOOK_ID
					 + SQLConstants.EQUALS_ARG, new String[] {
					 mBookId
			 }, null, null, null, null, this);
		 }

	 }

	 /**
	  * Gets references to the Views
	  * 
	  * @param view The content view of the fragment
	  */
	 private void initViews(final View view) {
		 mIsbnTextView = (TextView) view.findViewById(R.id.text_isbn);
		 mTitleTextView = (TextView) view.findViewById(R.id.text_title);
		 mAuthorTextView = (TextView) view.findViewById(R.id.text_author);
		 mBarterTypes = (TextView) view.findViewById(R.id.label_barter_types);
		 
		 mBookImageView = (ImageView) view.findViewById(R.id.book_avatar);
		 mOwnerLabel = (TextView) view.findViewById(R.id.label_owner_profile);
		 mOwnerImageView= (ImageView) view.findViewById(R.id.ownerpic);
		 mChatLinkImageView= (ImageView) view.findViewById(R.id.chatwithowner);
		 mDescriptionTextView = (TextView) view
				 .findViewById(R.id.text_description);

		 mSuggestedPriceTextView = (TextView) view
				 .findViewById(R.id.text_suggested_price);
		 mSuggestedPriceLabelTextView = (TextView) view
				 .findViewById(R.id.label_suggested_price);

		 mPublicationDateTextView = (TextView) view
				 .findViewById(R.id.text_publication_date);
		
		 
		 mOwnerImageView.setOnClickListener(this);
		 mChatLinkImageView.setOnClickListener(this);

		// initBarterTypeCheckBoxes(view);

	 }

	 //removing barter checkboxes
	 
//	 /**
//	  * Gets the references to the barter type checkboxes, set the tags to
//	  * simplify building the tags array when sending the request to server
//	  * 
//	  * @param view The content view of the fragment
//	  */
//	 private void initBarterTypeCheckBoxes(final View view) {
//		 mBarterCheckBox = (CheckBox) view.findViewById(R.id.checkbox_barter);
//		 mReadCheckBox = (CheckBox) view.findViewById(R.id.checkbox_read);
//		 mSellCheckBox = (CheckBox) view.findViewById(R.id.checkbox_sell);
//		 mWishlistCheckBox = (CheckBox) view
//				 .findViewById(R.id.checkbox_wishlist);
//		 mGiveAwayCheckBox = (CheckBox) view
//				 .findViewById(R.id.checkbox_give_away);
//		 mKeepPrivateCheckBox = (CheckBox) view
//				 .findViewById(R.id.checkbox_keep_private);
//
//		 // Set the barter tags
//		 mBarterCheckBox.setTag(R.string.tag_barter_type, BarterType.BARTER);
//		 mReadCheckBox.setTag(R.string.tag_barter_type, BarterType.READ);
//		 mSellCheckBox.setTag(R.string.tag_barter_type, BarterType.SALE);
//		 mWishlistCheckBox.setTag(R.string.tag_barter_type, BarterType.RENT);
//		 mGiveAwayCheckBox.setTag(R.string.tag_barter_type, BarterType.FREE);
//		 mKeepPrivateCheckBox
//		 .setTag(R.string.tag_barter_type, BarterType.PRIVATE);
//
//		 mBarterTypeCheckBoxes = new CheckBox[6];
//		 mBarterTypeCheckBoxes[0] = mBarterCheckBox;
//		 mBarterTypeCheckBoxes[1] = mReadCheckBox;
//		 mBarterTypeCheckBoxes[2] = mSellCheckBox;
//		 mBarterTypeCheckBoxes[3] = mWishlistCheckBox;
//		 mBarterTypeCheckBoxes[4] = mGiveAwayCheckBox;
//		 mBarterTypeCheckBoxes[5] = mKeepPrivateCheckBox;
//	 }

	 @Override
	 public void onBackPressed() {

		 if (getTag().equals(FragmentTags.MY_BOOK_FROM_ADD_OR_EDIT)) {
			 onUpNavigate();
		 } else {
			 super.onBackPressed();
		 }
	 }

	 @Override
	 protected Object getVolleyTag() {
		 return TAG;
	 }

	 @Override
	 public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {

		 if (mOwnedByUser) {
			 inflater.inflate(R.menu.menu_profile_show, menu);
		 }
	 }

	 @Override
	 public boolean onOptionsItemSelected(final MenuItem item) {
		 switch (item.getItemId()) {

		 case android.R.id.home: {
			 onUpNavigate();
			 return true;
		 }

		 case R.id.action_edit_profile: {
			 final Bundle args = new Bundle(2);
			 args.putString(Keys.BOOK_ID, mBookId);
			 args.putString(Keys.ID, mId);
			 args.putBoolean(Keys.EDIT_MODE, true);
			 loadFragment(mContainerViewId, (AbstractBarterLiFragment) Fragment
					 .instantiate(getActivity(), AddOrEditBookFragment.class
							 .getName(), args), FragmentTags.ADD_OR_EDIT_BOOK, true, FragmentTags.BS_EDIT_BOOK);

			 return true;
		 }

		 default: {
			 return super.onOptionsItemSelected(item);
		 }
		 }
	 }

	 @Override
	 public void onSuccess(final int requestId,
			 final IBlRequestContract request,
			 final ResponseInfo response) {

		 /*
		  * This will happen in the case where the user has newly signed in using
		  * email/passowrd and doesn't have a first name added yet. The request
		  * is placed into the queue from
		  * AbstractBarterLiFragment#onDialogClick()
		  */
		  if (requestId == RequestId.SAVE_USER_PROFILE) {

			  final Bundle userInfo = response.responseBundle;
			  UserInfo.INSTANCE.setFirstName(userInfo
					  .getString(HttpConstants.FIRST_NAME));
			  SharedPreferenceHelper
			  .set(getActivity(), R.string.pref_first_name, userInfo
					  .getString(HttpConstants.FIRST_NAME));
			  SharedPreferenceHelper
			  .set(getActivity(), R.string.pref_last_name, userInfo
					  .getString(HttpConstants.LAST_NAME));

			  loadChatFragment();

		  }
	 }

	 @Override
	 public void onBadRequestError(final int requestId,
			 final IBlRequestContract request, final int errorCode,
			 final String errorMessage, final Bundle errorResponseBundle) {
	 }

	 @Override
	 public void onInsertComplete(final int token, final Object cookie,
			 final long insertRowId) {
		 // TODO Auto-generated method stub

	 }

	 @Override
	 public void onDeleteComplete(final int token, final Object cookie,
			 final int deleteCount) {
		 // TODO Auto-generated method stub

	 }

	 @Override
	 public void onUpdateComplete(final int token, final Object cookie,
			 final int updateCount) {
		 // TODO Auto-generated method stub

	 }

	 @Override
	 public void onQueryComplete(final int token, final Object cookie,
			 final Cursor cursor) {

		 if ((token == QueryTokens.LOAD_BOOK_DETAIL_CURRENT_USER)
				 || (token == QueryTokens.LOAD_BOOK_DETAIL_OTHER_USER)) {
			 if (cursor.moveToFirst()) {
				 mIsbnTextView.setText(cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.ISBN_10)));
				 mTitleTextView.setText(cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.TITLE)));
				 mTitleTextView.setSelected(true);
				 mAuthorTextView.setText(cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.AUTHOR)));
				 mDescriptionTextView
				 .setText(Html.fromHtml(cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.DESCRIPTION))));

				 try {
					 if (!cursor.getString(cursor.getColumnIndex(DatabaseColumns.VALUE))
							 .equals(null)) {

						 mSuggestedPriceLabelTextView
						 .setVisibility(View.VISIBLE);
						 mSuggestedPriceTextView.setVisibility(View.VISIBLE);
						 mSuggestedPriceTextView
						 .setText(cursor.getString(cursor
								 .getColumnIndex(DatabaseColumns.VALUE)));
					 }
				 } catch (final Exception e) {
					 // handle value = null exception
				 }

				 mPublicationDateTextView
				 .setText(cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.PUBLICATION_YEAR)));

				 Logger.d(TAG, cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.IMAGE_URL)), "book image");

				 // Picasso.with(getActivity()).setDebugging(true);
				 Picasso.with(getActivity())
				 .load(cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.IMAGE_URL)))
						 .fit().into(mBookImageView);

				 final String barterType = cursor.getString(cursor
						 .getColumnIndex(DatabaseColumns.BARTER_TYPE));

				 if (!TextUtils.isEmpty(barterType)) {
					 setBarterCheckboxes(barterType);
				 }
			 }

			 cursor.close();
		 }

	 }

	 /**
	  * Checks the supported barter type of the book and updates the checkboxes
	  * 
	  * @param barterType The barter types supported by the book
	  */
	 private void setBarterCheckboxes(final String barterType) {

		 final String[] barterTypes = barterType
				 .split(AppConstants.BARTER_TYPE_SEPARATOR);
		 String barterTypeHashTag = "";
		 for (final String token : barterTypes) {
			  barterTypeHashTag=barterTypeHashTag+"#"+token+" ";

			
		 }
		 mBarterTypes.setText(barterTypeHashTag);
	 }

	 @Override
	 public void onClick(final View v) {
		 if (v.getId() == R.id.ownerpic) {

			 final Bundle args = new Bundle(1);

			 args.putString(Keys.USER_ID, mUserId);

			 loadFragment(R.id.frame_content, (AbstractBarterLiFragment) Fragment
					 .instantiate(getActivity(), OtherProfileFragment.class
							 .getName(), args), FragmentTags.OTHER_USER_PROFILE, true, null);

		 } 
		 
		 else if (v.getId() == R.id.chatwithowner) {

			 if (isLoggedIn()) {

				 if (hasFirstName()) {
					 loadChatFragment();
				 } else {
					 showAddFirstNameDialog();
				 }

			 } else {

				 final Bundle loginArgs = new Bundle(1);
				 loginArgs.putString(Keys.UP_NAVIGATION_TAG, FragmentTags.BS_LOGIN_FROM_BOOK_DETAIL);

				 loadFragment(R.id.frame_content, (AbstractBarterLiFragment) Fragment
						 .instantiate(getActivity(), LoginFragment.class
								 .getName(), loginArgs), FragmentTags.LOGIN_TO_CHAT, true, FragmentTags.BS_LOGIN_FROM_BOOK_DETAIL);

			 }
		 }
		 
		 
		 else {
			 // Show Login Fragment
		 }

	 }

	 /**
	  * Loads the Chat Fragment to chat with the book owner
	  */
	  private void loadChatFragment() {
		 final Bundle args = new Bundle(3);
		 args.putString(Keys.CHAT_ID, ChatService
				 .generateChatId(mUserId, UserInfo.INSTANCE.getId()));
		 args.putString(Keys.USER_ID, mUserId);
		 args.putString(Keys.BOOK_TITLE, mTitleTextView.getText().toString());

		 loadFragment(R.id.frame_content, (AbstractBarterLiFragment) Fragment
				 .instantiate(getActivity(), ChatDetailsFragment.class
						 .getName(), args), FragmentTags.CHAT_DETAILS, true, null);

	  }

	  private void getUserDetails(final String userid) {

		  final BlRequest request = new BlRequest(Method.GET, HttpConstants.getApiBaseUrl()
				  + ApiEndpoints.USERPROFILE, null, mVolleyCallbacks);
		  request.setRequestId(RequestId.GET_USER_PROFILE);

		  final Map<String, String> params = new HashMap<String, String>(2);

		  params.put(HttpConstants.ID, String.valueOf(userid));
		  request.setParams(params);

		  addRequestToQueue(request, true, 0);

	  }

	  /**
	   * Fetches books owned by the current user
	   */

	  private void loadUserDetails() {
		  getLoaderManager().restartLoader(Loaders.USER_DETAILS, null, this);
	  }

	  @Override
	  public Loader<Cursor> onCreateLoader(final int loaderId, final Bundle args) {
		  if(loaderId == Loaders.USER_DETAILS)
		  {
			  final String selection = DatabaseColumns.USER_ID
					  + SQLConstants.EQUALS_ARG;
			  final String[] argsId = new String[1];
			  argsId[0]=mUserId;
			  return new SQLiteLoader(getActivity(), false, ViewUsersWithLocations.NAME, null, selection, argsId, null, null, null, null);
		  }
		  else {


			  return null;
		  }
	  }

	  @Override
	  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		  if (loader.getId() == Loaders.USER_DETAILS) {

			  Logger.d(TAG, "Cursor Loaded with count: %d", cursor.getCount());
			  if(cursor.getCount()!=0)
			  {
				  cursor.moveToFirst();

				  mImageUrl = cursor.getString(cursor
						  .getColumnIndex(DatabaseColumns.PROFILE_PICTURE));
				  mOwnerImageView.setTag(mImageUrl);
				  Picasso.with(getActivity()).load(mImageUrl + "?type=large").fit()
				  .centerCrop().error(R.drawable.pic_avatar)
				  .into(mOwnerImageView);

				 
			  }

		  }

	  }

	  @Override
	  public void onLoaderReset(Loader<Cursor> arg0) {
		  // TODO Auto-generated method stub

	  }



}
