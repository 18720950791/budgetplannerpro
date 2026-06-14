package com.twansoftware.budgetplannerpro.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.twansoftware.budgetplannerpro.service.BudgetService;
import com.twansoftware.budgetplannerpro.util.SafeUiCallback;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.entity.Credit;
import com.twansoftware.budgetplannerpro.entity.Debit;
import com.twansoftware.budgetplannerpro.util.TextViewUtil;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;
import roboguice.util.Ln;

import javax.inject.Inject;


public class BudgetSummaryFragment extends RoboFragment {
    private static final String BUDGET_ID_KEY = "budget_id";

    private long budgetId;

    @Inject
    private BudgetService budgetService;

    @InjectView(R.id.manage_budget_balance)
    private TextView balance;

    @InjectView(R.id.manage_budget_starting_balance)
    private TextView startingBalance;

    @InjectView(R.id.manage_budget_credits)
    private TextView creditsAmount;

    @InjectView(R.id.manage_budget_debits)
    private TextView debitsAmount;

    @InjectView(R.id.manage_budget_largest_credit_description)
    private TextView largestCreditDescription;

    @InjectView(R.id.manage_budget_largest_credit_amount)
    private TextView largestCreditAmount;

    @InjectView(R.id.manage_budget_largest_debit_description)
    private TextView largestDebitDescription;

    @InjectView(R.id.manage_budget_largest_debit_amount)
    private TextView largestDebitAmount;

    /** Incremented on every view creation/destruction to invalidate stale callbacks. */
    private volatile int viewGeneration = 0;

    private Thread updateThread;

    public static BudgetSummaryFragment instantiate(final Long budgetId) {
        final BudgetSummaryFragment budgetSummaryFragment = new BudgetSummaryFragment();
        final Bundle bundle = new Bundle();
        bundle.putLong(BUDGET_ID_KEY, budgetId);
        budgetSummaryFragment.setArguments(bundle);
        return budgetSummaryFragment;
    }

    public BudgetSummaryFragment() {

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        budgetId = getArguments().getLong(BUDGET_ID_KEY);
        return LayoutInflater.from(getActivity()).inflate(R.layout.budget_summary_fragment, null);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewGeneration++;
        update();
    }

    @Override
    public void onDestroyView() {
        viewGeneration++;
        if (updateThread != null) {
            updateThread.interrupt();
        }
        super.onDestroyView();
    }

    public void update() {
        if (getActivity() == null) return;
        final SherlockFragmentActivity activity = (SherlockFragmentActivity) getActivity();
        activity.setSupportProgressBarIndeterminateVisibility(true);
        final int gen = viewGeneration;
        updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Budget budget = budgetService.loadBudgetById(budgetId);
                activity.runOnUiThread(SafeUiCallback.uiCallback(gen,
                        new SafeUiCallback.GenerationProvider() {
                            @Override
                            public int getViewGeneration() {
                                return viewGeneration;
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                if (!isAdded() || getActivity() == null) return;
                                // Guard against null budget from database
                                if (budget == null) {
                                    Ln.w("Budget not found for id %d, skipping summary update", budgetId);
                                    ((SherlockFragmentActivity) getActivity())
                                            .setSupportProgressBarIndeterminateVisibility(false);
                                    return;
                                }
                                Ln.d("Loading budget for summary fragment...");
                                final Resources resources = getActivity().getResources();
                                TextViewUtil.setupCurrencyTextView(balance, resources, budget.calculateBalance());
                                TextViewUtil.setupCurrencyTextView(startingBalance, resources, budget.getStartingBalance());
                                TextViewUtil.setupCurrencyTextView(creditsAmount, resources, budget.getCreditsTotal());
                                TextViewUtil.setupCurrencyTextView(debitsAmount, resources, budget.getDebitsTotal());
                                final Credit largestCredit = budget.getLargestCredit();
                                largestCreditDescription.setText(largestCredit.getDescription());
                                TextViewUtil.setupCurrencyTextView(largestCreditAmount, resources, largestCredit.getBalanceDelta());
                                final Debit largestDebit = budget.getLargestDebit();
                                largestDebitDescription.setText(largestDebit.getDescription());
                                TextViewUtil.setupCurrencyTextView(largestDebitAmount, resources, largestDebit.getBalanceDelta());
                                ((SherlockFragmentActivity) getActivity())
                                        .setSupportProgressBarIndeterminateVisibility(false);
                            }
                        }));
            }
        });
        updateThread.start();
    }
}
