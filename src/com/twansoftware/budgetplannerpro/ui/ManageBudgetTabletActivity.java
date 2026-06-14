package com.twansoftware.budgetplannerpro.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.FrameLayout;
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
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import roboguice.util.Ln;

import javax.inject.Inject;

/**
 * User: achuinard
 * Date: 12/20/12
 */
public class ManageBudgetTabletActivity extends RoboSherlockFragmentActivity implements MenuItem.OnMenuItemClickListener, OnCreditAddedListener, OnDebitAddedListener,
        OnDebitDeletedListener, OnCreditDeletedListener {
    public static final String BUDGET_TO_MANAGE_EXTRA_KEY = "budget_to_manage";
    private static final String ADD_TAG = "add_trans";

    @Inject
    private BudgetService budgetService;

    @InjectExtra(BUDGET_TO_MANAGE_EXTRA_KEY)
    private Budget budget;

    @InjectView(R.id.manage_budget_tablet_credits_frame)
    private FrameLayout creditsContainer;

    @InjectView(R.id.manage_budget_tablet_debits_frame)
    private FrameLayout debitsContainer;

    @InjectView(R.id.manage_budget_tablet_summary_frame)
    private FrameLayout summaryContainer;


    private ActionMode actionMode;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_budget_tablet);
        if (savedInstanceState != null && savedInstanceState.containsKey(BUDGET_TO_MANAGE_EXTRA_KEY)) {
            Ln.d("Restoring!");
            budget = (Budget) savedInstanceState.getSerializable(BUDGET_TO_MANAGE_EXTRA_KEY);
        }
        getSupportActionBar().setTitle(budget.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupFragments(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setupFragments(final Bundle savedInstanceState) {
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (savedInstanceState == null) {
            fragmentTransaction.add(R.id.manage_budget_tablet_credits_frame, CreditsListFragment.instantiate(budget.getBudgetId()));
            fragmentTransaction.add(R.id.manage_budget_tablet_debits_frame, DebitsListFragment.instantiate(budget.getBudgetId()));
            fragmentTransaction.add(R.id.manage_budget_tablet_summary_frame, BudgetSummaryFragment.instantiate(budget.getBudgetId()));
        } else {
            fragmentTransaction.replace(R.id.manage_budget_tablet_credits_frame, CreditsListFragment.instantiate(budget.getBudgetId()));
            fragmentTransaction.replace(R.id.manage_budget_tablet_debits_frame, DebitsListFragment.instantiate(budget.getBudgetId()));
            fragmentTransaction.replace(R.id.manage_budget_tablet_summary_frame, BudgetSummaryFragment.instantiate(budget.getBudgetId()));
        }
        fragmentTransaction.commit();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BUDGET_TO_MANAGE_EXTRA_KEY, budget);
    }

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
        ((CreditsListFragment) getSupportFragmentManager().findFragmentById(R.id.manage_budget_tablet_credits_frame)).addCredit(credit);
        updateBudgetSummary();
    }

    @Override
    public void onDebitAdded(final Debit debit) {
        ((DebitsListFragment) getSupportFragmentManager().findFragmentById(R.id.manage_budget_tablet_debits_frame)).addDebit(debit);
        updateBudgetSummary();
    }

    // these calls come FROM the list fragments

    @Override
    public void onCreditDeleted(final Credit credit) {
       updateBudgetSummary();
    }

    @Override
    public void onDebitDeleted(final Debit debit) {
        updateBudgetSummary();
    }

    private void updateBudgetSummary() {
        ((BudgetSummaryFragment) getSupportFragmentManager().findFragmentById(R.id.manage_budget_tablet_summary_frame)).update();
    }
}