package com.example.alert_detect_system.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.alert_detect_system.Model.CaseModel;
import com.example.alert_detect_system.Model.CaseStatus;

import jakarta.persistence.Id;
@Repository
public interface CaseRepository extends JpaRepository<CaseModel, UUID> {
    Optional<CaseModel> findByCaseNumber(String caseNumber);
    
    List<CaseModel> findByStatus(CaseStatus status);
    
    List<CaseModel> findByCreatedBy(String createdBy);
    
    @Query("SELECT c FROM CaseModel c WHERE c.alertId = :alertId")
    List<CaseModel> findByAlertId(String alertId);
    
    @Query("SELECT COUNT(c) FROM CaseModel c WHERE c.status = :status")
    long countByStatus(CaseStatus status); 

}
