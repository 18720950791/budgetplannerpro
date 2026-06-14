package com.twansoftware.budgetplannerpro.service;

import android.content.Context;
import com.twansoftware.budgetplannerpro.dao.BudgetDAO;
import com.twansoftware.budgetplannerpro.dao.CreditsDAO;
import com.twansoftware.budgetplannerpro.dao.DebitsDAO;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.entity.Credit;
import com.twansoftware.budgetplannerpro.entity.Debit;
import roboguice.inject.ContextSingleton;

import javax.inject.Inject;
import java.util.List;

@ContextSingleton
public class BudgetService {
    private final Context context;

    @Inject
    private BudgetDAO budgetDao;

    @Inject
    private CreditsDAO creditsDao;

    @Inject
    private DebitsDAO debitsDao;

    @Inject
    public BudgetService(final Context context) {
        this.context = context;
    }

    public List<Budget> loadAllBudgets(final boolean withCreditsAndDebits) {
        final List<Budget> budgets = budgetDao.loadAllBudgets();
        if (budgets.isEmpty()) {
            populateSampleBudget();
            return loadAllBudgets(withCreditsAndDebits);
        } else {
            if (withCreditsAndDebits) {
                joinCreditsAndDebits(budgets);
            }
            return budgets;
        }
    }

    private void populateSampleBudget() {
        final Budget budget = new Budget("Sample Budget", 100.0f);
        budgetDao.saveBudget(budget);
        final Debit sampleDebit = new Debit("New Clothes", 120.0f, budget.getBudgetId());
        final Credit sampleCredit = new Credit("Payday", 120.0f, budget.getBudgetId());
        creditsDao.saveCredit(sampleCredit);
        debitsDao.saveDebit(sampleDebit);
    }

    private List<Budget> joinCreditsAndDebits(final List<Budget> budgets) {
        for (final Budget budget : budgets) {
            joinCreditsAndDebits(budget);
        }
        return budgets;
    }

    private void joinCreditsAndDebits(final Budget budget) {
        final List<Credit> credits = creditsDao.loadCreditsForBudgetId(budget.getBudgetId());
        budget.getCredits().clear();
        budget.getCredits().addAll(credits);
        budget.getDebits().clear();
        budget.getDebits().addAll(debitsDao.loadDebitsForBudgetId(budget.getBudgetId()));
    }

    public Budget saveBudget(final Budget budget) {
        return budgetDao.saveBudget(budget);
    }

    public Credit saveCredit(final Credit credit) {
        return creditsDao.saveCredit(credit);
    }

    public Debit saveDebit(final Debit debit) {
        return debitsDao.saveDebit(debit);
    }

    public List<Credit> loadCreditsForBudget(final long budgetId) {
        return creditsDao.loadCreditsForBudgetId(budgetId);
    }

    public List<Debit> loadDebitsForBudget(final long budgetId) {
        return debitsDao.loadDebitsForBudgetId(budgetId);
    }

    public List<Budget> loadAllBudgets() {
        return loadAllBudgets(true);
    }

    public void deleteBudget(final Budget toDelete) {
        budgetDao.deleteBudgetById(toDelete.getBudgetId());
        debitsDao.deleteDebitsForBudgetId(toDelete.getBudgetId());
        creditsDao.deleteDebitsForBudgetId(toDelete.getBudgetId());
    }

    public void deleteCredit(final Credit credit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                creditsDao.deleteCredit(credit);
            }
        }).start();
    }

    public void deleteDebit(final Debit debit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                debitsDao.deleteDebit(debit);
            }
        }).start();
    }

    public Budget loadBudgetById(final long budgetId) {
        final Budget budget = budgetDao.loadBudgetById(budgetId);
        if (budget != null) {
            budget.getCredits().addAll(loadCreditsForBudget(budgetId));
            budget.getDebits().addAll(loadDebitsForBudget(budgetId));
        }
        return budget;
    }
}
