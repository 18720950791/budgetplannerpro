package com.twansoftware.budgetplannerpro.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.iface.OnBudgetAddedListener;
import com.twansoftware.budgetplannerpro.service.BudgetService;
import roboguice.fragment.RoboDialogFragment;

import javax.inject.Inject;
import java.util.Currency;
import java.util.Locale;

public class CreateBudgetDialogFragment extends RoboDialogFragment {
    private static final String BUDGET_NAME_KEY = "budget_name";
    private static final String BUDGET_BALANCE_KEY = "budget_balance";

    @Inject
    private BudgetService budgetService;
    private EditText budgetNameEdit;
    private EditText budgetStartingBalanceEdit;

    public CreateBudgetDialogFragment() {

    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.create_budget_dialog, null);
        budgetNameEdit = (EditText) view.findViewById(R.id.create_budget_dialog_budget_name);
        budgetStartingBalanceEdit = (EditText) view.findViewById(R.id.create_budget_dialog_budget_starting_balance);
        budgetStartingBalanceEdit.setHint(Currency.getInstance(Locale.getDefault()).getSymbol());
        if (savedInstanceState != null) {
            budgetNameEdit.setText(savedInstanceState.getString(BUDGET_NAME_KEY));
            budgetStartingBalanceEdit.setText(savedInstanceState.getString(BUDGET_BALANCE_KEY));
        }
        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    String startingBalance = budgetStartingBalanceEdit.getText().toString();
                    if ("".equals(startingBalance)) {
                        startingBalance = "0";
                    }
                    final Budget budget = new Budget(budgetNameEdit.getText().toString(), startingBalance);
                    ((OnBudgetAddedListener) getActivity()).onBudgetAdded(budget);
                }
            }
        };
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.new_budget_title)
                .setMessage(R.string.new_budget_message)
                .setView(view)
                .setPositiveButton(R.string.new_budget_dialog_create_text, listener)
                .setNegativeButton(R.string.new_budget_dialog_cancel_text, listener)
                .create();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUDGET_NAME_KEY, budgetNameEdit.getText().toString());
        outState.putString(BUDGET_BALANCE_KEY, budgetStartingBalanceEdit.getText().toString());
    }
}
