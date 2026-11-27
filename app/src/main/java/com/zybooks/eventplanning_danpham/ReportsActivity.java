package com.zybooks.eventplanning_danpham;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/**
 * ReportsActivity shows the maintenance reports for the user's aircraft
 * and lets the user verify that the audit log hasn't been tampered with.
 * It only displays what ReportsViewModel provides.
 */
public class ReportsActivity extends AppCompatActivity {

    private TextView textViewReport;
    private TextView textViewVerifyResult;

    private ReportsViewModel viewModel;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        userId = new SessionManager(this).getUserId();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(this))
                .get(ReportsViewModel.class);

        textViewReport = findViewById(R.id.textViewReport);
        textViewVerifyResult = findViewById(R.id.textViewVerifyResult);
        Button buttonVerify = findViewById(R.id.buttonVerifyAuditLog);
        buttonVerify.setOnClickListener(v -> viewModel.verifyAuditLog());

        viewModel.getReportText().observe(this, text -> {
            if (text != null) {
                textViewReport.setText(text);
            }
        });
        viewModel.getVerifyMessage().observe(this, text -> {
            if (text != null) {
                textViewVerifyResult.setText(text);
            }
        });
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }

    /**
     * Reload whenever the screen appears, so the report is always current
     */
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadReport(userId);
    }
}
