package com.twansoftware.budgetplannerpro.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.entity.Credit;
import com.twansoftware.budgetplannerpro.entity.Debit;
import com.twansoftware.budgetplannerpro.fragment.BudgetSummaryFragment;
import com.twansoftware.budgetplannerpro.fragment.CreateTransactionDialogFragment;
import com.twansoftware.budgetplannerpro.fragment.CreditsListFragment;
import com.twansoftware.budgetplannerpro.fragment.DebitsListFragment;
import com.twansoftware.budgetplannerpro.iface.OnCreditAddedListener;
import com.twansoftware.budgetplannerpro.iface.OnCreditDeletedListener;
import com.twansoftware.budgetplannerpro.iface.OnDebitAddedListener;
import com.twansoftware.budgetplannerpro.iface.OnDebitDeletedListener;
import com.twansoftware.budgetplannerpro.service.BudgetService;
import com.twansoftware.budgetplannerpro.util.TransactionType;
import com.viewpagerindicator.TitlePageIndicator;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import roboguice.util.Ln;

import javax.inject.Inject;

public class ManageBudgetActivity extends RoboSherlockFragmentActivity implements MenuItem.OnMenuItemClickListener, OnCreditAddedListener, OnDebitAddedListener,
        OnDebitDeletedListener, OnCreditDeletedListener {
    public static final String BUDGET_TO_MANAGE_EXTRA_KEY = "budget_to_manage";
    private static final String ADD_TAG = "add_trans";
    private static final int DEBIT_INDEX = BudgetFragmentPagerAdapter.DEBIT_INDEX;
    private static final int CREDIT_INDEX = BudgetFragmentPagerAdapter.CREDIT_INDEX;
    private static final int SUMMARY_INDEX = BudgetFragmentPagerAdapter.SUMMARY_INDEX;

    @Inject
    private BudgetService budgetService;

    @InjectExtra(BUDGET_TO_MANAGE_EXTRA_KEY)
    private Budget budget;

    @InjectView(R.id.manage_budget_titles)
    private TitlePageIndicator titlePageIndicator;

    @InjectView(R.id.manage_budget_view_pager)
    private ViewPager viewPager;

    private BudgetFragmentPagerAdapter fragmentPagerAdapter;

    private ActionMode actionMode;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_budget);
        if (savedInstanceState != null && savedInstanceState.containsKey(BUDGET_TO_MANAGE_EXTRA_KEY)) {
            Ln.d("Restoring!");
            budget = (Budget) savedInstanceState.getSerializable(BUDGET_TO_MANAGE_EXTRA_KEY);
        }
        getSupportActionBar().setTitle(budget.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupIndicator();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        finish();
        return true;
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BUDGET_TO_MANAGE_EXTRA_KEY, budget);
    }

    private void setupIndicator() {
        fragmentPagerAdapter = new BudgetFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);
        titlePageIndicator.setViewPager(viewPager);
    }

    private class BudgetFragmentPagerAdapter extends FragmentPagerAdapter {
        private static final int DEBIT_INDEX = 0;
        private static final int CREDIT_INDEX = 1;
        private static final int SUMMARY_INDEX = 2;

        public BudgetFragmentPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            if (position == 0) {
                return getString(R.string.debits_title);
            } else if (position == 1) {
                return getString(R.string.credits_title);
            } else if (position == 2) {
                return getString(R.string.summary_title);
            } else {
                return "";
            }
        }

        @Override
        public Fragment getItem(final int i) {
            if (i == DEBIT_INDEX) {
                return DebitsListFragment.instantiate(budget.getBudgetId());
            } else if (i == CREDIT_INDEX) {
                return CreditsListFragment.instantiate(budget.getBudgetId());
            } else if (i == SUMMARY_INDEX) {
                return BudgetSummaryFragment.instantiate(budget.getBudgetId());
            } else {
                return null;
            }
        }

        public Fragment getFragmentByPosition(final int position) {
            final String name = makeFragmentName(viewPager.getId(), position);
            return getSupportFragmentManager().findFragmentByTag(name);
        }

        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.add(R.string.add_transaction_menu_option_text)
                .setIcon(R.drawable.ic_menu_add_transaction)
                .setOnMenuItemClickListener(this)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        if (getString(R.string.add_transaction_menu_option_text).equals(item.getTitle())) {
            addTransactionWizard();
            return true;
        }
        return false;
    }

    private void addTransactionWizard() {
        final int viewPagerIndex = viewPager.getCurrentItem();
        if (viewPagerIndex == SUMMARY_INDEX) {
            final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    dialog.dismiss();
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        addCreditWizard();
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        addDebitWizard();
                    }
                }
            };
            new AlertDialog.Builder(this)
                    .setMessage(R.string.what_kind_of_transaction_dialog_message)
                    .setPositiveButton(R.string.credit_word, onClickListener)
                    .setNegativeButton(R.string.debit_word, onClickListener)
                    .create().show();
        } else if (viewPagerIndex == CREDIT_INDEX) {
            addCreditWizard();
        } else if (viewPagerIndex == DEBIT_INDEX) {
            addDebitWizard();
        }
    }

    private void addCreditWizard() {
        CreateTransactionDialogFragment.newInstance(budget.getBudgetId(), TransactionType.CREDIT).show(getSupportFragmentManager(), ADD_TAG);
    }

    private void addDebitWizard() {
        CreateTransactionDialogFragment.newInstance(budget.getBudgetId(), TransactionType.DEBIT).show(getSupportFragmentManager(), ADD_TAG);
    }

    // these calls come from the DialogFragment and need to be transmitted to the list fragments
    // the list fragments have their own budget service to get work done

    @Override
    public void onCreditAdded(final Credit credit) {
        Ln.d(credit);
        ((CreditsListFragment) fragmentPagerAdapter.getFragmentByPosition(CREDIT_INDEX)).addCredit(credit);
        updateBudgetSummary();
    }

    @Override
    public void onDebitAdded(final Debit debit) {
        ((DebitsListFragment) fragmentPagerAdapter.getFragmentByPosition(DEBIT_INDEX)).addDebit(debit);
        updateBudgetSummary();
    }

    // these calls come FROM the list fragments

    @Override
    public void onCreditDeleted(final Credit credit) {
        budget.getCredits().remove(credit);
        updateBudgetSummary();
    }

    @Override
    public void onDebitDeleted(final Debit debit) {
        budget.getDebits().remove(debit);
        updateBudgetSummary();
    }

    private void updateBudgetSummary() {
        final Fragment potentiallyNullSummaryFragment = fragmentPagerAdapter.getFragmentByPosition(SUMMARY_INDEX);
        if (potentiallyNullSummaryFragment != null) {
            ((BudgetSummaryFragment) potentiallyNullSummaryFragment).update();
        }
    }
}