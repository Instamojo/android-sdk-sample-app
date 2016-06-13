package com.instamojo.androidsdksample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.instamojo.android.Instamojo;
import com.instamojo.android.activities.PaymentDetailsActivity;
import com.instamojo.android.callbacks.OrderRequestCallBack;
import com.instamojo.android.helpers.Constants;
import com.instamojo.android.models.Errors;
import com.instamojo.android.models.Order;
import com.instamojo.android.network.Request;
import com.instamojo.android.network.Urls;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final HashMap<String, String> env_options = new HashMap<>();

    static {
        env_options.put("Test Environment", "https://test.instamojo.com/");
        env_options.put("Production Environment", "https://api.instamojo.com/");
    }

    // accessToken is Private Auth Token provided at https://www.instamojo.com/integrations/
    private String accessToken = "wLd5A00ZwskdBhIlGNuFSx5LyhrjpC";
    private Random random = new Random(System.currentTimeMillis());
    private ProgressDialog dialog;
    private AppCompatEditText nameBox, emailBox, phoneBox, amountBox, descriptionBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.pay);
        nameBox = (AppCompatEditText) findViewById(R.id.name);
        emailBox = (AppCompatEditText) findViewById(R.id.email);
        phoneBox = (AppCompatEditText) findViewById(R.id.phone);
        amountBox = (AppCompatEditText) findViewById(R.id.amount);
        descriptionBox = (AppCompatEditText) findViewById(R.id.description);
        AppCompatSpinner envSpinner = (AppCompatSpinner) findViewById(R.id.env_spinner);
        final ArrayList<String> envs = new ArrayList<>(env_options.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, envs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        envSpinner.setAdapter(adapter);
        envSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Instamojo.setBaseUrl(env_options.get(envs.get(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("please wait...");
        dialog.setCancelable(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPay();
            }
        });

        Button updateToken = (Button) findViewById(R.id.update_token);
        updateToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateToken();
            }
        });

        //let's set the log level to debug
        Instamojo.setLogLevel(Log.DEBUG);
    }

    private void onClickPay() {
        String name = nameBox.getText().toString();
        final String email = emailBox.getText().toString();
        String phone = phoneBox.getText().toString();
        String amount = amountBox.getText().toString();
        String description = descriptionBox.getText().toString();

        //this is only for testing. Actual transaciton_id must be fetched from the server
        String transactionID = String.valueOf(random.nextInt());

        //Create the Order
        Order order = new Order(accessToken, transactionID, name, email, phone, amount, description);

        //Validate the Order
        if (!order.isValid()) {
            //oops order validation failed. Pinpoint the issue(s).

            if (!order.isValidName()) {
                nameBox.setError("Buyer name is invalid");
            }

            if (!order.isValidEmail()) {
                emailBox.setError("Buyer email is invalid");
            }

            if (!order.isValidPhone()) {
                phoneBox.setError("Buyer phone is invalid");
            }

            if (!order.isValidAmount()) {
                amountBox.setError("Amount is invalid or has more than two decimal places");
            }

            if (!order.isValidDescription()) {
                descriptionBox.setError("Description is invalid");
            }

            if (!order.isValidTransactionID()) {
                showToast("Transaction is Invalid");
            }

            if (!order.isValidRedirectURL()) {
                showToast("Redirection URL is invalid");
            }

            return;
        }

        //Validation is successful. Proceed
        dialog.show();
        Request request = new Request(order, new OrderRequestCallBack() {
            @Override
            public void onFinish(final Order order, final Exception error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (error != null) {
                            if (error instanceof Errors.ConnectionError) {
                                showToast("No internet connection");
                            } else if (error instanceof Errors.ServerError) {
                                showToast("Server Error. Try again");
                            } else if (error instanceof Errors.AuthenticationError) {
                                showToast("Access token is invalid or expired. Please Update the token!!");
                            } else if (error instanceof Errors.ValidationError) {
                                // Cast object to validation to pinpoint the issue
                                Errors.ValidationError validationError = (Errors.ValidationError) error;

                                if (!validationError.isValidTransactionID()) {
                                    showToast("Transaction ID is not Unique");
                                    return;
                                }

                                if (!validationError.isValidRedirectURL()) {
                                    showToast("Redirect url is invalid");
                                    return;
                                }

                                if (!validationError.isValidPhone()) {
                                    phoneBox.setError("Buyer's Phone Number is invalid/empty");
                                    return;
                                }

                                if (!validationError.isValidEmail()) {
                                    emailBox.setError("Buyer's Email is invalid/empty");
                                    return;
                                }

                                if (!validationError.isValidAmount()) {
                                    amountBox.setError("Amount is either less than Rs.9 or has more than two decimal places");
                                    return;
                                }

                                if (!validationError.isValidName()) {
                                    nameBox.setError("Buyer's Name is required");
                                    return;
                                }
                            } else {
                                showToast(error.getMessage());
                            }
                            return;
                        }

                        startPreCreatedUI(order);
                    }
                });
            }
        });

        request.execute();
    }

    private void startPreCreatedUI(Order order) {
        //Using Pre created UI
        Intent intent = new Intent(getBaseContext(), PaymentDetailsActivity.class);
        intent.putExtra(Constants.ORDER, order);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    private void startCustomUI(Order order) {
        //Custom UI Implementation
        Intent intent = new Intent(getBaseContext(), CustomUIActivity.class);
        intent.putExtra(Constants.ORDER, order);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Token should not be generated on the app like this. The token must be generated on your server
     * and should be fetched to your app.
     */
    private void updateToken() {
        if (!dialog.isShowing()) {
            dialog.show();
        }
        OkHttpClient client = new OkHttpClient();

        // Both client_id and client_secret should be associated with your Instamojo's user account
        //The client ID and client secret used here should not be used on your application
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", "cNrgex0RQ3P176F0jCjFfEyCy2UnXjunM1AZCIT8")
                .add("client_secret", "SEqtkfR4GriSPtZkwgBWKEEYCpA8nxa7Q8bDRHqJSWEX1nPyTdNL8hglzYYNvI6kCVGlLr7abPWZ0L9S77VwpBDUTGdaSM9EdZdatQQjmmeykTlyyMqiNuSQs6N6WBsW")
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(Urls.getBaseUrl() + "oauth2/token/")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                response.body().close();
                try {
                    JSONObject responseObject = new JSONObject(responseBody);
                    accessToken = responseObject.getString("access_token");
                    showToast("Updated token");
                } catch (JSONException e) {
                    showToast("Failed to update token");
                }
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE && data != null) {
            String orderID = data.getStringExtra(Constants.ORDER_ID);
            String transactionID = data.getStringExtra(Constants.TRANSACTION_ID);
            String paymentID = data.getStringExtra(Constants.PAYMENT_ID);

            // Check transactionID, orderID, and orderID for null before using them to check the Payment status.
            if (orderID != null && transactionID != null && paymentID != null) {
                showToast("Check for Payment with Order ID - " + orderID);
            } else {
                showToast("Oops!! Payment was cancelled");
            }
        }
    }
}
