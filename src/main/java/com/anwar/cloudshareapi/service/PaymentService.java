package com.anwar.cloudshareapi.service;


import com.anwar.cloudshareapi.document.PaymentTransaction;
import com.anwar.cloudshareapi.document.ProfileDocument;
import com.anwar.cloudshareapi.dto.PaymentDTO;
import com.anwar.cloudshareapi.dto.PaymentVerificationDTO;
import com.anwar.cloudshareapi.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;



    public PaymentDTO createOrder(PaymentDTO paymentDTO) {
        try {
            System.out.println("🔑 Razorpay Key ID = " + razorpayKeyId);
            System.out.println("🔑 Razorpay Secret = " + razorpayKeySecret);
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", paymentDTO.getAmount());
            orderRequest.put("currency", paymentDTO.getCurrency());
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");
            //create pending transaction record
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .orderId(orderId)
                    .planId(paymentDTO.getPlanId())
                    .amount(paymentDTO.getAmount())
                    .currency(paymentDTO.getCurrency())
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentProfile.getEmail())
                    .userName(currentProfile.getFirstName() + " " + currentProfile.getLastName())
                    .build();

            paymentTransactionRepository.save(transaction);

            return PaymentDTO.builder()
                    .orderId(orderId)
                    .success(true)
                    .message("Order created successfully")
                    .build();

        } catch (Exception e) {
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error creating order: " + e.getMessage())
                    .build();
        }
    }

    public PaymentDTO verifyPayment(PaymentVerificationDTO request){
        try{
            ProfileDocument currentProfile=profileService.getCurrentProfile();
            String clerkId=currentProfile.getClerkId();

            String data = request.getRazorpay_order_id() + "|" + request.getRazorpay_payment_id();
            String generatedSignature = generateHmaSha256Signature(data, razorpayKeySecret);

            if (!generatedSignature.equals(request.getRazorpay_signature())) {
                updateTransactionStatus(request.getRazorpay_order_id(), "FAILED", request.getRazorpay_payment_id(), null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("Payment signature verification failed")
                        .build();
            }

            //Add credits based on plan
            int creditsToAdd=0;
            String plan = "BASIC";

            switch (request.getPlanId()){
                case "premium":
                    creditsToAdd=500;
                    plan="PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd=5000;
                    plan="ULTIMATE";
                    break;
            }
            if(creditsToAdd>0) {
                userCreditsService.addCredits(clerkId, creditsToAdd, plan);
                updateTransactionStatus(request.getRazorpay_order_id(), "SUCCESS", request.getRazorpay_payment_id(), creditsToAdd);
                return PaymentDTO.builder()
                        .success(true)
                        .message("Payment verified and credits added successfully")
                        .credits(userCreditsService.getUserCredits(clerkId).getCredits())
                        .build();
            }else{
                updateTransactionStatus(request.getRazorpay_order_id(),"FAILED",request.getRazorpay_payment_id(),null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("Invalid plan selected")
                        .build();
            }
        } catch(Exception e){
            try{
                updateTransactionStatus(request.getRazorpay_order_id(),"ERROR",request.getRazorpay_payment_id(),null);

            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error verifying payment: " +e.getMessage())
                    .build();
        }
    }

    private String generateHmaSha256Signature(String message, String secret) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    private void updateTransactionStatus(String razorpayOrderId, String status, String razorpayPaymentId, Integer creditsToAdd) {
        paymentTransactionRepository.findAll().stream()
                .filter(t->t.getOrderId() !=null && t.getOrderId().equals(razorpayOrderId))
                .findFirst()
                .map(transaction->{
                    transaction.setStatus(status);
                    transaction.setPaymentId(razorpayPaymentId);
                    if(creditsToAdd != null){
                        transaction.setCreditsAdded(creditsToAdd);
                    }
                    return paymentTransactionRepository.save(transaction);
                })
                .orElse(null);
    }
}
