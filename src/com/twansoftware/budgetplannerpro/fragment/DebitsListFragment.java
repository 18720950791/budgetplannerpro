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
import com.twansoftware.budgetplannerpro.adapter.DebitsAdapter;
import com.twansoftware.budgetplannerpro.callback.TransactionActionModeCallback;
import com.twansoftware.budgetplannerpro.entity.Debit;
import com.twansoftware.budgetplannerpro.iface.OnDebitDeletedListener;
import com.twansoftware.budgetplannerpro.service.BudgetService;
import com.twansoftware.budgetplannerpro.util.SafeUiCallback;
import com.twansoftware.budgetplannerpro.util.TransactionType;
import roboguice.fragment.RoboListFragment;
import roboguice.util.Ln;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class DebitsListFragment extends RoboListFragment {
    private static final String BUDGET_ID_KEY = "budget_id";

    private final List<Debit> debits = new ArrayList<Debit>();

    private long budgetId;

    private DebitsAdapter debitsAdapter;

    @Inject
    private BudgetService budgetService;

    /** Incremented on every view creation/destruction to invalidate stale callbacks. */
    private volatile int viewGeneration = 0;

    private Thread fetchThread;
    private Thread deleteThread;

    public static DebitsListFragment instantiate(final long budgetId) {
        final DebitsListFragment debitsListFragment = new DebitsListFragment();
        final Bundle bundle = new Bundle();
        bundle.putLong(BUDGET_ID_KEY, budgetId);
        debitsListFragment.setArguments(bundle);
        return debitsListFragment;
    }

    public DebitsListFragment() {

    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        Ln.d("onAttach DebitsListFragment");
        budgetId = getArguments().getLong(BUDGET_ID_KEY);
        debitsAdapter = new DebitsAdapter(activity, debits);
        setListAdapter(debitsAdapter);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewGeneration++;
        Ln.d("onViewCreated DebitsListFragment");
        fetchDebits();
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

    private void fetchDebits() {
        final SherlockFragmentActivity activity = (SherlockFragmentActivity) getActivity();
        activity.setSupportProgressBarIndeterminateVisibility(true);
        final int gen = viewGeneration;
        fetchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Debit> newDebits = budgetService.loadDebitsForBudget(budgetId);
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
                                debits.clear();
                                debits.addAll(newDebits);
                                ((SherlockFragmentActivity) getActivity())
                                        .setSupportProgressBarIndeterminateVisibility(false);
                                debitsAdapter.notifyDataSetChanged();
                            }
                        }));
            }
        });
        fetchThread.start();
    }

    @Override
    public void onListItemClick(final ListView listView, final View view, final int position, final long id) {
        Ln.d(debitsAdapter == null);
        Ln.d(debitsAdapter.getCount());
        if (id != 0) {
            final Debit selected = (Debit) listView.getItemAtPosition(position);
            ((SherlockFragmentActivity) getActivity()).startActionMode(new TransactionActionModeCallback(selected) {
                @Override
                public TransactionType getTransactionType() {
                    return TransactionType.DEBIT;
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                    deleteDebit(selected);
                    mode.finish();
                    return true;
                }
            });
        }
    }

    private void deleteDebit(final Debit selected) {
        debits.remove(selected);
        debitsAdapter.notifyDataSetChanged();
        Toast.makeText(getActivity(), R.string.debit_deleted_toast, Toast.LENGTH_LONG).show();
        final int gen = viewGeneration;
        deleteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                budgetService.deleteDebit(selected);
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
                                ((OnDebitDeletedListener) getActivity()).onDebitDeleted(selected);
                            }
                        }));
            }
        });
        deleteThread.start();
    }

    public void addDebit(final Debit debit) {
        debits.add(debit);
        debitsAdapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            @Override
            public void run() {
                budgetService.saveDebit(debit);
            }
        }).start();
    }
}
