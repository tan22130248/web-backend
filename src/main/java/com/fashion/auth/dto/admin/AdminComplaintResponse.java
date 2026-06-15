package com.fashion.auth.dto.admin;

import com.fashion.auth.model.Review;

import java.time.LocalDateTime;

/**
 * Complaint view for the admin "Quản lý khiếu nại" screen.
 *
 * The backend has no dedicated ticket/complaint entity, so complaints are
 * mapped onto low-rating product reviews (rating &lt;= 3). This is a read-only
 * projection — priority is derived from the rating and the ticket code from
 * the review id. No schema change is involved.
 *
 *  - rating 1 → priority "high"
 *  - rating 2 → priority "medium"
 *  - rating 3 → priority "low"
 */
public class AdminComplaintResponse {

    private final String id;
    private final String ticketCode;
    private final String subject;
    private final String content;
    private final int rating;
    private final String priority;
    private final String status;

    private final String reporterId;
    private final String reporterName;
    private final String reporterEmail;
    private final String reporterAvatarUrl;

    private final String productId;
    private final String productName;

    private final String shopId;
    private final String shopName;

    private final LocalDateTime createdAt;

    public AdminComplaintResponse(Review r) {
        this.id = r.getId();
        this.ticketCode = r.getId() != null ? "REP-" + r.getId().substring(0, 6).toUpperCase() : null;
        this.rating = r.getRating();
        this.content = r.getComment();
        this.priority = derivePriority(r.getRating());
        // No persisted status on reviews — treat every complaint as pending.
        this.status = "pending";
        this.createdAt = r.getCreatedAt();

        if (r.getProduct() != null) {
            this.productId = r.getProduct().getId();
            this.productName = r.getProduct().getName();
            this.subject = "Khiếu nại về: " + r.getProduct().getName();
        } else {
            this.productId = null;
            this.productName = null;
            this.subject = "Khiếu nại sản phẩm";
        }

        // The complaint author: prefer buyer, fall back to user.
        var reporter = r.getBuyer() != null ? r.getBuyer() : r.getUser();
        if (reporter != null) {
            this.reporterId = reporter.getId();
            this.reporterName = reporter.getFullName();
            this.reporterEmail = reporter.getEmail();
            this.reporterAvatarUrl = reporter.getAvatarUrl();
        } else {
            this.reporterId = null;
            this.reporterName = null;
            this.reporterEmail = null;
            this.reporterAvatarUrl = null;
        }

        if (r.getShop() != null) {
            this.shopId = r.getShop().getId();
            this.shopName = r.getShop().getShopName();
        } else {
            this.shopId = null;
            this.shopName = null;
        }
    }

    private static String derivePriority(int rating) {
        if (rating <= 1) return "high";
        if (rating == 2) return "medium";
        return "low";
    }

    public String getId() { return id; }
    public String getTicketCode() { return ticketCode; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public int getRating() { return rating; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public String getReporterEmail() { return reporterEmail; }
    public String getReporterAvatarUrl() { return reporterAvatarUrl; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getShopId() { return shopId; }
    public String getShopName() { return shopName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
