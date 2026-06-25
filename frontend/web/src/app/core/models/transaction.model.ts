export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED';
export type TransactionType = 'TRANSFER' | 'DEPOSIT' | 'WITHDRAWAL';

export interface TransferRequest {
  sourceAccount: string;
  targetAccount: string;
  amount: number;
  currency: string;
  description?: string;
}

export interface TransactionResponse {
  id: string;
  sourceIban: string;
  targetIban: string;
  amount: number;
  currency: string;
  description?: string;
  status: TransactionStatus;
  type: TransactionType;
  createdAt: string;
  completedAt?: string;
}

export interface PagedTransactionsResponse {
  transactions: TransactionResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
