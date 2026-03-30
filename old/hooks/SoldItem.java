package dev.jackwith.skyCoreV2.hooks;

public class SoldItem {

    private int amount;
    private double totalPrice;

    public SoldItem() {
        this.amount = 0;
        this.totalPrice = 0;
    }

    public void add(int amount, double price) {
        this.amount += amount;
        this.totalPrice += price;
    }

    public int getAmount() {
        return amount;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}