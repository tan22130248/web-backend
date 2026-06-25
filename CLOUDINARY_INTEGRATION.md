# 🏪 Backend - Seller Registration with Cloudinary

## Architecture

### Database Schema (Already Created)

**seller_registrations table**
```sql
CREATE TABLE seller_registrations (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(255),
  address TEXT,
  cccd_front_url VARCHAR(500),  -- Cloudinary URL
  cccd_back_url VARCHAR(500),   -- Cloudinary URL
  status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
  rejection_reason TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_id (user_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);
```

### Entities

**SellerRegistration.java** ✅
- Maps to `seller_registrations` table
- Stores CCCD URLs from Cloudinary
- Tracks approval status
- Includes rejection reason

**SellerRegistrationRepository.java** ✅
- `findByUserId(String userId)` - Find registration for user
- `existsByUserId(String userId)` - Check if user has registration

### Services

**SellerService.java** ✅
- `registerAsSeller(userEmail, request)` - Create pending registration
  - Takes `cccdFrontUrl` and `cccdBackUrl` from request (already Cloudinary URLs from frontend)
  - Creates SellerRegistration with status = pending
  - Updates user info but keeps role = buyer
  
- `getSellerRegistrationStatus(userEmail)` - Check current status
  - Returns enum: "seller", "pending", or "none"
  - Admin can see this in management panel

- `isUserSeller(userEmail)` - Quick seller check

### Controllers

**SellerController.java** ✅
- `POST /api/seller/register` - Create registration
  - Receives CCCD URLs directly (uploaded by frontend)
  - Validates and saves registration
  
- `GET /api/seller/check-seller-status` - Check status
  - Checks both Shop (user.role) and SellerRegistration

## Request/Response Flow

### Frontend → Backend
```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0123456789",
  "email": "user@example.com",
  "address": "123 Đường ABC",
  "cccdFrontUrl": "https://res.cloudinary.com/djcngebxr/image/upload/v123/fashion_marketplace/cccd/abc123_front.jpg",
  "cccdBackUrl": "https://res.cloudinary.com/djcngebxr/image/upload/v123/fashion_marketplace/cccd/abc123_back.jpg"
}
```

### Backend Processing
1. Extract user email from Bearer token
2. Fetch user from database
3. Update user.phone, user.address (keep role = buyer)
4. Create SellerRegistration record:
   - status = pending
   - cccdFrontUrl, cccdBackUrl = URLs from request
   - Set timestamps
5. Return success response

### Database Storage
```
seller_registrations:
  id: 550e8400-e29b-41d4-a716-446655440000
  user_id: 123e4567-e89b-12d3-a456-426614174000
  full_name: "Nguyễn Văn A"
  phone: "0123456789"
  email: "user@example.com"
  address: "123 Đường ABC"
  cccd_front_url: "https://res.cloudinary.com/djcngebxr/image/upload/..."
  cccd_back_url: "https://res.cloudinary.com/djcngebxr/image/upload/..."
  status: "pending"
  created_at: 2026-06-09 10:30:00
```

## URL Validation (Optional)

If you want to validate Cloudinary URLs before saving:

```java
// In SellerService.registerAsSeller()
private void validateCloudinaryUrl(String url) {
    if (url == null || !url.startsWith("https://res.cloudinary.com/")) {
        throw new IllegalArgumentException("Invalid Cloudinary URL: " + url);
    }
}
```

## Next Steps: Admin Approval Flow (TODO)

### New Endpoints Needed
1. **GET /api/admin/seller-registrations**
   - List pending registrations with pagination
   - Include user info + CCCD image URLs
   - Admin can view images directly from Cloudinary

2. **PATCH /api/admin/seller-registrations/{id}/approve**
   - Create Shop record
   - Set Shop.cccdFrontUrl, cccdBackUrl from registration
   - Upgrade user.role = 'seller'
   - Update registration.status = 'approved'
   - Delete registration or mark as archived

3. **PATCH /api/admin/seller-registrations/{id}/reject**
   - Set registration.status = 'rejected'
   - Store rejection_reason
   - Send notification to user
   - User can see "Bị từ chối" status

### Image Display in Admin Panel (TODO)
```jsx
// In admin component
<img 
  src={registration.cccdFrontUrl} 
  alt="CCCD Front"
  className="w-48 h-auto rounded border"
/>
```

Cloudinary will serve images directly with optimizations (lazy load, responsive sizing, etc.)

## Current Status

✅ Frontend uploads CCCD to Cloudinary
✅ Cloudinary returns secure URLs  
✅ Frontend sends URLs to backend
✅ Backend saves URLs in database
⏳ Admin endpoints to approve/reject (next)
⏳ Shop creation on approval (next)
⏳ Role upgrade to seller (next)

## Testing Commands

```bash
# Build backend
cd web-backend
mvn clean package

# Check migrations ran
# SELECT * FROM seller_registrations;

# Test registration endpoint
curl -X POST http://localhost:8080/api/seller/register \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "phone": "0123456789",
    "email": "test@example.com",
    "address": "123 Test St",
    "cccdFrontUrl": "https://res.cloudinary.com/djcngebxr/image/upload/v123/test_front.jpg",
    "cccdBackUrl": "https://res.cloudinary.com/djcngebxr/image/upload/v123/test_back.jpg"
  }'

# Check status
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/seller/check-seller-status
```
