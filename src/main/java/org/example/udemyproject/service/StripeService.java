package org.example.udemyproject.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.example.udemyproject.payload.StripePaymentDTO;

public interface StripeService {


    PaymentIntent paymentIntent(StripePaymentDTO stripePaymentDTO) throws StripeException;
}
