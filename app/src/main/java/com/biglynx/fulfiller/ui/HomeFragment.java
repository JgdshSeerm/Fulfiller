package com.biglynx.fulfiller.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.biglynx.fulfiller.MainActivity;
import com.biglynx.fulfiller.R;
import com.biglynx.fulfiller.adapter.FulfillerConfirmAdapter;
import com.biglynx.fulfiller.adapter.FulfillerPendingAdapter;
import com.biglynx.fulfiller.app.MyApplication;
import com.biglynx.fulfiller.listeners.OnRecyclerItemClickListener;
import com.biglynx.fulfiller.models.FulfillersDTO;
import com.biglynx.fulfiller.models.FullfillerKpi;
import com.biglynx.fulfiller.models.SignInResult;
import com.biglynx.fulfiller.network.FullFillerApiWrapper;
import com.biglynx.fulfiller.utils.AppPreferences;
import com.biglynx.fulfiller.utils.AppUtil;
import com.biglynx.fulfiller.utils.Common;
import com.biglynx.fulfiller.utils.Constants;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.biglynx.fulfiller.utils.Constants.OOPS_SOMETHING_WENT_WRONG;

/*
 *
 * Created by Biglynx on 7/21/2016.
 *
*/

public class HomeFragment extends Fragment implements View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener, OnRecyclerItemClickListener {

    List<FulfillersDTO> compltedFulfillerList;
    List<FulfillersDTO> compltedFulfillerList_less;
    List<FulfillersDTO> pendingdFulfillerList;
    List<FulfillersDTO> pendingdFulfillerList_less;
    RecyclerView confirmList, waitinglist_LI;
    FulfillerConfirmAdapter fulfillerConfirmAdapter;
    FulfillerPendingAdapter fulfillerPendingAdapter;
    LocationManager locationManager;
    SwitchCompat switchCompat;
    ImageView userimage_imv;
    Date date;
    TextView complete_sm_tv, waiting_sm_tv, username_tv,
            fulfillemts_tv, earned_tv, mildesdriven_tv;
    private FullFillerApiWrapper apiWrapper;
    private String rollType, editProfileType, getProfile;
    private TextView userType_tv;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout fulfiller_profile_info_LI;
    private AlertDialog alertDialog;
    private static final String TAG = HomeFragment.class.getSimpleName();
    public static final int CANCEL_INTEREST = 1;
    public static String FULFLMNT_DELIVERED = "Delivered";

    private RelativeLayout noItems_confirm_LI, noItems_wait_LI;
    private RecyclerView.LayoutManager mConfrirmListManager, mPendingListManager;
    private List<FulfillersDTO> dasBoardListCall = new ArrayList<>();
    private boolean dialogShown = false;

    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if (!Common.isNetworkAvailable(getActivity())) {
                AppUtil.toast(getActivity(), getString(R.string.check_interent_connection));
                return;
            }
            Common.showDialog(getContext());
            HashMap<String, Object> infoObject = new HashMap<>();
            infoObject = getProfileInfo(isChecked);


            apiWrapper.editProfileCallInHome(AppPreferences.getInstance(getActivity()).getSignInResult().optString("AuthNToken"),
                    editProfileType, infoObject, new Callback<SignInResult>() {
                        @Override
                        public void onResponse(Call<SignInResult> call, Response<SignInResult> response) {
                            switchCompat.setOnCheckedChangeListener(null);
                            if (response.isSuccessful()) {
                                SignInResult editProfileModel = response.body();
                                if (editProfileModel != null) {
                                    switchCompat.setChecked(editProfileModel.ReadyFulfill);
                                    AppPreferences.getInstance(getActivity()).setSignInResult(editProfileModel);
                                }
                            } else {
                                switchCompat.setChecked(!switchCompat.isChecked());
                                try {
                                    AppUtil.parseErrorMessage(getActivity(), response.errorBody().string());
                                } catch (IOException e) {
                                    AppUtil.toast(getActivity(), OOPS_SOMETHING_WENT_WRONG);
                                    e.printStackTrace();
                                }

                                AppUtil.CheckErrorCode(getActivity(), response.code());
                            }
                            Common.disMissDialog();
                            switchCompat.setOnCheckedChangeListener(onCheckedChangeListener);
                        }

                        @Override
                        public void onFailure(Call<SignInResult> call, Throwable t) {
                            switchCompat.setOnCheckedChangeListener(null);
                            switchCompat.setChecked(!switchCompat.isChecked());
                            switchCompat.setOnCheckedChangeListener(onCheckedChangeListener);
                            //AppUtil.toast(getContext(), OOPS_SOMETHING_WENT_WRONG);
                            Common.disMissDialog();
                        }
                    });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 21) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
        }
        View v = inflater.inflate(R.layout.homeactivity, container, false);
        if (getActivity().getIntent().hasExtra(FULFLMNT_DELIVERED)) {
            if (!dialogShown)
                showFulfilmntDeliveredDialog();
        }
        showNoticeDialog();

        initViews(v);

        /*waitinglist_LI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivityForResult(new Intent(getActivity(), InterestDetails.class)
                        .putExtra("interestId", "" + pendingdFulfillerList.get(position).FulfillerInterestId)
                        .putExtra("completed", "not"), CANCEL_INTEREST);
            }
        });*/
        /*confirmList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("confir id", "" + compltedFulfillerList.get(position).FulfillerInterestId);
                startActivity(new Intent(getActivity(), InterestDetails.class)
                        .putExtra("interestId", "" + compltedFulfillerList.get(position).FulfillerInterestId)
                        .putExtra("completed", "completed"));
            }
        });*/

        apiWrapper = new FullFillerApiWrapper();
        callServices(true);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Common.showGPSDisabledAlertToUser(getContext());
        }

        switchCompat.setOnCheckedChangeListener(onCheckedChangeListener);

        return v;
    }

    private void showFulfilmntDeliveredDialog() {
        dialogShown = true;
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.navigate_to_payments_page))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(getActivity(), PaymentsActivity.class));
                    }
                })
                .create();
        dialog.show();
    }

    private void initViews(View v) {
        compltedFulfillerList = new ArrayList<>();
        pendingdFulfillerList = new ArrayList<>();
        compltedFulfillerList_less = new ArrayList<>();
        pendingdFulfillerList_less = new ArrayList<>();

        switchCompat = (SwitchCompat) v.findViewById(R.id.Switch);
        confirmList = (RecyclerView) v.findViewById(R.id.confirmlist_LI);
        waitinglist_LI = (RecyclerView) v.findViewById(R.id.waitinglist_LI);
        userimage_imv = (ImageView) v.findViewById(R.id.userimage_imv);
        complete_sm_tv = (TextView) v.findViewById(R.id.complete_sm_tv);
        waiting_sm_tv = (TextView) v.findViewById(R.id.waiting_sm_tv);
        username_tv = (TextView) v.findViewById(R.id.username_tv);
        noItems_wait_LI = (RelativeLayout) v.findViewById(R.id.noitems_wait_LI);
        noItems_confirm_LI = (RelativeLayout) v.findViewById(R.id.noitems_confirm_LI);
        fulfillemts_tv = (TextView) v.findViewById(R.id.fulfillemts_tv);
        earned_tv = (TextView) v.findViewById(R.id.earned_tv);
        mildesdriven_tv = (TextView) v.findViewById(R.id.mildesdriven_tv);
        userType_tv = (TextView) v.findViewById(R.id.userType_tv);
        userType_tv.setVisibility(View.VISIBLE);
        swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_To_Refresh_Layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        fulfiller_profile_info_LI = (LinearLayout) v.findViewById(R.id.fulfiller_profile_info_LI);

        fulfiller_profile_info_LI.setOnClickListener(this);
        waiting_sm_tv.setOnClickListener(this);
        complete_sm_tv.setOnClickListener(this);

        mConfrirmListManager = new LinearLayoutManager(getActivity());
        mPendingListManager = new LinearLayoutManager(getActivity());
        confirmList.setHasFixedSize(true);
        waitinglist_LI.setHasFixedSize(true);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(),
                LinearLayoutManager.VERTICAL);
        confirmList.addItemDecoration(dividerItemDecoration);
        waitinglist_LI.addItemDecoration(dividerItemDecoration);

        confirmList.setLayoutManager(mConfrirmListManager);
        waitinglist_LI.setLayoutManager(mPendingListManager);
        fulfillerConfirmAdapter = new FulfillerConfirmAdapter(getActivity(), compltedFulfillerList, this);
        fulfillerPendingAdapter = new FulfillerPendingAdapter(getActivity(), pendingdFulfillerList, true, this);
        confirmList.setAdapter(fulfillerConfirmAdapter);
        waitinglist_LI.setAdapter(fulfillerPendingAdapter);


        if (AppPreferences.getInstance(getActivity()).getSignInResult() != null) {
            Log.e(HomeFragment.class.getSimpleName(), "AuthToken :: " + AppPreferences.getInstance(getActivity()).getSignInResult().optString("AuthNToken"));
            if (AppPreferences.getInstance(getActivity()).getSignInResult().optString("Role").equals("DeliveryPartner")) {
                rollType = "Partner";
                editProfileType = "editprofilepartner";
                getProfile = "getdeliveypartnerprofile";
                userType_tv.setText("Deliver Partner");
            } else {
                rollType = "Person";
                editProfileType = "editprofileperson";
                userType_tv.setText("Deliver Person");
                getProfile = "getdeliverypersonprofile";
            }
        }

        if (AppPreferences.getInstance(getActivity()).getSignInResult().optString("Role").equals("DeliveryPartner")) {
            if (!TextUtils.isEmpty(AppPreferences.getInstance(getActivity()).getSignInResult().optString("BusinessLegalName")))
                username_tv.setText(AppPreferences.getInstance(getActivity()).getSignInResult().optString("BusinessLegalName"));
            if (!TextUtils.isEmpty(AppPreferences.getInstance(getActivity()).getSignInResult().optString("CompanyLogo"))) {
                Picasso.with(getActivity()).load(AppPreferences.getInstance(getActivity()).getSignInResult().optString("CompanyLogo"))
                        .error(R.drawable.ic_no_profile_img).into(userimage_imv);
            }
        } else {
            if (!TextUtils.isEmpty(AppPreferences.getInstance(getActivity()).getSignInResult().optString("FirstName")))
                username_tv.setText(AppPreferences.getInstance(getActivity()).getSignInResult().optString("FirstName"));
            if (!TextUtils.isEmpty(AppPreferences.getInstance(getActivity()).getSignInResult().optString("ProfileImage"))) {
                Picasso.with(getActivity()).load(AppPreferences.getInstance(getActivity()).getSignInResult().optString("ProfileImage"))
                        .error(R.drawable.ic_no_profile_img).into(userimage_imv);
            }
        }

        switchCompat.setOnCheckedChangeListener(null);
        switchCompat.setChecked(AppPreferences.getInstance(getActivity()).getSignInResult() != null ?
                AppPreferences.getInstance(getActivity()).getSignInResult().optBoolean("ReadyFufill") : false);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CANCEL_INTEREST:
                if (resultCode == Activity.RESULT_OK) {
                    callServices(true);
                }
                break;
        }
    }

    private void showNoticeDialog() {
        boolean showNoticeDialog = checkStatus();
        if (!showNoticeDialog)
            return;

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.not_activated_dialog, null);
        TextView refresh_tv = (TextView) view.findViewById(R.id.refresh_tv);
        TextView resendActivationMail_tv = (TextView) view.findViewById(R.id.resend_verification_mail_tv);
        refresh_tv.setOnClickListener(this);
        SpannableString spannableString = new SpannableString(getString(R.string.resend_verification_mail));
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                resendActivationMail();
            }
        }, 39, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), 39, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        resendActivationMail_tv.setMovementMethod(LinkMovementMethod.getInstance());
        resendActivationMail_tv.setText(spannableString);

        alertDialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();
        //alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
        /*try {
            JSONObject signInREsult = AppPreferences.getInstance(getActivity()).getSignInResult();
            signInREsult.put("showNoticeDialog", false);
            Gson gson = new Gson();
            String resultString = signInREsult.toString();
            SignInResult signInResult = gson.fromJson(resultString, SignInResult.class);
            AppPreferences.getInstance(getActivity()).setSignInResult(signInResult);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    private void resendActivationMail() {
        if (!Common.isNetworkAvailable(getActivity())) {
            AppUtil.toast(getActivity(), "Network Disconnected. Please check...");
            return;
        }
        /*String fulfillerID = AppPreferences.getInstance(getActivity()).getSignInResult() != null ?
                AppPreferences.getInstance(getActivity()).getSignInResult().optString("FulfillerId") : "";
        String userType = "";
        if (AppPreferences.getInstance(getActivity()).getSignInResult() != null) {
            if (AppPreferences.getInstance(getActivity()).getSignInResult().optString("Role").equals("DeliveryPerson"))
                userType = HttpAdapter.RESEND_ACTIVATION_MAIL_DRIVER;
            else
                userType = HttpAdapter.RESEND_ACTIVATION_MAIL_PARTNER;
        }*/
        if (alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();

        Common.showDialog(getActivity());
        apiWrapper.resendActivationCall(AppPreferences.getInstance(getActivity()).getSignInResult() != null ?
                        AppPreferences.getInstance(getActivity()).getSignInResult().optString("AuthNToken") : "",
                new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.e(TAG, "resend Activation Successful");
                        } else {
                            /*try {
                                AppUtil.parseErrorMessage(getActivity(), response.errorBody().string());
                            } catch (IOException e) {
                                AppUtil.toast(getActivity(), OOPS_SOMETHING_WENT_WRONG);
                                e.printStackTrace();
                            }*/

                            Log.e(TAG, "resend Activation error");
                        }
                        if (alertDialog != null && !alertDialog.isShowing())
                            showNoticeDialog();
                        AppUtil.CheckErrorCode(getActivity(), response.code());
                        Common.disMissDialog();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "resend Activation error");
                        AppUtil.toast(getActivity(), OOPS_SOMETHING_WENT_WRONG);
                        if (alertDialog != null && !alertDialog.isShowing())
                            showNoticeDialog();
                        Common.disMissDialog();
                    }
                });
    }

    private boolean checkStatus() {
        if (TextUtils.isEmpty(AppPreferences.getInstance(getActivity()).getSignInResult().optString("Status")))
            return true;
        else if (!AppPreferences.getInstance(getActivity()).getSignInResult().optString("Status")
                .equalsIgnoreCase("activated"))
            return true;
        else
            return false;
    }

    private void callServices(final boolean showProgress) {
        if (Common.isNetworkAvailable(getActivity())) {
            if (showProgress)
                Common.showDialog(getActivity());

            //FullFillerKpi(fields) API
            apiWrapper.fullfillerKpi(AppPreferences.getInstance(getActivity()).getSignInResult().optString("AuthNToken"),
                    AppPreferences.getInstance(getActivity()).getSignInResult().optString("FulfillerId"), new Callback<FullfillerKpi>() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void onResponse(Call<FullfillerKpi> call, Response<FullfillerKpi> response) {
                            if (!isVisible())
                                return;
                            if (response.isSuccessful()) {
                                FullfillerKpi fullfillerKpi = response.body();
                                Log.d("HomeFragment", "FulfilerId :: " + fullfillerKpi.FulfilerId);
                                fulfillemts_tv.setText(fullfillerKpi.Count);
                                earned_tv.setText("$" + fullfillerKpi.Amount);
                                mildesdriven_tv.setText(AppUtil.getTwoDecimals(fullfillerKpi.NoOfMilesDriven));
                            } else {
                                try {
                                    AppUtil.parseErrorMessage(getActivity(), response.errorBody().string());
                                } catch (IOException e) {
                                    AppUtil.toast(getActivity(), OOPS_SOMETHING_WENT_WRONG);
                                    e.printStackTrace();
                                }

                                AppUtil.CheckErrorCode(getActivity(), response.code());
                            }
                            if (showProgress)
                                Common.disMissDialog();
                            else
                                swipeRefreshLayout.setRefreshing(false);
                        }

                        @Override
                        public void onFailure(Call<FullfillerKpi> call, Throwable t) {
                            if (!isVisible())
                                return;
                            Log.e("HomeFragment", "fullfillerKpi :: " + t.getMessage());
                            if (t instanceof UnknownHostException) {
                                finishActivity();
                            }
                            //AppUtil.toast(getContext(), OOPS_SOMETHING_WENT_WRONG);
                            if (showProgress)
                                Common.disMissDialog();
                            else
                                swipeRefreshLayout.setRefreshing(false);
                        }
                    });

            if (showProgress)
                Common.showDialog(getActivity());
            //DashboardAPI (fulfillments)
            apiWrapper.dashBoardModelCall(AppPreferences.getInstance(getActivity()).getSignInResult().optString("AuthNToken"),
                    AppPreferences.getInstance(getActivity()).getSignInResult().optString("FulfillerId"),
                    rollType, new Callback<List<FulfillersDTO>>() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void onResponse(Call<List<FulfillersDTO>> call, Response<List<FulfillersDTO>> response) {
                            if (!isVisible())
                                return;
                            if (response.isSuccessful()) {
                                dasBoardListCall = response.body();
                                updateListItems();
                                MyApplication.getInstance().dasBoardListCall = dasBoardListCall;

                                if (compltedFulfillerList != null && compltedFulfillerList_less.size() > 0) {
                                    if (compltedFulfillerList.size() > 2)
                                        complete_sm_tv.setVisibility(View.VISIBLE);
                                    else
                                        complete_sm_tv.setVisibility(View.GONE);
                                    /*fulfillerConfirmAdapter = new FulfillerConfirmAdapter(getActivity(), compltedFulfillerList_less);
                                    confirmList.setAdapter(fulfillerConfirmAdapter);*/
                                    fulfillerConfirmAdapter.setList(compltedFulfillerList_less);
                                    //Common.setListViewHeightBasedOnItems(confirmList);
                                    noItems_confirm_LI.setVisibility(View.GONE);
                                    confirmList.setVisibility(View.VISIBLE);
                                } else {
                                    complete_sm_tv.setVisibility(View.GONE);
                                    noItems_confirm_LI.setVisibility(View.VISIBLE);
                                    confirmList.setVisibility(View.GONE);
                                }
                                updateListItems();
                                if (pendingdFulfillerList != null && pendingdFulfillerList_less.size() > 0) {
                                    if (pendingdFulfillerList.size() > 2)
                                        waiting_sm_tv.setVisibility(View.VISIBLE);
                                    else
                                        waiting_sm_tv.setVisibility(View.GONE);
                                   /* fulfillerPendingAdapter = new FulfillerPendingAdapter(getActivity(), pendingdFulfillerList_less, true);
                                    waitinglist_LI.setAdapter(fulfillerPendingAdapter);*/
                                    fulfillerPendingAdapter.setList(pendingdFulfillerList_less);
                                    //Common.setListViewHeightBasedOnItems(waitinglist_LI);
                                    noItems_wait_LI.setVisibility(View.GONE);
                                    waitinglist_LI.setVisibility(View.VISIBLE);
                                } else {
                                    waiting_sm_tv.setVisibility(View.GONE);
                                    noItems_wait_LI.setVisibility(View.VISIBLE);
                                    waitinglist_LI.setVisibility(View.GONE);
                                }
                            } else {
                                try {
                                    AppUtil.parseErrorMessage(getActivity(), response.errorBody().string());
                                } catch (IOException e) {
                                    AppUtil.toast(getActivity(), OOPS_SOMETHING_WENT_WRONG);
                                    e.printStackTrace();
                                }
                                waiting_sm_tv.setVisibility(View.GONE);
                                complete_sm_tv.setVisibility(View.GONE);
                                noItems_wait_LI.setVisibility(View.VISIBLE);
                                noItems_confirm_LI.setVisibility(View.VISIBLE);

                                AppUtil.CheckErrorCode(getActivity(), response.code());
                            }
                            if (showProgress)
                                Common.disMissDialog();
                            else
                                swipeRefreshLayout.setRefreshing(false);
                        }

                        @Override
                        public void onFailure(Call<List<FulfillersDTO>> call, Throwable t) {
                            if (!isVisible())
                                return;
                            Log.e("HomeFragment", "DashboardAPI :: " + t.getMessage());
                            if (t instanceof UnknownHostException) {
                                finishActivity();
                            }
                            waiting_sm_tv.setVisibility(View.GONE);
                            complete_sm_tv.setVisibility(View.GONE);
                            noItems_wait_LI.setVisibility(View.VISIBLE);
                            noItems_confirm_LI.setVisibility(View.VISIBLE);
                            // AppUtil.toast(getContext(), OOPS_SOMETHING_WENT_WRONG);
                            if (showProgress)
                                Common.disMissDialog();
                            else
                                swipeRefreshLayout.setRefreshing(false);
                        }
                    });
        } else
            AppUtil.toast(getActivity(), "Network Disconnected. Please check...");
    }

    private void updateListItems() {
        emptyAllLists();
        if (dasBoardListCall != null) {
            for (int i = 0; i < dasBoardListCall.size(); i++) {
                FulfillersDTO fulfillersDTO = dasBoardListCall.get(i);
                if (fulfillersDTO.Status.equalsIgnoreCase("Confirmed")) {
                    compltedFulfillerList.add(fulfillersDTO);
                    if (compltedFulfillerList_less.size() < 2) {
                        compltedFulfillerList_less.add(fulfillersDTO);
                    }
                } else if (fulfillersDTO.Status.equalsIgnoreCase("Expressed")) {
                    pendingdFulfillerList.add(fulfillersDTO);
                    if (pendingdFulfillerList_less.size() < 2) {
                        pendingdFulfillerList_less.add(fulfillersDTO);
                    }
                }
            }
        }
    }

    private void finishActivity() {
        Common.disMissDialog();
        if (swipeRefreshLayout.isRefreshing())
            swipeRefreshLayout.setRefreshing(false);
        if (alertDialog != null)
            alertDialog.dismiss();
        AppUtil.toast(getActivity(), "Server error. Please login...");
        startActivity(new Intent(getActivity(), LoginActivity.class));
        getActivity().finish();
    }

    private void emptyAllLists() {
        if (compltedFulfillerList != null && compltedFulfillerList.size() > 0)
            compltedFulfillerList.clear();
        if (compltedFulfillerList_less != null && compltedFulfillerList_less.size() > 0)
            compltedFulfillerList_less.clear();
        if (pendingdFulfillerList != null && pendingdFulfillerList.size() > 0)
            pendingdFulfillerList.clear();
        if (pendingdFulfillerList_less != null && pendingdFulfillerList_less.size() > 0)
            pendingdFulfillerList_less.clear();
    }

    public HashMap getProfileInfo(boolean isChecked) {
        HashMap<String, Object> editProfileBodyObject = new HashMap<>();
        editProfileBodyObject.put("fulfillerid", AppPreferences.getInstance(getActivity()).getSignInResult().optString("FulfillerId"));
        editProfileBodyObject.put("fulfillertype", AppPreferences.getInstance(getActivity()).getSignInResult().optString("Role"));
        editProfileBodyObject.put("Proximity", AppPreferences.getInstance(getActivity()).getSignInResult().optString("Proximity"));
        editProfileBodyObject.put("ReadyFulfill", isChecked ? 1 : 0);

        return editProfileBodyObject;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.waiting_sm_tv:

                if (waiting_sm_tv.getText().toString().equalsIgnoreCase("See More")) {
                    /*fulfillerPendingAdapter = new FulfillerPendingAdapter(getActivity(), pendingdFulfillerList, true);
                    waitinglist_LI.setAdapter(fulfillerPendingAdapter);*/
                    //Common.setListViewHeightBasedOnItems(waitinglist_LI);
                    updateListItems();
                    fulfillerPendingAdapter.setList(pendingdFulfillerList);
                    waiting_sm_tv.setText("See Less");
                } else if (waiting_sm_tv.getText().toString().equalsIgnoreCase("See Less")) {
                    /*fulfillerPendingAdapter = new FulfillerPendingAdapter(getActivity(), pendingdFulfillerList_less, true);
                    waitinglist_LI.setAdapter(fulfillerPendingAdapter);*/
                    //Common.setListViewHeightBasedOnItems(waitinglist_LI);
                    updateListItems();
                    fulfillerPendingAdapter.setList(pendingdFulfillerList_less);
                    waiting_sm_tv.setText("See More");
                }
                break;
            case R.id.complete_sm_tv:

                if (complete_sm_tv.getText().toString().equalsIgnoreCase("See More")) {

                    updateListItems();
                    fulfillerConfirmAdapter.setList(compltedFulfillerList);
                    /*fulfillerConfirmAdapter = new FulfillerConfirmAdapter(getActivity(), compltedFulfillerList);
                    confirmList.setAdapter(fulfillerConfirmAdapter);*/
                    //Common.setListViewHeightBasedOnItems(confirmList);
                    complete_sm_tv.setText("See Less");
                } else if (complete_sm_tv.getText().toString().equalsIgnoreCase("See Less")) {
                    updateListItems();
                    fulfillerConfirmAdapter.setList(compltedFulfillerList_less);
                    /*fulfillerConfirmAdapter = new FulfillerConfirmAdapter(getActivity(), compltedFulfillerList_less);
                    confirmList.setAdapter(fulfillerConfirmAdapter);*/
                    // Common.setListViewHeightBasedOnItems(confirmList);
                    complete_sm_tv.setText("See More");
                }
                break;
            case R.id.fulfiller_profile_info_LI:
                MainActivity.setCurrentTab(3);
                getFragmentManager().beginTransaction().add(R.id.tabFrameLayout, new SettingsFragment()).addToBackStack(null).commit();
                break;
            case R.id.refresh_tv:
                if (alertDialog != null && alertDialog.isShowing())
                    alertDialog.dismiss();
                Common.showDialog(getActivity());
                apiWrapper.getProfileInfo(AppPreferences.getInstance(getActivity()).getSignInResult() != null ?
                                AppPreferences.getInstance(getActivity()).getSignInResult().optString("AuthNToken") : "",
                        getProfile, new Callback<SignInResult>() {
                            @Override
                            public void onResponse(Call<SignInResult> call, Response<SignInResult> response) {
                                if (response.isSuccessful()) {
                                    SignInResult signInResult = response.body();
                                    /*if (TextUtils.isEmpty(signInResult.Status))
                                        signInResult.showNoticeDialog = true;
                                    else if (AppUtil.ifNotEmpty(signInResult.Status) && !signInResult.Status.equalsIgnoreCase("active"))
                                        signInResult.showNoticeDialog = true;*/
                                    AppPreferences.getInstance(getActivity()).setSignInResult(signInResult);
                                } else {
                                    /*try {
                                        //AppUtil.parseErrorMessage(getActivity(), response.errorBody().string());
									    AppUtil.toast(getActivity(), "Unable to process the request. Please try again later...");
                                    } catch (IOException e) {
                                        AppUtil.toast(getActivity(), OOPS_SOMETHING_WENT_WRONG);
                                        e.printStackTrace();
                                    }*/
                                }
                                showNoticeDialog();
                                Common.disMissDialog();
                            }

                            @Override
                            public void onFailure(Call<SignInResult> call, Throwable t) {
                                AppUtil.toast(getActivity(), "Unable to process the request. Please try again later...");
                                showNoticeDialog();
                                Common.disMissDialog();
                            }
                        });
                break;
        }
    }

    @Override
    public void onRefresh() {
        callServices(false);
    }

    @Override
    public void onRecyclerItemClcik(String fulfillerInterestId) {
        if (fulfillerInterestId == null)
            return;
        Log.d(TAG, "fulfillerInterestId :: " + fulfillerInterestId);
        /*if (tag.equals(Constants.PENDING)) {
            startActivityForResult(new Intent(getActivity(), InterestDetails.class)
                    .putExtra("interestId", "" + pendingdFulfillerList.get(position).FulfillerInterestId)
                    .putExtra("completed", "not"), CANCEL_INTEREST);
        } else {
            Log.d("confir id", "" + compltedFulfillerList.get(position).FulfillerInterestId);
            startActivity(new Intent(getActivity(), InterestDetails.class)
                    .putExtra("interestId", "" + compltedFulfillerList.get(position).FulfillerInterestId)
                    .putExtra("completed", "completed"));
        }*/
        startActivityForResult(new Intent(getActivity(), InterestDetails.class)
                        .putExtra("interestId", "" + fulfillerInterestId)
                , CANCEL_INTEREST);
    }
}
