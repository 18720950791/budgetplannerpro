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
import com.twansoftware.budgetplannerpro.util.SafeUiCallback;
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

    /** Incremented on every view creation/destruction to invalidate stale callbacks. */
    private volatile int viewGeneration = 0;

    private Thread fetchThread;
    private Thread deleteThread;

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
        viewGeneration++;
        fetchCredits();
        setListAdapter(creditsAdapter);
    }

    @Override
    public void onDestroyView() {
        viewGeneration++;
        if (fetchThread != null) {
            fetchThread.interrupt();
        }
        if (deleteThread != null) {
            deleteThread.interrupt();
        }
        super.onDestroyView();
    }

    private void fetchCredits() {
        final SherlockFragmentActivity activity = (SherlockFragmentActivity) getActivity();
        activity.setSupportProgressBarIndeterminateVisibility(true);
        final int gen = viewGeneration;
        fetchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Credit> newCredits = budgetService.loadCreditsForBudget(budgetId);
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
                                credits.clear();
                                credits.addAll(newCredits);
                                ((SherlockFragmentActivity) getActivity())
                                        .setSupportProgressBarIndeterminateVisibility(false);
                                creditsAdapter.notifyDataSetChanged();
                            }
                        }));
            }
        });
        fetchThread.start();
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
        final int gen = viewGeneration;
        deleteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                budgetService.deleteCredit(selected);
                final android.app.Activity act = getActivity();
                if (act == null) return;
                act.runOnUiThread(SafeUiCallback.uiCallback(gen,
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
                                ((OnCreditDeletedListener) getActivity()).onCreditDeleted(selected);
                            }
                        }));
            }
        });
        deleteThread.start();
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
