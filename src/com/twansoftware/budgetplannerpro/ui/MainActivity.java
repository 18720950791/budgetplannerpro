package com.twansoftware.budgetplannerpro.ui;

import android.os.Bundle;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.fragment.BudgetsListFragment;
import com.twansoftware.budgetplannerpro.fragment.CreateBudgetDialogFragment;
import com.twansoftware.budgetplannerpro.iface.OnBudgetAddedListener;
import com.twansoftware.budgetplannerpro.service.BudgetService;
import roboguice.inject.InjectFragment;

import javax.inject.Inject;

public class MainActivity extends RoboSherlockFragmentActivity implements MenuItem.OnMenuItemClickListener, OnBudgetAddedListener {
    private static final String NEW_BUDGET_TAG = "budget234";
    @Inject
    private BudgetService budgetService;
    @InjectFragment(R.id.main_budgets_fragment)
    private BudgetsListFragment budgetsListFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.add(R.string.create_budget_menu_title)
                .setIcon(R.drawable.ic_menu_add_budget)
                .setOnMenuItemClickListener(this)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        if (item.getTitle().equals(getString(R.string.create_budget_menu_title))) {
            showNewBudgetDialog();
        }
        return true;
    }

    private void showNewBudgetDialog() {
        new CreateBudgetDialogFragment().show(getSupportFragmentManager(), NEW_BUDGET_TAG);
    }

    @Override
    public void onBudgetAdded(final Budget budget) {
        budgetsListFragment.addBudget(budget);
    }
}
