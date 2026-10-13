package com.japstransfort.service;

import com.japstransfort.dto.TierFormDto;
import com.japstransfort.entity.ResellerTier;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ResellerTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service encapsulating business logic for ResellerTier operations.
 */
@Service
public class TierService {

    private final ResellerTierRepository tierRepository;
    private final ResellerRepository resellerRepository;

    public TierService(ResellerTierRepository tierRepository, ResellerRepository resellerRepository) {
        this.tierRepository = tierRepository;
        this.resellerRepository = resellerRepository;
    }

    @Transactional(readOnly = true)
    public List<ResellerTier> findAll() {
        return tierRepository.findAllByOrderByDiscountPctAsc();
    }

    @Transactional(readOnly = true)
    public ResellerTier findById(Integer tierId) {
        return tierRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Tier not found with ID: " + tierId));
    }

    @Transactional
    public ResellerTier create(TierFormDto dto) {
        String trimmedName = dto.getTierName().trim();

        if (tierRepository.existsByTierName(trimmedName)) {
            throw new IllegalArgumentException("A tier with the name '" + trimmedName + "' already exists.");
        }

        ResellerTier tier = new ResellerTier();
        tier.setTierName(trimmedName);
        tier.setDiscountPct(dto.getDiscountPct());
        tier.setCommissionPct(dto.getCommissionPct());
        tier.setMinMonthlyPurchase(dto.getMinMonthlyPurchase());
        return tierRepository.save(tier);
    }

    @Transactional
    public ResellerTier update(Integer tierId, TierFormDto dto) {
        ResellerTier tier = findById(tierId);
        String trimmedName = dto.getTierName().trim();

        tierRepository.findByTierName(trimmedName).ifPresent(existing -> {
            if (!existing.getTierId().equals(tierId)) {
                throw new IllegalArgumentException("Another tier with the name '" + trimmedName + "' already exists.");
            }
        });

        tier.setTierName(trimmedName);
        tier.setDiscountPct(dto.getDiscountPct());
        tier.setCommissionPct(dto.getCommissionPct());
        tier.setMinMonthlyPurchase(dto.getMinMonthlyPurchase());
        return tierRepository.save(tier);
    }

    @Transactional
    public void delete(Integer tierId) {
        ResellerTier tier = findById(tierId);

        long resellerCount = resellerRepository.countByTier_TierId(tierId);
        if (resellerCount > 0) {
            throw new IllegalStateException("Cannot delete tier '" + tier.getTierName() +
                    "' because " + resellerCount + " reseller(s) are currently assigned to it.");
        }

        tierRepository.delete(tier);
    }
}
