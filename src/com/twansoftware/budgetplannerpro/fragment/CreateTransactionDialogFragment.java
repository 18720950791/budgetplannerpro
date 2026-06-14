package com.twansoftware.budgetplannerpro.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.entity.Credit;
import com.twansoftware.budgetplannerpro.entity.Debit;
import com.twansoftware.budgetplannerpro.iface.OnCreditAddedListener;
import com.twansoftware.budgetplannerpro.iface.OnDebitAddedListener;
import com.twansoftware.budgetplannerpro.util.TransactionType;
import roboguice.fragment.RoboDialogFragment;

import java.util.Currency;
import java.util.Locale;

/**
 * User: achuinard
 * Date: 12/18/12
 */
public class CreateTransactionDialogFragment extends RoboDialogFragment {
    private static final String BUDGET_ID_KEY = "budget_id";
    private static final String TRANS_TYPE_KEY = "transaction_type";
    private static final String EDIT_TEXT_DESCRIPTION = "edit_text_desc";
    private static final String EDIT_TEXT_AMOUNT = "edit_text_amount";

    private EditText transactionDescription;
    private EditText transactionAmount;

    public CreateTransactionDialogFragment() {

    }

    public static CreateTransactionDialogFragment newInstance(final long budgetId, final TransactionType transactionType) {
        final CreateTransactionDialogFragment createTransactionDialogFragment = new CreateTransactionDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putLong(BUDGET_ID_KEY, budgetId);
        arguments.putSerializable(TRANS_TYPE_KEY, transactionType);
        createTransactionDialogFragment.setArguments(arguments);
        return createTransactionDialogFragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final long budgetId = getArguments().getLong(BUDGET_ID_KEY);
        final TransactionType transactionType = (TransactionType) getArguments().getSerializable(TRANS_TYPE_KEY);
        final View transactionDetailsView = LayoutInflater.from(getActivity()).inflate(R.layout.create_transaction_dialog, null, false);
        transactionDescription = (EditText) transactionDetailsView.findViewById(R.id.create_transaction_dialog_transaction_description);
        transactionAmount = (EditText) transactionDetailsView.findViewById(R.id.create_transaction_dialog_delta);
        transactionAmount.setHint(Currency.getInstance(Locale.getDefault()).getSymbol());
        if (savedInstanceState != null) {
            transactionDescription.setText(savedInstanceState.getString(EDIT_TEXT_DESCRIPTION));
            transactionAmount.setText(savedInstanceState.getString(EDIT_TEXT_AMOUNT));
        }
        final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    final String transactionAmountString = transactionAmount.getText().toString();
                    if (!"".equals(transactionAmountString)) {
                        final Float amount = Float.valueOf(transactionAmountString);
                        final String description = transactionDescription.getText().toString();
                        if (transactionType == TransactionType.CREDIT) {
                            final Credit credit = new Credit(description, amount, budgetId);
                            ((OnCreditAddedListener) getActivity()).onCreditAdded(credit);
                        } else if (transactionType == TransactionType.DEBIT) {
                            final Debit debit = new Debit(description, amount, budgetId);
                            ((OnDebitAddedListener) getActivity()).onDebitAdded(debit);
                        }
                    } else {
                        Toast.makeText(getActivity(), R.string.invalid_amount_entered_toast, Toast.LENGTH_SHORT).show();
                    }
                }

            }
        };
        return new AlertDialog.Builder(getActivity())
                .setView(transactionDetailsView)
                .setTitle(transactionType == TransactionType.CREDIT ? R.string.new_credit_title : R.string.new_debit_title)
                .setMessage(R.string.new_transaction_message)
                .setPositiveButton(R.string.save_transaction_text, onClickListener)
                .setNegativeButton(R.string.cancel_transaction_text, onClickListener)
                .create();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EDIT_TEXT_DESCRIPTION, transactionDescription.getText().toString());
        outState.putString(EDIT_TEXT_AMOUNT, transactionAmount.getText().toString());
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
