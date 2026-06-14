package com.twansoftware.budgetplannerpro.callback;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.twansoftware.budgetplannerpro.R;
import com.twansoftware.budgetplannerpro.iface.Transaction;
import com.twansoftware.budgetplannerpro.util.TransactionType;

public abstract class TransactionActionModeCallback implements ActionMode.Callback {
    private final Transaction transaction;

    public TransactionActionModeCallback(final Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
        mode.setTitle(transaction.getDescription());
        final TransactionType transactionType = getTransactionType();
        final int deleteStringId = transactionType == TransactionType.CREDIT ? R.string.delete_credit_text : R.string.delete_debit_text;
        menu.add(deleteStringId)
                .setIcon(R.drawable.ic_menu_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT | MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
        return false;
    }

    @Override
   public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
       return false;
   }

    @Override
    public void onDestroyActionMode(final ActionMode actionMode) {
    }

    public abstract TransactionType getTransactionType();
}
