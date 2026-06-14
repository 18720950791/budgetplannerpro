package com.twansoftware.budgetplannerpro.entity;

import com.twansoftware.budgetplannerpro.iface.Transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Budget implements Serializable {
    private Long budgetId;
    private String name;
    private Float startingBalance;
    private final List<Debit> debits;
    private final List<Credit> credits;

    public Budget(final String name, final String startingBalance) {
        this(name, Float.parseFloat(startingBalance));
    }

    public Budget(final String name, final Float startingBalance) {
        this(null, name, startingBalance);
    }

    public Budget(final Long budgetId, final String name, final Float startingBalance) {
        this.budgetId = budgetId;
        this.name = name;
        this.startingBalance = startingBalance;
        this.debits = new ArrayList<Debit>();
        this.credits = new ArrayList<Credit>();
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(final Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getName() {
        return name;
    }

    public Float getStartingBalance() {
        return startingBalance;
    }

    public List<Debit> getDebits() {
        return debits;
    }

    public List<Credit> getCredits() {
        return credits;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setStartingBalance(final Float startingBalance) {
        this.startingBalance = startingBalance;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Budget");
        sb.append("{budgetId=").append(budgetId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", startingBalance=").append(startingBalance);
        sb.append(", debits=").append(debits);
        sb.append(", credits=").append(credits);
        sb.append('}');
        return sb.toString();
    }

    public Float calculateBalance() {
        return getStartingBalance() + getCreditsTotal() + getDebitsTotal();
    }

    public Float getCreditsTotal() {
        return getTransactionTotal(new ArrayList<Transaction>(getCredits()));
    }

    public Float getDebitsTotal() {
        return getTransactionTotal(new ArrayList<Transaction>(getDebits()));
    }

    private Float getTransactionTotal(final ArrayList<Transaction> transactions) {
        Float total = 0f;
        for (final Transaction transaction : transactions) {
            total += transaction.getBalanceDelta();
        }
        return total;
    }

    public Credit getLargestCredit() {
        if (credits == null || getCredits().isEmpty()) {
            return new Credit("None", 0.00f, 0l);
        } else {
            final ArrayList<Credit> newList = new ArrayList<Credit>(getCredits());
            Collections.sort(newList, new Comparator<Credit>() {
                @Override
                public int compare(final Credit credit, final Credit credit2) {
                    return -(credit.getAmount().compareTo(credit2.getAmount()));
                }
            });
            return newList.get(0);
        }
    }

    public Debit getLargestDebit() {
        if (debits == null || getDebits().isEmpty()) {
            return new Debit("None", 0.00f, 0l);
        } else {
            final ArrayList<Debit> newList = new ArrayList<Debit>(getDebits());
            Collections.sort(newList, new Comparator<Debit>() {
                @Override
                public int compare(final Debit debit, final Debit debit2) {
                    return debit.getBalanceDelta().compareTo(debit2.getBalanceDelta());
                }
            });
            return newList.get(0);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Budget budget = (Budget) o;

        if (budgetId != null ? !budgetId.equals(budget.budgetId) : budget.budgetId != null) return false;
        if (credits != null ? !credits.equals(budget.credits) : budget.credits != null) return false;
        if (debits != null ? !debits.equals(budget.debits) : budget.debits != null) return false;
        if (name != null ? !name.equals(budget.name) : budget.name != null) return false;
        if (startingBalance != null ? !startingBalance.equals(budget.startingBalance) : budget.startingBalance != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = budgetId != null ? budgetId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (startingBalance != null ? startingBalance.hashCode() : 0);
        result = 31 * result + (debits != null ? debits.hashCode() : 0);
        result = 31 * result + (credits != null ? credits.hashCode() : 0);
        return result;
    }
}
