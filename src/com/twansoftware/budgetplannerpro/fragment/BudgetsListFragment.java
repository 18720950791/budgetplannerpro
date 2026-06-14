package com.twansoftware.budgetplannerpro.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.adapter.BudgetsAdapter;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.service.BudgetService;
import com.twansoftware.budgetplannerpro.ui.ManageBudgetActivity;
import com.twansoftware.budgetplannerpro.ui.ManageBudgetTabletActivity;
import com.twansoftware.budgetplannerpro.util.EnvironmentUtil;
import roboguice.fragment.RoboListFragment;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class BudgetsListFragment extends RoboListFragment {
    @Inject
    private BudgetService budgetService;

    private final List<Budget> budgets = new ArrayList<Budget>();

    private BudgetsAdapter budgetsAdapter;

    public static BudgetsListFragment instantiate() {
        return new BudgetsListFragment();
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        budgetsAdapter = new BudgetsAdapter(getActivity(), budgets);
        setListAdapter(budgetsAdapter);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                final Budget longPressed = (Budget) parent.getItemAtPosition(position);
                ((SherlockFragmentActivity) getActivity()).startActionMode(new BudgetLongClickCallback(longPressed));
                return true;
            }
        });
        loadBudgetsFromDatabase();
    }

    private void loadBudgetsFromDatabase() {
        final SherlockFragmentActivity sherlockFragmentActivity = (SherlockFragmentActivity) getActivity();
        sherlockFragmentActivity.setSupportProgressBarIndeterminateVisibility(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Budget> budgets = budgetService.loadAllBudgets();
                final List<Budget> fragmentBudgets = BudgetsListFragment.this.budgets;
                sherlockFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fragmentBudgets.clear();
                        fragmentBudgets.addAll(budgets);
                        sherlockFragmentActivity.setSupportProgressBarIndeterminateVisibility(false);
                        BudgetsListFragment.this.budgetsAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Budget budgetSelected = (Budget) getListView().getItemAtPosition(position);
        final Intent intent = new Intent(getActivity(),
                EnvironmentUtil.shouldUseManageBudgetTabletActivity(getActivity().getWindowManager())
                ? ManageBudgetTabletActivity.class
                : ManageBudgetActivity.class);
        intent.putExtra(ManageBudgetActivity.BUDGET_TO_MANAGE_EXTRA_KEY, budgetSelected);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        loadBudgetsFromDatabase();
    }

    public void addBudget(final Budget budget) {
        getActivity().setProgressBarIndeterminateVisibility(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Budget newBudget = budgetService.saveBudget(budget);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        budgets.add(newBudget);
                        getActivity().setProgressBarIndeterminateVisibility(false);
                        budgetsAdapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(), R.string.budget_saved_toast, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private class BudgetLongClickCallback implements ActionMode.Callback {
        private final Budget budget;

        public BudgetLongClickCallback(final Budget longPressed) {
            this.budget = longPressed;
        }

        @Override
        public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
            mode.setTitle(budget.getName());
            menu.add(R.string.delete_budget_text)
                    .setIcon(R.drawable.ic_menu_delete)
                    .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS | android.view.MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
            if (budgets.size() > 1) {
                budgets.remove(budget);
                budgetsAdapter.notifyDataSetChanged();
                Toast.makeText(getActivity(), R.string.budget_deleted_toast, Toast.LENGTH_LONG).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        budgetService.deleteBudget(budget);
                    }
                }).start();
            } else {
                Toast.makeText(getActivity(), R.string.one_budget_required_toast, Toast.LENGTH_SHORT).show();
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(final ActionMode mode) {

        }
    }


}
