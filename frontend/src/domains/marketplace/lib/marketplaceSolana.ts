import {
    ASSOCIATED_TOKEN_PROGRAM_ID,
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
    const latest = await connection.getLatestBlockhash("finalized");
    transaction.feePayer = publicKey;
    transaction.recentBlockhash = latest.blockhash;

    const signature = await sendTransaction(transaction, connection);
    const confirmation = await connection.confirmTransaction(
        {
            signature,
            blockhash: latest.blockhash,
            lastValidBlockHeight: latest.lastValidBlockHeight,
        },
        "finalized",
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

export async function buyNftOnChain(params: {
    connection: Connection;
    publicKey: PublicKey;
    sendTransaction: (transaction: Transaction, connection: Connection) => Promise<string>;
    sellerWalletAddress: string;
    nftMintAddress: string;
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
