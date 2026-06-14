package com.twansoftware.budgetplannerpro.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.util.TextViewUtil;

import java.util.List;

public class BudgetsAdapter extends BaseAdapter {
    private final Context context;
    private final List<Budget> budgets;

    public BudgetsAdapter(final Context context, final List<Budget> budgets) {
        this.context = context;
        this.budgets = budgets;
    }

    @Override
    public int getCount() {
        return budgets.size();
    }

    @Override
    public Object getItem(final int position) {
        return budgets.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return ((Budget) getItem(position)).getBudgetId();
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final Budget budget = (Budget) getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.budget_list_item, parent, false);
        }
        final TextView budgetName = (TextView) convertView.findViewById(R.id.budget_list_item_name);
        final TextView budgetBalance = (TextView) convertView.findViewById(R.id.budget_list_item_balance);
        budgetName.setText(budget.getName());
        final Float balance = budget.calculateBalance();
        TextViewUtil.setupCurrencyTextView(budgetBalance, context.getResources(), balance);
        return convertView;
    }
}
