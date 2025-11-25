package com.example.smsapplication;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SMSApplication";
    private static final int SMS_PERMISSION_CODE = 100;
    private static final int SMS_CHARACTER_LIMIT = 159;

    private Button sendBtn, composeBtn;
    private EditText txtPhoneNo;
    private EditText txtMessage;
    private TextView tvCharacterCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        sendBtn = findViewById(R.id.btnSendSMS);
        composeBtn = findViewById(R.id.btnComposeSMS);
        txtPhoneNo = findViewById(R.id.editTextPhone);
        txtMessage = findViewById(R.id.editTextMessage);
        tvCharacterCount = findViewById(R.id.tvCharacterCount);

        // Set up character counter
        setupCharacterCounter();

        // Set up button click listeners
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateInputs()) {
                    checkSmsPermissionAndSend();
                }
            }
        });

        composeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateInputs()) {
                    composeSMS();
                }
            }
        });
    }

    /**
     * Sets up real-time character counter for SMS message
     */
    private void setupCharacterCounter() {
        txtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvCharacterCount.setText(length + "/" + SMS_CHARACTER_LIMIT);

                // Change color based on character count
                if (length > SMS_CHARACTER_LIMIT) {
                    tvCharacterCount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (length > SMS_CHARACTER_LIMIT - 20) {
                    tvCharacterCount.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                } else {
                    tvCharacterCount.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    /**
     * Validates phone number and message inputs
     * @return true if all inputs are valid
     */
    private boolean validateInputs() {
        String phoneNo = txtPhoneNo.getText().toString().trim();
        String message = txtMessage.getText().toString().trim();

        // Validate phone number
        if (phoneNo.isEmpty()) {
            txtPhoneNo.setError("Phone number is required");
            txtPhoneNo.requestFocus();
            return false;
        }

        if (!isValidKenyanPhoneNumber(phoneNo)) {
            txtPhoneNo.setError("Invalid Kenyan phone number format. Use 07..., +2547..., or 2547...");
            txtPhoneNo.requestFocus();
            return false;
        }

        // Validate message
        if (message.isEmpty()) {
            txtMessage.setError("Message cannot be empty");
            txtMessage.requestFocus();
            return false;
        }

        // Warn about message length
        if (message.length() > SMS_CHARACTER_LIMIT) {
            Toast.makeText(this,
                    "Warning: Your message exceeds " + SMS_CHARACTER_LIMIT +
                            " characters and will be sent as multiple SMS",
                    Toast.LENGTH_LONG).show();
        }

        return true;
    }

    /**
     * Validates Kenyan phone number format
     * Accepts: 07XXXXXXXX, +2547XXXXXXXX, 2547XXXXXXXX
     * @param phoneNumber the phone number to validate
     * @return true if valid Kenyan phone number
     */
    private boolean isValidKenyanPhoneNumber(String phoneNumber) {
        // Remove spaces and dashes
        phoneNumber = phoneNumber.replaceAll("[\\s-]", "");

        // Kenyan phone number patterns
        Pattern pattern1 = Pattern.compile("^07[0-9]{8}$");           // 07XXXXXXXX
        Pattern pattern2 = Pattern.compile("^\\+2547[0-9]{8}$");      // +2547XXXXXXXX
        Pattern pattern3 = Pattern.compile("^2547[0-9]{8}$");         // 2547XXXXXXXX

        Matcher matcher1 = pattern1.matcher(phoneNumber);
        Matcher matcher2 = pattern2.matcher(phoneNumber);
        Matcher matcher3 = pattern3.matcher(phoneNumber);

        return matcher1.matches() || matcher2.matches() || matcher3.matches();
    }

    /**
     * Normalizes Kenyan phone number to standard format (07XXXXXXXX)
     * @param phoneNumber the phone number to normalize
     * @return normalized phone number
     */
    private String normalizePhoneNumber(String phoneNumber) {
        // Remove spaces and dashes
        phoneNumber = phoneNumber.replaceAll("[\\s-]", "");

        // Convert to 07XXXXXXXX format
        if (phoneNumber.startsWith("+254")) {
            phoneNumber = "0" + phoneNumber.substring(4);
        } else if (phoneNumber.startsWith("254")) {
            phoneNumber = "0" + phoneNumber.substring(3);
        }

        return phoneNumber;
    }

    /**
     * Checks SMS permission and sends message if granted
     */
    private void checkSmsPermissionAndSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE);
        } else {
            // Permission already granted
            sendSMSMessage();
        }
    }

    /**
     * Handles permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                sendSMSMessage();
            } else {
                Toast.makeText(this,
                        "SMS permission denied. Cannot send SMS.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Sends SMS message directly using SmsManager
     */
    protected void sendSMSMessage() {
        Log.i(TAG, "Sending SMS");

        String phoneNo = normalizePhoneNumber(txtPhoneNo.getText().toString().trim());
        String message = txtMessage.getText().toString().trim();

        try {
            SmsManager smsManager = SmsManager.getDefault();

            // If message is longer than SMS limit, divide it into multiple parts
            if (message.length() > SMS_CHARACTER_LIMIT) {
                // Send as multiple SMS
                smsManager.sendMultipartTextMessage(
                        phoneNo,
                        null,
                        smsManager.divideMessage(message),
                        null,
                        null
                );
                Toast.makeText(getApplicationContext(),
                        "SMS sent successfully (Multiple parts)",
                        Toast.LENGTH_LONG).show();
            } else {
                // Send single SMS
                smsManager.sendTextMessage(phoneNo, null, message, null, null);
                Toast.makeText(getApplicationContext(),
                        "SMS sent successfully",
                        Toast.LENGTH_LONG).show();
            }

            // Clear inputs after successful send
            clearInputs();

        } catch (SecurityException se) {
            Log.e(TAG, "Security Exception", se);
            Toast.makeText(getApplicationContext(),
                    "Permission denied. Please enable SMS permission in settings.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "SMS send failed", e);
            Toast.makeText(getApplicationContext(),
                    "SMS failed: " + e.getMessage() + ". Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Opens default SMS app with pre-filled data
     * Works with Google Messages and other SMS apps
     */
    protected void composeSMS() {
        Log.i(TAG, "Composing SMS");

        String phoneNo = normalizePhoneNumber(txtPhoneNo.getText().toString().trim());
        String message = txtMessage.getText().toString().trim();

        try {
            // Use ACTION_SENDTO with sms: URI scheme (works best with Google Messages)
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("sms:" + phoneNo));
            smsIntent.putExtra("sms_body", message);

            // Verify an app can handle this intent
            if (smsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(smsIntent);
                Log.i(TAG, "SMS composer opened successfully");
            } else {
                // No SMS app found
                Toast.makeText(MainActivity.this,
                        "No SMS app found. Please install an SMS application.",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "No SMS app available");
            }

        } catch (Exception e) {
            Log.e(TAG, "Compose SMS failed", e);
            Toast.makeText(MainActivity.this,
                    "Failed to open SMS app: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Clears all input fields
     */
    private void clearInputs() {
        txtPhoneNo.setText("");
        txtMessage.setText("");
        tvCharacterCount.setText("0/" + SMS_CHARACTER_LIMIT);
    }
}