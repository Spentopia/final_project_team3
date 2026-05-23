import {
    ASSOCIATED_TOKEN_PROGRAM_ID,
    ACCOUNT_SIZE,
    TOKEN_PROGRAM_ID,
    getAssociatedTokenAddressSync,
} from "@solana/spl-token";
import {Buffer} from "buffer";
import {
    Connection,
    PublicKey,
    SystemProgram,
    Transaction,
    TransactionInstruction,
} from "@solana/web3.js";

const PROGRAM_ID = new PublicKey(
    import.meta.env.VITE_SPENTOPIA_PROGRAM_ID?.trim() ||
    "9s5Z96GSLVgVsnj5NAZ1HoxPvaF8Re8B1LeSmcBKQv61",
);

const SPT_DECIMALS = 1_000_000n;
const LIST_NFT_DISCRIMINATOR = [88, 221, 93, 166, 63, 220, 106, 232];
const BUY_NFT_DISCRIMINATOR = [96, 0, 28, 190, 49, 107, 83, 222];
const CANCEL_LISTING_DISCRIMINATOR = [41, 183, 50, 232, 230, 233, 157, 70];

function pda(seed: Buffer[], programId = PROGRAM_ID): PublicKey {
    return PublicKey.findProgramAddressSync(seed, programId)[0];
}

function u64Le(value: bigint): Buffer {
    if (value < 0n || value > 18_446_744_073_709_551_615n) {
        throw new Error("u64 범위를 벗어난 값입니다.");
    }

    const out = Buffer.alloc(8);
    out.writeBigUInt64LE(value);
    return out;
}

function priceToBaseUnits(priceSpt: number): bigint {
    if (!Number.isSafeInteger(priceSpt) || priceSpt <= 0) {
        throw new Error("판매가는 0보다 큰 정수여야 합니다.");
    }
    return BigInt(priceSpt) * SPT_DECIMALS;
}

async function assertBuyerCanPaySpt(
    connection: Connection,
    buyerSptAccount: PublicKey,
    requiredBaseUnits: bigint,
): Promise<void> {
    const accountInfo = await connection.getAccountInfo(buyerSptAccount);
    if (!accountInfo) {
        throw new Error(
            "SPT가 부족합니다. SOL로 SPT를 구매하거나 보상으로 SPT 토큰을 획득한 뒤 다시 시도해 주세요.",
        );
    }

    const balance = await connection.getTokenAccountBalance(buyerSptAccount);
    const currentBaseUnits = BigInt(balance.value.amount);
    if (currentBaseUnits < requiredBaseUnits) {
        throw new Error(
            "SPT가 부족합니다. SOL로 SPT를 구매하거나 보상으로 SPT 토큰을 획득한 뒤 다시 시도해 주세요.",
        );
    }
}

async function assertBuyerCanPayAccountRent(params: {
    connection: Connection;
    buyer: PublicKey;
    sellerSptAccount: PublicKey;
    buyerNftAccount: PublicKey;
}): Promise<void> {
    const [sellerSptAccountInfo, buyerNftAccountInfo, buyerSolBalance, tokenAccountRent] =
        await Promise.all([
            params.connection.getAccountInfo(params.sellerSptAccount),
            params.connection.getAccountInfo(params.buyerNftAccount),
            params.connection.getBalance(params.buyer),
            params.connection.getMinimumBalanceForRentExemption(ACCOUNT_SIZE),
        ]);

    const missingTokenAccounts =
        (sellerSptAccountInfo ? 0 : 1) + (buyerNftAccountInfo ? 0 : 1);
    const feeReserveLamports = 20_000;
    const requiredLamports = missingTokenAccounts * tokenAccountRent + feeReserveLamports;

    if (buyerSolBalance < requiredLamports) {
        throw new Error(
            "가스비가 부족합니다. 소량의 SOL을 보유해야 구매가 가능합니다.",
        );
    }
}

function summarizeSimulationLogs(logs: string[] | null | undefined): string {
    if (!logs?.length) return "시뮬레이션 로그 없음";
    return logs.slice(-8).join(" / ");
}

async function assertTransactionSimulationSucceeds(
    connection: Connection,
    transaction: Transaction,
): Promise<void> {
    const simulation = await connection.simulateTransaction(transaction);

    if (simulation.value.err) {
        console.error("[marketplaceSolana] transaction simulation failed", simulation.value);
        throw new Error(`온체인 시뮬레이션 실패: ${summarizeSimulationLogs(simulation.value.logs)}`);
    }
}

function listingPda(seller: PublicKey, nftMint: PublicKey): PublicKey {
    return pda([Buffer.from("listing"), seller.toBuffer(), nftMint.toBuffer()]);
}

export function deriveEscrowAddress(sellerAddress: string, nftMintAddress: string): string {
    const seller = new PublicKey(sellerAddress);
    const nftMint = new PublicKey(nftMintAddress);
    const listing = listingPda(seller, nftMint);
    return pda([Buffer.from("escrow"), listing.toBuffer()]).toBase58();
}

async function sendAndFinalize(
    connection: Connection,
    publicKey: PublicKey,
    sendTransaction: (transaction: Transaction, connection: Connection) => Promise<string>,
    transaction: Transaction,
): Promise<string> {
    const latest = await connection.getLatestBlockhash("confirmed");
    transaction.feePayer = publicKey;
    transaction.recentBlockhash = latest.blockhash;

    await assertTransactionSimulationSucceeds(connection, transaction);

    const signature = await sendTransaction(transaction, connection);
    const confirmation = await connection.confirmTransaction(
        {
            signature,
            blockhash: latest.blockhash,
            lastValidBlockHeight: latest.lastValidBlockHeight,
        },
        "confirmed",
    );

    if (confirmation.value.err) {
        throw new Error(`온체인 트랜잭션 실패: ${JSON.stringify(confirmation.value.err)}`);
    }

    return signature;
}

export async function listNftOnChain(params: {
    connection: Connection;
    publicKey: PublicKey;
    sendTransaction: (transaction: Transaction, connection: Connection) => Promise<string>;
    nftMintAddress: string;
    priceSpt: number;
}): Promise<{ signature: string; escrowAddress: string }> {
    const nftMint = new PublicKey(params.nftMintAddress);
    const listing = listingPda(params.publicKey, nftMint);
    const escrow = pda([Buffer.from("escrow"), listing.toBuffer()]);
    const sellerTokenAccount = getAssociatedTokenAddressSync(nftMint, params.publicKey);
    const priceBaseUnits = priceToBaseUnits(params.priceSpt);

    const ix = new TransactionInstruction({
        programId: PROGRAM_ID,
        keys: [
            {pubkey: params.publicKey, isSigner: true, isWritable: true},
            {pubkey: nftMint, isSigner: false, isWritable: false},
            {pubkey: sellerTokenAccount, isSigner: false, isWritable: true},
            {pubkey: listing, isSigner: false, isWritable: true},
            {pubkey: escrow, isSigner: false, isWritable: true},
            {pubkey: TOKEN_PROGRAM_ID, isSigner: false, isWritable: false},
            {pubkey: ASSOCIATED_TOKEN_PROGRAM_ID, isSigner: false, isWritable: false},
            {pubkey: SystemProgram.programId, isSigner: false, isWritable: false},
        ],
        data: Buffer.concat([
            Buffer.from(LIST_NFT_DISCRIMINATOR),
            u64Le(priceBaseUnits),
        ]),
    });

    const signature = await sendAndFinalize(
        params.connection,
        params.publicKey,
        params.sendTransaction,
        new Transaction().add(ix),
    );

    return {
        signature,
        escrowAddress: escrow.toBase58(),
    };
}

export async function cancelListingOnChain(params: {
    connection: Connection;
    publicKey: PublicKey;
    sendTransaction: (transaction: Transaction, connection: Connection) => Promise<string>;
    nftMintAddress: string;
}): Promise<string> {
    const seller = params.publicKey;

    const nftMint = new PublicKey(params.nftMintAddress);
    const listing = listingPda(seller, nftMint);
    const escrow = pda([Buffer.from("escrow"), listing.toBuffer()]);
    const sellerNftAccount = getAssociatedTokenAddressSync(nftMint, seller);

    const latest = await params.connection.getLatestBlockhash("confirmed");

    const ix = new TransactionInstruction({
        programId: PROGRAM_ID,
        keys: [
            {pubkey: seller, isSigner: true, isWritable: true},                // seller
            {pubkey: listing, isSigner: false, isWritable: true},              // listing PDA
            {pubkey: nftMint, isSigner: false, isWritable: false},             // nft_mint
            {pubkey: escrow, isSigner: false, isWritable: true},               // escrow_token_account
            {pubkey: sellerNftAccount, isSigner: false, isWritable: true},     // seller_nft_account
            {pubkey: TOKEN_PROGRAM_ID, isSigner: false, isWritable: false},    // token_program
            {pubkey: SystemProgram.programId, isSigner: false, isWritable: false}, // system_program
        ],
        data: Buffer.from(CANCEL_LISTING_DISCRIMINATOR),
    });

    const transaction = new Transaction();
    transaction.add(ix);
    transaction.feePayer = seller;
    transaction.recentBlockhash = latest.blockhash;

    // listing PDA 존재 여부 확인
    const listingInfo = await params.connection.getAccountInfo(listing);
    if (!listingInfo) {
        // escrow도 없으면 이미 온체인에서 취소/구매 완료된 케이스 → DB만 정리 필요
        const escrowInfo = await params.connection.getAccountInfo(escrow);
        if (!escrowInfo) {
            throw new Error("이 리스팅은 이미 온체인에서 처리(취소 또는 구매)됐습니다. 관리자에게 DB 정리를 요청하거나 고객센터에 문의해주세요.");
        }
        throw new Error(`리스팅 PDA가 온체인에 존재하지 않습니다. (${listing.toBase58()})`);
    }

    await assertTransactionSimulationSucceeds(params.connection, transaction);

    const signature = await params.sendTransaction(transaction, params.connection);

    const confirmation = await params.connection.confirmTransaction(
        { signature, blockhash: latest.blockhash, lastValidBlockHeight: latest.lastValidBlockHeight },
        "confirmed",
    );

    if (confirmation.value.err) {
        throw new Error(`온체인 트랜잭션 실패: ${JSON.stringify(confirmation.value.err)}`);
    }

    return signature;
}

export async function buyNftOnChain(params: {
    connection: Connection;
    publicKey: PublicKey;
    sendTransaction: (transaction: Transaction, connection: Connection) => Promise<string>;
    sellerWalletAddress: string;
    nftMintAddress: string;
    priceSpt: number;
}): Promise<string> {
    const seller = new PublicKey(params.sellerWalletAddress);
    const nftMint = new PublicKey(params.nftMintAddress);
    const platformConfig = pda([Buffer.from("platform_config")]);
    const listing = listingPda(seller, nftMint);
    const escrow = pda([Buffer.from("escrow"), listing.toBuffer()]);
    const sptTokenMint = pda([Buffer.from("spt_token_mint")]);
    const buyerSptAccount = getAssociatedTokenAddressSync(sptTokenMint, params.publicKey);
    const sellerSptAccount = getAssociatedTokenAddressSync(sptTokenMint, seller);
    const buyerNftAccount = getAssociatedTokenAddressSync(nftMint, params.publicKey);
    const priceBaseUnits = priceToBaseUnits(params.priceSpt);

    await assertBuyerCanPaySpt(params.connection, buyerSptAccount, priceBaseUnits);
    await assertBuyerCanPayAccountRent({
        connection: params.connection,
        buyer: params.publicKey,
        sellerSptAccount,
        buyerNftAccount,
    });

    const ix = new TransactionInstruction({
        programId: PROGRAM_ID,
        keys: [
            {pubkey: platformConfig, isSigner: false, isWritable: false},
            {pubkey: params.publicKey, isSigner: true, isWritable: true},
            {pubkey: seller, isSigner: false, isWritable: true},
            {pubkey: listing, isSigner: false, isWritable: true},
            {pubkey: nftMint, isSigner: false, isWritable: false},
            {pubkey: sptTokenMint, isSigner: false, isWritable: true},
            {pubkey: buyerSptAccount, isSigner: false, isWritable: true},
            {pubkey: sellerSptAccount, isSigner: false, isWritable: true},
            {pubkey: escrow, isSigner: false, isWritable: true},
            {pubkey: buyerNftAccount, isSigner: false, isWritable: true},
            {pubkey: TOKEN_PROGRAM_ID, isSigner: false, isWritable: false},
            {pubkey: ASSOCIATED_TOKEN_PROGRAM_ID, isSigner: false, isWritable: false},
            {pubkey: SystemProgram.programId, isSigner: false, isWritable: false},
        ],
        data: Buffer.from(BUY_NFT_DISCRIMINATOR),
    });

    return sendAndFinalize(
        params.connection,
        params.publicKey,
        params.sendTransaction,
        new Transaction().add(ix),
    );
}
