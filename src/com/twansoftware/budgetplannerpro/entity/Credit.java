package com.twansoftware.budgetplannerpro.entity;

import com.twansoftware.budgetplannerpro.iface.Transaction;

import java.io.Serializable;

public class Credit implements Serializable, Transaction {
    private Long creditId;
    private String description;
    private Float amount;
    private Long budgetId;

    public Credit(final String description, final Float amount, final Long budgetId) {
        this(null, description, amount, budgetId);
    }

    public Credit(final Long creditId, final String description, final Float amount, final Long budgetId) {
        this.creditId = creditId;
        this.description = description;
        this.amount = amount;
        this.budgetId = budgetId;
    }

    public Long getCreditId() {
        return creditId;
    }

    public void setCreditId(final Long creditId) {
        this.creditId = creditId;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(final Long budgetId) {
        this.budgetId = budgetId;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(final Float amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public Float getBalanceDelta() {
        return getAmount();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Credit");
        sb.append("{creditId=").append(creditId);
        sb.append(", budgetId=").append(budgetId);
        sb.append(", amount=").append(amount);
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Credit credit = (Credit) o;

        if (amount != null ? !amount.equals(credit.amount) : credit.amount != null) return false;
        if (budgetId != null ? !budgetId.equals(credit.budgetId) : credit.budgetId != null) return false;
        if (creditId != null ? !creditId.equals(credit.creditId) : credit.creditId != null) return false;
        if (description != null ? !description.equals(credit.description) : credit.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = creditId != null ? creditId.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (budgetId != null ? budgetId.hashCode() : 0);
        return result;
    }
}
