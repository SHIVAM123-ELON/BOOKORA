/**
 * BOOKORA SHARED TYPES & DOMAIN ENUMS
 * Cross-platform data contracts shared between Android, Backend, and Admin Web.
 */

export enum UserRole {
  READER = 'READER',
  AUTHOR = 'AUTHOR',
  PUBLISHER = 'PUBLISHER',
  MODERATOR = 'MODERATOR',
  ADMIN = 'ADMIN',
  SUPER_ADMIN = 'SUPER_ADMIN',
}

export enum Permission {
  BOOK_CREATE = 'book:create',
  BOOK_UPDATE = 'book:update',
  BOOK_PUBLISH = 'book:publish',
  BOOK_DELETE = 'book:delete',
  ORDER_VIEW = 'order:view',
  ORDER_CREATE = 'order:create',
  PAYMENT_PROCESS = 'payment:process',
  PAYMENT_VIEW = 'payment:view',
  PAYOUT_REQUEST = 'payout:request',
  PAYOUT_APPROVE = 'payout:approve',
  USER_SUSPEND = 'user:suspend',
  REPORT_MODERATE = 'report:moderate',
  COPYRIGHT_RESOLVE = 'copyright:resolve',
  ADMIN_ACCESS = 'admin:access',
}

export enum BookStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  APPROVED = 'APPROVED',
  PUBLISHED = 'PUBLISHED',
  REJECTED = 'REJECTED',
  ARCHIVED = 'ARCHIVED',
}

export enum OrderStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
}

export enum PaymentStatus {
  INITIATED = 'INITIATED',
  AUTHORIZED = 'AUTHORIZED',
  CAPTURED = 'CAPTURED',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
}

export enum ReadingStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
}

export enum CopyrightClaimStatus {
  OPEN = 'OPEN',
  UNDER_REVIEW = 'UNDER_REVIEW',
  RESOLVED = 'RESOLVED',
  REJECTED = 'REJECTED',
}

export interface UserDto {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  avatarUrl?: string;
  isVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AuthTokensDto {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface AuthResponseDto {
  user: UserDto;
  tokens: AuthTokensDto;
}

export interface CategoryDto {
  id: string;
  slug: string;
  name: string;
  description: string;
  iconName: string;
  bookCount: number;
}

export interface AuthorDto {
  id: string;
  userId: string;
  penName: string;
  bio: string;
  avatarUrl?: string;
  websiteUrl?: string;
  isVerified: boolean;
  totalBooks: number;
  rating: number;
}

export interface BookDto {
  id: string;
  title: string;
  subtitle?: string;
  slug: string;
  authorId: string;
  authorName: string;
  publisherId?: string;
  publisherName?: string;
  description: string;
  coverImageUrl: string;
  previewUrl?: string;
  contentUrl?: string;
  price: number;
  currency: string;
  discountPercentage?: number;
  finalPrice: number;
  pageCount: number;
  language: string;
  isbn?: string;
  status: BookStatus;
  categories: CategoryDto[];
  tags: string[];
  averageRating: number;
  totalReviews: number;
  publishedAt?: string;
  createdAt: string;
}

export interface LibraryItemDto {
  id: string;
  userId: string;
  book: BookDto;
  purchaseDate: string;
  orderId: string;
  readingProgress: number; // 0-100%
  lastReadPage: number;
  lastReadAt?: string;
  status: ReadingStatus;
  isDownloaded: boolean;
}

export interface ApiErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
    details?: any;
    timestamp: string;
    path: string;
  };
}

export interface ApiResponse<T> {
  success: true;
  data: T;
  meta?: {
    total?: number;
    page?: number;
    limit?: number;
    hasMore?: boolean;
  };
}
