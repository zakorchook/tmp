package com.tipsi.stripe.upgradestripeandroir;

public interface PaymentForm {
    public String getCardNumber();
    public String getCvc();
    public Integer getExpMonth();
    public Integer getExpYear();
    public String getCurrency();
}
