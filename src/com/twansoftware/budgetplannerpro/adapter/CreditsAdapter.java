package com.twansoftware.budgetplannerpro.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.entity.Credit;
import com.twansoftware.budgetplannerpro.util.TextViewUtil;

import java.util.List;

public class CreditsAdapter extends BaseAdapter {
    private final Context context;
    private final List<Credit> credits;

    public CreditsAdapter(final Context context, final List<Credit> credits) {
        this.context = context;
        this.credits = credits;
    }

    @Override
    public int getCount() {
        return credits.size() + 1;
    }

    @Override
    public Object getItem(final int position) {
        if (position != credits.size()) {
            return credits.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(final int position) {
        if (position != credits.size()) {
            return getItem(position).hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.transaction_list_item, parent, false);
        final TextView transactionName = (TextView) convertView.findViewById(R.id.transaction_list_item_description);
        final TextView transactionAmount = (TextView) convertView.findViewById(R.id.transaction_list_item_amount);
        Float amountToDisplay;
        if (position != credits.size()) {
            final Credit selectedTransaction = (Credit) getItem(position);
            transactionName.setText(selectedTransaction.getDescription());
            transactionName.setTypeface(null, Typeface.NORMAL);
            amountToDisplay = selectedTransaction.getBalanceDelta();
        } else {
            transactionName.setText(context.getString(R.string.total_transaction_amount_label));
            transactionName.setTypeface(null, Typeface.BOLD);
            amountToDisplay = summate(credits);
        }
        TextViewUtil.setupCurrencyTextView(transactionAmount, context.getResources(), amountToDisplay);

        return convertView;
    }

    private static Float summate(final List<Credit> transactions) {
        Float summation = 0f;
        for (final Credit transaction : transactions) {
            summation += transaction.getBalanceDelta();
        }
        return summation;
    }
}
