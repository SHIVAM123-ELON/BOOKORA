package com.example.core.security

/**
 * Enterprise Role-Based Access Control (RBAC) Permission Matrix for Bookora.
 * Covers READER, AUTHOR, PUBLISHER, MODERATOR, ADMIN, and SUPER_ADMIN.
 * Enforces least-privilege access and fail-secure authorization rules.
 */
enum class UserRole(val hierarchyLevel: Int) {
    READER(1),
    AUTHOR(2),
    PUBLISHER(3),
    MODERATOR(4),
    ADMIN(5),
    SUPER_ADMIN(6)
}

enum class Permission(val code: String, val description: String) {
    // Reader Permissions
    BOOK_READ("book:read", "Read and browse public books"),
    LIBRARY_READ("library:read", "Access owned library items"),
    REVIEW_CREATE("review:create", "Write ratings and reviews"),
    CART_MANAGE("cart:manage", "Add, edit, and checkout cart items"),
    ORDER_CREATE("order:create", "Create orders and pay"),
    REFUND_REQUEST("refund:request", "Request order refund"),

    // Author Permissions
    BOOK_CREATE("book:create", "Draft and upload new manuscripts"),
    BOOK_UPDATE("book:update", "Edit owned books and metadata"),
    SALES_READ("sales:read", "View royalty ledger and sales stats"),
    PAYOUT_REQUEST("payout:request", "Request earnings payout to bank"),
    STUDIO_ACCESS("studio:access", "Access Author Studio workspace"),

    // Publisher Permissions
    BULK_PUBLISH("bulk:publish", "Manage multiple catalog authors"),
    CATALOG_ANALYTICS("catalog:analytics", "View aggregate imprint metrics"),

    // Moderator Permissions
    REVIEW_MODERATE("review:moderate", "Approve, flag, or remove reviews"),
    CONTENT_REPORT_RESOLVE("report:resolve", "Handle reader content flags"),

    // Admin Permissions
    BOOK_APPROVE("book:approve", "Approve submitted book publications"),
    BOOK_REJECT("book:reject", "Reject book publication with reason"),
    USER_SUSPEND("user:suspend", "Suspend violating user accounts"),
    REFUND_APPROVE("refund:approve", "Approve or decline customer refunds"),
    PAYOUT_APPROVE("payout:approve", "Authorize financial payout transfers"),
    COUPON_MANAGE("coupon:manage", "Create and modify promo coupons"),
    SUBSCRIPTION_MANAGE("subscription:manage", "Configure membership tiers"),
    AUDIT_LOG_READ("audit:read", "Inspect admin compliance logs"),

    // Super Admin Permissions
    COMMISSION_CHANGE("commission:change", "Adjust platform fee percentage"),
    SYSTEM_CONFIGURE("system:configure", "Update system settings & API keys"),
    FEATURE_FLAG_MANAGE("featureflag:manage", "Toggle runtime feature flags"),
    ADMIN_USER_MANAGE("admin:manage", "Grant or revoke admin roles")
}

object RbacController {

    private val rolePermissions: Map<UserRole, Set<Permission>> = mapOf(
        UserRole.READER to setOf(
            Permission.BOOK_READ,
            Permission.LIBRARY_READ,
            Permission.REVIEW_CREATE,
            Permission.CART_MANAGE,
            Permission.ORDER_CREATE,
            Permission.REFUND_REQUEST
        ),
        UserRole.AUTHOR to setOf(
            Permission.BOOK_READ,
            Permission.LIBRARY_READ,
            Permission.REVIEW_CREATE,
            Permission.CART_MANAGE,
            Permission.ORDER_CREATE,
            Permission.REFUND_REQUEST,
            Permission.BOOK_CREATE,
            Permission.BOOK_UPDATE,
            Permission.SALES_READ,
            Permission.PAYOUT_REQUEST,
            Permission.STUDIO_ACCESS
        ),
        UserRole.PUBLISHER to setOf(
            Permission.BOOK_READ,
            Permission.LIBRARY_READ,
            Permission.REVIEW_CREATE,
            Permission.CART_MANAGE,
            Permission.ORDER_CREATE,
            Permission.REFUND_REQUEST,
            Permission.BOOK_CREATE,
            Permission.BOOK_UPDATE,
            Permission.SALES_READ,
            Permission.PAYOUT_REQUEST,
            Permission.STUDIO_ACCESS,
            Permission.BULK_PUBLISH,
            Permission.CATALOG_ANALYTICS
        ),
        UserRole.MODERATOR to setOf(
            Permission.BOOK_READ,
            Permission.LIBRARY_READ,
            Permission.REVIEW_CREATE,
            Permission.REVIEW_MODERATE,
            Permission.CONTENT_REPORT_RESOLVE
        ),
        UserRole.ADMIN to setOf(
            Permission.BOOK_READ,
            Permission.LIBRARY_READ,
            Permission.REVIEW_CREATE,
            Permission.REVIEW_MODERATE,
            Permission.CONTENT_REPORT_RESOLVE,
            Permission.BOOK_APPROVE,
            Permission.BOOK_REJECT,
            Permission.USER_SUSPEND,
            Permission.REFUND_APPROVE,
            Permission.PAYOUT_APPROVE,
            Permission.COUPON_MANAGE,
            Permission.SUBSCRIPTION_MANAGE,
            Permission.AUDIT_LOG_READ
        ),
        UserRole.SUPER_ADMIN to Permission.values().toSet()
    )

    /**
     * Verifies if a user role has the specific required permission.
     * Enforces fail-secure policy (returns false if role or permission is unknown).
     */
    fun hasPermission(role: UserRole?, permission: Permission): Boolean {
        if (role == null) return false
        val permissions = rolePermissions[role] ?: return false
        return permissions.contains(permission)
    }

    /**
     * Validates access and throws SecurityException if unauthorized.
     */
    fun requirePermission(role: UserRole?, permission: Permission) {
        if (!hasPermission(role, permission)) {
            throw SecurityException("Access Denied: Role '${role?.name}' lacks required permission '${permission.code}'")
        }
    }

    fun getAllowedPermissions(role: UserRole): Set<Permission> {
        return rolePermissions[role] ?: emptySet()
    }
}
