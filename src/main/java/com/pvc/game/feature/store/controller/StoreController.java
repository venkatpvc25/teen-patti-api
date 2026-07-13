package com.pvc.game.feature.store.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.economy.dto.RewardResponse;
import com.pvc.game.feature.store.dto.CreateRazorpayOrderRequest;
import com.pvc.game.feature.store.dto.RazorpayOrderResponse;
import com.pvc.game.feature.store.dto.VerifyRazorpayPaymentRequest;
import com.pvc.game.feature.store.entity.Purchase;
import com.pvc.game.feature.store.entity.PurchaseStatus;
import com.pvc.game.feature.store.entity.ShopItem;
import com.pvc.game.feature.store.repository.PurchaseRepository;
import com.pvc.game.feature.store.repository.ShopItemRepository;
import com.pvc.game.feature.store.service.RazorpayService;
import com.pvc.game.feature.user.service.CurrentUserService;
import com.pvc.game.feature.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@Slf4j
public class StoreController {

    private final ShopItemRepository shopItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final CurrentUserService currentUserService;
    private final WalletService walletService;
    private final RazorpayService razorpayService;

    @GetMapping("/items")
    public ApiResponse<List<ShopItem>> items() {
        List<ShopItem> items = shopItemRepository.findByActiveTrueOrderByChipsAsc();
        log.info("Store items fetched count={}", items.size());
        return ApiResponse.ok(items);
    }

    @PostMapping("/razorpay/orders")
    public ApiResponse<RazorpayOrderResponse> createRazorpayOrder(@Valid @RequestBody CreateRazorpayOrderRequest request) {
        var user = currentUserService.requireCurrentUser();
        log.info("Razorpay order requested userId={} shopItemId={} platform={}",
                user.getId(), request.getShopItemId(), request.getPlatform());
        var item = shopItemRepository.findById(request.getShopItemId())
                .orElseThrow(() -> new IllegalArgumentException("Shop item not found"));

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setShopItem(item);
        purchase.setPlatform(request.getPlatform());
        purchase.setAmountPaise(item.getPriceAmountPaise());
        purchase.setCurrency(item.getCurrency());
        purchaseRepository.save(purchase);

        String orderId = razorpayService.createOrder(purchase);
        purchase.setProviderOrderId(orderId);
        purchaseRepository.save(purchase);
        log.info("Razorpay order created userId={} purchaseId={} razorpayOrderId={} amountPaise={} chips={}",
                user.getId(), purchase.getId(), orderId, purchase.getAmountPaise(), item.getChips());

        return ApiResponse.ok(new RazorpayOrderResponse(
                purchase.getId(),
                razorpayService.keyId(),
                orderId,
                purchase.getAmountPaise(),
                purchase.getCurrency(),
                item.getName(),
                item.getChips()));
    }

    @PostMapping("/razorpay/verify")
    public ApiResponse<RewardResponse> verifyRazorpayPayment(@Valid @RequestBody VerifyRazorpayPaymentRequest request) {
        var user = currentUserService.requireCurrentUser();
        log.info("Razorpay payment verification requested userId={} razorpayOrderId={} razorpayPaymentId={}",
                user.getId(), request.getRazorpayOrderId(), request.getRazorpayPaymentId());
        Purchase purchase = purchaseRepository.findByProviderOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));

        if (!purchase.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Purchase does not belong to current user");
        }

        if (purchase.getStatus() == PurchaseStatus.PAID) {
            var wallet = walletService.getWallet(user);
            log.info("Razorpay payment verification skipped alreadyPaid userId={} purchaseId={} balance={}",
                    user.getId(), purchase.getId(), wallet.getBalance());
            return ApiResponse.ok(new RewardResponse(0, wallet.getBalance(), "ALREADY_PAID"));
        }

        if (purchaseRepository.existsByProviderPaymentId(request.getRazorpayPaymentId())) {
            throw new IllegalStateException("Payment was already used");
        }

        boolean valid = razorpayService.isValidSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());
        if (!valid) {
            purchase.setStatus(PurchaseStatus.FAILED);
            purchaseRepository.save(purchase);
            log.warn("Razorpay payment verification failed invalidSignature userId={} purchaseId={} razorpayOrderId={} razorpayPaymentId={}",
                    user.getId(), purchase.getId(), request.getRazorpayOrderId(), request.getRazorpayPaymentId());
            throw new IllegalArgumentException("Invalid Razorpay signature");
        }

        purchase.setStatus(PurchaseStatus.PAID);
        purchase.setProviderPaymentId(request.getRazorpayPaymentId());
        purchase.setReceiptReference(request.getRazorpaySignature());
        purchase.setPaidAt(Instant.now());
        purchaseRepository.save(purchase);

        ShopItem item = purchase.getShopItem();
        var wallet = walletService.credit(user, item.getChips(), item.isVip() ? "VIP_PURCHASE" : "CHIP_PURCHASE", purchase.getId().toString());
        log.info("Razorpay payment verified userId={} purchaseId={} razorpayPaymentId={} chips={} balance={}",
                user.getId(), purchase.getId(), request.getRazorpayPaymentId(), item.getChips(), wallet.getBalance());
        return ApiResponse.ok(new RewardResponse(item.getChips(), wallet.getBalance(), item.isVip() ? "VIP_PURCHASE" : "CHIP_PURCHASE"));
    }
}
