import {
  TransactionInstruction,
  PublicKey,
  TransactionMessage,
  VersionedTransaction,
  type Connection,
} from "@solana/web3.js";
import {
  TOKEN_PROGRAM_ID,
  createAssociatedTokenAccountIdempotentInstruction,
  createTransferCheckedInstruction,
  getAssociatedTokenAddressSync,
} from "@solana/spl-token";

const MEMO_PROGRAM_ID = new PublicKey("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr");

export interface SolanaPaymentRequirement {
  scheme: string;
  network: "solana-devnet" | "mainnet-beta";
  maxAmountRequired: string;
  resource: string;
  description: string;
  mimeType: string;
  payTo: string;
  maxTimeoutSeconds: number;
  asset: string;
  extra?: Record<string, unknown>;
}

export interface Solana402Body {
  x402Version: number;
  error: string;
  accepts: SolanaPaymentRequirement[];
}

export function isSolana402Body(value: unknown): value is Solana402Body {
  const body = value as Solana402Body;
  return Array.isArray(body?.accepts);
}

export async function sendSolanaX402Payment(params: {
  body: Solana402Body;
  connection: Connection;
  publicKey: PublicKey | null;
  signTransaction?: (<T extends VersionedTransaction>(transaction: T) => Promise<T>) | undefined;
}): Promise<string> {
  const requirement = params.body.accepts.find(
    (accept) => accept.network === "solana-devnet" || accept.network === "mainnet-beta"
  );

  if (!requirement) {
    throw new Error("결제 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
  }
  if (!params.publicKey) {
    throw new Error("결제를 진행하려면 Solana 지갑을 먼저 연결해주세요.");
  }
  if (!params.signTransaction) {
    throw new Error("현재 연결된 지갑에서는 결제 서명을 진행할 수 없습니다.");
  }

  const expectedNetwork = import.meta.env.VITE_SOLANA_NETWORK === "mainnet-beta"
    ? "mainnet-beta"
    : "solana-devnet";
  if (requirement.network !== expectedNetwork) {
    throw new Error("지갑 네트워크 설정을 확인해주세요.");
  }

  const expectedMint =
    import.meta.env.VITE_SOLANA_USDC_MINT?.trim() ??
    "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU";
  if (requirement.asset !== expectedMint) {
    throw new Error("결제 토큰 정보를 확인하지 못했습니다. 관리자에게 문의해주세요.");
  }

  const expectedRecipient = import.meta.env.VITE_SOLANA_PLATFORM_WALLET?.trim();
  if (expectedRecipient && requirement.payTo !== expectedRecipient) {
    throw new Error("결제 수신 정보를 확인하지 못했습니다. 관리자에게 문의해주세요.");
  }

  const amount = BigInt(requirement.maxAmountRequired);
  if (amount <= 0n) {
    throw new Error("결제 금액을 확인하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }

  const usdcMint = new PublicKey(requirement.asset);
  const recipient = new PublicKey(requirement.payTo);
  const senderAta = getAssociatedTokenAddressSync(usdcMint, params.publicKey);
  const recipientAta = getAssociatedTokenAddressSync(usdcMint, recipient);

  const paymentMemo =
    typeof requirement.extra?.paymentMemo === "string" ? requirement.extra.paymentMemo : "";
  if (!paymentMemo) {
    throw new Error("결제 요청을 확인하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }

  const { blockhash } = await params.connection.getLatestBlockhash("confirmed");
  const message = new TransactionMessage({
    payerKey: params.publicKey,
    recentBlockhash: blockhash,
    instructions: [
      new TransactionInstruction({
        programId: MEMO_PROGRAM_ID,
        keys: [],
        data: new TextEncoder().encode(paymentMemo),
      }),
      createAssociatedTokenAccountIdempotentInstruction(
        params.publicKey,
        recipientAta,
        recipient,
        usdcMint
      ),
      createTransferCheckedInstruction(
        senderAta,
        usdcMint,
        recipientAta,
        params.publicKey,
        amount,
        6,
        [],
        TOKEN_PROGRAM_ID
      ),
    ],
  }).compileToV0Message();

  const transaction = new VersionedTransaction(message);
  let signed: VersionedTransaction;
  try {
    signed = await params.signTransaction(transaction);
  } catch {
    throw new Error("지갑에서 결제 서명이 취소되었습니다.");
  }

  let signature: string;
  try {
    signature = await params.connection.sendRawTransaction(signed.serialize(), {
      skipPreflight: false,
      preflightCommitment: "confirmed",
      maxRetries: 3,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message.toLowerCase() : "";
    if (message.includes("insufficient") || message.includes("custom program error: 0x1")) {
      throw new Error("USDC 또는 SOL 잔액이 부족합니다. 지갑에서 잔액을 확인해주세요.");
    }
    throw new Error("결제 트랜잭션을 전송하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }

  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline) {
    const { value } = await params.connection.getSignatureStatuses([signature]);
    const status = value[0];
    if (status?.err) {
      throw new Error("결제 트랜잭션이 실패했습니다. 지갑 내역을 확인한 뒤 다시 시도해주세요.");
    }
    if (status?.confirmationStatus === "confirmed" || status?.confirmationStatus === "finalized") {
      return encodePaymentHeader(signature, params.publicKey.toBase58(), requirement.network);
    }
    await new Promise((resolve) => setTimeout(resolve, 1500));
  }

  throw new Error("결제 확인이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
}

function encodePaymentHeader(signature: string, buyerAddress: string, network: string) {
  const payload = JSON.stringify({ signature, buyerAddress, network });
  const bytes = new TextEncoder().encode(payload);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary);
}
