package com.twansoftware.budgetplannerpro.entity;

import com.twansoftware.budgetplannerpro.iface.Transaction;

import java.io.Serializable;

public class Debit implements Serializable, Transaction {
    private Long debitId;
    private String description;
    private Float amount;
    private Long budgetId;

    public Debit(final String description, final Float amount, final Long budgetId) {
        this(null, description, amount, budgetId);
    }

    public Debit(final Long debitId, final String description, final Float amount, final Long budgetId) {
        this.debitId = debitId;
        this.description = description;
        this.amount = amount > 0 ? amount : -amount;
        this.budgetId = budgetId;
    }

    public Long getDebitId() {
        return debitId;
    }

    public void setDebitId(final Long debitId) {
        this.debitId = debitId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(final Float amount) {
        this.amount = amount;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(final Long budgetId) {
        this.budgetId = budgetId;
    }

    @Override
    public Float getBalanceDelta() {
        return -getAmount();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Debit");
        sb.append("{debitId=").append(debitId);
        sb.append(", description='").append(description).append('\'');
        sb.append(", amount=").append(amount);
        sb.append(", budgetId=").append(budgetId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Debit debit = (Debit) o;

        if (amount != null ? !amount.equals(debit.amount) : debit.amount != null) return false;
        if (budgetId != null ? !budgetId.equals(debit.budgetId) : debit.budgetId != null) return false;
        if (debitId != null ? !debitId.equals(debit.debitId) : debit.debitId != null) return false;
        if (description != null ? !description.equals(debit.description) : debit.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = debitId != null ? debitId.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (budgetId != null ? budgetId.hashCode() : 0);
        return result;
    }
}

