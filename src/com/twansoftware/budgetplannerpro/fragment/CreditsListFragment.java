package com.twansoftware.budgetplannerpro.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.MenuItem;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.adapter.CreditsAdapter;
import com.twansoftware.budgetplannerpro.callback.TransactionActionModeCallback;
import com.twansoftware.budgetplannerpro.entity.Credit;
import com.twansoftware.budgetplannerpro.iface.OnCreditDeletedListener;
import com.twansoftware.budgetplannerpro.service.BudgetService;
import com.twansoftware.budgetplannerpro.util.TransactionType;
import roboguice.fragment.RoboListFragment;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CreditsListFragment extends RoboListFragment {
    private static final String BUDGET_ID_KEY = "budget_id";

    private final List<Credit> credits = new ArrayList<Credit>();

    private long budgetId;

    private CreditsAdapter creditsAdapter;

    @Inject
    private BudgetService budgetService;

    public static CreditsListFragment instantiate(final long budgetId) {
        final CreditsListFragment creditsListFragment = new CreditsListFragment();
        final Bundle bundle = new Bundle();
        bundle.putLong(BUDGET_ID_KEY, budgetId);
        creditsListFragment.setArguments(bundle);
        return creditsListFragment;
    }

    public CreditsListFragment() {

    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        budgetId = getArguments().getLong(BUDGET_ID_KEY);
        creditsAdapter = new CreditsAdapter(activity, credits);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchCredits();
        setListAdapter(creditsAdapter);
    }

    private void fetchCredits() {
        final SherlockFragmentActivity sherlockFragmentActivity = (SherlockFragmentActivity) getActivity();
        sherlockFragmentActivity.setSupportProgressBarIndeterminateVisibility(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Credit> newCredits = budgetService.loadCreditsForBudget(budgetId);
                sherlockFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        credits.clear();
                        credits.addAll(newCredits);
                        sherlockFragmentActivity.setSupportProgressBarIndeterminateVisibility(false);
                        creditsAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onListItemClick(final ListView listView, final View view, final int position, final long id) {
        if (id != 0) {
            final Credit selected = (Credit) listView.getItemAtPosition(position);
            ((SherlockFragmentActivity) getActivity()).startActionMode(new TransactionActionModeCallback(selected) {
                @Override
                public TransactionType getTransactionType() {
                    return TransactionType.CREDIT;
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                    deleteCredit(selected);
                    mode.finish();
                    return true;
                }
            });
        }
    }

    private void deleteCredit(final Credit selected) {
        credits.remove(selected);
        creditsAdapter.notifyDataSetChanged();
        Toast.makeText(getActivity(), R.string.credit_deleted_toast, Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                budgetService.deleteCredit(selected);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((OnCreditDeletedListener) getActivity()).onCreditDeleted(selected);
                    }
                });
            }
        }).start();
    }

    public void addCredit(final Credit credit) {
        credits.add(credit);
        creditsAdapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            @Override
            public void run() {
                budgetService.saveCredit(credit);
            }
        }).start();
    }
}
