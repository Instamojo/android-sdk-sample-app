package com.instamojo.androidsdksample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.instamojo.android.activities.PaymentActivity;
import com.instamojo.android.callbacks.JuspayRequestCallback;
import com.instamojo.android.helpers.Constants;
import com.instamojo.android.models.Card;
import com.instamojo.android.models.Errors;
import com.instamojo.android.models.Order;
import com.instamojo.android.network.Request;

import java.util.ArrayList;
import java.util.Collections;

public class CustomUIActivity extends AppCompatActivity {

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_form);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(R.layout.layout_loading_dialog);
        dialog = builder.create();

        makeUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //send back the result to Main activity
        if (requestCode == Constants.REQUEST_CODE) {
            setResult(resultCode);
            setIntent(data);
            finish();
        }
    }

    private void makeUI() {
        final Order order = getIntent().getParcelableExtra(Constants.ORDER);
        //finish the activity if the order is null or both the debit and netbanking is disabled
        if (order == null || (order.getCardOptions() == null
                && order.getNetBankingOptions() == null)) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        final AppCompatEditText cardNumber = findViewById(R.id.card_number);
        final AppCompatEditText cardExpiryDate = findViewById(R.id.card_expiry_date);
        cardNumber.setNextFocusDownId(R.id.card_expiry_date);
        final AppCompatEditText cardHoldersName = findViewById(R.id.card_holder_name);
        cardExpiryDate.setNextFocusDownId(R.id.card_holder_name);
        final AppCompatEditText cvv = findViewById(R.id.card_cvv);
        cardHoldersName.setNextFocusDownId(R.id.card_cvv);
        AppCompatButton proceed = findViewById(R.id.proceed_with_card);
        View separator = findViewById(R.id.net_banking_separator);
        AppCompatSpinner netBankingSpinner = findViewById(R.id.net_banking_spinner);

        if (order.getCardOptions() == null) {
            //seems like card payment is not enabled
            findViewById(R.id.card_layout_1).setVisibility(View.GONE);
            findViewById(R.id.card_layout_2).setVisibility(View.GONE);
            proceed.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);
        } else {
            proceed.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Card card = new Card();
                    card.setCardNumber(cardNumber.getText().toString());
                    card.setDate(cardExpiryDate.getText().toString());
                    card.setCardHolderName(cardHoldersName.getText().toString());
                    card.setCvv(cvv.getText().toString());

                    //Validate the card here
                    if (!cardValid(card)) {
                        return;
                    }

                    //Get order details form Juspay
                    proceedWithCard(order, card);
                }
            });
        }

        if (order.getNetBankingOptions() == null) {
            //seems like netbanking is not enabled
            separator.setVisibility(View.GONE);
            netBankingSpinner.setVisibility(View.GONE);
        } else {
            final ArrayList<String> banks = new ArrayList<>(order.getNetBankingOptions().getBanks().keySet());
            Collections.sort(banks);
            banks.add(0, "Select a Bank");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, banks);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            netBankingSpinner.setAdapter(adapter);
            netBankingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        return;
                    }

                    //User selected a Bank. Hence proceed to Juspay
                    String bankCode = order.getNetBankingOptions().getBanks().get(banks.get(position));
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.URL, order.getNetBankingOptions().getUrl());
                    bundle.putString(Constants.POST_DATA, order.
                            getNetBankingOptions().getPostData(bankCode));
                    startPaymentActivity(bundle);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });


        }
    }

    private void proceedWithCard(Order order, Card card) {
        dialog.show();
        Request request = new Request(order, card, new JuspayRequestCallback() {
            @Override
            public void onFinish(final Bundle bundle, final Exception error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (error != null) {
                            if (error instanceof Errors.ConnectionError) {
                                Log.e("App", "No internet");
                            } else if (error instanceof Errors.ServerError) {
                                Log.e("App", "Server Error. try again");
                            } else {
                                Log.e("App", error.getMessage());
                            }
                            return;
                        }
                        startPaymentActivity(bundle);
                    }
                });
            }
        });
        request.execute();
    }

    private boolean cardValid(Card card) {
        if (!card.isCardValid()) {

            if (!card.isCardNameValid()) {
                showErrorToast("Card Holder's Name is invalid");
            }

            if (!card.isCardNumberValid()) {
                showErrorToast("Card Number is invalid");
            }

            if (!card.isDateValid()) {
                showErrorToast("Expiry date is invalid");
            }

            if (!card.isCVVValid()) {
                showErrorToast("CVV is invalid");
            }

            return false;
        }

        return true;
    }

    private void startPaymentActivity(Bundle bundle) {
        // Start the payment activity
        //Do not change this unless you know what you are doing
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra(Constants.PAYMENT_BUNDLE, bundle);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
