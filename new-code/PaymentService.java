package com.marketplace.payment.service;

import com.marketplace.payment.client.PaymentGatewayClient;
import com.marketplace.payment.dto.PaymentRequestDTO;
import com.marketplace.payment.dto.PaymentResponseDTO;
import com.marketplace.payment.entity.PaymentEntity;
import com.marketplace.payment.entity.PaymentStatus;
import com.marketplace.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient gatewayClient;

    public PaymentService(PaymentRepository paymentRepository, PaymentGatewayClient gatewayClient) {
        this.paymentRepository = paymentRepository;
        this.gatewayClient = gatewayClient;
    }

    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        log.info("Initiating payment processing for orderId: {}", request.orderId());

        // 1. Persistência inicial via JPA (substitui JDBC cru e elimina risco de SQL Injection)
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(request.orderId());
        payment.setAmount(request.amount());
        // Mascara o número do cartão (mantém apenas os últimos 4 dígitos para conformidade PCI-DSS)
        payment.setCardLastFour(request.cardNumber().substring(request.cardNumber().length() - 4));
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // 2. Chamada resiliente ao gateway de pagamento
        boolean paymentApproved = gatewayClient.charge(request.cardNumber(), request.amount());

        // 3. Atualização transacional de estado conforme o resultado
        if (paymentApproved) {
            payment.setStatus(PaymentStatus.APPROVED);
            paymentRepository.save(payment);

            log.info("Payment successfully approved for orderId: {}", request.orderId());
            return new PaymentResponseDTO(request.orderId(), StatusResponseEnum.APPROVED, "Payment processed successfully");
        } else {
            payment.setStatus(PaymentStatus.REJECTED);
            paymentRepository.save(payment);

            log.warn("Payment rejected by gateway for orderId: {}", request.orderId());
            return new PaymentResponseDTO(request.orderId(), StatusResponseEnum.REJECTED, "Payment declined by provider");
        }
    }
}